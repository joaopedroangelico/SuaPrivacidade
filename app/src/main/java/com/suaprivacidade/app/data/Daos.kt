package com.suaprivacidade.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UsoDao {
    @Query("SELECT * FROM usos ORDER BY inicioMillis DESC")
    fun observarTodos(): Flow<List<UsoSensor>>

    @Insert
    suspend fun inserir(uso: UsoSensor): Long

    @Query("UPDATE usos SET fimMillis = :fim WHERE id = :id")
    suspend fun fechar(id: Long, fim: Long)

    @Query("UPDATE usos SET silenciado = 1 WHERE id = :id")
    suspend fun marcarSilenciado(id: Long)

    /** Usos que ficaram abertos porque a vigia parou no meio: o fim real é desconhecido. */
    @Query("UPDATE usos SET fimMillis = inicioMillis WHERE fimMillis IS NULL")
    suspend fun fecharInterrompidos()

    @Query("DELETE FROM usos WHERE inicioMillis < :millis")
    suspend fun apagarAntes(millis: Long)

    @Query("DELETE FROM usos")
    suspend fun apagarTudo()
}

@Dao
interface NotificacaoDao {
    @Query("SELECT * FROM notificacoes ORDER BY postadaMillis DESC")
    fun observarTodas(): Flow<List<Notificacao>>

    @Query("SELECT * FROM notificacoes WHERE chave = :chave ORDER BY id DESC LIMIT 1")
    suspend fun ultimaDaChave(chave: String): Notificacao?

    @Insert
    suspend fun inserir(notificacao: Notificacao): Long

    @Query("UPDATE notificacoes SET removidaMillis = :millis WHERE chave = :chave AND removidaMillis IS NULL")
    suspend fun marcarRemovida(chave: String, millis: Long)

    @Query("DELETE FROM notificacoes WHERE postadaMillis < :millis")
    suspend fun apagarAntes(millis: Long)

    @Query("DELETE FROM notificacoes")
    suspend fun apagarTudo()
}

@Dao
interface SupervisionadoDao {
    @Query("SELECT * FROM supervisionados ORDER BY adicionadoMillis")
    fun observarTodos(): Flow<List<Supervisionado>>

    @Query("SELECT * FROM supervisionados")
    suspend fun todos(): List<Supervisionado>

    @Upsert
    suspend fun salvar(supervisionado: Supervisionado)

    @Query("DELETE FROM supervisionados WHERE pacote = :pacote")
    suspend fun excluir(pacote: String)
}

@Dao
interface MudancaDao {
    @Query("SELECT * FROM mudancas_permissao ORDER BY momentoMillis DESC")
    fun observarTodas(): Flow<List<MudancaPermissao>>

    @Query("SELECT * FROM mudancas_permissao WHERE pacote = :pacote")
    suspend fun doPacote(pacote: String): List<MudancaPermissao>

    @Insert
    suspend fun inserirTodas(mudancas: List<MudancaPermissao>)

    @Query("DELETE FROM mudancas_permissao WHERE pacote = :pacote")
    suspend fun excluirDoPacote(pacote: String)

    @Query("DELETE FROM mudancas_permissao WHERE momentoMillis < :millis")
    suspend fun apagarAntes(millis: Long)

    @Query("DELETE FROM mudancas_permissao")
    suspend fun apagarTudo()
}
