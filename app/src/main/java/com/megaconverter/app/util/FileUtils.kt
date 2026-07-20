package com.megaconverter.app.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.megaconverter.app.converter.FileFormat
import java.io.File
import java.util.Locale

object FileUtils {

    private const val INPUT_DIR = "mc_input"
    private const val OUTPUT_DIR = "mc_output"

    fun displayNameFromUri(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && it.moveToFirst()) {
                    name = it.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name ?: "file"
    }

    fun extensionOf(fileName: String): String? {
        val dot = fileName.lastIndexOf('.')
        if (dot < 0 || dot == fileName.length - 1) return null
        return fileName.substring(dot + 1).lowercase(Locale.ROOT)
    }

    fun detectFormat(context: Context, uri: Uri, displayName: String): FileFormat? {
        extensionOf(displayName)?.let { ext ->
            FileFormat.fromExtension(ext)?.let { return it }
        }
        val mime = context.contentResolver.getType(uri)
        return FileFormat.fromMimeType(mime)
    }

    /** Copies the picked content:// (or file://) Uri into a real file the conversion
     * libraries and ffmpeg can operate on directly. */
    fun copyToCache(context: Context, uri: Uri, displayName: String): File {
        val dir = File(context.cacheDir, INPUT_DIR).apply { mkdirs() }
        val safeName = sanitizeFileName(displayName)
        val dest = uniqueFile(dir, safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Impossibile leggere il file selezionato")
        return dest
    }

    fun newOutputFile(context: Context, baseName: String, format: FileFormat): File =
        newOutputFile(context, baseName, format.extension)

    fun newOutputFile(context: Context, baseName: String, extension: String): File {
        val dir = File(context.cacheDir, OUTPUT_DIR).apply { mkdirs() }
        val safeBase = sanitizeFileName(baseName).substringBeforeLast('.', baseName)
        return uniqueFile(dir, "$safeBase.$extension")
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val base = fileName.substringBeforeLast('.', fileName)
        val ext = fileName.substringAfterLast('.', "")
        var candidate = File(dir, fileName)
        var counter = 1
        while (candidate.exists()) {
            val suffixed = if (ext.isNotEmpty()) "$base ($counter).$ext" else "$base ($counter)"
            candidate = File(dir, suffixed)
            counter++
        }
        return candidate
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "file" }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun shareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = uriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Condividi file convertito")
    }

    fun openIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = uriForFile(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** The real output file extension can differ from the format the user picked
     * (e.g. a multi-page PDF exported as images becomes a .zip), so mime type
     * resolution must look at the actual file, falling back to [fallback]. */
    fun guessMimeType(file: File, fallback: FileFormat): String {
        val ext = extensionOf(file.name) ?: return fallback.mimeType
        if (ext == "zip") return "application/zip"
        return FileFormat.fromExtension(ext)?.mimeType ?: fallback.mimeType
    }

    fun humanFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB")
        var value = bytes / 1024.0
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}
