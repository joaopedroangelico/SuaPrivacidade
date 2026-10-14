package com.suaprivacidade.app.data

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File
import java.text.NumberFormat

/** Espaço do celular e do app, em bytes. */
data class Armazenamento(
    val totalCelular: Long,
    val livreCelular: Long,
    /** Tudo o que o app ocupa: instalação + dados + cache. */
    val app: Long,
    /** Só as informações cadastradas: banco de dados e preferências. */
    val dados: Long,
)

/**
 * Mede o armazenamento. O Android permite consultar o próprio app sem permissão; se a consulta
 * falhar em algum aparelho, cai para a soma das pastas do app.
 */
fun medirArmazenamento(context: Context): Armazenamento {
    val stats = context.getSystemService(StorageStatsManager::class.java)
    val (total, livre) = runCatching {
        stats.getTotalBytes(StorageManager.UUID_DEFAULT) to stats.getFreeBytes(StorageManager.UUID_DEFAULT)
    }.getOrElse {
        val fs = StatFs(Environment.getDataDirectory().path)
        fs.totalBytes to fs.availableBytes
    }
    val app = runCatching {
        val s = stats.queryStatsForPackage(StorageManager.UUID_DEFAULT, context.packageName, Process.myUserHandle())
        s.appBytes + s.dataBytes // dataBytes já inclui o cache
    }.getOrElse {
        File(context.applicationInfo.sourceDir).length() + tamanho(context.dataDir)
    }
    val banco = context.getDatabasePath(NOME_BANCO)
    val arquivosDoBanco = listOf(banco, File("${banco.path}-wal"), File("${banco.path}-shm"))
    val dados = arquivosDoBanco.sumOf { it.length() } + tamanho(File(context.filesDir, "datastore"))
    return Armazenamento(total, livre, app, dados)
}

private fun tamanho(arquivo: File): Long =
    if (arquivo.isDirectory) arquivo.listFiles().orEmpty().sumOf(::tamanho) else arquivo.length()

private const val MB = 1024.0 * 1024.0

/** "0,08 MB", "25,4 MB", "1.024 MB". */
fun formatarMB(bytes: Long): String {
    val mb = bytes / MB
    val formato = NumberFormat.getNumberInstance(LocaleBR).apply {
        maximumFractionDigits = when {
            mb < 1 -> 2
            mb < 100 -> 1
            else -> 0
        }
        minimumFractionDigits = maximumFractionDigits
    }
    return "${formato.format(mb)} MB"
}

/** Tamanho do celular: em GB, que é como o aparelho é vendido ("128 GB"). */
fun formatarGB(bytes: Long): String {
    val formato = NumberFormat.getNumberInstance(LocaleBR).apply { maximumFractionDigits = 1 }
    return "${formato.format(bytes / (MB * 1024))} GB"
}

/** "12%", "0,3%", "< 0,01%". */
fun formatarPorcentagem(parte: Long, todo: Long): String {
    if (todo <= 0 || parte <= 0) return "0%"
    val p = parte * 100.0 / todo
    if (p < 0.01) return "< 0,01%"
    val formato = NumberFormat.getNumberInstance(LocaleBR).apply {
        maximumFractionDigits = when {
            p < 1 -> 2
            p < 10 -> 1
            else -> 0
        }
    }
    return "${formato.format(p)}%"
}
