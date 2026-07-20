package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils

/** PPTX -> TXT / PDF (text extraction only, see PptxReader). Read-only, like DOCX:
 * generating a real .pptx (slide layouts/masters/themes) is out of scope. */
class PptxConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        from == FileFormat.PPTX && (to == FileFormat.TXT || to == FileFormat.PDF)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> =
        if (from == FileFormat.PPTX) setOf(FileFormat.TXT, FileFormat.PDF) else emptySet()

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        onProgress(0.1f)
        val text = try {
            PptxReader.extractText(input.sourceFile)
        } catch (e: Exception) {
            return ConversionResult.Failure("Impossibile leggere il file PPTX: ${e.message}", e)
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
            else -> ConversionResult.Failure("Formato di destinazione non supportato per PPTX")
        }
    }
}
