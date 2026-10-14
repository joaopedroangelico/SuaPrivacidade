package com.suaprivacidade.app.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import java.util.concurrent.ConcurrentHashMap

/** Permissões que valem um aviso quando um app supervisionado as ganha, com o nome que o Android mostra. */
val PERMISSOES_SENSIVEIS: Map<String, String> = linkedMapOf(
    Manifest.permission.RECORD_AUDIO to "Microfone",
    Manifest.permission.CAMERA to "Câmera",
    Manifest.permission.ACCESS_FINE_LOCATION to "Localização precisa",
    Manifest.permission.ACCESS_COARSE_LOCATION to "Localização aproximada",
    Manifest.permission.ACCESS_BACKGROUND_LOCATION to "Localização o tempo todo",
    Manifest.permission.READ_CONTACTS to "Contatos",
    Manifest.permission.READ_CALENDAR to "Agenda",
    Manifest.permission.READ_SMS to "SMS",
    Manifest.permission.READ_CALL_LOG to "Registro de chamadas",
    Manifest.permission.READ_PHONE_STATE to "Telefone",
    Manifest.permission.READ_MEDIA_IMAGES to "Fotos",
    Manifest.permission.READ_MEDIA_VIDEO to "Vídeos",
    Manifest.permission.READ_EXTERNAL_STORAGE to "Arquivos",
    Manifest.permission.BODY_SENSORS to "Sensores do corpo",
    Manifest.permission.ACTIVITY_RECOGNITION to "Atividade física",
    Manifest.permission.BLUETOOTH_SCAN to "Dispositivos próximos",
)

fun rotuloPermissao(permissao: String): String = PERMISSOES_SENSIVEIS[permissao] ?: permissao.substringAfterLast('.')

/** Um app instalado, com o que interessa para a Auditoria. */
data class InfoApp(
    val pacote: String,
    val nome: String,
    /** Sem ícone na gaveta de apps: componente do sistema que roda por trás. */
    val sistema: Boolean,
    val microfone: Boolean,
    val camera: Boolean,
)

object Apps {
    private val nomes = ConcurrentHashMap<String, String>()

    /** Nome do app como aparece no celular; o pacote, se o app não estiver mais instalado. */
    fun nome(context: Context, pacote: String): String = nomes.getOrPut(pacote) {
        val pm = context.packageManager
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pacote, 0)).toString() }.getOrDefault(pacote)
    }

    fun icone(context: Context, pacote: String): Drawable? =
        runCatching { context.packageManager.getApplicationIcon(pacote) }.getOrNull()

    fun instalado(context: Context, pacote: String): Boolean =
        runCatching { context.packageManager.getApplicationInfo(pacote, 0) }.isSuccess

    fun uid(context: Context, pacote: String): Int? =
        runCatching { context.packageManager.getApplicationInfo(pacote, 0).uid }.getOrNull()

    fun temPermissao(context: Context, pacote: String, permissao: String): Boolean =
        context.packageManager.checkPermission(permissao, pacote) == PackageManager.PERMISSION_GRANTED

    /** Todos os apps instalados (menos este), em ordem alfabética. Lento: chamar fora da thread principal. */
    fun listar(context: Context): List<InfoApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0)
            .filter { it.packageName != context.packageName && it.enabled }
            .map { ai ->
                val nome = pm.getApplicationLabel(ai).toString()
                nomes[ai.packageName] = nome
                InfoApp(
                    pacote = ai.packageName,
                    nome = nome,
                    sistema = pm.getLaunchIntentForPackage(ai.packageName) == null &&
                        ai.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    microfone = temPermissao(context, ai.packageName, Manifest.permission.RECORD_AUDIO),
                    camera = temPermissao(context, ai.packageName, Manifest.permission.CAMERA),
                )
            }
            .sortedBy { it.nome.lowercase(LocaleBR) }
    }

    /** Permissões sensíveis concedidas agora ao app; null se ele não está instalado. */
    fun permissoesSensiveis(context: Context, pacote: String): Set<String>? {
        val info = runCatching {
            context.packageManager.getPackageInfo(pacote, PackageManager.GET_PERMISSIONS)
        }.getOrNull() ?: return null
        val pedidas = info.requestedPermissions ?: return emptySet()
        val flags = info.requestedPermissionsFlags ?: return emptySet()
        return pedidas.indices
            .filter { i -> pedidas[i] in PERMISSOES_SENSIVEIS && flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
            .map { pedidas[it] }
            .toSet()
    }

    /** Tela "Informações do app" do Android, onde ficam as permissões dele. */
    fun abrirDetalhes(context: Context, pacote: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pacote, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
