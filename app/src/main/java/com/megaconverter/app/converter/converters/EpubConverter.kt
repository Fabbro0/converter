package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils

/** EPUB -> TXT / PDF (text extraction) and TXT -> EPUB (single-chapter EPUB3). */
class EpubConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        (from == FileFormat.EPUB && (to == FileFormat.TXT || to == FileFormat.PDF)) ||
            (from == FileFormat.TXT && to == FileFormat.EPUB)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> = when (from) {
        FileFormat.EPUB -> setOf(FileFormat.TXT, FileFormat.PDF)
        FileFormat.TXT -> setOf(FileFormat.EPUB)
        else -> emptySet()
    }

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult = if (input.format == FileFormat.EPUB) {
        epubToOther(context, input, targetFormat, onProgress)
    } else {
        textToEpub(context, input, onProgress)
    }

    private fun epubToOther(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        onProgress(0.1f)
        val text = try {
            EpubReader.extractText(input.sourceFile)
        } catch (e: Exception) {
            return ConversionResult.Failure("Impossibile leggere l'EPUB: ${e.message}", e)
        }
        onProgress(0.4f)

        return when (targetFormat) {
            FileFormat.TXT -> {
                val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.TXT)
                outputFile.writeText(text, Charsets.UTF_8)
                onProgress(1f)
                ConversionResult.Success(outputFile, FileFormat.TXT)
            }
            FileFormat.PDF -> {
                val outputFile = TextPdfRenderer.renderToPdf(context, input.displayName, text) { p ->
                    onProgress(0.4f + p * 0.6f)
                }
                onProgress(1f)
                ConversionResult.Success(outputFile, FileFormat.PDF)
            }
            else -> ConversionResult.Failure("Formato di destinazione non supportato per EPUB")
        }
    }

    private fun textToEpub(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        onProgress(0.2f)
        val text = input.sourceFile.readText(Charsets.UTF_8)
        val title = input.displayName.substringBeforeLast('.', input.displayName)
        val outputFile = EpubWriter.write(context, title, text, input.displayName)
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.EPUB)
    }
}
