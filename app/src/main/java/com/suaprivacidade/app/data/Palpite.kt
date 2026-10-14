package com.suaprivacidade.app.data

/**
 * Qual app provavelmente ligou o microfone ou a câmera. O Android avisa que o sensor ligou, mas
 * esconde o nome do app de quem não é do sistema; então o palpite junta pistas:
 * - o app aberto na tela, se ele tem a permissão do sensor;
 * - senão, apps com serviço em primeiro plano (notificação fixa obrigatória), única forma de um
 *   app usar microfone ou câmera fora da tela desde o Android 11.
 */
data class Palpite(val pacote: String?, val confianca: Confianca, val naTela: Boolean)

fun palpitar(
    naTela: String?,
    comServico: List<String>,
    temPermissao: (String) -> Boolean,
    proprio: String,
): Palpite {
    if (naTela != null && naTela != proprio && temPermissao(naTela)) {
        return Palpite(naTela, Confianca.PROVAVEL, naTela = true)
    }
    val candidatos = comServico.filter { it != proprio && it != naTela && temPermissao(it) }.distinct()
    return when (candidatos.size) {
        0 -> Palpite(null, Confianca.DESCONHECIDO, naTela = false)
        1 -> Palpite(candidatos.single(), Confianca.PROVAVEL, naTela = false)
        else -> Palpite(candidatos.first(), Confianca.POSSIVEL, naTela = false)
    }
}

/** Uso que você não estava vendo: fora da tela, com a tela apagada/bloqueada ou de origem desconhecida. */
fun ehSuspeito(palpite: Palpite, telaLigada: Boolean, bloqueado: Boolean, emLigacao: Boolean): Boolean =
    !emLigacao && !(palpite.naTela && telaLigada && !bloqueado)

/**
 * Quando o uso vira notificação: para qualquer app, só com a tela apagada ou bloqueada; para
 * apps supervisionados, a qualquer uso fora da tela.
 */
fun deveAvisar(uso: UsoSensor, supervisionados: Set<String>): Boolean =
    uso.suspeito && (!uso.telaLigada || uso.bloqueado || uso.pacote in supervisionados)
