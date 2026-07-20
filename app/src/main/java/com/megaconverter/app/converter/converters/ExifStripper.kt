package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.BitmapFactory
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File

/**
 * Strips EXIF/GPS metadata from an image by decoding then re-encoding it: a decoded
 * [android.graphics.Bitmap] carries no metadata, so writing it back out produces a
 * fresh file with none — no need for a metadata-editing library (e.g.
 * androidx.exifinterface) when a full strip, not selective editing, is the goal.
 */
object ExifStripper {

    private val encodableFormats = setOf(FileFormat.JPG, FileFormat.PNG, FileFormat.WEBP, FileFormat.BMP)

    fun strip(context: Context, sourceFile: File, sourceFormat: FileFormat, baseName: String): File {
        val targetFormat = if (sourceFormat in encodableFormats) sourceFormat else FileFormat.JPG
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: error("Impossibile leggere l'immagine")
        val outputFile = FileUtils.newOutputFile(context, "${baseName}_senza_dati", targetFormat)
        val ok = outputFile.outputStream().use { out -> ImageEncoder.write(bitmap, targetFormat, out) }
        bitmap.recycle()
        if (!ok) error("Formato immagine non supportato per la rimozione dei metadati")
        return outputFile
    }
}
