package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils

/** DOCX -> TXT / PDF, using DocxReader for the actual text extraction. */
class DocxConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        from == FileFormat.DOCX && (to == FileFormat.TXT || to == FileFormat.PDF)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> =
        if (from == FileFormat.DOCX) setOf(FileFormat.TXT, FileFormat.PDF) else emptySet()

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        onProgress(0.1f)
        val text = try {
            DocxReader.extractText(input.sourceFile)
        } catch (e: Exception) {
            return ConversionResult.Failure("Impossibile leggere il file DOCX: ${e.message}", e)
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
                val outputFile = TextPdfRenderer.renderToPdf(context, input.displayName, text) { progress ->
                    onProgress(0.4f + progress * 0.6f)
                }
                onProgress(1f)
                ConversionResult.Success(outputFile, FileFormat.PDF)
            }
            else -> ConversionResult.Failure("Formato di destinazione non supportato per DOCX")
        }
    }
}
