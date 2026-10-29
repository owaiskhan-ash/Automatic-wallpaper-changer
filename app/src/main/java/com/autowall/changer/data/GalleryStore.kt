package com.autowall.changer.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Photos chosen by the user are copied into the app's private storage so wallpapers
 * keep rotating even if the original file or its access grant disappears.
 */
class GalleryStore(private val context: Context) {

    private val root: File
        get() = File(context.filesDir, "gallery").apply { mkdirs() }

    fun photos(): List<File> =
        root.listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedBy { it.name }
            ?: emptyList()

    suspend fun import(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var added = 0
        for (uri in uris) {
            val name = "wall_${System.currentTimeMillis()}_${uri.hashCode().toUInt()}${
                extensionFor(uri)
            }"
            val target = File(root, name)
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Cannot open $uri")
            }.isSuccess
            if (copied && target.length() > 0) added++ else target.delete()
        }
        added
    }

    fun delete(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun clear() {
        root.listFiles()?.forEach { it.delete() }
    }

    private fun extensionFor(uri: Uri): String = when (context.contentResolver.getType(uri)) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "image/heic", "image/heif" -> ".heic"
        else -> ".jpg"
    }
}
