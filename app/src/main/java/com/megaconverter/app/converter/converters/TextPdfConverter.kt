package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/** Plain text <-> PDF. PDF -> TXT uses PdfBox-Android for real text extraction
 * (Android's built-in PdfRenderer only rasterizes pages, it can't read text). */
class TextPdfConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        (from == FileFormat.TXT && to == FileFormat.PDF) || (from == FileFormat.PDF && to == FileFormat.TXT)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> = when (from) {
        FileFormat.TXT -> setOf(FileFormat.PDF)
        FileFormat.PDF -> setOf(FileFormat.TXT)
        else -> emptySet()
    }

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult = if (input.format == FileFormat.TXT) {
        textToPdf(context, input, onProgress)
    } else {
        pdfToText(context, input, onProgress)
    }

    private fun textToPdf(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        val text = input.sourceFile.readText(Charsets.UTF_8)
        val outputFile = TextPdfRenderer.renderToPdf(context, input.displayName, text, onProgress)
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.PDF)
    }

    private fun pdfToText(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        onProgress(0.1f)
        val document = PDDocument.load(input.sourceFile)
        val text = try {
            PDFTextStripper().getText(document)
        } finally {
            document.close()
        }
        onProgress(0.8f)

        val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.TXT)
        outputFile.writeText(text, Charsets.UTF_8)
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.TXT)
    }
}
