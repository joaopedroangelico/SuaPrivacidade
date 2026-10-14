package com.suaprivacidade.app.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** O celular tem PIN, padrão ou senha de bloqueio (sem isso não há o que pedir). */
fun temSenhaNoCelular(context: Context): Boolean =
    context.getSystemService(KeyguardManager::class.java).isDeviceSecure

/**
 * Pede a senha de bloqueio do celular (PIN, padrão ou senha; digital não vale) antes de uma ação
 * que apaga dados. Devolve uma função: `pedir(motivo) { ação }`, e a ação só roda se a senha
 * for confirmada. Celular sem bloqueio de tela: a ação roda direto (a confirmação do app já veio antes).
 *
 * Android 11+: tela de verificação do sistema só com a credencial do aparelho.
 * Android 8–10: tela de confirmação do bloqueio de tela (KeyguardManager).
 */
@Composable
fun lembrarPedidoSenha(): (motivo: String, acao: () -> Unit) -> Unit {
    val context = LocalContext.current
    val pendente = remember { arrayOfNulls<() -> Unit>(1) }
    val telaBloqueio = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        val acao = pendente[0]
        pendente[0] = null
        if (resultado.resultCode == Activity.RESULT_OK) acao?.invoke()
    }
    return remember(context) {
        { motivo, acao ->
            when {
                !temSenhaNoCelular(context) -> acao()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> pedirComPrompt(context, motivo, acao)
                else -> {
                    @Suppress("DEPRECATION")
                    val intent = context.getSystemService(KeyguardManager::class.java)
                        .createConfirmDeviceCredentialIntent("Confirme que é você", motivo)
                    if (intent == null) {
                        acao()
                    } else {
                        pendente[0] = acao
                        telaBloqueio.launch(intent)
                    }
                }
            }
        }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
private fun pedirComPrompt(context: Context, motivo: String, acao: () -> Unit) {
    val prompt = BiometricPrompt.Builder(context)
        .setTitle("Confirme que é você")
        .setSubtitle(motivo)
        .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(
        CancellationSignal(),
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = acao()
        },
    )
}
