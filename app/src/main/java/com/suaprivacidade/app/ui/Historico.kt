package com.suaprivacidade.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.data.Confianca
import com.suaprivacidade.app.data.Notificacao
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.UsoSensor
import com.suaprivacidade.app.data.contar
import com.suaprivacidade.app.data.formatarDuracao
import com.suaprivacidade.app.data.formatarHora
import com.suaprivacidade.app.data.formatarQuando
import com.suaprivacidade.app.data.nomeDoDia
import com.suaprivacidade.app.ui.theme.aviso
import com.suaprivacidade.app.ui.theme.sucesso
import java.time.LocalDate
import java.time.LocalDateTime

/** Quantas linhas uma gaveta mostra antes do botão "Mostrar mais" (dias com centenas de notificações). */
private const val LIMITE_INICIAL = 40

/** Texto e cor da situação de um uso de microfone/câmera. */
@Composable
fun situacaoDoUso(uso: UsoSensor): Pair<String, Color> {
    val cores = MaterialTheme.colorScheme
    return when {
        uso.fimMillis == null -> "Em uso agora" to cores.error
        uso.emLigacao -> "Em ligação" to cores.onSurfaceVariant
        uso.silenciado -> "Barrado pelo Android" to cores.aviso
        !uso.suspeito -> "Com o app aberto" to cores.sucesso
        !uso.telaLigada -> "Tela apagada" to cores.error
        uso.bloqueado -> "Celular bloqueado" to cores.error
        uso.pacote != null -> "Fora da tela" to cores.error
        else -> "Nenhum app aberto com permissão" to cores.error
    }
}

/** "Microfone · 3 min", "Câmera · ligada há 2 min", acrescido de "palpite incerto" quando for o caso. */
fun descricaoDoUso(uso: UsoSensor, agora: LocalDateTime): String = buildList {
    add(uso.sensor.rotulo)
    val duracao = uso.duracaoMillis()
    add(
        when {
            duracao == null -> "ligado desde ${formatarHora(uso.inicioMillis)}"
            uso.interrompido -> "duração desconhecida"
            else -> formatarDuracao(duracao)
        },
    )
    if (uso.confianca == Confianca.POSSIVEL) add("palpite incerto")
}.joinToString(" · ")

@Composable
fun LinhaUso(uso: UsoSensor, nome: String, agora: LocalDateTime, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val (texto, cor) = situacaoDoUso(uso)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            formatarHora(uso.inicioMillis),
            style = MaterialTheme.typography.titleMedium,
            color = if (uso.suspeito) cores.error else cores.onSurface,
            modifier = Modifier.width(54.dp),
        )
        IconeApp(uso.pacote, tamanho = 36.dp, reserva = uso.sensor.icone)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                descricaoDoUso(uso, agora),
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Etiqueta(texto, cor)
        }
    }
}

@Composable
fun GavetaDiaUsos(
    dia: DiaUsos,
    hoje: LocalDate,
    aberta: Boolean,
    agora: LocalDateTime,
    nome: (String?) -> String,
    onAlternar: () -> Unit,
    onAbrirUso: (UsoSensor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val suspeitos = dia.usos.count { it.suspeito }
    val microfone = dia.usos.count { it.sensor == Sensor.MICROFONE }
    val camera = dia.usos.count { it.sensor == Sensor.CAMERA }
    var limite by remember { mutableIntStateOf(LIMITE_INICIAL) }
    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = suspeitos > 0,
        icone = { IconeRedondo(Icons.Rounded.Event, cores.primary) },
        titulo = nomeDoDia(dia.dia, hoje),
        subtitulo = buildList {
            if (microfone > 0) add("microfone ${microfone}x")
            if (camera > 0) add("câmera ${camera}x")
        }.joinToString(" · "),
        etiqueta = if (suspeitos > 0) {
            { Etiqueta(contar(suspeitos, "uso suspeito", "usos suspeitos"), cores.error) }
        } else {
            { Etiqueta("Nada suspeito", cores.sucesso) }
        },
        lateral = { Contador(dia.usos.size.toString(), if (dia.usos.size == 1) "uso" else "usos") },
        modifier = modifier,
    ) {
        dia.usos.take(limite).forEachIndexed { i, uso ->
            key(uso.id) {
                if (i > 0) Divisoria()
                LinhaUso(uso, nome(uso.pacote), agora, onClick = { onAbrirUso(uso) })
            }
        }
        BotaoMostrarMais(dia.usos.size, limite) { limite += LIMITE_INICIAL }
    }
}

