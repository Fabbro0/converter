package com.megaconverter.app.converter

import android.content.Context
import com.megaconverter.app.converter.converters.AudioVideoConverter
import com.megaconverter.app.converter.converters.DocxConverter
import com.megaconverter.app.converter.converters.EpubConverter
import com.megaconverter.app.converter.converters.ImagePdfConverter
import com.megaconverter.app.converter.converters.ImageToImageConverter
import com.megaconverter.app.converter.converters.TextPdfConverter

class ConversionEngine(private val converters: List<FileConverter>) {

    fun supportedTargets(from: FileFormat): List<FileFormat> =
        converters
            .flatMap { it.possibleOutputs(from) }
            .filter { it != from }
            .distinct()
            .sortedBy { it.name }

    fun findConverter(from: FileFormat, to: FileFormat): FileConverter? =
        converters.firstOrNull { it.canConvert(from, to) }

    suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit = {},
    ): ConversionResult {
        val converter = findConverter(input.format, targetFormat)
            ?: return ConversionResult.Failure(
                "Nessun convertitore disponibile da .${input.format.extension} a .${targetFormat.extension}",
            )
        return try {
            converter.convert(context, input, targetFormat, onProgress)
        } catch (e: Exception) {
            ConversionResult.Failure(e.message ?: "Errore sconosciuto durante la conversione", e)
        }
    }

    companion object {
        fun default(): ConversionEngine = ConversionEngine(
            listOf(
                ImageToImageConverter(),
                ImagePdfConverter(),
                TextPdfConverter(),
                DocxConverter(),
                EpubConverter(),
                AudioVideoConverter(),
            ),
        )
    }
}
