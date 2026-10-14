package com.suaprivacidade.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Sensor(val rotulo: String) { MICROFONE("Microfone"), CAMERA("Câmera") }

/** Quão certo é o palpite de qual app usou o sensor (o Android não diz o nome). */
enum class Confianca { PROVAVEL, POSSIVEL, DESCONHECIDO }

/** Um período em que o microfone ou a câmera ficaram ligados. */
@Entity(tableName = "usos", indices = [Index("inicioMillis"), Index("pacote")])
data class UsoSensor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensor: Sensor,
    val inicioMillis: Long,
    /** Null enquanto o sensor está ligado. Igual ao início quando a vigia foi interrompida no meio. */
    val fimMillis: Long? = null,
    /** Palpite do app responsável; null quando não deu para saber. */
    val pacote: String?,
    val confianca: Confianca,
    /** O app do palpite estava aberto na tela quando o uso começou. */
    val appNaTela: Boolean,
    val telaLigada: Boolean,
    val bloqueado: Boolean,
    /** Ligação telefônica ou chamada de voz/vídeo em andamento. */
    val emLigacao: Boolean = false,
    /** O Android entregou silêncio ao app (ele tentou gravar em segundo plano e foi barrado). */
    val silenciado: Boolean = false,
    /** Uso fora da tela, com a tela apagada/bloqueada ou de origem desconhecida. */
    val suspeito: Boolean,
)

/** Uma notificação recebida (cada mudança de conteúdo numa mesma notificação vira uma linha). */
@Entity(tableName = "notificacoes", indices = [Index("postadaMillis"), Index("pacote"), Index("chave")])
data class Notificacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacote: String,
    /** Identificador do Android para a notificação; atualizações chegam com a mesma chave. */
    val chave: String,
    val titulo: String?,
    val texto: String?,
    /** O app está na lista de "texto oculto": só o app e o horário foram guardados. */
    val oculta: Boolean,
    val postadaMillis: Long,
    val removidaMillis: Long? = null,
)

/** App sob supervisão, com as permissões sensíveis que ele tinha na última verificação. */
@Entity(tableName = "supervisionados")
data class Supervisionado(
    @PrimaryKey val pacote: String,
    val adicionadoMillis: Long,
    /** Permissões sensíveis concedidas, separadas por vírgula. */
    val permissoes: String,
) {
    val listaPermissoes: Set<String> get() = permissoes.split(',').filter { it.isNotBlank() }.toSet()
}

/** Um app supervisionado ganhou ou perdeu uma permissão sensível. */
@Entity(tableName = "mudancas_permissao", indices = [Index("pacote")])
data class MudancaPermissao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacote: String,
    val permissao: String,
    val concedida: Boolean,
    val momentoMillis: Long,
)
