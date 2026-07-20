package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File

/** Paginates plain text onto A4 PDF pages. Shared by TXT -> PDF and DOCX -> PDF. */
object TextPdfRenderer {

    fun renderToPdf(context: Context, baseName: String, text: String, onProgress: (Float) -> Unit): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = (pageWidth - margin * 2).toInt().coerceAtLeast(1)
        val contentHeight = (pageHeight - margin * 2).toInt().coerceAtLeast(1)

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val safeText = text.ifEmpty { " " }
        val layout = StaticLayout.Builder
            .obtain(safeText, 0, safeText.length, textPaint, contentWidth)
            .setLineSpacing(0f, 1.15f)
            .build()
        val totalHeight = layout.height.coerceAtLeast(1)

        val pdfDocument = PdfDocument()
        var currentY = 0
        var pageNumber = 1
        while (currentY < totalHeight) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.save()
            canvas.translate(margin, margin)
            canvas.clipRect(0f, 0f, contentWidth.toFloat(), contentHeight.toFloat())
            canvas.translate(0f, -currentY.toFloat())
            layout.draw(canvas)
            canvas.restore()
            pdfDocument.finishPage(page)

            currentY += contentHeight
            pageNumber++
            onProgress((currentY.toFloat() / totalHeight).coerceAtMost(0.95f))
        }

        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.PDF)
        outputFile.outputStream().use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return outputFile
    }
}
