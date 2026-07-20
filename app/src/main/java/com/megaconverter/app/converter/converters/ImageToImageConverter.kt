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

    private val imageFormats = FileFormat.entries.filter { it.category == FormatCategory.IMAGE }.toSet()

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        from.category == FormatCategory.IMAGE && to.category == FormatCategory.IMAGE && from != to

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> =
        if (from.category == FormatCategory.IMAGE) imageFormats - from else emptySet()

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
