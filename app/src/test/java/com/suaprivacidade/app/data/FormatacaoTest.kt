package com.suaprivacidade.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatacaoTest {
    @Test
    fun duracao() {
        assertEquals("menos de 1 s", formatarDuracao(400))
        assertEquals("45 s", formatarDuracao(45_000))
        assertEquals("3 min", formatarDuracao(3 * 60_000 + 20_000))
        assertEquals("1h", formatarDuracao(60 * 60_000))
        assertEquals("1h 20min", formatarDuracao(80 * 60_000))
    }

    @Test
    fun tempoDeUso() {
        assertEquals("0 min", formatarTempoDeUso(30_000))
        assertEquals("59 min", formatarTempoDeUso(59 * 60_000))
        assertEquals("2h 05min", formatarTempoDeUso(125 * 60_000))
    }

    @Test
    fun tempoCurto() {
        assertEquals("45m", formatarTempoCurto(45 * 60_000))
        assertEquals("1h28", formatarTempoCurto(88 * 60_000))
        assertEquals("6h", formatarTempoCurto(6 * 60 * 60_000))
        assertEquals("2h05", formatarTempoCurto(125 * 60_000))
    }

    @Test
    fun bytes() {
        assertEquals("0 KB", formatarBytes(0))
        assertEquals("820 KB", formatarBytes(820 * 1024))
        assertEquals("45,3 MB", formatarBytes((45.3 * 1024 * 1024).toLong()))
        assertEquals("250 MB", formatarBytes(250L * 1024 * 1024))
        assertEquals("1,2 GB", formatarBytes((1.2 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun dias() {
        val hoje = LocalDate.of(2026, 10, 14)
        assertEquals("Hoje", nomeDoDia(hoje, hoje))
        assertEquals("Ontem", nomeDoDia(hoje.minusDays(1), hoje))
        assertEquals("Segunda-feira, 12 de outubro", nomeDoDia(hoje.minusDays(2), hoje))
    }

    @Test
    fun quando() {
        val hoje = LocalDate.of(2026, 10, 14)
        val millis = hoje.inicioMillis() + (14 * 60 + 5) * 60_000L
        assertEquals("hoje às 14:05", formatarQuando(millis, hoje))
        assertEquals("ontem às 14:05", formatarQuando(millis, hoje.plusDays(1)))
        assertEquals("14/10 às 14:05", formatarQuando(millis, hoje.plusDays(5)))
    }
}
