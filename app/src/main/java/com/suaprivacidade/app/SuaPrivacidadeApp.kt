package com.suaprivacidade.app

import android.app.Application
import com.suaprivacidade.app.data.PreferenciasRepo
import com.suaprivacidade.app.vigia.Acessos
import com.suaprivacidade.app.vigia.Alertas
import com.suaprivacidade.app.vigia.Vigia
import com.suaprivacidade.app.vigia.VigiaService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SuaPrivacidadeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Alertas.criarCanais(this)
        // Acesso concedido mas vigia parada (ex.: o celular encerrou o app): pede para religar,
        // a não ser que o usuário a tenha desligado nos Ajustes.
        MainScope().launch {
            val pausada = PreferenciasRepo(this@SuaPrivacidadeApp).preferencias.first().vigiaPausada
            if (!pausada && Acessos.temLeitorNotificacoes(this@SuaPrivacidadeApp) && !Vigia.ativa.value) {
                VigiaService.religar(this@SuaPrivacidadeApp)
            }
        }
    }
}
