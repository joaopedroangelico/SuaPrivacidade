package com.suaprivacidade.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.data.LocaleBR
import com.suaprivacidade.app.data.formatarDataCurta
import com.suaprivacidade.app.data.formatarTempoCurto
import com.suaprivacidade.app.data.formatarTempoDeUso
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.abs

/** Quantos apps aparecem antes de "Ver todos". */
private const val APPS_VISIVEIS = 5
/** Diferença da média abaixo da qual o dia conta como "na média". */
private const val MARGEM_MEDIA = 10L * 60 * 1000

/** "Seg", "Ter"… e "Hoje" para o dia atual. */
private fun rotuloDia(dia: LocalDate, hoje: LocalDate): String =
    if (dia == hoje) "Hoje" else dia.dayOfWeek.getDisplayName(TextStyle.SHORT, LocaleBR)
        .removeSuffix(".").replaceFirstChar { it.titlecase(LocaleBR) }

/** "hoje", "ontem", "segunda, 12/10". */
private fun nomeCurtoDia(dia: LocalDate, hoje: LocalDate): String = when (dia) {
    hoje -> "hoje"
    hoje.minusDays(1) -> "ontem"
    else -> "${dia.dayOfWeek.getDisplayName(TextStyle.FULL, LocaleBR).substringBefore('-')}, ${formatarDataCurta(dia)}"
}

/**
 * Bem-estar digital na tela inicial: tempo de tela do dia em destaque, a semana em colunas
 * (toque num dia para ver os detalhes dele) e os apps mais usados em barras.
 */
@Composable
fun CartaoBemEstar(
    dias: List<DiaTela>?,
    hoje: LocalDate,
    temAcessoUso: Boolean,
    notificacoesNoDia: (LocalDate) -> Int,
    onPermitir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    Cartao(modifier.animateContentSize()) {
        TituloSecao(Icons.Rounded.HourglassTop, "Bem-estar digital")
        Spacer(Modifier.height(12.dp))
        when {
            !temAcessoUso -> {
                Text(
                    "Para mostrar seu tempo de tela, quantas vezes você desbloqueou o celular e os apps mais usados, " +
                        "libere o “Acesso ao uso”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cores.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onPermitir) { Text("Permitir") }
            }
            dias.isNullOrEmpty() -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
            else -> ConteudoBemEstar(dias, hoje, notificacoesNoDia)
        }
    }
}

@Composable
private fun ConteudoBemEstar(dias: List<DiaTela>, hoje: LocalDate, notificacoesNoDia: (LocalDate) -> Int) {
    val cores = MaterialTheme.colorScheme
    var escolhido by rememberSaveable { mutableStateOf(dias.last().dia.toString()) }
    val dia = dias.firstOrNull { it.dia.toString() == escolhido } ?: dias.last()
    var todos by rememberSaveable { mutableStateOf(false) }

    // Número principal: o tempo de tela do dia escolhido.
    Text(
        "Tempo de tela ${nomeCurtoDia(dia.dia, hoje)}",
        style = MaterialTheme.typography.labelLarge,
        color = cores.onSurfaceVariant,
    )
    Text(formatarTempoDeUso(dia.totalMillis), style = MaterialTheme.typography.displaySmall)
    ComparacaoMedia(dia, dias)

    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Indicador("Desbloqueios", dia.desbloqueios?.toString() ?: "—", Modifier.weight(1f))
        Indicador("Aberturas", dia.aberturas.toString(), Modifier.weight(1f))
        Indicador("Notificações", notificacoesNoDia(dia.dia).toString(), Modifier.weight(1f))
    }

    Spacer(Modifier.height(18.dp))
    GraficoSemana(dias, dia.dia, hoje, onEscolher = { escolhido = it.toString() })

    Spacer(Modifier.height(18.dp))
    HorizontalDivider(color = cores.outlineVariant.copy(alpha = 0.6f))
    Spacer(Modifier.height(12.dp))
    Text("Apps mais usados ${nomeCurtoDia(dia.dia, hoje)}", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    if (dia.apps.isEmpty()) {
        Text("Nenhum app usado neste dia.", style = MaterialTheme.typography.bodyMedium, color = cores.onSurfaceVariant)
    } else {
        val maior = dia.apps.first().uso.tempoMillis.coerceAtLeast(1)
        val visiveis = if (todos) dia.apps else dia.apps.take(APPS_VISIVEIS)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visiveis.forEach { app -> LinhaAppTela(app, maior) }
        }
        if (dia.apps.size > APPS_VISIVEIS) {
            TextButton(onClick = { todos = !todos }) {
                Text(if (todos) "Mostrar menos" else "Ver todos (${dia.apps.size})")
            }
        }
    }
}

