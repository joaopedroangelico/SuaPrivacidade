package com.suaprivacidade.app.vigia

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** O que o Android permite ao app, e os atalhos para as telas de configuração certas. */
object Acessos {
    /** Leitor de notificações: é onde a vigia mora. Sem ele, nada é vigiado. */
    fun temLeitorNotificacoes(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** "Acesso ao uso": qual app está na tela, tempo de uso e internet por app. */
    fun temAcessoUso(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val modo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return modo == AppOpsManager.MODE_ALLOWED
    }

    fun temNotificacoes(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun semEconomiaDeBateria(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    private fun pacote(context: Context) = Uri.parse("package:${context.packageName}")

    fun abrirLeitorNotificacoes(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: vai direto para o interruptor deste app.
            val direto = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, VigiaService::class.java).flattenToString(),
                )
            if (abrir(context, direto)) return
        }
        abrir(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun abrirAcessoUso(context: Context) {
        if (!abrir(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, pacote(context)))) {
            abrir(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    fun abrirNotificacoes(context: Context) = abrir(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )

    fun abrirEconomiaDeBateria(context: Context) {
        val pedido = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pacote(context))
        if (!abrir(context, pedido)) abrir(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    /**
     * "Informações do app". No Android 13+, apps instalados por APK precisam de "Permitir
     * configurações restritas" (menu ⋮ dessa tela) antes de ganhar o leitor de notificações.
     */
    fun abrirInformacoesDoApp(context: Context) =
        abrir(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pacote(context)))

    private fun abrir(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
