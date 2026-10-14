package com.suaprivacidade.app.vigia

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suaprivacidade.app.MainActivity
import com.suaprivacidade.app.R
import com.suaprivacidade.app.data.Confianca
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.UsoSensor
import com.suaprivacidade.app.data.rotuloPermissao

/** Notificações do próprio app: uso suspeito de microfone/câmera e permissões novas. */
object Alertas {
    private const val CANAL_SENSORES = "sensores"
    private const val CANAL_PERMISSOES = "permissoes"

    /** Extra do toque na notificação: qual aba abrir (nome de [com.suaprivacidade.app.ui.Aba]). */
    const val EXTRA_ABA = "aba"

    fun criarCanais(context: Context) {
        val gerenciador = NotificationManagerCompat.from(context)
        gerenciador.createNotificationChannel(
            NotificationChannelCompat.Builder(CANAL_SENSORES, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Microfone e câmera")
                .setDescription("Uso do microfone ou da câmera com a tela apagada ou por apps supervisionados")
                .build(),
        )
        gerenciador.createNotificationChannel(
            NotificationChannelCompat.Builder(CANAL_PERMISSOES, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Permissões novas")
                .setDescription("Um app supervisionado ganhou uma permissão sensível")
                .build(),
        )
    }

    fun usoSuspeito(context: Context, uso: UsoSensor, nomeApp: String?) {
        val ligado = if (uso.sensor == Sensor.MICROFONE) "Microfone ligado" else "Câmera ligada"
        val titulo = when {
            !uso.telaLigada -> "$ligado com a tela apagada"
            uso.bloqueado -> "$ligado com o celular bloqueado"
            else -> "$ligado em segundo plano"
        }
        val texto = when {
            nomeApp == null -> "Não deu para saber qual app foi. Veja no histórico."
            uso.confianca == Confianca.PROVAVEL -> "Provavelmente pelo app $nomeApp."
            else -> "Possivelmente pelo app $nomeApp."
        }
        notificar(context, CANAL_SENSORES, (uso.id % Int.MAX_VALUE).toInt(), titulo, texto, "HISTORICO")
    }

    fun permissaoNova(context: Context, pacote: String, nomeApp: String, permissoes: List<String>) {
        val nomes = permissoes.joinToString(", ") { rotuloPermissao(it) }
        notificar(
            context,
            CANAL_PERMISSOES,
            pacote.hashCode(),
            "$nomeApp ganhou ${if (permissoes.size == 1) "uma permissão nova" else "permissões novas"}",
            nomes,
            "SUPERVISAO",
        )
    }

    @SuppressLint("MissingPermission")
    private fun notificar(context: Context, canal: String, id: Int, titulo: String, texto: String, aba: String) {
        val gerenciador = NotificationManagerCompat.from(context)
        if (!gerenciador.areNotificationsEnabled()) return
        val abrir = PendingIntent.getActivity(
            context,
            aba.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ABA, aba)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacao = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        runCatching { gerenciador.notify(id, notificacao) }
    }
}
