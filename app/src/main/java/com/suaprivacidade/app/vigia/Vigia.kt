package com.suaprivacidade.app.vigia

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.suaprivacidade.app.data.AppDatabase
import com.suaprivacidade.app.data.Apps
import com.suaprivacidade.app.data.Estatisticas
import com.suaprivacidade.app.data.MudancaPermissao
import com.suaprivacidade.app.data.Notificacao
import com.suaprivacidade.app.data.Preferencias
import com.suaprivacidade.app.data.PreferenciasRepo
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.UsoSensor
import com.suaprivacidade.app.data.deveAvisar
import com.suaprivacidade.app.data.ehSuspeito
import com.suaprivacidade.app.data.palpitar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Percebe quando o microfone e a câmera ligam e desligam, guarda as notificações e confere as
 * permissões dos apps supervisionados.
 *
 * Os avisos do Android chegam na thread principal e viram [Evento]s numa fila, tratados um por vez
 * (assim um "desligou" nunca passa na frente do "ligou").
 */
class Vigia(private val servico: VigiaService) {
    private val context: Context = servico.applicationContext
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventos = Channel<Evento>(Channel.UNLIMITED)
    private val principal = Handler(Looper.getMainLooper())
    private val banco = AppDatabase.get(context)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val cameras = context.getSystemService(CameraManager::class.java)

    @Volatile private var prefs = Preferencias()
    @Volatile private var supervisionados: Set<String> = emptySet()

    // Só mexidos dentro da fila de eventos.
    private var usoMicrofone: Long? = null
    private var usoCamera: Long? = null

    // Só mexidos na thread principal (callbacks do Android).
    private val camerasOcupadas = mutableSetOf<String>()
    private val lanternasAcesas = mutableSetOf<String>()

    private sealed interface Evento {
        data class Microfone(val gravando: Boolean, val silenciado: Boolean) : Evento
        data class Camera(val emUso: Boolean) : Evento
        data class NotificacaoPostada(val pacote: String, val chave: String, val titulo: String, val texto: String) : Evento
        data class NotificacaoRemovida(val chave: String) : Evento
        data object Manutencao : Evento
    }

