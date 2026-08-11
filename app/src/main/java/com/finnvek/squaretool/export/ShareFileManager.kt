package com.finnvek.squaretool.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ShareFileManager(
    private val context: Context,
) {
    private val shareDirectory = File(context.cacheDir, "shared_exports")

    fun createFile(
        projectName: String,
        purpose: String,
        extension: String,
    ): File {
        shareDirectory.mkdirs()
        cleanOldFiles()
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.now().atZone(ZoneId.systemDefault()))
        val base = ExportPolicy.sanitizedBaseName(projectName)
        val safePurpose = ExportPolicy.sanitizedBaseName(purpose)
        return File(shareDirectory, "${base}_${safePurpose}_$date.${extension.trimStart('.')}")
    }

    fun contentUri(file: File): Uri {
        require(file.canonicalPath.startsWith(shareDirectory.canonicalPath + File.separator))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(
        file: File,
        mimeType: String,
        chooserTitle: String,
    ): Intent {
        val uri = contentUri(file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri(file.name, uri)
            }
        return Intent.createChooser(send, chooserTitle)
    }

    fun cleanOldFiles(maxAgeMillis: Long = 7L * 24 * 60 * 60 * 1_000) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        shareDirectory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach(File::delete)
    }
}
