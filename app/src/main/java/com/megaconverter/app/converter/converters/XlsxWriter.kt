package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Builds a minimal, spec-valid single-sheet .xlsx from a grid of cell text, using
 * inline strings so no separate sharedStrings.xml part is needed. */
object XlsxWriter {

    fun write(context: Context, rows: List<List<String>>, baseName: String): File {
        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.XLSX)

        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent()

        val rootRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()

        val workbookXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
            <sheet name="Foglio1" sheetId="1" r:id="rId1"/>
            </sheets>
            </workbook>
        """.trimIndent()

        val workbookRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()

        val sheetXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
            append("<sheetData>\n")
            rows.forEachIndexed { rowIndex, row ->
                append("<row r=\"${rowIndex + 1}\">\n")
                row.forEachIndexed { colIndex, value ->
                    if (value.isNotEmpty()) {
                        val cellRef = "${columnIndexToLetters(colIndex)}${rowIndex + 1}"
                        append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        append(escapeXml(value))
                        append("</t></is></c>\n")
                    }
                }
                append("</row>\n")
            }
            append("</sheetData>\n")
            append("</worksheet>\n")
        }

        ZipOutputStream(outputFile.outputStream()).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypes)
            writeEntry(zip, "_rels/.rels", rootRels)
            writeEntry(zip, "xl/workbook.xml", workbookXml)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels)
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return outputFile
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun columnIndexToLetters(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, 'A' + rem)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
