package com.suaprivacidade.app.data

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")
private val formatoDataCurta = DateTimeFormatter.ofPattern("dd/MM", LocaleBR)
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", LocaleBR)
private val formatoDiaLongo = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LocaleBR)

fun Long.emDataHora(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun LocalDate.inicioMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatarDataCurta(data: LocalDate): String = formatoDataCurta.format(data)

fun formatarHora(momento: LocalDateTime): String = formatoHora.format(momento)

fun formatarHora(millis: Long): String = formatarHora(millis.emDataHora())

/** "Terça-feira, 13 de outubro". */
fun formatarDiaLongo(data: LocalDate): String =
    formatoDiaLongo.format(data).replaceFirstChar { it.titlecase(LocaleBR) }

/** "Hoje", "Ontem" ou "Sexta-feira, 9 de outubro". */
fun nomeDoDia(data: LocalDate, hoje: LocalDate): String = when (data) {
    hoje -> "Hoje"
    hoje.minusDays(1) -> "Ontem"
    else -> formatarDiaLongo(data)
}

/** "hoje às 15:00", "ontem às 07:00", "16/10 às 07:00". */
fun formatarQuando(millis: Long, hoje: LocalDate): String {
    val momento = millis.emDataHora()
    return when (momento.toLocalDate()) {
        hoje -> "hoje às ${formatarHora(momento)}"
        hoje.minusDays(1) -> "ontem às ${formatarHora(momento)}"
        else -> "${formatarDataCurta(momento.toLocalDate())} às ${formatarHora(momento)}"
    }
}

/** Duração de um uso de microfone/câmera: "menos de 1 s", "45 s", "3 min", "1h 20min". */
fun formatarDuracao(millis: Long): String {
    val segundos = millis / 1000
    return when {
        segundos < 1 -> "menos de 1 s"
        segundos < 60 -> "$segundos s"
        segundos < 3600 -> "${segundos / 60} min"
        else -> {
            val h = segundos / 3600
            val m = (segundos % 3600) / 60
            if (m == 0L) "${h}h" else "${h}h ${m}min"
        }
    }
}

/** Tempo de tela: "0 min", "12 min", "2h 05min". */
fun formatarTempoDeUso(millis: Long): String {
    val minutos = millis / 60_000
    return when {
        minutos < 60 -> "$minutos min"
        else -> "${minutos / 60}h %02dmin".format(minutos % 60)
    }
}

/** Tempo de tela curto, para caber em cima de uma coluna de gráfico: "45m", "1h28", "6h". */
fun formatarTempoCurto(millis: Long): String {
    val minutos = millis / 60_000
    return when {
        minutos < 60 -> "${minutos}m"
        minutos % 60 == 0L -> "${minutos / 60}h"
        else -> "${minutos / 60}h%02d".format(minutos % 60)
    }
}

/** Dados de internet: "0 KB", "820 KB", "45,3 MB", "1,2 GB". */
fun formatarBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    fun numero(valor: Double, casas: Int) =
        NumberFormat.getNumberInstance(LocaleBR).apply { maximumFractionDigits = casas }.format(valor)
    return when {
        mb < 1 -> "${numero(kb, 0)} KB"
        gb < 1 -> "${numero(mb, if (mb < 100) 1 else 0)} MB"
        else -> "${numero(gb, 1)} GB"
    }
}

fun contar(n: Int, singular: String, plural: String) = "$n ${if (n == 1) singular else plural}"
