package com.megaconverter.app.converter.converters

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Reads the first worksheet of an .xlsx into a rectangular grid of cell text.
 * Handles the common cases (shared-string cells, inline strings, plain numeric/text
 * values) but not every OOXML feature — formulas are read as their cached value only,
 * and only the first sheet (xl/worksheets/sheet1.xml) is read, which covers the vast
 * majority of simple/single-sheet spreadsheets including everything XlsxWriter itself
 * produces.
 */
object XlsxReader {

    fun readRows(file: File): List<List<String>> {
        ZipFile(file).use { zip ->
            val sharedStrings = zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
                zip.getInputStream(entry).use { parseSharedStrings(it) }
            } ?: emptyList()

            val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml")
                ?: error("XLSX non valido: manca xl/worksheets/sheet1.xml")
            return zip.getInputStream(sheetEntry).use { parseSheet(it, sharedStrings) }
        }
    }

    private fun parseSharedStrings(input: InputStream): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")

        var inSi = false
        val textBuilder = StringBuilder()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "si") {
                    inSi = true
                    textBuilder.setLength(0)
                }
                XmlPullParser.TEXT -> if (inSi) textBuilder.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    result.add(textBuilder.toString())
                    inSi = false
                }
            }
            eventType = parser.next()
        }
        return result
    }

    private fun parseSheet(input: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableMap<Int, String>>()
        var maxCol = -1

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, "UTF-8")

        var currentRow: MutableMap<Int, String>? = null
        var currentCellIndex = -1
        var currentCellType: String? = null
        var inValue = false
        var inInlineText = false
        val valueBuilder = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableMapOf()
                    "c" -> {
                        currentCellType = parser.getAttributeValue(null, "t")
                        val ref = parser.getAttributeValue(null, "r")
                        currentCellIndex = if (ref != null) columnIndexFromRef(ref) else currentCellIndex + 1
                        valueBuilder.setLength(0)
                    }
                    "v" -> inValue = true
                    "t" -> if (currentCellType == "inlineStr") inInlineText = true
                }
                XmlPullParser.TEXT -> if (inValue || inInlineText) valueBuilder.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inValue = false
                    "t" -> inInlineText = false
                    "c" -> {
                        val raw = valueBuilder.toString()
                        val value = if (currentCellType == "s") {
                            raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                        } else {
                            raw
                        }
                        currentRow?.put(currentCellIndex, value)
                        if (currentCellIndex > maxCol) maxCol = currentCellIndex
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                    }
                }
            }
            eventType = parser.next()
        }

        if (maxCol < 0) return emptyList()
        return rows.map { rowMap -> (0..maxCol).map { col -> rowMap[col] ?: "" } }
    }

    private fun columnIndexFromRef(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }
}
