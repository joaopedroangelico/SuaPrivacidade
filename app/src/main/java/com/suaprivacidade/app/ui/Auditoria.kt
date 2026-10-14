package com.suaprivacidade.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.VideoCameraFront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.data.InfoApp
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.UsoSensor
import com.suaprivacidade.app.data.contar
import com.suaprivacidade.app.data.formatarQuando
import com.suaprivacidade.app.ui.theme.corDoSensor
import java.time.LocalDate

/** Apps de um grupo da Auditoria ("AMBOS", "MICROFONE" ou "CAMERA"). */
fun appsDoGrupo(grupo: String, apps: List<InfoApp>): List<InfoApp> = when (grupo) {
    "AMBOS" -> apps.filter { it.microfone && it.camera }
    "MICROFONE" -> apps.filter { it.microfone && !it.camera }
    else -> apps.filter { it.camera && !it.microfone }
}

private fun tituloGrupo(grupo: String) = when (grupo) {
    "AMBOS" -> "Microfone e câmera"
    "MICROFONE" -> "Só microfone"
    else -> "Só câmera"
}

@Composable
private fun iconeGrupo(grupo: String): Pair<ImageVector, androidx.compose.ui.graphics.Color> {
    val cores = MaterialTheme.colorScheme
    return when (grupo) {
        "AMBOS" -> Icons.Rounded.VideoCameraFront to cores.error
        "MICROFONE" -> Icons.Rounded.Mic to cores.corDoSensor(Sensor.MICROFONE)
        else -> Icons.Rounded.PhotoCamera to cores.corDoSensor(Sensor.CAMERA)
    }
}

/** Dica no topo da Auditoria: o que fazer com o que aparece aqui. */
@Composable
fun CartaoDicaAuditoria(modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(cores.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = cores.primary, modifier = Modifier.size(20.dp))
            Text("Quem pode ligar o quê", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "Estes apps têm permissão de microfone ou câmera agora. Para os que você usa pouco, toque no app e " +
                "troque para “Perguntar sempre” ou “Não permitir”: o Android passa a pedir sua autorização a cada uso.",
            style = MaterialTheme.typography.bodyMedium,
            color = cores.onSurfaceVariant,
        )
    }
}

@Composable
fun GavetaAuditoria(
    grupo: String,
    apps: List<InfoApp>,
    /** Último uso registrado de cada app (qualquer sensor). */
    ultimoUso: Map<String, UsoSensor>,
    hoje: LocalDate,
    aberta: Boolean,
    onAlternar: () -> Unit,
    onAbrirApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val (icone, cor) = iconeGrupo(grupo)
    val usados = apps.count { it.pacote in ultimoUso }
    GavetaBase(
        aberta = aberta,
        onAlternar = onAlternar,
        alerta = false,
        icone = { IconeRedondo(icone, cor) },
        titulo = tituloGrupo(grupo),
        subtitulo = if (usados > 0) "${contar(usados, "app usou", "apps usaram")} no período guardado" else "Nenhum uso registrado",
        etiqueta = null,
        lateral = { Contador(apps.size.toString(), if (apps.size == 1) "app" else "apps") },
        modifier = modifier,
    ) {
        apps.forEachIndexed { i, app ->
            key(app.pacote) {
                if (i > 0) Divisoria()
                LinhaAuditoria(app, ultimoUso[app.pacote], hoje, onClick = { onAbrirApp(app.pacote) })
            }
        }
    }
}

@Composable
private fun LinhaAuditoria(app: InfoApp, ultimo: UsoSensor?, hoje: LocalDate, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClickLabel = "Abrir permissões", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        IconeApp(app.pacote, tamanho = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.nome, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (ultimo != null) {
                Spacer(Modifier.height(4.dp))
                Etiqueta(
                    "Usou ${if (ultimo.sensor == Sensor.MICROFONE) "o microfone" else "a câmera"} ${formatarQuando(ultimo.inicioMillis, hoje)}",
                    if (ultimo.suspeito) cores.error else cores.onSurfaceVariant,
                )
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = cores.onSurfaceVariant)
    }
}
