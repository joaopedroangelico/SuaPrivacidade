package com.suaprivacidade.app

import android.app.Application
import com.suaprivacidade.app.vigia.Acessos
import com.suaprivacidade.app.vigia.Alertas
import com.suaprivacidade.app.vigia.Vigia
import com.suaprivacidade.app.vigia.VigiaService

class SuaPrivacidadeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Alertas.criarCanais(this)
        // Acesso concedido mas vigia parada (ex.: o celular encerrou o app): pede para religar.
        if (Acessos.temLeitorNotificacoes(this) && !Vigia.ativa.value) VigiaService.religar(this)
    }
}
