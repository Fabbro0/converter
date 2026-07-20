package com.megaconverter.app.converter.converters

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

/**
 * A .docx is a zip archive with the document body as word/document.xml; a minimal
 * namespace-aware pull-parser walk extracts the paragraph text runs (<w:t>) without
 * pulling in a heavyweight OOXML library. Shared by DocxConverter and the in-app reader.
 */
object DocxReader {

    fun extractText(file: File): String {
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
