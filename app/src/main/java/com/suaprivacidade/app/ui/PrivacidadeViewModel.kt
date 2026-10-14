package com.suaprivacidade.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.suaprivacidade.app.data.AppDatabase
import com.suaprivacidade.app.data.Apps
import com.suaprivacidade.app.data.Armazenamento
import com.suaprivacidade.app.data.DadosApp
import com.suaprivacidade.app.data.Estatisticas
import com.suaprivacidade.app.data.InfoApp
import com.suaprivacidade.app.data.MudancaPermissao
import com.suaprivacidade.app.data.Notificacao
import com.suaprivacidade.app.data.Preferencias
import com.suaprivacidade.app.data.PreferenciasRepo
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.Supervisionado
import com.suaprivacidade.app.data.Tema
import com.suaprivacidade.app.data.UsoApp
import com.suaprivacidade.app.data.UsoSensor
import com.suaprivacidade.app.data.emDataHora
import com.suaprivacidade.app.data.inicioMillis
import com.suaprivacidade.app.data.medirArmazenamento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

enum class Aba(val rotulo: String) {
    INICIO("Início"),
    SUPERVISAO("Supervisão"),
    HISTORICO("Histórico"),
    AUDITORIA("Auditoria"),
}

enum class TipoHistorico(val rotulo: String) { SENSORES("Microfone e câmera"), NOTIFICACOES("Notificações") }

/** Uma gaveta do Histórico de microfone e câmera: os usos de um dia. */
data class DiaUsos(val dia: LocalDate, val usos: List<UsoSensor>)

/** Uma gaveta do Histórico de notificações: as notificações de um dia. */
data class DiaNotificacoes(val dia: LocalDate, val notificacoes: List<Notificacao>)

data class ResumoHoje(
    val microfone: Int = 0,
    val camera: Int = 0,
    val suspeitos: Int = 0,
    val notificacoes: Int = 0,
    /** Sensores ligados neste momento. */
    val emUso: Set<Sensor> = emptySet(),
)

/** Cartão de um app supervisionado, com o que vem do banco. */
data class InfoSupervisionado(
    val supervisionado: Supervisionado,
    val nome: String,
    val suspeitosSemana: Int,
    val ultimoUso: UsoSensor?,
    /** Ganhou permissão sensível nos últimos 7 dias. */
    val permissaoNova: Boolean,
    val notificacoesHoje: Int,
)

/** Tempo de tela e internet do dia, lidos do Android (dependem do "Acesso ao uso"). */
data class ResumoApp(val uso: UsoApp = UsoApp(), val dados: DadosApp = DadosApp())

/** Um app no Bem-estar digital: quanto tempo ficou na tela e quantas vezes foi aberto. */
data class AppTela(val pacote: String, val nome: String, val uso: UsoApp)

/** Um dia do Bem-estar digital: tempo de tela total, desbloqueios e apps do mais usado ao menos usado. */
data class DiaTela(val dia: LocalDate, val totalMillis: Long, val desbloqueios: Int?, val apps: List<AppTela>) {
    val aberturas: Int get() = apps.sumOf { it.uso.aberturas }
}

/** Ficha completa de um app supervisionado. */
data class Ficha(
    val usoHoje: UsoApp,
    val usoSemana: UsoApp,
    val dadosHoje: DadosApp,
    val dadosSemana: DadosApp,
    /** Permissões sensíveis concedidas agora; null se o app foi desinstalado. */
    val permissoes: Set<String>?,
)

