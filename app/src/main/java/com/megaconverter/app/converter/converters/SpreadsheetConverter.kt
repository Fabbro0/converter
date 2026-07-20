package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils

/** CSV <-> XLSX. See XlsxReader/XlsxWriter for the hand-rolled minimal-OOXML details. */
class SpreadsheetConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        (from == FileFormat.XLSX && to == FileFormat.CSV) || (from == FileFormat.CSV && to == FileFormat.XLSX)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> = when (from) {
        FileFormat.XLSX -> setOf(FileFormat.CSV)
        FileFormat.CSV -> setOf(FileFormat.XLSX)
        else -> emptySet()
    }

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        onProgress(0.2f)
        return try {
            when {
                input.format == FileFormat.XLSX && targetFormat == FileFormat.CSV -> {
                    val rows = XlsxReader.readRows(input.sourceFile)
                    onProgress(0.7f)
                    val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.CSV)
                    outputFile.writeText(CsvUtils.format(rows), Charsets.UTF_8)
                    onProgress(1f)
                    ConversionResult.Success(outputFile, FileFormat.CSV)
                }
                input.format == FileFormat.CSV && targetFormat == FileFormat.XLSX -> {
                    val rows = CsvUtils.parse(input.sourceFile.readText(Charsets.UTF_8))
                    onProgress(0.7f)
                    val outputFile = XlsxWriter.write(context, rows, input.displayName)
                    onProgress(1f)
                    ConversionResult.Success(outputFile, FileFormat.XLSX)
                }
                else -> ConversionResult.Failure("Conversione foglio di calcolo non supportata")
            }
        } catch (e: Exception) {
            ConversionResult.Failure("Impossibile convertire il foglio di calcolo: ${e.message}", e)
        }
    }
}
