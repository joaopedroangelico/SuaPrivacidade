package com.suaprivacidade.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suaprivacidade.app.ui.PrivacidadeViewModel
import com.suaprivacidade.app.ui.TelaPrincipal
import com.suaprivacidade.app.ui.theme.SuaPrivacidadeTheme
import com.suaprivacidade.app.vigia.Alertas

class MainActivity : ComponentActivity() {
    /** Aba pedida pelo toque numa notificação do app; consumida pela tela. */
    private val abaPedida = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) abaPedida.value = intent.getStringExtra(Alertas.EXTRA_ABA)
        setContent {
            val vm: PrivacidadeViewModel = viewModel()
            val preferencias by vm.preferencias.collectAsStateWithLifecycle()
            // Até as preferências carregarem (milissegundos), só o fundo da janela aparece — evita piscar o tema errado.
            preferencias?.let { prefs ->
                SuaPrivacidadeTheme(tema = prefs.tema) {
                    TelaPrincipal(
                        vm = vm,
                        preferencias = prefs,
                        abaPedida = abaPedida.value,
                        onAbaAtendida = { abaPedida.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        abaPedida.value = intent.getStringExtra(Alertas.EXTRA_ABA)
    }
}
