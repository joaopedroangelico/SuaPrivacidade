package com.suaprivacidade.app.ui

import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.suaprivacidade.app.data.Apps
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.ui.theme.corDoSensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Sensor.icone: ImageVector
    get() = when (this) {
        Sensor.MICROFONE -> Icons.Rounded.Mic
        Sensor.CAMERA -> Icons.Rounded.Videocam
    }

@Composable
fun IconeSensor(sensor: Sensor, modifier: Modifier = Modifier, tamanho: Dp = 44.dp) {
    IconeRedondo(sensor.icone, MaterialTheme.colorScheme.corDoSensor(sensor), modifier, tamanho)
}

/** Ícone redondo genérico para cabeçalhos de gaveta e linhas sem app. */
@Composable
fun IconeRedondo(icone: ImageVector, cor: Color, modifier: Modifier = Modifier, tamanho: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(tamanho)
            .background(cor.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(tamanho * 0.5f))
    }
}

private val cacheIcones = LruCache<String, ImageBitmap>(150)
private const val PX_ICONE = 128

/** Ícone do app instalado; se não houver (desinstalado ou pacote null), um ícone genérico. */
@Composable
fun IconeApp(pacote: String?, modifier: Modifier = Modifier, tamanho: Dp = 44.dp, reserva: ImageVector = Icons.Rounded.Apps) {
    val context = LocalContext.current
    val bitmap by produceState(pacote?.let { cacheIcones.get(it) }, pacote) {
        if (value == null && pacote != null) {
            value = withContext(Dispatchers.IO) {
                Apps.icone(context, pacote)?.toBitmap(PX_ICONE, PX_ICONE)?.asImageBitmap()?.also { cacheIcones.put(pacote, it) }
            }
        }
    }
    val atual = bitmap
    if (atual != null) {
        Image(atual, contentDescription = null, modifier = modifier.size(tamanho))
    } else {
        IconeRedondo(reserva, MaterialTheme.colorScheme.onSurfaceVariant, modifier, tamanho)
    }
}

/** Cabeçalho clicável com resumo; ao abrir, mostra [conteudo] num painel embutido. */
@Composable
internal fun GavetaBase(
    aberta: Boolean,
    onAlternar: () -> Unit,
    alerta: Boolean,
    icone: @Composable () -> Unit,
    titulo: String,
    subtitulo: String,
    etiqueta: (@Composable () -> Unit)?,
    lateral: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val rotacao by animateFloatAsState(if (aberta) 180f else 0f, label = "seta")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cores.surfaceContainerLow,
        border = if (alerta) BorderStroke(1.dp, cores.error.copy(alpha = 0.45f)) else null,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (aberta) "Recolher" else "Expandir", onClick = onAlternar)
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            ) {
                icone()
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (etiqueta != null) {
                        Spacer(Modifier.height(6.dp))
                        etiqueta()
                    }
                }
                Spacer(Modifier.width(8.dp))
                lateral()
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = cores.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .rotate(rotacao),
                )
            }

            AnimatedVisibility(
                visible = aberta,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        .background(cores.surface, RoundedCornerShape(18.dp))
                        .padding(vertical = 4.dp),
                    content = conteudo,
                )
            }
        }
    }
}

@Composable
internal fun Divisoria() {
    HorizontalDivider(
        Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
internal fun Etiqueta(texto: String, cor: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = cor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(cor.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Número grande à direita do cabeçalho de uma gaveta, com a legenda embaixo. */
@Composable
internal fun Contador(valor: String, legenda: String, cor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.End) {
        Text(valor, style = MaterialTheme.typography.titleMedium, color = cor)
        Text(legenda, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun EstadoVazio(icone: ImageVector, titulo: String, texto: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Título de uma seção dentro de uma aba ou folha, com ícone colorido. */
@Composable
internal fun TituloSecao(icone: ImageVector, titulo: String, cor: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Bloco clicável de cartão (fundo levemente destacado), usado em listas simples. */
@Composable
internal fun Cartao(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
            content = conteudo,
        )
    }
}