/**
 * Uma notificação do histórico. Fora do modo de seleção, tocar expande o texto e o toque longo
 * entra na seleção já marcando esta; dentro dele, tocar marca/desmarca.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinhaNotificacao(
    n: Notificacao,
    nome: String,
    selecao: Set<Long>?,
    onAlternarSelecao: () -> Unit,
    onEntrarNaSelecao: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    var expandida by remember { mutableStateOf(false) }
    val selecionando = selecao != null
    val marcada = selecao?.contains(n.id) == true
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (marcada) cores.primary.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(
                onClick = {
                    when {
                        selecionando -> onAlternarSelecao()
                        !n.oculta -> expandida = !expandida
                    }
                },
                onLongClick = { if (selecionando) onAlternarSelecao() else onEntrarNaSelecao() },
                onLongClickLabel = "Selecionar",
            )
            .padding(start = 12.dp, end = if (selecionando) 0.dp else 12.dp, top = 10.dp, bottom = 10.dp)
            .animateContentSize(),
    ) {
        Text(
            formatarHora(n.postadaMillis),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(54.dp),
        )
        IconeApp(n.pacote, tamanho = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.labelMedium, color = cores.onSurfaceVariant, maxLines = 1)
            if (n.oculta) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(
                        "Texto oculto",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = cores.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            } else {
                if (!n.titulo.isNullOrBlank()) {
                    Text(
                        n.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = if (expandida) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!n.texto.isNullOrBlank()) {
                    Text(
                        n.texto,
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                        maxLines = if (expandida) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (expandida && n.removidaMillis != null) {
                Text(
                    "Apagada às ${formatarHora(n.removidaMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (selecionando) {
            Checkbox(checked = marcada, onCheckedChange = { onAlternarSelecao() })
        }
    }
}

@Composable
fun GavetaDiaNotificacoes(
    dia: DiaNotificacoes,
    hoje: LocalDate,
    aberta: Boolean,
    nome: (String?) -> String,
    selecao: Set<Long>?,
    onAlternar: () -> Unit,
    onAlternarSelecao: (Long) -> Unit,
    onEntrarNaSelecao: (Long) -> Unit,
    onMarcarDia: (List<Long>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val apps = dia.notificacoes.map { it.pacote }.distinct()
    // Os três apps que mais mandaram notificações no dia.
    val principais = dia.notificacoes.groupingBy { it.pacote }.eachCount()
        .entries.sortedByDescending { it.value }.take(3).joinToString(", ") { nome(it.key) }
    var limite by remember { mutableIntStateOf(LIMITE_INICIAL) }
    val ids = dia.notificacoes.map { it.id }
    val marcadas = selecao?.let { s -> ids.count { it in s } } ?: 0
    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = false,
        icone = { IconeRedondo(Icons.Rounded.Event, cores.primary) },
        titulo = nomeDoDia(dia.dia, hoje),
        subtitulo = if (selecao != null) {
            "$marcadas de ${dia.notificacoes.size} marcadas"
        } else {
            "de ${contar(apps.size, "app", "apps")} · mais: $principais"
        },
        etiqueta = null,
        lateral = {
            if (selecao != null) {
                // Marca ou desmarca o dia inteiro (inclusive as ainda escondidas em "Mostrar mais").
                TriStateCheckbox(
                    state = when (marcadas) {
                        0 -> ToggleableState.Off
                        ids.size -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    },
                    onClick = { onMarcarDia(ids, marcadas < ids.size) },
                    modifier = Modifier.semantics { contentDescription = "Marcar o dia inteiro" },
                )
            } else {
                Contador(dia.notificacoes.size.toString(), if (dia.notificacoes.size == 1) "notificação" else "notificações")
            }
        },
        modifier = modifier,
    ) {
        dia.notificacoes.take(limite).forEachIndexed { i, n ->
            key(n.id) {
                if (i > 0) Divisoria()
                LinhaNotificacao(
                    n = n,
                    nome = nome(n.pacote),
                    selecao = selecao,
                    onAlternarSelecao = { onAlternarSelecao(n.id) },
                    onEntrarNaSelecao = { onEntrarNaSelecao(n.id) },
                )
            }
        }
        BotaoMostrarMais(dia.notificacoes.size, limite) { limite += LIMITE_INICIAL }
    }
}

@Composable
private fun BotaoMostrarMais(total: Int, limite: Int, onMais: () -> Unit) {
    if (total > limite) {
        Divisoria()
        TextButton(onClick = onMais, modifier = Modifier.fillMaxWidth()) {
            Text("Mostrar mais (${total - limite} restantes)")
        }
    }
}

/** Detalhes de um uso de microfone/câmera e o que dá para fazer a partir dele. */
@Composable
fun DialogoUso(
    uso: UsoSensor,
    nome: String,
    agora: LocalDateTime,
    supervisionado: Boolean,
    onSupervisionar: () -> Unit,
    onAbrirApp: () -> Unit,
    onFechar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val (situacao, corSituacao) = situacaoDoUso(uso)
    AlertDialog(
        onDismissRequest = onFechar,
        icon = { IconeApp(uso.pacote, tamanho = 52.dp, reserva = uso.sensor.icone) },
        title = { Text(nome, textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${uso.sensor.rotulo} · ${formatarQuando(uso.inicioMillis, agora.toLocalDate())}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(descricaoDoUso(uso, agora), style = MaterialTheme.typography.bodyMedium, color = cores.onSurfaceVariant)
                Etiqueta(situacao, corSituacao)
                Text(
                    explicacaoDoPalpite(uso),
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (uso.pacote != null) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onAbrirApp, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Permissões do app")
                    }
                    if (!supervisionado) {
                        OutlinedButton(onClick = onSupervisionar, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Supervisionar este app")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
    )
}

private fun explicacaoDoPalpite(uso: UsoSensor): String = when {
    uso.pacote == null ->
        "O Android não diz qual app usou o sensor, e nenhum app aberto ou rodando em segundo plano tinha a permissão. " +
            "Pode ter sido o próprio sistema (ex.: assistente de voz)."
    uso.appNaTela -> "O app estava aberto na tela e tem a permissão, por isso é o provável responsável."
    uso.confianca == Confianca.PROVAVEL ->
        "O app estava rodando em segundo plano (com notificação fixa) e é o único com a permissão nessa situação."
    else -> "Mais de um app rodava em segundo plano com a permissão; este é um dos possíveis."
}

@Composable
fun EstadoVazioHistorico(tipo: TipoHistorico, filtrado: Boolean, modifier: Modifier = Modifier) {
    when {
        filtrado -> EstadoVazio(Icons.Rounded.EventBusy, "Nada deste app", "Nenhum registro dele no período guardado.", modifier)
        tipo == TipoHistorico.SENSORES -> EstadoVazio(
            Icons.Rounded.EventBusy,
            "Nenhum uso registrado",
            "Cada vez que algum app ligar o microfone ou a câmera, aparece aqui, dia a dia.",
            modifier,
        )
        else -> EstadoVazio(
            Icons.Rounded.EventBusy,
            "Nenhuma notificação ainda",
            "As notificações que chegarem a partir de agora ficam guardadas aqui.",
            modifier,
        )
    }
}