/** "35 min a mais que a média da semana": compara o dia com a média dos outros dias que tiveram uso. */
@Composable
private fun ComparacaoMedia(dia: DiaTela, dias: List<DiaTela>) {
    val outros = dias.filter { it.dia != dia.dia && it.totalMillis > 0 }
    if (outros.isEmpty()) return
    val media = outros.sumOf { it.totalMillis } / outros.size
    val diferenca = dia.totalMillis - media
    val (icone, texto) = when {
        abs(diferenca) < MARGEM_MEDIA -> Icons.Rounded.TrendingFlat to "Na média da semana (${formatarTempoDeUso(media)})"
        diferenca > 0 -> Icons.Rounded.TrendingUp to "${formatarTempoDeUso(diferenca)} a mais que a média (${formatarTempoDeUso(media)})"
        else -> Icons.Rounded.TrendingDown to "${formatarTempoDeUso(-diferenca)} a menos que a média (${formatarTempoDeUso(media)})"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Colunas dos últimos 7 dias, numa cor só: o dia escolhido em cor cheia, os outros mais claros.
 * Cada coluna inteira é clicável (área maior que a barra); o valor aparece só no dia escolhido.
 */
@Composable
private fun GraficoSemana(dias: List<DiaTela>, escolhido: LocalDate, hoje: LocalDate, onEscolher: (LocalDate) -> Unit) {
    val cores = MaterialTheme.colorScheme
    val maior = dias.maxOf { it.totalMillis }.coerceAtLeast(1)
    Column {
        Row(Modifier.fillMaxWidth().height(132.dp), verticalAlignment = Alignment.Bottom) {
            dias.forEach { d ->
                val selecionado = d.dia == escolhido
                val fracao by animateFloatAsState(d.totalMillis.toFloat() / maior, label = "coluna")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(role = Role.Tab) { onEscolher(d.dia) }
                        .semantics {
                            this.selected = selecionado
                            contentDescription = "${rotuloDia(d.dia, hoje)}: ${formatarTempoDeUso(d.totalMillis)} de tela"
                        },
                ) {
                    if (selecionado) {
                        Text(
                            formatarTempoCurto(d.totalMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = cores.onSurface,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Box(
                        Modifier
                            .widthIn(max = 24.dp)
                            .fillMaxWidth(0.62f)
                            // A área do valor ocupa o topo: a barra usa até ~100 dp.
                            .height((100 * fracao).dp.coerceAtLeast(if (d.totalMillis > 0) 3.dp else 0.dp))
                            .background(
                                if (selecionado) cores.primary else cores.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            ),
                    )
                }
            }
        }
        // Linha de base, discreta.
        Box(Modifier.fillMaxWidth().height(1.dp).background(cores.outlineVariant))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            dias.forEach { d ->
                val selecionado = d.dia == escolhido
                Text(
                    rotuloDia(d.dia, hoje),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selecionado) cores.onSurface else cores.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LinhaAppTela(app: AppTela, maior: Long) {
    val cores = MaterialTheme.colorScheme
    val fracao by animateFloatAsState(app.uso.tempoMillis.toFloat() / maior, label = "barra")
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconeApp(app.pacote, tamanho = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatarTempoDeUso(app.uso.tempoMillis), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(4.dp))
            // Trilho no mesmo tom (bem claro) e a barra proporcional ao app mais usado do dia.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(cores.primary.copy(alpha = 0.12f), CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fracao.coerceIn(0.01f, 1f))
                        .height(6.dp)
                        .background(cores.primary, CircleShape),
                )
            }
        }
    }
}