data class UiState(
    val agora: LocalDateTime = LocalDateTime.now(),
    val resumo: ResumoHoje = ResumoHoje(),
    /** Usos suspeitos dos últimos 7 dias, do mais novo para o mais antigo. */
    val alertas: List<UsoSensor> = emptyList(),
    val supervisionados: List<InfoSupervisionado> = emptyList(),
    val diasUsos: List<DiaUsos> = emptyList(),
    val diasNotificacoes: List<DiaNotificacoes> = emptyList(),
    val usos: List<UsoSensor> = emptyList(),
    val notificacoes: List<Notificacao> = emptyList(),
    val mudancas: List<MudancaPermissao> = emptyList(),
    /** Nome de cada app citado nas listas. */
    val nomes: Map<String, String> = emptyMap(),
    val abertas: Set<String> = emptySet(),
    val aba: Aba = Aba.INICIO,
    val tipoHistorico: TipoHistorico = TipoHistorico.SENSORES,
    /** Histórico filtrado por um app (vindo da ficha ou de um alerta). */
    val filtroApp: String? = null,
    val carregando: Boolean = true,
) {
    fun nome(pacote: String?): String = pacote?.let { nomes[it] ?: it } ?: "Origem desconhecida"
}

/** Mensagem da barra inferior com a ação que desfaz o que acabou de acontecer. */
class Desfazer(val mensagem: String, val desfazer: () -> Unit)

fun chaveDiaUsos(dia: LocalDate) = "U:$dia"
fun chaveDiaNotificacoes(dia: LocalDate) = "N:$dia"
fun chaveAuditoria(grupo: String) = "A:$grupo"

private class Navegacao(
    val aba: Aba = Aba.INICIO,
    val tipoHistorico: TipoHistorico = TipoHistorico.SENSORES,
    val filtroApp: String? = null,
    /** Null = ainda não mexeu: abre o dia de hoje e as gavetas da Auditoria. */
    val abertas: Set<String>? = null,
)

private class Dados(
    val usos: List<UsoSensor>,
    val notificacoes: List<Notificacao>,
    val supervisionados: List<Supervisionado>,
    val mudancas: List<MudancaPermissao>,
)

class PrivacidadeViewModel(app: Application) : AndroidViewModel(app) {
    private val banco = AppDatabase.get(app)
    private val prefsRepo = PreferenciasRepo(app)
    private val navegacao = MutableStateFlow(Navegacao())
    private val eventos = Channel<Desfazer>(Channel.BUFFERED)

    val desfazer: Flow<Desfazer> = eventos.receiveAsFlow()

