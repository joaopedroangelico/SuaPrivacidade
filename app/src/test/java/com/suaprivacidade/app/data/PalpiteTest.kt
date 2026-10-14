package com.suaprivacidade.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PalpiteTest {
    private val proprio = "com.suaprivacidade.app"
    private val comMicrofone = setOf("com.instagram.android", "com.whatsapp", "com.spotify.music")
    private val temPermissao: (String) -> Boolean = { it in comMicrofone }

    @Test
    fun appNaTelaComPermissaoEhProvavel() {
        val p = palpitar("com.whatsapp", listOf("com.spotify.music"), temPermissao, proprio)
        assertEquals(Palpite("com.whatsapp", Confianca.PROVAVEL, naTela = true), p)
    }

    @Test
    fun appNaTelaSemPermissaoCaiParaQuemTemServico() {
        val p = palpitar("com.android.launcher", listOf("com.instagram.android"), temPermissao, proprio)
        assertEquals(Palpite("com.instagram.android", Confianca.PROVAVEL, naTela = false), p)
    }

    @Test
    fun variosComServicoViramPossivel() {
        val p = palpitar(null, listOf("com.instagram.android", "com.spotify.music", "com.instagram.android"), temPermissao, proprio)
        assertEquals(Confianca.POSSIVEL, p.confianca)
        assertEquals("com.instagram.android", p.pacote)
    }

    @Test
    fun ignoraServicosSemPermissaoEOProprioApp() {
        val p = palpitar(null, listOf("com.android.vending", proprio), temPermissao, proprio)
        assertNull(p.pacote)
        assertEquals(Confianca.DESCONHECIDO, p.confianca)
    }

    @Test
    fun usoComOAppAbertoNaoEhSuspeito() {
        val naTela = Palpite("com.whatsapp", Confianca.PROVAVEL, naTela = true)
        assertFalse(ehSuspeito(naTela, telaLigada = true, bloqueado = false, emLigacao = false))
    }

    @Test
    fun usoForaDaTelaOuComTelaApagadaEhSuspeito() {
        val fora = Palpite("com.instagram.android", Confianca.PROVAVEL, naTela = false)
        val desconhecido = Palpite(null, Confianca.DESCONHECIDO, naTela = false)
        assertTrue(ehSuspeito(fora, telaLigada = true, bloqueado = false, emLigacao = false))
        assertTrue(ehSuspeito(desconhecido, telaLigada = false, bloqueado = true, emLigacao = false))
    }

    @Test
    fun ligacaoNuncaEhSuspeita() {
        val desconhecido = Palpite(null, Confianca.DESCONHECIDO, naTela = false)
        assertFalse(ehSuspeito(desconhecido, telaLigada = false, bloqueado = true, emLigacao = true))
    }

    private fun uso(pacote: String?, telaLigada: Boolean, bloqueado: Boolean, suspeito: Boolean = true) = UsoSensor(
        sensor = Sensor.MICROFONE,
        inicioMillis = 0,
        pacote = pacote,
        confianca = Confianca.PROVAVEL,
        appNaTela = false,
        telaLigada = telaLigada,
        bloqueado = bloqueado,
        suspeito = suspeito,
    )

    @Test
    fun avisaComTelaApagadaParaQualquerApp() {
        assertTrue(deveAvisar(uso("com.whatsapp", telaLigada = false, bloqueado = true), emptySet()))
    }

    @Test
    fun comTelaLigadaSoAvisaDeSupervisionados() {
        val instagram = uso("com.instagram.android", telaLigada = true, bloqueado = false)
        assertFalse(deveAvisar(instagram, emptySet()))
        assertTrue(deveAvisar(instagram, setOf("com.instagram.android")))
    }

    @Test
    fun usoNaoSuspeitoNuncaAvisa() {
        assertFalse(deveAvisar(uso("com.instagram.android", telaLigada = false, bloqueado = true, suspeito = false), setOf("com.instagram.android")))
    }
}
