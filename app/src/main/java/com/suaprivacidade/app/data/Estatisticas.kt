package com.suaprivacidade.app.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager

/** Tempo de tela e quantas vezes o app foi aberto num período. */
data class UsoApp(val tempoMillis: Long = 0, val aberturas: Int = 0)

/** Bytes de internet (Wi-Fi + dados móveis), com o app aberto e em segundo plano. */
data class DadosApp(val primeiroPlano: Long = 0, val segundoPlano: Long = 0) {
    val total: Long get() = primeiroPlano + segundoPlano
}

// Mesmos valores de ACTIVITY_RESUMED / ACTIVITY_PAUSED (Android 10+), que não existem no Android 8–9.
@Suppress("DEPRECATION")
private const val ABRIU = UsageEvents.Event.MOVE_TO_FOREGROUND
@Suppress("DEPRECATION")
private const val SAIU = UsageEvents.Event.MOVE_TO_BACKGROUND
/** UsageEvents.Event.SCREEN_NON_INTERACTIVE (Android 9+): a tela apagou. */
private const val TELA_APAGOU = 16

/**
 * Tudo aqui depende do "Acesso ao uso". Sem ele o Android lança SecurityException ou devolve
 * listas vazias, e as funções devolvem vazio.
 */
object Estatisticas {
    private const val JANELA_APP_NA_TELA = 6L * 60 * 60 * 1000

    /** App com uma tela aberta agora (o último que abriu e ainda não saiu); null se nenhum ou sem acesso. */
    fun appNaTela(context: Context, agoraMillis: Long = System.currentTimeMillis()): String? = runCatching {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val eventos = usm.queryEvents(agoraMillis - JANELA_APP_NA_TELA, agoraMillis + 1000)
        val e = UsageEvents.Event()
        var atual: String? = null
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(e)
            when (e.eventType) {
                ABRIU -> atual = e.packageName
                SAIU -> if (e.packageName == atual) atual = null
                TELA_APAGOU -> atual = null
            }
        }
        atual
    }.getOrNull()

    /** Tempo de tela e aberturas de cada app entre [desde] e [ate]. */
    fun uso(context: Context, desde: Long, ate: Long): Map<String, UsoApp> = runCatching {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val eventos = usm.queryEvents(desde, ate)
        val e = UsageEvents.Event()
        val tempo = HashMap<String, Long>()
        val aberturas = HashMap<String, Int>()
        var atual: String? = null
        var inicioAtual = 0L
        // Trocar de tela dentro do mesmo app não conta como abrir de novo.
        var ultimoAberto: String? = null

        fun fecharAtual(momento: Long) {
            val pacote = atual ?: return
            tempo[pacote] = (tempo[pacote] ?: 0) + (momento - inicioAtual).coerceAtLeast(0)
            atual = null
        }

        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(e)
            val pacote = e.packageName
            when (e.eventType) {
                ABRIU -> if (pacote != atual) {
                    fecharAtual(e.timeStamp)
                    atual = pacote
                    inicioAtual = e.timeStamp
                    if (pacote != ultimoAberto) aberturas[pacote] = (aberturas[pacote] ?: 0) + 1
                    ultimoAberto = pacote
                }
                SAIU -> if (pacote == atual) fecharAtual(e.timeStamp)
                TELA_APAGOU -> {
                    fecharAtual(e.timeStamp)
                    ultimoAberto = null
                }
            }
        }
        fecharAtual(ate)
        (tempo.keys + aberturas.keys).associateWith { UsoApp(tempo[it] ?: 0, aberturas[it] ?: 0) }
    }.getOrDefault(emptyMap())

    /** Internet gasta por cada app (pelo uid do Android) entre [desde] e [ate]. */
    @Suppress("DEPRECATION")
    fun dados(context: Context, desde: Long, ate: Long): Map<Int, DadosApp> {
        val nsm = context.getSystemService(NetworkStatsManager::class.java)
        val porUid = HashMap<Int, DadosApp>()
        for (rede in listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            // Dados móveis com assinante null: soma todos os chips (o Android 10+ não deixa ler o IMSI).
            val stats = runCatching { nsm.querySummary(rede, null, desde, ate) }.getOrNull() ?: continue
            try {
                val b = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(b)
                    val bytes = b.rxBytes + b.txBytes
                    val atual = porUid[b.uid] ?: DadosApp()
                    porUid[b.uid] = if (b.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                        atual.copy(primeiroPlano = atual.primeiroPlano + bytes)
                    } else {
                        atual.copy(segundoPlano = atual.segundoPlano + bytes)
                    }
                }
            } finally {
                stats.close()
            }
        }
        return porUid
    }
}
