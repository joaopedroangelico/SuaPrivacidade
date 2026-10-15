package com.suaprivacidade.app.vigia

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.suaprivacidade.app.data.PreferenciasRepo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Leitor de notificações. Além de guardar o histórico, ele hospeda a [Vigia]: o Android mantém
 * este serviço ligado enquanto o acesso estiver concedido, e o religa sozinho depois de reiniciar.
 */
class VigiaService : NotificationListenerService() {
    private var vigia: Vigia? = null
    private val escopo = MainScope()

    override fun onListenerConnected() {
        escopo.launch {
            // Pausada nos Ajustes: o Android religa o leitor (ex.: ao reiniciar), e ele se desliga.
            if (PreferenciasRepo(applicationContext).preferencias.first().vigiaPausada) {
                desligar()
            } else {
                vigia?.parar()
                vigia = Vigia(this@VigiaService).also { it.iniciar() }
            }
        }
    }

    /** Pede ao Android para soltar o leitor; ele só volta com [religar]. */
    fun desligar() {
        runCatching { requestUnbind() }
    }

    override fun onListenerDisconnected() {
        vigia?.parar()
        vigia = null
    }

    override fun onDestroy() {
        vigia?.parar()
        vigia = null
        escopo.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        vigia?.notificacaoPostada(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        vigia?.notificacaoRemovida(sbn)
    }

    /** Apps com serviço em primeiro plano agora (cada um precisa mostrar uma notificação fixa). */
    fun pacotesComServico(): List<String> = runCatching {
        activeNotifications.orEmpty()
            .filter { it.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0 }
            .map { it.packageName }
    }.getOrDefault(emptyList())

    companion object {
        /** Pede ao Android para religar o leitor (ex.: o celular encerrou o app para poupar bateria). */
        fun religar(context: Context) {
            runCatching { requestRebind(ComponentName(context, VigiaService::class.java)) }
        }
    }
}
