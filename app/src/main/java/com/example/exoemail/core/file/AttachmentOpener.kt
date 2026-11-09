package com.example.exoemail.core.file

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Saves downloaded attachment bytes and hands them to other apps.
 *
 * Two paths:
 *  - [open] writes to the app cache and launches an external viewer through
 *    FileProvider (read-only grant). Nothing lands in shared storage.
 *  - [saveToDownloads] writes into the public Downloads collection so the user
 *    keeps a permanent copy.
 */
object AttachmentOpener {

    private fun cacheFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
        // Sanitise: strip path separators so a hostile filename can't escape cacheDir.
        val safe = fileName.replace('/', '_').replace('\\', '_').ifBlank { "attachment" }
        // Unique prefix keeps repeated downloads of the same name from clobbering
        // a file an external viewer may still have open.
        return File(dir, "${System.currentTimeMillis()}_$safe")
    }

    /** Write bytes to cache and return a content:// URI other apps can read. */
    fun stageForView(context: Context, fileName: String, mimeType: String?, bytes: ByteArray): Uri {
        val f = cacheFile(context, fileName)
        f.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    }

    /** Launch an external viewer for already-downloaded bytes. */
    fun open(context: Context, fileName: String, mimeType: String?, bytes: ByteArray): Result<Unit> = runCatching {
        val uri = stageForView(context, fileName, mimeType, bytes)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Copy bytes into the public Downloads collection (API 29+ MediaStore). */
    fun saveToDownloads(context: Context, fileName: String, mimeType: String?, bytes: ByteArray): Result<Uri> = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            mimeType?.let { put(MediaStore.Downloads.MIME_TYPE, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create Downloads entry")
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not open output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    }
}
