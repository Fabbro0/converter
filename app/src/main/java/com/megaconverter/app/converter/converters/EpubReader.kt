package com.megaconverter.app.converter.converters

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Minimal EPUB text extractor: resolves META-INF/container.xml -> the OPF package
 * document -> spine order -> each XHTML chapter's text, in reading order.
 *
 * Chapter bodies are parsed with a tolerant regex-based tag strip rather than a strict
 * XML parser, since real-world EPUB XHTML often contains bare HTML entities (&nbsp; and
 * friends) that aren't valid XML without a DTD and would otherwise abort a strict parse.
 */
object EpubReader {

    fun extractText(file: File): String {
        ZipFile(file).use { zip ->
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: error("EPUB non valido: manca META-INF/container.xml")
            val opfPath = zip.getInputStream(containerEntry).use { parseOpfPath(it) }

            val opfEntry = zip.getEntry(opfPath) ?: error("EPUB non valido: file OPF '$opfPath' non trovato")
            val opfDir = opfPath.substringBeforeLast('/', "")
            val (manifest, spine) = zip.getInputStream(opfEntry).use { parseOpf(it) }

            val sb = StringBuilder()
            spine.forEach { idref ->
                val href = manifest[idref] ?: return@forEach
                val fullPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
                val entry = zip.getEntry(fullPath) ?: return@forEach
                val raw = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                sb.append(stripXhtmlToText(raw))
                sb.append("\n\n")
            }
            return sb.toString().trim()
        }
    }

    private fun parseOpfPath(input: InputStream): String {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                parser.getAttributeValue(null, "full-path")?.let { return it }
            }
            eventType = parser.next()
        }
        error("EPUB non valido: elemento <rootfile> non trovato")
    }

    private fun parseOpf(input: InputStream): Pair<Map<String, String>, List<String>> {
        val manifest = mutableMapOf<String, String>()
        val spine = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        val mediaType = parser.getAttributeValue(null, "media-type")
                        if (id != null && href != null && mediaType?.contains("html") == true) {
                            manifest[id] = href
                        }
                    }
                    "itemref" -> parser.getAttributeValue(null, "idref")?.let { spine.add(it) }
                }
            }
            eventType = parser.next()
        }
        return manifest to spine
    }

    private val blockClosingTags = Regex("(?i)</(p|div|h[1-6]|li|tr)>")
    private val breakTags = Regex("(?i)<br\\s*/?>")
    private val anyTag = Regex("<[^>]+>")

    private fun stripXhtmlToText(raw: String): String {
        var text = blockClosingTags.replace(raw, "\n")
        text = breakTags.replace(text, "\n")
        text = anyTag.replace(text, "")
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .lines()
            .joinToString("\n") { it.trim() }
    }
}
