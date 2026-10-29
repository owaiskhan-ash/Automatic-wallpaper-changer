package com.autowall.changer.data

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/** Outcome of copying one or more picked photos into app storage. */
data class ImportResult(
    val imported: Int,
    val failed: Int,
    val lastError: String? = null,
)

/**
 * Photos chosen by the user are copied into the app's private storage so wallpapers
 * keep rotating even if the original file or its access grant disappears.
 */
class GalleryStore(private val context: Context) {

    private val root: File
        get() = File(context.filesDir, "gallery").apply { mkdirs() }

    fun photos(): List<File> =
        runCatching {
            root.listFiles()
                ?.filter { it.isFile && it.length() > 0 }
                ?.sortedBy { it.name }
                ?: emptyList()
        }.getOrDefault(emptyList())

    suspend fun import(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var failed = 0
        var lastError: String? = null

        for (uri in uris.distinct()) {
            val outcome = runCatching { copyOne(uri) }
            if (outcome.isSuccess) {
                imported++
            } else {
                failed++
                lastError = outcome.exceptionOrNull()?.let { error ->
                    val label = error.javaClass.simpleName
                    val detail = error.message?.take(160)
                    if (detail.isNullOrBlank()) label else "$label: $detail"
                } ?: "Unknown error"
            }
        }
        ImportResult(imported = imported, failed = failed, lastError = lastError)
    }

    private fun copyOne(uri: Uri) {
        // Harmless when the source does not support persistence (e.g. the photo picker).
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        val target = File(root, nameFor(uri))
        try {
            copyStream(uri, target)
            if (target.length() == 0L) throw IOException("The file is empty")
            if (!isDecodableImage(target)) {
                throw IOException("Not a readable image (${target.length()} bytes)")
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private fun copyStream(uri: Uri, target: File) {
        val resolver = context.contentResolver
        val input = resolver.openInputStream(uri)
            ?: resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor)
            }
            ?: throw IOException("This app can't open that file")
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
    }

    private fun isDecodableImage(file: File): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        bounds.outWidth > 0 && bounds.outHeight > 0
    }.getOrDefault(false)

    fun delete(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun clear() {
        root.listFiles()?.forEach { it.delete() }
    }

    private fun nameFor(uri: Uri): String {
        val extension = when (context.contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/heic", "image/heif" -> ".heic"
            "image/gif" -> ".gif"
            else -> ".jpg"
        }
        return "wall_${System.currentTimeMillis()}_${uri.hashCode().toUInt()}$extension"
    }
}
