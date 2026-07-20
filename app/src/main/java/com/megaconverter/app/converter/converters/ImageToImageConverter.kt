package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.BitmapFactory
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.converter.FormatCategory
import com.megaconverter.app.util.FileUtils

class ImageToImageConverter : FileConverter {

    /** Formats Android's BitmapFactory can both decode and re-encode. */
    private val encodableFormats = setOf(FileFormat.JPG, FileFormat.PNG, FileFormat.WEBP, FileFormat.BMP)

    /** Android decodes these (HEIC since API 28, GIF as its first frame) but has no
     * public encoder for either, so they can only ever be a conversion source. */
    private val decodeOnlyFormats = setOf(FileFormat.HEIC, FileFormat.GIF)

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        isReadableSource(from) && to in encodableFormats && from != to

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> =
        if (isReadableSource(from)) encodableFormats - from else emptySet()

    private fun isReadableSource(format: FileFormat): Boolean =
        format in encodableFormats || format in decodeOnlyFormats

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        onProgress(0.1f)
        val bitmap = BitmapFactory.decodeFile(input.sourceFile.absolutePath)
            ?: return ConversionResult.Failure("Impossibile leggere l'immagine sorgente")
        onProgress(0.5f)

        val outputFile = FileUtils.newOutputFile(context, input.displayName, targetFormat)
        val ok = outputFile.outputStream().use { out -> ImageEncoder.write(bitmap, targetFormat, out) }
        bitmap.recycle()
        if (!ok) {
            return ConversionResult.Failure("Formato immagine di destinazione non supportato")
        }
        onProgress(1f)
        return ConversionResult.Success(outputFile, targetFormat)
    }
}
