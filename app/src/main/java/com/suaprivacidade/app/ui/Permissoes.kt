package com.suaprivacidade.app.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.suaprivacidade.app.ui.theme.aviso
import com.suaprivacidade.app.ui.theme.sucesso
import com.suaprivacidade.app.vigia.Acessos

/** O que o Android permite ao app. O leitor de notificações é essencial: é onde a vigia roda. */
data class EstadoPermissoes(
    val leitor: Boolean = true,
    val acessoUso: Boolean = true,
    val notificacoes: Boolean = true,
    val bateria: Boolean = true,
) {
    val faltaAlgo: Boolean get() = !leitor || !acessoUso || !notificacoes || !bateria
}

private fun lerPermissoes(context: Context) = EstadoPermissoes(
    leitor = Acessos.temLeitorNotificacoes(context),
    acessoUso = Acessos.temAcessoUso(context),
    notificacoes = Acessos.temNotificacoes(context),
    bateria = Acessos.semEconomiaDeBateria(context),
)

/** Estado das permissões, relido sempre que o app volta das configurações do sistema. */
@Composable
fun lembrarPermissoes(): EstadoPermissoes {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(lerPermissoes(context)) }
    LifecycleResumeEffect(Unit) {
        estado = lerPermissoes(context)
        onPauseOrDispose {}
    }
    return estado
}

/**
 * Pede a permissão de notificações (Android 13+); se o usuário já recusou antes, o Android não
 * pergunta de novo, então abre a tela de notificações do app.
 */
@Composable
fun lembrarPedidoNotificacoes(): () -> Unit {
    val context = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (!concedida) Acessos.abrirNotificacoes(context)
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Acessos.abrirNotificacoes(context)
        }
    }
}

/** Cartão da tela inicial: aparece só quando falta alguma permissão. */
@Composable
fun CartaoPermissoes(estado: EstadoPermissoes, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val grave = !estado.leitor
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (grave) cores.errorContainer else cores.aviso.copy(alpha = 0.12f),
                RoundedCornerShape(24.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (grave) cores.onErrorContainer else cores.aviso,
                modifier = Modifier.size(20.dp),
            )
            Text(
                if (grave) "A vigia está desligada" else "A vigia está incompleta",
                style = MaterialTheme.typography.titleMedium,
                color = if (grave) cores.onErrorContainer else cores.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        ListaPermissoes(estado, mostrarConcedidas = false)
    }
}

/** Uma linha por permissão, com o botão que leva à tela certa do Android. */
@Composable
fun ListaPermissoes(estado: EstadoPermissoes, mostrarConcedidas: Boolean = true) {
    val context = LocalContext.current
    val pedirNotificacoes = lembrarPedidoNotificacoes()
    var ajudaRestrita by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (mostrarConcedidas || !estado.leitor) {
            LinhaPermissao(
                icone = Icons.Rounded.Visibility,
                titulo = "Acesso a notificações",
                texto = "É por ele que o app percebe o microfone, a câmera e guarda as notificações.",
                concedida = estado.leitor,
                onPermitir = { Acessos.abrirLeitorNotificacoes(context) },
            )
            if (!estado.leitor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                TextButton(
                    onClick = { ajudaRestrita = true },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.padding(start = 26.dp),
                ) {
                    Text("Aparece “configuração restrita”?")
                }
            }
        }
        if (mostrarConcedidas || !estado.acessoUso) {
            LinhaPermissao(
                icone = Icons.Rounded.QueryStats,
                titulo = "Acesso ao uso",
                texto = "Para saber qual app estava aberto, o tempo de uso e a internet gasta por app.",
                concedida = estado.acessoUso,
                onPermitir = { Acessos.abrirAcessoUso(context) },
            )
        }
        if (mostrarConcedidas || !estado.notificacoes) {
            LinhaPermissao(
                icone = Icons.Rounded.NotificationsActive,
                titulo = "Notificações",
                texto = "Para avisar na hora em que o microfone ou a câmera ligarem.",
                concedida = estado.notificacoes,
                onPermitir = pedirNotificacoes,
            )
        }
        if (mostrarConcedidas || !estado.bateria) {
            LinhaPermissao(
                icone = Icons.Rounded.BatterySaver,
                titulo = "Sem restrição de bateria",
                texto = "Alguns celulares (Xiaomi, Samsung, Motorola) encerram a vigia para economizar bateria.",
                concedida = estado.bateria,
                onPermitir = { Acessos.abrirEconomiaDeBateria(context) },
            )
        }
    }
    if (ajudaRestrita) {
        AlertDialog(
            onDismissRequest = { ajudaRestrita = false },
            icon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
            title = { Text("Configuração restrita") },
            text = {
                Text(
                    "O Android bloqueia esse acesso para apps instalados por APK até você liberar. " +
                        "Em “Informações do app”, toque no menu ⋮ (canto superior direito), escolha " +
                        "“Permitir configurações restritas” e volte para ligar o acesso a notificações.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ajudaRestrita = false
                    Acessos.abrirInformacoesDoApp(context)
                }) { Text("Abrir informações do app") }
            },
            dismissButton = { TextButton(onClick = { ajudaRestrita = false }) { Text("Fechar") } },
        )
    }
}

@Composable
private fun LinhaPermissao(
    icone: ImageVector,
    titulo: String,
    texto: String,
    concedida: Boolean,
    onPermitir: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (concedida) Icons.Rounded.CheckCircle else icone,
            contentDescription = null,
            tint = if (concedida) cores.sucesso else cores.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = cores.onSurfaceVariant)
        }
        if (!concedida) {
            FilledTonalButton(onClick = onPermitir) { Text("Permitir") }
        }
    }
}