    val preferencias: StateFlow<Preferencias?> =
        prefsRepo.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Bate a cada virada de minuto, para "hoje" e "em uso há 3 min" andarem sozinhos. */
    private val relogio: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
        }
    }

    private val dados: Flow<Dados> = combine(
        banco.usoDao().observarTodos(),
        banco.notificacaoDao().observarTodas(),
        banco.supervisionadoDao().observarTodos(),
        banco.mudancaDao().observarTodas(),
    ) { u, n, s, m -> Dados(u, n, s, m) }

    val ui: StateFlow<UiState> =
        combine(dados, relogio, navegacao) { d, agora, nav -> montar(d, agora, nav) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** Apps instalados (Auditoria e escolha de apps); null até carregar. */
    private val _apps = MutableStateFlow<List<InfoApp>?>(null)
    val apps: StateFlow<List<InfoApp>?> = _apps

    /** Tempo de tela e internet de hoje de cada app supervisionado. */
    private val _resumosApps = MutableStateFlow<Map<String, ResumoApp>>(emptyMap())
    val resumosApps: StateFlow<Map<String, ResumoApp>> = _resumosApps

    /**
     * Notificações marcadas no Histórico para apagar; null fora do modo de seleção. O modo
     * continua aberto mesmo com zero marcadas, até o usuário sair dele.
     */
    private val _selecao = MutableStateFlow<Set<Long>?>(null)
    val selecao: StateFlow<Set<Long>?> = _selecao

    /** Os últimos 7 dias do Bem-estar digital, do mais antigo para hoje; null até carregar. */
    private val _bemEstar = MutableStateFlow<List<DiaTela>?>(null)
    val bemEstar: StateFlow<List<DiaTela>?> = _bemEstar

    init {
        // Limpa o que passou do prazo mesmo se a vigia estiver parada.
        viewModelScope.launch {
            banco.apagarAntigos(prefsRepo.preferencias.first().diasHistorico)
        }
    }

    private fun montar(d: Dados, agora: LocalDateTime, nav: Navegacao): UiState {
        val context = getApplication<Application>()
        val hoje = agora.toLocalDate()
        val inicioHoje = hoje.inicioMillis()
        val inicioSemana = hoje.minusDays(6).inicioMillis()

        val usosHoje = d.usos.filter { it.inicioMillis >= inicioHoje }
        val resumo = ResumoHoje(
            microfone = usosHoje.count { it.sensor == Sensor.MICROFONE },
            camera = usosHoje.count { it.sensor == Sensor.CAMERA },
            suspeitos = usosHoje.count { it.suspeito },
            notificacoes = d.notificacoes.count { it.postadaMillis >= inicioHoje },
            emUso = d.usos.filter { it.fimMillis == null }.map { it.sensor }.toSet(),
        )
        val alertas = d.usos.filter { it.suspeito && it.inicioMillis >= inicioSemana }

        val supervisionados = d.supervisionados.map { s ->
            val usosDoApp = d.usos.filter { it.pacote == s.pacote }
            InfoSupervisionado(
                supervisionado = s,
                nome = Apps.nome(context, s.pacote),
                suspeitosSemana = usosDoApp.count { it.suspeito && it.inicioMillis >= inicioSemana },
                ultimoUso = usosDoApp.firstOrNull(),
                permissaoNova = d.mudancas.any { it.pacote == s.pacote && it.concedida && it.momentoMillis >= inicioSemana },
                notificacoesHoje = d.notificacoes.count { it.pacote == s.pacote && it.postadaMillis >= inicioHoje },
            )
        }.sortedBy { it.nome.lowercase() }

        val filtro = nav.filtroApp
        val diasUsos = d.usos.filter { filtro == null || it.pacote == filtro }
            .groupBy { it.inicioMillis.emDataHora().toLocalDate() }
            .map { (dia, usos) -> DiaUsos(dia, usos) }
        val diasNotificacoes = d.notificacoes.filter { filtro == null || it.pacote == filtro }
            .groupBy { it.postadaMillis.emDataHora().toLocalDate() }
            .map { (dia, notificacoes) -> DiaNotificacoes(dia, notificacoes) }

        val pacotes = buildSet {
            d.usos.mapNotNullTo(this) { it.pacote }
            d.notificacoes.mapTo(this) { it.pacote }
            d.supervisionados.mapTo(this) { it.pacote }
            filtro?.let { add(it) }
        }
        val padrao = buildSet {
            add(chaveDiaUsos(hoje))
            add(chaveDiaNotificacoes(hoje))
            GRUPOS_AUDITORIA.forEach { add(chaveAuditoria(it)) }
        }

        return UiState(
            agora = agora,
            resumo = resumo,
            alertas = alertas,
            supervisionados = supervisionados,
            diasUsos = diasUsos,
            diasNotificacoes = diasNotificacoes,
            usos = d.usos,
            notificacoes = d.notificacoes,
            mudancas = d.mudancas,
            nomes = pacotes.associateWith { Apps.nome(context, it) },
            abertas = nav.abertas ?: padrao,
            aba = nav.aba,
            tipoHistorico = nav.tipoHistorico,
            filtroApp = filtro,
            carregando = false,
        )
    }

    // --- Navegação ---

    private fun navegar(mudar: (Navegacao) -> Navegacao) = navegacao.update(mudar)

    fun definirAba(aba: Aba) {
        if (aba != Aba.HISTORICO) sairDaSelecao()
        navegar { Navegacao(aba, it.tipoHistorico, it.filtroApp, it.abertas) }
    }

    fun definirTipoHistorico(tipo: TipoHistorico) {
        if (tipo != TipoHistorico.NOTIFICACOES) sairDaSelecao()
        navegar { Navegacao(it.aba, tipo, it.filtroApp, it.abertas) }
    }

    /** Abre o Histórico mostrando só o que é de [pacote]. */
    fun verHistoricoDe(pacote: String, tipo: TipoHistorico) =
        navegar { Navegacao(Aba.HISTORICO, tipo, pacote, it.abertas) }

    fun limparFiltro() = navegar { Navegacao(it.aba, it.tipoHistorico, null, it.abertas) }

    fun alternarGaveta(chave: String) = navegar {
        val base = it.abertas ?: ui.value.abertas
        Navegacao(it.aba, it.tipoHistorico, it.filtroApp, if (chave in base) base - chave else base + chave)
    }

    // --- Seleção de notificações para apagar ---

    /** Entra no modo de seleção, já marcando [id] se vier (toque longo numa notificação). */
    fun entrarNaSelecao(id: Long? = null) {
        _selecao.value = setOfNotNull(id)
    }

    fun sairDaSelecao() {
        _selecao.value = null
    }

    fun alternarSelecao(id: Long) {
        _selecao.update { atual -> atual?.let { if (id in it) it - id else it + id } }
    }

    /** Marca (ou desmarca) várias de uma vez: um dia inteiro ou todas as visíveis. */
    fun marcarVarias(ids: Collection<Long>, marcar: Boolean) {
        _selecao.update { atual -> atual?.let { if (marcar) it + ids else it - ids.toSet() } }
    }

    /** Apaga as notificações marcadas (depois da senha do celular); desfazer traz de volta. */
    fun apagarSelecionadas() {
        val ids = _selecao.value?.toList().orEmpty()
        _selecao.value = null
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val dao = banco.notificacaoDao()
            // O SQLite aceita no máximo 999 parâmetros por consulta.
            val apagadas = banco.withTransaction {
                ids.chunked(500).flatMap { lote -> dao.porIds(lote).also { dao.apagar(lote) } }
            }
            val n = apagadas.size
            eventos.send(
                Desfazer(if (n == 1) "1 notificação apagada" else "$n notificações apagadas") {
                    viewModelScope.launch { dao.inserirTodas(apagadas) }
                },
            )
        }
    }

    // --- Apps instalados e estatísticas ---

    fun carregarApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { Apps.listar(getApplication()) }
        }
    }

    fun atualizarResumosApps() {
        viewModelScope.launch {
            val pacotes = ui.value.supervisionados.map { it.supervisionado.pacote }
            if (pacotes.isEmpty()) return@launch
            _resumosApps.value = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val agora = System.currentTimeMillis()
                val inicio = LocalDate.now().inicioMillis()
                val uso = Estatisticas.uso(context, inicio, agora)
                val dados = Estatisticas.dados(context, inicio, agora)
                pacotes.associateWith { p ->
                    ResumoApp(uso[p] ?: UsoApp(), Apps.uid(context, p)?.let { dados[it] } ?: DadosApp())
                }
            }
        }
    }

    /**
     * Relê o tempo de tela. Os dias anteriores não mudam: só são lidos de novo quando o dia
     * vira (ou na primeira vez); hoje é sempre relido.
     */
    fun atualizarBemEstar() {
        viewModelScope.launch {
            _bemEstar.value = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val hoje = LocalDate.now()
                val agora = System.currentTimeMillis()
                val fora = Estatisticas.foraDoTempoDeTela(context)
                fun lerDia(dia: LocalDate): DiaTela {
                    val tela = Estatisticas.tela(context, dia.inicioMillis(), minOf(dia.plusDays(1).inicioMillis(), agora))
                    val apps = tela.porApp
                        .filter { (pacote, uso) -> pacote !in fora && uso.tempoMillis >= 1000 }
                        .map { (pacote, uso) -> AppTela(pacote, Apps.nome(context, pacote), uso) }
                        .sortedByDescending { it.uso.tempoMillis }
                    return DiaTela(dia, apps.sumOf { it.uso.tempoMillis }, tela.desbloqueios, apps)
                }
                val anteriores = _bemEstar.value
                    ?.takeIf { it.lastOrNull()?.dia == hoje }
                    ?.dropLast(1)
                    ?: (6 downTo 1).map { lerDia(hoje.minusDays(it.toLong())) }
                anteriores + lerDia(hoje)
            }
        }
    }

    suspend fun ficha(pacote: String): Ficha = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val agora = System.currentTimeMillis()
        val hoje = LocalDate.now()
        val inicioHoje = hoje.inicioMillis()
        val inicioSemana = hoje.minusDays(6).inicioMillis()
        val uid = Apps.uid(context, pacote)
        val usoSemana = Estatisticas.uso(context, inicioSemana, agora)[pacote] ?: UsoApp()
        Ficha(
            usoHoje = Estatisticas.uso(context, inicioHoje, agora)[pacote] ?: UsoApp(),
            usoSemana = usoSemana,
            dadosHoje = uid?.let { Estatisticas.dados(context, inicioHoje, agora)[it] } ?: DadosApp(),
            dadosSemana = uid?.let { Estatisticas.dados(context, inicioSemana, agora)[it] } ?: DadosApp(),
            permissoes = Apps.permissoesSensiveis(context, pacote),
        )
    }

    // --- Supervisão ---

    fun supervisionar(pacote: String) {
        viewModelScope.launch {
            val permissoes = withContext(Dispatchers.IO) { Apps.permissoesSensiveis(getApplication(), pacote) }.orEmpty()
            banco.supervisionadoDao().salvar(
                Supervisionado(pacote, System.currentTimeMillis(), permissoes.joinToString(",")),
            )
            atualizarResumosApps()
        }
    }

    /** Tira o app da supervisão com o histórico de permissões dele; desfazer traz os dois de volta. */
    fun pararDeSupervisionar(pacote: String) {
        viewModelScope.launch {
            val anterior = ui.value.supervisionados.firstOrNull { it.supervisionado.pacote == pacote } ?: return@launch
            val mudancas = banco.mudancaDao().doPacote(pacote)
            banco.withTransaction {
                banco.mudancaDao().excluirDoPacote(pacote)
                banco.supervisionadoDao().excluir(pacote)
            }
            eventos.send(
                Desfazer("“${anterior.nome}” saiu da supervisão") {
                    viewModelScope.launch {
                        banco.withTransaction {
                            banco.supervisionadoDao().salvar(anterior.supervisionado)
                            banco.mudancaDao().inserirTodas(mudancas.map { it.copy(id = 0) })
                        }
                    }
                },
            )
        }
    }

    // --- Armazenamento e histórico ---

    suspend fun armazenamento(): Armazenamento =
        withContext(Dispatchers.IO) { medirArmazenamento(getApplication()) }

    fun apagarHistorico() {
        viewModelScope.launch {
            banco.withTransaction {
                banco.usoDao().apagarTudo()
                banco.notificacaoDao().apagarTudo()
                banco.mudancaDao().apagarTudo()
            }
        }
    }

    // --- Preferências ---

    fun definirTema(tema: Tema) {
        viewModelScope.launch { prefsRepo.definirTema(tema) }
    }

    fun definirDiasHistorico(dias: Int) {
        viewModelScope.launch {
            prefsRepo.definirDiasHistorico(dias)
            banco.apagarAntigos(dias)
        }
    }

    fun alternarTextoOculto(pacote: String) {
        viewModelScope.launch { prefsRepo.alternarTextoOculto(pacote) }
    }

    fun marcarAvisoLido() {
        viewModelScope.launch { prefsRepo.marcarAvisoLido() }
    }
}

/** Gavetas da Auditoria, pelas permissões que o app tem. */
val GRUPOS_AUDITORIA = listOf("AMBOS", "MICROFONE", "CAMERA")

/** Duração de um uso em milissegundos; null se ainda está ligado. */
fun UsoSensor.duracaoMillis(): Long? = fimMillis?.let { it - inicioMillis }

/** Uso cujo fim não foi visto (a vigia parou no meio). */
val UsoSensor.interrompido: Boolean get() = fimMillis != null && fimMillis == inicioMillis
