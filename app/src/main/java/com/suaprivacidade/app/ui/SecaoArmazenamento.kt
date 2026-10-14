package com.suaprivacidade.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.data.Armazenamento
import com.suaprivacidade.app.data.formatarGB
import com.suaprivacidade.app.data.formatarMB
import com.suaprivacidade.app.data.formatarPorcentagem

/** Ajustes → Armazenamento: espaço livre do celular, quanto o app ocupa e quanto disso são as informações. */
@Composable
fun SecaoArmazenamento(vm: PrivacidadeViewModel) {
    var armazenamento by remember { mutableStateOf<Armazenamento?>(null) }
    LaunchedEffect(Unit) { armazenamento = vm.armazenamento() }
    armazenamento?.let { BlocoArmazenamento(it) }
}

@Composable
private fun BlocoArmazenamento(a: Armazenamento) {
    val cores = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(cores.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Storage, contentDescription = null, tint = cores.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text("Armazenamento", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinhaEspaco(
                "Espaço disponível no celular",
                formatarGB(a.livreCelular),
                "${formatarPorcentagem(a.livreCelular, a.totalCelular)} livre de ${formatarGB(a.totalCelular)}",
            )
            LinearProgressIndicator(
                progress = { if (a.totalCelular > 0) 1f - a.livreCelular.toFloat() / a.totalCelular else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                drawStopIndicator = {},
            )
        }
        LinhaEspaco("O app ocupa", formatarMB(a.app), "${formatarPorcentagem(a.app, a.totalCelular)} do celular")
        LinhaEspaco("Suas informações ocupam", formatarMB(a.dados), "${formatarPorcentagem(a.dados, a.app)} do app")
    }
}

@Composable
private fun LinhaEspaco(rotulo: String, valor: String, detalhe: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(valor, style = MaterialTheme.typography.titleSmall)
            Text(detalhe, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
