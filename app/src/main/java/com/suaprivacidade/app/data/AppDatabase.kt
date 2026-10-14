package com.suaprivacidade.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Arquivo do banco (mais -wal e -shm ao lado), também medido em Ajustes → Armazenamento. */
const val NOME_BANCO = "sua_privacidade.db"

@Database(
    entities = [UsoSensor::class, Notificacao::class, Supervisionado::class, MudancaPermissao::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usoDao(): UsoDao
    abstract fun notificacaoDao(): NotificacaoDao
    abstract fun supervisionadoDao(): SupervisionadoDao
    abstract fun mudancaDao(): MudancaDao

    /** Apaga o que passou do prazo do histórico. */
    suspend fun apagarAntigos(dias: Int, agoraMillis: Long = System.currentTimeMillis()) {
        val limite = agoraMillis - dias * DIA_MILLIS
        usoDao().apagarAntes(limite)
        notificacaoDao().apagarAntes(limite)
        mudancaDao().apagarAntes(limite)
    }

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NOME_BANCO)
                    .build()
                    .also { instancia = it }
            }
    }
}

const val DIA_MILLIS = 24L * 60 * 60 * 1000
