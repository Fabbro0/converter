package com.megaconverter.app.converter.converters

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

private const val RELATIONSHIPS_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

/**
 * Minimal PPTX text extractor: resolves ppt/presentation.xml's slide-id order via
 * ppt/_rels/presentation.xml.rels -> each ppt/slides/slideN.xml's text runs (<a:t>), in
 * actual presentation order. PowerPoint does not rename slideN.xml files when the user
 * reorders slides in the UI, so relying on filename order instead would silently get the
 * order wrong for any reordered deck — this mirrors the EPUB spine-resolution approach.
 */
object PptxReader {

    fun extractText(file: File): String {
        ZipFile(file).use { zip ->
            val presentationEntry = zip.getEntry("ppt/presentation.xml")
                ?: error("PPTX non valido: manca ppt/presentation.xml")
            val relIds = zip.getInputStream(presentationEntry).use { parseSlideRelIds(it) }

            val relsEntry = zip.getEntry("ppt/_rels/presentation.xml.rels")
                ?: error("PPTX non valido: manca ppt/_rels/presentation.xml.rels")
            val relTargets = zip.getInputStream(relsEntry).use { parseRelationships(it) }

            val sb = StringBuilder()
            relIds.forEach { relId ->
                val target = relTargets[relId] ?: return@forEach
                val slidePath = if (target.startsWith("/")) target.trimStart('/') else "ppt/$target"
                val entry = zip.getEntry(slidePath) ?: return@forEach
                sb.append(zip.getInputStream(entry).use { extractSlideText(it) })
                sb.append("\n\n")
            }
            return sb.toString().trim()
        }
    }

    private fun parseSlideRelIds(input: InputStream): List<String> {
        val ids = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "sldId") {
                parser.getAttributeValue(RELATIONSHIPS_NS, "id")?.let { ids.add(it) }
            }
            eventType = parser.next()
        }
        return ids
    }

    private fun parseRelationships(input: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) map[id] = target
            }
            eventType = parser.next()
        }
        return map
    }

    private fun extractSlideText(input: InputStream): String {
        val sb = StringBuilder()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")
        var inText = false
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "t") inText = true
                XmlPullParser.TEXT -> if (inText) sb.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "p" -> sb.append('\n')
                }
            }
            eventType = parser.next()
        }
        return sb.toString()
    }
}
