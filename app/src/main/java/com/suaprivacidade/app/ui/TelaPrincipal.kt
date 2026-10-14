package com.suaprivacidade.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suaprivacidade.app.data.Apps
import com.suaprivacidade.app.data.Preferencias
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.formatarDiaLongo
import com.suaprivacidade.app.data.inicioMillis
import com.suaprivacidade.app.ui.theme.escuro
import com.suaprivacidade.app.vigia.Acessos
import com.suaprivacidade.app.vigia.Vigia
import com.suaprivacidade.app.vigia.VigiaService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NENHUM = -1L

/** Qual lista de apps está aberta para escolher. */
private enum class Escolha { SUPERVISAO, OCULTOS }

val Aba.icone: ImageVector
    get() = when (this) {
        Aba.INICIO -> Icons.Rounded.Home
        Aba.SUPERVISAO -> Icons.Rounded.Visibility
        Aba.HISTORICO -> Icons.Rounded.History
        Aba.AUDITORIA -> Icons.Rounded.Policy
    }

@Composable
fun TelaPrincipal(
    vm: PrivacidadeViewModel,
    preferencias: Preferencias,
    abaPedida: String?,
    onAbaAtendida: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val resumosApps by vm.resumosApps.collectAsStateWithLifecycle()
    val bemEstar by vm.bemEstar.collectAsStateWithLifecycle()
    val selecao by vm.selecao.collectAsStateWithLifecycle()
    val pedirSenha = lembrarPedidoSenha()
    val vigiaAtiva by Vigia.ativa.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val permissoes = lembrarPermissoes()
    val pedirNotificacoes = lembrarPedidoNotificacoes()

    var ajustesAbertos by rememberSaveable { mutableStateOf(false) }
    var fichaAberta by rememberSaveable { mutableStateOf<String?>(null) }
    var escolhendo by rememberSaveable { mutableStateOf<Escolha?>(null) }
    var usoAberto by rememberSaveable { mutableLongStateOf(NENHUM) }
    var ocultarAvisoIncompleto by rememberSaveable { mutableStateOf(false) }
    var mostrarSistema by rememberSaveable { mutableStateOf(false) }
    var confirmarApagarNotificacoes by rememberSaveable { mutableStateOf(false) }
    // Ids das notificações visíveis no Histórico (respeita o filtro por app): o "Todas" da seleção.
    val idsVisiveis = ui.diasNotificacoes.flatMap { d -> d.notificacoes.map { it.id } }

    BackHandler(enabled = selecao != null) { vm.sairDaSelecao() }

    // Acesso ligado mas vigia parada: pede para religar e, se em 3 s ela não subir, avisa
    // (na Xiaomi, quase sempre é o "Início automático" desligado).
    var vigiaParada by remember { mutableStateOf(false) }
    LaunchedEffect(permissoes.leitor, vigiaAtiva) {
        vigiaParada = false
        if (permissoes.leitor && !vigiaAtiva) {
            VigiaService.religar(context)
            delay(3_000)
            vigiaParada = true
        }
    }

    LaunchedEffect(abaPedida) {
        if (abaPedida != null) {
            Aba.entries.firstOrNull { it.name == abaPedida }?.let(vm::definirAba)
            onAbaAtendida()
        }
    }

    LaunchedEffect(vm) {
        vm.desfazer.collect { evento ->
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val resultado = snackbar.showSnackbar(
                    message = evento.mensagem,
                    actionLabel = "Desfazer",
                    duration = SnackbarDuration.Short,
                )
                if (resultado == SnackbarResult.ActionPerformed) evento.desfazer()
            }
        }
    }

    // Relê do Android ao abrir a aba e ao voltar das configurações (permissões podem ter mudado).
    LifecycleResumeEffect(ui.aba, ui.supervisionados.size) {
        when (ui.aba) {
            Aba.AUDITORIA -> vm.carregarApps()
            Aba.SUPERVISAO -> vm.atualizarResumosApps()
            else -> Unit
        }
        onPauseOrDispose {}
    }
    LaunchedEffect(escolhendo) { if (escolhendo != null) vm.carregarApps() }
    // Tempo de tela: relido ao abrir o Início, a cada minuto com ele aberto e ao voltar ao app.
    LifecycleResumeEffect(ui.aba, ui.agora, permissoes.acessoUso) {
        if (ui.aba == Aba.INICIO && permissoes.acessoUso) vm.atualizarBemEstar()
        onPauseOrDispose {}
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (ui.aba == Aba.SUPERVISAO) {
                ExtendedFloatingActionButton(
                    onClick = { escolhendo = Escolha.SUPERVISAO },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Adicionar app") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        bottomBar = {
            val marcadas = selecao
            if (marcadas != null) {
                BarraSelecao(
                    quantidade = marcadas.size,
                    todasMarcadas = idsVisiveis.isNotEmpty() && marcadas.containsAll(idsVisiveis),
                    onTodas = { marcar -> vm.marcarVarias(idsVisiveis, marcar) },
                    onCancelar = vm::sairDaSelecao,
                    onApagar = { confirmarApagarNotificacoes = true },
                )
            } else NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Aba.entries.forEach { aba ->
                    NavigationBarItem(
                        selected = ui.aba == aba,
                        onClick = { vm.definirAba(aba) },
                        icon = { Icon(aba.icone, contentDescription = null) },
                        label = { Text(aba.rotulo, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        key(ui.aba) {
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + if (ui.aba == Aba.SUPERVISAO) 96.dp else 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "cabecalho") {
                    Cabecalho(
                        texto = when (ui.aba) {
                            Aba.INICIO -> formatarDiaLongo(ui.agora.toLocalDate())
                            Aba.SUPERVISAO -> "Apps que você acompanha de perto"
                            Aba.HISTORICO -> "Últimos ${preferencias.diasHistorico} dias"
                            Aba.AUDITORIA -> "Quem tem microfone e câmera"
                        },
                        onAjustes = { ajustesAbertos = true },
                    )
                }

                when (ui.aba) {
                    Aba.INICIO -> {
                        item(key = "resumo") { CartaoResumo(ui.resumo, vigiaAtiva && permissoes.leitor, permissoes.leitor) }
                        if (!permissoes.leitor || vigiaParada || (permissoes.faltaAlgo && !ocultarAvisoIncompleto)) {
                            item(key = "permissoes") {
                                Column(Modifier.animateItem()) {
                                    CartaoPermissoes(permissoes, vigiaParada)
                                    if (permissoes.leitor && !vigiaParada) {
                                        TextButton(onClick = { ocultarAvisoIncompleto = true }, modifier = Modifier.align(Alignment.End)) {
                                            Text("Agora não")
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "bem-estar") {
                            CartaoBemEstar(
                                dias = bemEstar,
                                hoje = ui.agora.toLocalDate(),
                                temAcessoUso = permissoes.acessoUso,
                                notificacoesNoDia = { dia ->
                                    val inicio = dia.inicioMillis()
                                    val fim = dia.plusDays(1).inicioMillis()
                                    ui.notificacoes.count { it.postadaMillis in inicio until fim }
                                },
                                onPermitir = { Acessos.abrirAcessoUso(context) },
                            )
                        }
                        item(key = "titulo-alertas") {
                            Text(
                                "Usos suspeitos da semana",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (!ui.carregando && ui.alertas.isEmpty()) {
                            item(key = "vazio-alertas") {
                                EstadoVazio(
                                    Icons.Rounded.Shield,
                                    "Nada suspeito",
                                    "Quando o microfone ou a câmera ligarem fora da tela, com a tela apagada ou sem " +
                                        "explicação, aparece aqui.",
                                    Modifier.animateItem(),
                                )
                            }
                        }
                        items(ui.alertas.take(30), key = { "alerta-${it.id}" }) { uso ->
                            Cartao(Modifier.animateItem()) {
                                LinhaUso(uso, ui.nome(uso.pacote), ui.agora, onClick = { usoAberto = uso.id })
                            }
                        }
                    }

                    Aba.SUPERVISAO -> {
                        if (!ui.carregando && ui.supervisionados.isEmpty()) {
                            item(key = "vazio-supervisao") {
                                EstadoVazio(
                                    Icons.Rounded.Visibility,
                                    "Nenhum app supervisionado",
                                    "Toque em “Adicionar app” e escolha os apps que você quer acompanhar de perto: " +
                                        "microfone e câmera fora da tela, internet, permissões novas, tempo de uso e notificações.",
                                    Modifier.animateItem(),
                                )
                            }
                        }
                        items(ui.supervisionados, key = { "S:${it.supervisionado.pacote}" }) { info ->
                            CartaoSupervisionado(
                                info = info,
                                resumo = resumosApps[info.supervisionado.pacote],
                                temAcessoUso = permissoes.acessoUso,
                                onClick = { fichaAberta = info.supervisionado.pacote },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    Aba.HISTORICO -> {
                        item(key = "tipo-historico") { BarraTipoHistorico(ui.tipoHistorico, vm::definirTipoHistorico) }
                        ui.filtroApp?.let { pacote ->
                            item(key = "filtro") {
                                InputChip(
                                    selected = true,
                                    onClick = vm::limparFiltro,
                                    label = { Text("Só ${ui.nome(pacote)}") },
                                    leadingIcon = { IconeApp(pacote, tamanho = 20.dp) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Mostrar todos", modifier = Modifier.size(16.dp)) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        val hoje = ui.agora.toLocalDate()
                        when (ui.tipoHistorico) {
                            TipoHistorico.SENSORES -> {
                                if (!ui.carregando && ui.diasUsos.isEmpty()) {
                                    item(key = "vazio-usos") {
                                        EstadoVazioHistorico(ui.tipoHistorico, ui.filtroApp != null, Modifier.animateItem())
                                    }
                                }
                                items(ui.diasUsos, key = { chaveDiaUsos(it.dia) }) { dia ->
                                    GavetaDiaUsos(
                                        dia = dia,
                                        hoje = hoje,
                                        aberta = chaveDiaUsos(dia.dia) in ui.abertas,
                                        agora = ui.agora,
                                        nome = ui::nome,
                                        onAlternar = { vm.alternarGaveta(chaveDiaUsos(dia.dia)) },
                                        onAbrirUso = { usoAberto = it.id },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                            TipoHistorico.NOTIFICACOES -> {
                                if (!ui.carregando && ui.diasNotificacoes.isEmpty()) {
                                    item(key = "vazio-notificacoes") {
                                        EstadoVazioHistorico(ui.tipoHistorico, ui.filtroApp != null, Modifier.animateItem())
                                    }
                                } else if (selecao == null) {
                                    item(key = "selecionar") {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateItem()) {
                                            Text(
                                                "Toque longo numa notificação para selecionar",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f),
                                            )
                                            TextButton(onClick = { vm.entrarNaSelecao() }) {
                                                Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Selecionar")
                                            }
                                        }
                                    }
                                }
                                items(ui.diasNotificacoes, key = { chaveDiaNotificacoes(it.dia) }) { dia ->
                                    GavetaDiaNotificacoes(
                                        dia = dia,
                                        hoje = hoje,
                                        aberta = chaveDiaNotificacoes(dia.dia) in ui.abertas,
                                        nome = ui::nome,
                                        selecao = selecao,
                                        onAlternar = { vm.alternarGaveta(chaveDiaNotificacoes(dia.dia)) },
                                        onAlternarSelecao = vm::alternarSelecao,
                                        onEntrarNaSelecao = vm::entrarNaSelecao,
                                        onMarcarDia = vm::marcarVarias,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        }
                    }

                    Aba.AUDITORIA -> {
                        item(key = "dica") { CartaoDicaAuditoria() }
                        item(key = "sistema") {
                            FilterChip(
                                selected = mostrarSistema,
                                onClick = { mostrarSistema = !mostrarSistema },
                                label = { Text("Mostrar apps do sistema") },
                                shape = CircleShape,
                            )
                        }
                        val lista = apps
                        if (lista == null) {
                            item(key = "carregando") {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            val visiveis = lista.filter { mostrarSistema || !it.sistema }
                            val ultimoUso = ui.usos.filter { it.pacote != null }.distinctBy { it.pacote }.associateBy { it.pacote!! }
                            GRUPOS_AUDITORIA.forEach { grupo ->
                                val doGrupo = appsDoGrupo(grupo, visiveis)
                                if (doGrupo.isNotEmpty()) {
                                    item(key = chaveAuditoria(grupo)) {
                                        GavetaAuditoria(
                                            grupo = grupo,
                                            apps = doGrupo,
                                            ultimoUso = ultimoUso,
                                            hoje = ui.agora.toLocalDate(),
                                            aberta = chaveAuditoria(grupo) in ui.abertas,
                                            onAlternar = { vm.alternarGaveta(chaveAuditoria(grupo)) },
                                            onAbrirApp = { Apps.abrirDetalhes(context, it) },
                                            modifier = Modifier.animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val uso = ui.usos.firstOrNull { it.id == usoAberto }
    if (uso != null) {
        val pacote = uso.pacote
        DialogoUso(
            uso = uso,
            nome = ui.nome(pacote),
            agora = ui.agora,
            supervisionado = ui.supervisionados.any { it.supervisionado.pacote == pacote },
            onSupervisionar = {
                if (pacote != null) {
                    vm.supervisionar(pacote)
                    if (!Acessos.temNotificacoes(context)) pedirNotificacoes()
                }
                usoAberto = NENHUM
            },
            onAbrirApp = { pacote?.let { Apps.abrirDetalhes(context, it) } },
            onFechar = { usoAberto = NENHUM },
        )
    }

    val ficha = ui.supervisionados.firstOrNull { it.supervisionado.pacote == fichaAberta }
    if (ficha != null) {
        val pacote = ficha.supervisionado.pacote
        FolhaFicha(
            info = ficha,
            usos = ui.usos,
            notificacoes = ui.notificacoes,
            mudancas = ui.mudancas,
            agora = ui.agora,
            temAcessoUso = permissoes.acessoUso,
            carregarFicha = vm::ficha,
            onVerHistorico = { tipo ->
                fichaAberta = null
                vm.verHistoricoDe(pacote, tipo)
            },
            onAbrirApp = { Apps.abrirDetalhes(context, pacote) },
            onParar = {
                fichaAberta = null
                vm.pararDeSupervisionar(pacote)
            },
            onFechar = { fichaAberta = null },
        )
    }

    when (escolhendo) {
        Escolha.SUPERVISAO -> {
            val selecionados = ui.supervisionados.map { it.supervisionado.pacote }.toSet()
            FolhaEscolherApps(
                titulo = "Supervisionar apps",
                texto = "Marque os apps que você quer acompanhar de perto.",
                apps = apps,
                selecionados = selecionados,
                onAlternar = { pacote ->
                    if (pacote in selecionados) {
                        vm.pararDeSupervisionar(pacote)
                    } else {
                        vm.supervisionar(pacote)
                        if (!Acessos.temNotificacoes(context)) pedirNotificacoes()
                    }
                },
                onFechar = { escolhendo = null },
            )
        }
        Escolha.OCULTOS -> FolhaEscolherApps(
            titulo = "Texto oculto",
            texto = "As notificações destes apps entram no histórico sem título nem texto.",
            apps = apps,
            selecionados = preferencias.textoOculto,
            onAlternar = vm::alternarTextoOculto,
            onFechar = {
                escolhendo = null
                ajustesAbertos = true
            },
        )
        null -> Unit
    }

    if (ajustesAbertos) {
        FolhaAjustes(
            preferencias = preferencias,
            onFechar = { ajustesAbertos = false },
            onTema = vm::definirTema,
            onDiasHistorico = vm::definirDiasHistorico,
            onTirarOculto = vm::alternarTextoOculto,
            onEscolherOcultos = {
                ajustesAbertos = false
                escolhendo = Escolha.OCULTOS
            },
            onApagarHistorico = { pedirSenha("Para apagar todo o histórico") { vm.apagarHistorico() } },
            secaoArmazenamento = { SecaoArmazenamento(vm) },
        )
    }

    val marcadas = selecao?.size ?: 0
    if (confirmarApagarNotificacoes && marcadas > 0) {
        AlertDialog(
            onDismissRequest = { confirmarApagarNotificacoes = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(if (marcadas == 1) "Apagar 1 notificação?" else "Apagar $marcadas notificações?") },
            text = {
                Text(
                    if (temSenhaNoCelular(context)) {
                        "Elas saem do histórico do Sua Privacidade (não da barra do celular). Para confirmar, " +
                            "o Android vai pedir a senha de bloqueio do celular."
                    } else {
                        "Elas saem do histórico do Sua Privacidade (não da barra do celular). Seu celular não " +
                            "tem senha de bloqueio, então não dá para confirmar com senha."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApagarNotificacoes = false
                    pedirSenha(if (marcadas == 1) "Para apagar 1 notificação" else "Para apagar $marcadas notificações") {
                        vm.apagarSelecionadas()
                    }
                }) { Text("Apagar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarApagarNotificacoes = false }) { Text("Cancelar") } },
        )
    }

    if (!preferencias.avisoLido) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
            title = { Text("Antes de começar") },
            text = { Text(TEXTO_COMO_FUNCIONA) },
            confirmButton = { TextButton(onClick = vm::marcarAvisoLido) { Text("Entendi") } },
        )
    }
}

/** Barra inferior do modo de seleção: quantas marcadas, marcar todas, cancelar e apagar. */
@Composable
private fun BarraSelecao(
    quantidade: Int,
    todasMarcadas: Boolean,
    onTodas: (Boolean) -> Unit,
    onCancelar: () -> Unit,
    onApagar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Surface(color = cores.surfaceContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            IconButton(onClick = onCancelar) { Icon(Icons.Rounded.Close, contentDescription = "Sair da seleção") }
            Text(
                when (quantidade) {
                    0 -> "Nenhuma marcada"
                    1 -> "1 marcada"
                    else -> "$quantidade marcadas"
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onTodas(!todasMarcadas) }) {
                Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (todasMarcadas) "Nenhuma" else "Todas")
            }
            Button(
                onClick = onApagar,
                enabled = quantidade > 0,
                colors = ButtonDefaults.buttonColors(containerColor = cores.error, contentColor = cores.onError),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apagar")
            }
        }
    }
}

@Composable
private fun Cabecalho(texto: String, onAjustes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Sua Privacidade", style = MaterialTheme.typography.headlineMedium)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = onAjustes) {
            Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
        }
    }
}

/** Situação agora (vigia ligada, sensor em uso) e o placar do dia. */
@Composable
private fun CartaoResumo(resumo: ResumoHoje, vigiando: Boolean, temLeitor: Boolean) {
    val cores = MaterialTheme.colorScheme
    // No escuro, o primário é claro demais para um bloco grande: usa o container (fundo escuro, texto claro).
    val base = when {
        !vigiando -> if (cores.escuro) cores.errorContainer else cores.error
        cores.escuro -> cores.primaryContainer
        else -> cores.primary
    }
    val texto = when {
        !vigiando -> if (cores.escuro) cores.onErrorContainer else cores.onError
        cores.escuro -> cores.onPrimaryContainer
        else -> cores.onPrimary
    }
    val fundo = Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.22f)))
    val emUso = resumo.emUso
    val manchete = when {
        !vigiando -> "Vigia desligada"
        Sensor.MICROFONE in emUso && Sensor.CAMERA in emUso -> "Microfone e câmera em uso"
        Sensor.MICROFONE in emUso -> "Microfone em uso agora"
        Sensor.CAMERA in emUso -> "Câmera em uso agora"
        resumo.suspeitos == 1 -> "1 uso suspeito hoje"
        resumo.suspeitos > 1 -> "${resumo.suspeitos} usos suspeitos hoje"
        else -> "Tudo tranquilo hoje"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(fundo, RoundedCornerShape(28.dp))
            .padding(22.dp),
    ) {
        Column(Modifier.animateContentSize()) {
            Text(
                when {
                    vigiando -> "Vigiando microfone, câmera e notificações"
                    temLeitor -> "O celular impediu a vigia de iniciar"
                    else -> "Libere o acesso a notificações abaixo"
                },
                style = MaterialTheme.typography.labelLarge,
                color = texto.copy(alpha = 0.85f),
            )
            AnimatedContent(
                targetState = manchete,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "manchete",
            ) { m ->
                Text(m, style = MaterialTheme.typography.headlineMedium, color = texto, maxLines = 2)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniEstatistica("Microfone", "${resumo.microfone}x", texto, Modifier.weight(1f))
                MiniEstatistica("Câmera", "${resumo.camera}x", texto, Modifier.weight(1f))
                MiniEstatistica(
                    rotulo = "Suspeitos",
                    valor = resumo.suspeitos.toString(),
                    corTexto = texto,
                    modifier = Modifier.weight(1f),
                    destaque = resumo.suspeitos > 0,
                )
            }
            Text(
                "${resumo.notificacoes} notificações recebidas hoje",
                style = MaterialTheme.typography.bodySmall,
                color = texto.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun MiniEstatistica(
    rotulo: String,
    valor: String,
    corTexto: Color,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
) {
    Column(
        modifier
            .background(corTexto.copy(alpha = if (destaque) 0.22f else 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (destaque) {
                Icon(Icons.Rounded.WarningAmber, null, tint = corTexto, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = corTexto.copy(alpha = 0.8f), maxLines = 1)
        }
        Text(valor, style = MaterialTheme.typography.titleMedium, color = corTexto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraTipoHistorico(tipo: TipoHistorico, onTipo: (TipoHistorico) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TipoHistorico.entries.forEachIndexed { i, t ->
            SegmentedButton(
                selected = tipo == t,
                onClick = { onTipo(t) },
                shape = SegmentedButtonDefaults.itemShape(i, TipoHistorico.entries.size),
                icon = {},
            ) {
                Text(t.rotulo, maxLines = 1)
            }
        }
    }
}
