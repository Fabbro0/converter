package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils

/** Markdown -> TXT / PDF, via MarkdownStripper's best-effort syntax removal. */
class MarkdownConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        from == FileFormat.MD && (to == FileFormat.TXT || to == FileFormat.PDF)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> =
        if (from == FileFormat.MD) setOf(FileFormat.TXT, FileFormat.PDF) else emptySet()

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        onProgress(0.2f)
        val plain = MarkdownStripper.toPlainText(input.sourceFile.readText(Charsets.UTF_8))
        onProgress(0.4f)

        return when (targetFormat) {
            FileFormat.TXT -> {
                val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.TXT)
                outputFile.writeText(plain, Charsets.UTF_8)
                onProgress(1f)
                ConversionResult.Success(outputFile, FileFormat.TXT)
            }
            FileFormat.PDF -> {
                val outputFile = TextPdfRenderer.renderToPdf(context, input.displayName, plain) { p ->
                    onProgress(0.4f + p * 0.6f)
                }
                onProgress(1f)
                ConversionResult.Success(outputFile, FileFormat.PDF)
            }
            else -> ConversionResult.Failure("Formato di destinazione non supportato per Markdown")
        }
    }
}
