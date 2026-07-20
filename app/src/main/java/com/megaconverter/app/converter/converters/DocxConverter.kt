package com.megaconverter.app.converter.converters

import android.content.Context
import android.util.Xml
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

/**
 * DOCX -> TXT / PDF. A .docx is a zip archive with the document body as
 * word/document.xml; a minimal namespace-aware pull-parser walk extracts the
 * paragraph text runs (<w:t>) without pulling in a heavyweight OOXML library.
 */
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
            extractText(input.sourceFile)
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

    private fun extractText(file: File): String {
        val zip = ZipFile(file)
        val entry = zip.getEntry("word/document.xml") ?: run {
            zip.close()
            return ""
        }
        val text = zip.getInputStream(entry).use { input ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(input, "UTF-8")

            val sb = StringBuilder()
            var inTextRun = false
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "t" -> inTextRun = true
                        "br" -> sb.append('\n')
                        "tab" -> sb.append('\t')
                    }
                    XmlPullParser.TEXT -> if (inTextRun) sb.append(parser.text)
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "t" -> inTextRun = false
                        "p" -> sb.append('\n')
                    }
                }
                eventType = parser.next()
            }
            sb.toString()
        }
        zip.close()
        return text
    }
}