    private val callbackAudio = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) = mudouMicrofone(configs)
    }

    private val callbackCamera = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            camerasOcupadas += cameraId
            mudouCamera()
        }

        override fun onCameraAvailable(cameraId: String) {
            camerasOcupadas -= cameraId
            mudouCamera()
        }
    }

    // Em vários celulares a lanterna também deixa a câmera "ocupada": não é uso da câmera.
    private val callbackLanterna = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (enabled) lanternasAcesas += cameraId else lanternasAcesas -= cameraId
            mudouCamera()
        }
    }

    /** Apps instalados ou atualizados: confere se algum supervisionado ganhou permissão. */
    private val receptorPacotes = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            eventos.trySend(Evento.Manutencao)
        }
    }

    fun iniciar() {
        estaAtiva.value = true
        escopo.launch { PreferenciasRepo(context).preferencias.collect { prefs = it } }
        escopo.launch {
            banco.supervisionadoDao().observarTodos().collect { lista -> supervisionados = lista.map { it.pacote }.toSet() }
        }
        escopo.launch {
            banco.usoDao().fecharInterrompidos()
            for (evento in eventos) {
                runCatching { tratar(evento) }.onFailure { Log.w(TAG, "Falha ao tratar $evento", it) }
            }
        }
        escopo.launch {
            while (isActive) {
                eventos.trySend(Evento.Manutencao)
                delay(INTERVALO_MANUTENCAO)
            }
        }
        principal.post {
            audio.registerAudioRecordingCallback(callbackAudio, principal)
            cameras.registerAvailabilityCallback(callbackCamera, principal)
            cameras.registerTorchCallback(callbackLanterna, principal)
            mudouMicrofone(audio.activeRecordingConfigurations)
        }
        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receptorPacotes, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun parar() {
        estaAtiva.value = false
        principal.post {
            runCatching { audio.unregisterAudioRecordingCallback(callbackAudio) }
            runCatching { cameras.unregisterAvailabilityCallback(callbackCamera) }
            runCatching { cameras.unregisterTorchCallback(callbackLanterna) }
        }
        runCatching { context.unregisterReceiver(receptorPacotes) }
        eventos.close()
        val abertos = listOfNotNull(usoMicrofone, usoCamera)
        if (abertos.isEmpty()) {
            escopo.cancel()
        } else {
            // Fecha os usos em andamento com a hora de agora antes de encerrar.
            escopo.launch {
                val agora = System.currentTimeMillis()
                abertos.forEach { banco.usoDao().fechar(it, agora) }
                escopo.cancel()
            }
        }
    }

    // --- Avisos do Android (thread principal) ---

    private fun mudouMicrofone(configs: List<AudioRecordingConfiguration>) {
        val gravando = configs.isNotEmpty()
        // Android 10+: o app que grava em segundo plano recebe silêncio; o Android marca isso.
        val silenciado = gravando && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && configs.all { it.isClientSilenced }
        eventos.trySend(Evento.Microfone(gravando, silenciado))
    }

    private fun mudouCamera() {
        eventos.trySend(Evento.Camera((camerasOcupadas - lanternasAcesas).isNotEmpty()))
    }

    fun notificacaoPostada(sbn: StatusBarNotification) {
        if (sbn.packageName == context.packageName) return
        val n = sbn.notification ?: return
        // Notificações fixas (música, download, serviço em primeiro plano) e resumos de grupo
        // mudam o tempo todo e não são mensagens: ficam de fora do histórico.
        if (n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_GROUP_SUMMARY) != 0) return
        val extras = n.extras ?: return
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val texto = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()?.trim().orEmpty()
        if (titulo.isEmpty() && texto.isEmpty()) return
        eventos.trySend(Evento.NotificacaoPostada(sbn.packageName, sbn.key, titulo, texto))
    }

    fun notificacaoRemovida(sbn: StatusBarNotification) {
        if (sbn.packageName == context.packageName) return
        eventos.trySend(Evento.NotificacaoRemovida(sbn.key))
    }

    // --- Fila de eventos ---

    private suspend fun tratar(evento: Evento) {
        when (evento) {
            is Evento.Microfone -> {
                val aberto = usoMicrofone
                when {
                    evento.gravando && aberto == null -> usoMicrofone = abrirUso(Sensor.MICROFONE, evento.silenciado)
                    evento.gravando && evento.silenciado && aberto != null -> banco.usoDao().marcarSilenciado(aberto)
                    !evento.gravando && aberto != null -> {
                        banco.usoDao().fechar(aberto, System.currentTimeMillis())
                        usoMicrofone = null
                    }
                }
            }
            is Evento.Camera -> {
                val aberto = usoCamera
                if (evento.emUso && aberto == null) {
                    usoCamera = abrirUso(Sensor.CAMERA, silenciado = false)
                } else if (!evento.emUso && aberto != null) {
                    banco.usoDao().fechar(aberto, System.currentTimeMillis())
                    usoCamera = null
                }
            }
            is Evento.NotificacaoPostada -> guardarNotificacao(evento)
            is Evento.NotificacaoRemovida -> banco.notificacaoDao().marcarRemovida(evento.chave, System.currentTimeMillis())
            Evento.Manutencao -> {
                banco.apagarAntigos(prefs.diasHistorico)
                verificarPermissoes()
            }
        }
    }

    private suspend fun abrirUso(sensor: Sensor, silenciado: Boolean): Long {
        val agora = System.currentTimeMillis()
        val telaLigada = context.getSystemService(PowerManager::class.java).isInteractive
        val bloqueado = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val naTela = if (telaLigada && !bloqueado) Estatisticas.appNaTela(context, agora) else null
        val emLigacao = sensor == Sensor.MICROFONE &&
            audio.mode in setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)
        val permissao = if (sensor == Sensor.MICROFONE) Manifest.permission.RECORD_AUDIO else Manifest.permission.CAMERA
        val palpite = palpitar(
            naTela = naTela,
            comServico = servico.pacotesComServico(),
            temPermissao = { Apps.temPermissao(context, it, permissao) },
            proprio = context.packageName,
        )
        val uso = UsoSensor(
            sensor = sensor,
            inicioMillis = agora,
            pacote = palpite.pacote,
            confianca = palpite.confianca,
            appNaTela = palpite.naTela,
            telaLigada = telaLigada,
            bloqueado = bloqueado,
            emLigacao = emLigacao,
            silenciado = silenciado,
            suspeito = ehSuspeito(palpite, telaLigada, bloqueado, emLigacao),
        )
        val id = banco.usoDao().inserir(uso)
        if (deveAvisar(uso, supervisionados)) {
            Alertas.usoSuspeito(context, uso.copy(id = id), palpite.pacote?.let { Apps.nome(context, it) })
        }
        return id
    }

    private suspend fun guardarNotificacao(e: Evento.NotificacaoPostada) {
        val dao = banco.notificacaoDao()
        val oculta = e.pacote in prefs.textoOculto
        val titulo = e.titulo.takeUnless { oculta }
        val texto = e.texto.takeUnless { oculta }
        // A mesma notificação reenviada sem mudar o conteúdo (ex.: só o horário) não entra de novo.
        val anterior = dao.ultimaDaChave(e.chave)
        if (anterior != null && anterior.removidaMillis == null && anterior.titulo == titulo && anterior.texto == texto) return
        dao.inserir(
            Notificacao(
                pacote = e.pacote,
                chave = e.chave,
                titulo = titulo,
                texto = texto,
                oculta = oculta,
                postadaMillis = System.currentTimeMillis(),
            ),
        )
    }

    /** Compara as permissões sensíveis de cada app supervisionado com as da última verificação. */
    private suspend fun verificarPermissoes() {
        val agora = System.currentTimeMillis()
        for (s in banco.supervisionadoDao().todos()) {
            val atuais = Apps.permissoesSensiveis(context, s.pacote) ?: continue
            val antes = s.listaPermissoes
            val novas = atuais - antes
            val retiradas = antes - atuais
            if (novas.isEmpty() && retiradas.isEmpty()) continue
            banco.mudancaDao().inserirTodas(
                novas.map { MudancaPermissao(pacote = s.pacote, permissao = it, concedida = true, momentoMillis = agora) } +
                    retiradas.map { MudancaPermissao(pacote = s.pacote, permissao = it, concedida = false, momentoMillis = agora) },
            )
            banco.supervisionadoDao().salvar(s.copy(permissoes = atuais.joinToString(",")))
            if (novas.isNotEmpty()) Alertas.permissaoNova(context, s.pacote, Apps.nome(context, s.pacote), novas.toList())
        }
    }

    companion object {
        private const val TAG = "Vigia"
        private const val INTERVALO_MANUTENCAO = 15L * 60 * 1000

        private val estaAtiva = MutableStateFlow(false)

        /** A vigia está rodando (o leitor de notificações está conectado). */
        val ativa: StateFlow<Boolean> = estaAtiva
    }
}
