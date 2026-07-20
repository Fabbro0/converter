package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File

/** Combines several images (already picked, one page per image, in list order) into a single PDF. */
object MultiImageToPdf {

    fun combine(context: Context, files: List<File>, baseName: String, onProgress: (Float) -> Unit): File {
        val pdfDocument = PdfDocument()
        val renderDpi = 150f

        files.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: error("Impossibile leggere ${file.name} come immagine")

            val pageWidth = (bitmap.width * 72f / renderDpi).toInt().coerceAtLeast(1)
            val pageHeight = (bitmap.height * 72f / renderDpi).toInt().coerceAtLeast(1)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, null, Rect(0, 0, pageWidth, pageHeight), null)
            pdfDocument.finishPage(page)
            bitmap.recycle()

            onProgress((index + 1f) / files.size * 0.95f)
        }

        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.PDF)
        outputFile.outputStream().use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return outputFile
    }
}
