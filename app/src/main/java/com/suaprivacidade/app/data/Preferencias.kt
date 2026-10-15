package com.suaprivacidade.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Tema(val rotulo: String) {
    SISTEMA("Sistema"),
    LAVANDA("Lavanda"),
    MENTA("Menta"),
    PESSEGO("Pêssego"),
    OCEANO("Oceano"),
    ROSE("Rosé"),
}

data class Preferencias(
    val tema: Tema = Tema.OCEANO,
    /** Por quantos dias o histórico (microfone, câmera, notificações) fica guardado. */
    val diasHistorico: Int = DIAS_HISTORICO_PADRAO,
    /** Apps cujas notificações entram no histórico sem título nem texto (ex.: banco). */
    val textoOculto: Set<String> = emptySet(),
    /** O usuário já leu o que o app consegue (e não consegue) fazer. */
    val avisoLido: Boolean = false,
    /** O usuário desligou a vigia nos Ajustes: ela não religa sozinha (nem ao reiniciar o celular). */
    val vigiaPausada: Boolean = false,
)

const val DIAS_HISTORICO_PADRAO = 30
val OPCOES_DIAS_HISTORICO = listOf(7, 30, 90)

private val Context.dataStore by preferencesDataStore(name = "preferencias")

class PreferenciasRepo(private val context: Context) {
    private val chaveTema = stringPreferencesKey("tema")
    private val chaveDias = intPreferencesKey("dias_historico")
    private val chaveOculto = stringSetPreferencesKey("texto_oculto")
    private val chaveAviso = booleanPreferencesKey("aviso_lido")
    private val chavePausada = booleanPreferencesKey("vigia_pausada")

    val preferencias: Flow<Preferencias> = context.dataStore.data.map { p ->
        Preferencias(
            tema = p[chaveTema]?.let { nome -> Tema.entries.firstOrNull { it.name == nome } } ?: Tema.OCEANO,
            diasHistorico = p[chaveDias] ?: DIAS_HISTORICO_PADRAO,
            textoOculto = p[chaveOculto].orEmpty(),
            avisoLido = p[chaveAviso] ?: false,
            vigiaPausada = p[chavePausada] ?: false,
        )
    }

    suspend fun definirTema(tema: Tema) {
        context.dataStore.edit { it[chaveTema] = tema.name }
    }

    suspend fun definirDiasHistorico(dias: Int) {
        context.dataStore.edit { it[chaveDias] = dias }
    }

    suspend fun alternarTextoOculto(pacote: String) {
        context.dataStore.edit {
            val atual = it[chaveOculto].orEmpty()
            it[chaveOculto] = if (pacote in atual) atual - pacote else atual + pacote
        }
    }

    suspend fun definirVigiaPausada(pausada: Boolean) {
        context.dataStore.edit { it[chavePausada] = pausada }
    }

    suspend fun marcarAvisoLido() {
        context.dataStore.edit { it[chaveAviso] = true }
    }
}
