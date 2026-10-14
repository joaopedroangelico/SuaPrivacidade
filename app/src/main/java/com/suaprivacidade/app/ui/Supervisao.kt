package com.suaprivacidade.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.data.DadosApp
import com.suaprivacidade.app.data.InfoApp
import com.suaprivacidade.app.data.LocaleBR
import com.suaprivacidade.app.data.MudancaPermissao
import com.suaprivacidade.app.data.Notificacao
import com.suaprivacidade.app.data.UsoApp
import com.suaprivacidade.app.data.UsoSensor
import com.suaprivacidade.app.data.contar
import com.suaprivacidade.app.data.emDataHora
import com.suaprivacidade.app.data.formatarBytes
import com.suaprivacidade.app.data.formatarDataCurta
import com.suaprivacidade.app.data.formatarQuando
import com.suaprivacidade.app.data.formatarTempoDeUso
import com.suaprivacidade.app.data.inicioMillis
import com.suaprivacidade.app.data.rotuloPermissao
import com.suaprivacidade.app.ui.theme.aviso
import com.suaprivacidade.app.ui.theme.sucesso
import java.text.Normalizer
import java.time.LocalDateTime

/** "Hoje: 1h 05min · 12 aberturas · 45 MB de internet". */
private fun textoHoje(resumo: ResumoApp?): String {
    if (resumo == null) return "Carregando…"
    return "Hoje: ${formatarTempoDeUso(resumo.uso.tempoMillis)} · " +
        "${contar(resumo.uso.aberturas, "abertura", "aberturas")} · ${formatarBytes(resumo.dados.total)} de internet"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartaoSupervisionado(
    info: InfoSupervisionado,
    resumo: ResumoApp?,
    temAcessoUso: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    Cartao(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconeApp(info.supervisionado.pacote, tamanho = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(info.nome, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (temAcessoUso) textoHoje(resumo) else "Libere o “Acesso ao uso” para ver tempo e internet",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (info.suspeitosSemana > 0) {
                Etiqueta("Microfone/câmera fora da tela: ${info.suspeitosSemana}x na semana", cores.error)
            }
            if (info.permissaoNova) Etiqueta("Permissão nova", cores.aviso)
            if (info.notificacoesHoje > 0) {
                Etiqueta(contar(info.notificacoesHoje, "notificação hoje", "notificações hoje"), cores.onSurfaceVariant)
            }
            if (info.suspeitosSemana == 0 && !info.permissaoNova) Etiqueta("Nada estranho na semana", cores.sucesso)
        }
    }
}

// ---------------------------------------------------------------------------
// Ficha de um app supervisionado
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolhaFicha(
    info: InfoSupervisionado,
    usos: List<UsoSensor>,
    notificacoes: List<Notificacao>,
    mudancas: List<MudancaPermissao>,
    agora: LocalDateTime,
    temAcessoUso: Boolean,
    carregarFicha: suspend (String) -> Ficha,
    onVerHistorico: (TipoHistorico) -> Unit,
    onAbrirApp: () -> Unit,
    onParar: () -> Unit,
    onFechar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val pacote = info.supervisionado.pacote
    val ficha by produceState<Ficha?>(null, pacote) { value = carregarFicha(pacote) }
    val hoje = agora.toLocalDate()
    val inicioHoje = hoje.inicioMillis()
    val inicioSemana = hoje.minusDays(6).inicioMillis()
    val usosDoApp = usos.filter { it.pacote == pacote }
    val notificacoesDoApp = notificacoes.filter { it.pacote == pacote }
    val mudancasDoApp = mudancas.filter { it.pacote == pacote }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cores.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeApp(pacote, tamanho = 56.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(info.nome, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Supervisionado desde ${formatarDataCurta(info.supervisionado.adicionadoMillis.emDataHora().toLocalDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            }

            // Microfone e câmera
            HorizontalDivider(color = cores.outlineVariant)
            TituloSecao(Icons.Rounded.Mic, "Microfone e câmera")
            val usosSemana = usosDoApp.filter { it.inicioMillis >= inicioSemana }
            Bloco {
                LinhaNumero("Usos nos últimos 7 dias", usosSemana.size.toString())
                LinhaNumero(
                    "Fora da tela",
                    usosSemana.count { it.suspeito }.toString(),
                    destaque = usosSemana.any { it.suspeito },
                )
            }
            usosDoApp.take(3).forEach { uso ->
                LinhaUso(uso, info.nome, agora, onClick = { onVerHistorico(TipoHistorico.SENSORES) })
            }
            if (usosDoApp.isNotEmpty()) {
                TextButton(onClick = { onVerHistorico(TipoHistorico.SENSORES) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Ver todos no histórico")
                }
            }

            // Tempo de uso e internet
            HorizontalDivider(color = cores.outlineVariant)
            TituloSecao(Icons.Rounded.Schedule, "Tempo de uso")
            val f = ficha
            when {
                !temAcessoUso -> TextoSemAcesso()
                f == null -> Carregando()
                else -> Bloco {
                    LinhaUsoApp("Hoje", f.usoHoje)
                    LinhaUsoApp("Últimos 7 dias", f.usoSemana)
                }
            }
            TituloSecao(Icons.Rounded.DataUsage, "Internet")
            when {
                !temAcessoUso -> TextoSemAcesso()
                f == null -> Carregando()
                else -> Bloco {
                    LinhaDados("Hoje", f.dadosHoje)
                    LinhaDados("Últimos 7 dias", f.dadosSemana)
                    Text(
                        "“Em segundo plano” é o que o app gastou sem estar aberto na tela. Wi-Fi e dados móveis somados.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            }

            // Notificações
            HorizontalDivider(color = cores.outlineVariant)
            TituloSecao(Icons.Rounded.Notifications, "Notificações")
            Bloco {
                LinhaNumero("Hoje", notificacoesDoApp.count { it.postadaMillis >= inicioHoje }.toString())
                LinhaNumero("Últimos 7 dias", notificacoesDoApp.count { it.postadaMillis >= inicioSemana }.toString())
            }
            if (notificacoesDoApp.isNotEmpty()) {
                TextButton(onClick = { onVerHistorico(TipoHistorico.NOTIFICACOES) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Ver notificações no histórico")
                }
            }

            // Permissões
            HorizontalDivider(color = cores.outlineVariant)
            TituloSecao(Icons.Rounded.Shield, "Permissões")
            val permissoes = f?.permissoes
            when {
                f == null -> Carregando()
                permissoes == null -> Text("O app não está mais instalado.", style = MaterialTheme.typography.bodyMedium)
                permissoes.isEmpty() -> Text(
                    "Nenhuma permissão sensível concedida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cores.onSurfaceVariant,
                )
                else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    permissoes.sortedBy { rotuloPermissao(it) }.forEach { Etiqueta(rotuloPermissao(it), cores.primary) }
                }
            }
            mudancasDoApp.take(10).forEach { m ->
                Text(
                    "${if (m.concedida) "Ganhou" else "Perdeu"} ${rotuloPermissao(m.permissao)} · ${formatarQuando(m.momentoMillis, hoje)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (m.concedida) cores.aviso else cores.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onAbrirApp, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Abrir permissões do app")
            }

            HorizontalDivider(color = cores.outlineVariant)
            TextButton(onClick = onParar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = cores.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Parar de supervisionar", color = cores.error)
            }
        }
    }
}

@Composable
private fun Bloco(conteudo: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) { conteudo() }
}

@Composable
private fun LinhaNumero(rotulo: String, valor: String, detalhe: String? = null, destaque: Boolean = false) {
    val cores = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(valor, style = MaterialTheme.typography.titleSmall, color = if (destaque) cores.error else cores.onSurface)
            if (detalhe != null) {
                Text(detalhe, style = MaterialTheme.typography.labelSmall, color = cores.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LinhaUsoApp(rotulo: String, uso: UsoApp) =
    LinhaNumero(rotulo, formatarTempoDeUso(uso.tempoMillis), contar(uso.aberturas, "abertura", "aberturas"))

@Composable
private fun LinhaDados(rotulo: String, dados: DadosApp) =
    LinhaNumero(rotulo, formatarBytes(dados.total), "${formatarBytes(dados.segundoPlano)} em segundo plano")

@Composable
private fun TextoSemAcesso() {
    Text(
        "Libere o “Acesso ao uso” (Ajustes → Permissões) para ver esta parte.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Carregando() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            "Lendo do Android…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Escolher apps (supervisão e texto oculto)
// ---------------------------------------------------------------------------

/** Tira acentos e maiúsculas, para "camera" achar "Câmera". */
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(LocaleBR), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaEscolherApps(
    titulo: String,
    texto: String,
    apps: List<InfoApp>?,
    selecionados: Set<String>,
    onAlternar: (String) -> Unit,
    onFechar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    var busca by remember { mutableStateOf("") }
    var incluirSistema by remember { mutableStateOf(false) }
    val filtrados = remember(apps, busca, incluirSistema) {
        val termo = normalizar(busca.trim())
        apps.orEmpty().filter { app ->
            (incluirSistema || !app.sistema || app.pacote in selecionados) &&
                (termo.isEmpty() || normalizar(app.nome).contains(termo) || app.pacote.contains(termo))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cores.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 24.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = cores.onSurfaceVariant)
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                placeholder = { Text("Buscar app") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
            )
            FilterChip(
                selected = incluirSistema,
                onClick = { incluirSistema = !incluirSistema },
                label = { Text("Mostrar apps do sistema") },
                shape = CircleShape,
            )
            if (apps == null) {
                Carregando()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(filtrados, key = { it.pacote }) { app ->
                        LinhaEscolherApp(app, app.pacote in selecionados, onClick = { onAlternar(app.pacote) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaEscolherApp(app: InfoApp, selecionado: Boolean, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        IconeApp(app.pacote, tamanho = 40.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(app.nome, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sensores = listOfNotNull("Microfone".takeIf { app.microfone }, "Câmera".takeIf { app.camera })
            if (sensores.isNotEmpty()) {
                Text("Tem: ${sensores.joinToString(" e ")}", style = MaterialTheme.typography.bodySmall, color = cores.onSurfaceVariant)
            }
        }
        Checkbox(checked = selecionado, onCheckedChange = { onClick() })
    }
}
