package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Builds a minimal, spec-valid single-chapter EPUB3 from plain text. */
object EpubWriter {

    fun write(context: Context, title: String, bodyText: String, baseName: String): File {
        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.EPUB)
        val uuid = "urn:uuid:${UUID.randomUUID()}"

        val paragraphs = bodyText.split(Regex("\n\\s*\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val bodyHtml = if (paragraphs.isEmpty()) {
            "<p></p>"
        } else {
            paragraphs.joinToString("\n") { "<p>${escapeXml(it)}</p>" }
        }
        val safeTitle = escapeXml(title.ifBlank { "Documento" })

        val chapterXhtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>$safeTitle</title></head>
            <body>
            $bodyHtml
            </body>
            </html>
        """.trimIndent()

        val navXhtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
            <head><title>Indice</title></head>
            <body>
            <nav epub:type="toc" id="toc">
            <ol><li><a href="chapter1.xhtml">$safeTitle</a></li></ol>
            </nav>
            </body>
            </html>
        """.trimIndent()

        val contentOpf = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid">
            <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:identifier id="bookid">$uuid</dc:identifier>
            <dc:title>$safeTitle</dc:title>
            <dc:language>it</dc:language>
            <meta property="dcterms:modified">${isoTimestamp()}</meta>
            </metadata>
            <manifest>
            <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
            <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
            </manifest>
            <spine>
            <itemref idref="chapter1"/>
            </spine>
            </package>
        """.trimIndent()

        val containerXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            </rootfiles>
            </container>
        """.trimIndent()

        ZipOutputStream(outputFile.outputStream()).use { zip ->
            writeStoredMimetype(zip)
            writeEntry(zip, "META-INF/container.xml", containerXml)
            writeEntry(zip, "OEBPS/content.opf", contentOpf)
            writeEntry(zip, "OEBPS/nav.xhtml", navXhtml)
            writeEntry(zip, "OEBPS/chapter1.xhtml", chapterXhtml)
        }
        return outputFile
    }

    /** The "mimetype" entry must be first in the zip and stored uncompressed per the EPUB spec. */
    private fun writeStoredMimetype(zip: ZipOutputStream) {
        val bytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
        val entry = ZipEntry("mimetype")
        entry.method = ZipEntry.STORED
        entry.size = bytes.size.toLong()
        entry.compressedSize = bytes.size.toLong()
        val crc = CRC32()
        crc.update(bytes)
        entry.crc = crc.value
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun isoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
