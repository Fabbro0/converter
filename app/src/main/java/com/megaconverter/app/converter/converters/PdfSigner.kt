package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.Bitmap
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File

/** Stamps a finger-drawn signature bitmap onto the bottom-right of a PDF's last page,
 * appending to the existing page content (PDPageContentStream.AppendMode.APPEND) rather
 * than rasterizing the page — the rest of the document keeps its original quality. */
object PdfSigner {

    fun sign(context: Context, sourceFile: File, signature: Bitmap, baseName: String): File {
        val document = PDDocument.load(sourceFile)
        try {
            val page = document.getPage(document.numberOfPages - 1)
            val pdImage = LosslessFactory.createFromImage(document, signature)

            val pageWidth = page.mediaBox.width
            val marginPt = 36f
            val sigWidthPt = (pageWidth * 0.3f).coerceAtMost(220f)
            val sigHeightPt = sigWidthPt * signature.height / signature.width.toFloat()
            val x = pageWidth - sigWidthPt - marginPt
            val y = marginPt

            val stream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
            try {
                stream.drawImage(pdImage, x, y, sigWidthPt, sigHeightPt)
            } finally {
                stream.close()
            }

            val outputFile = FileUtils.newOutputFile(context, "${baseName}_firmato", FileFormat.PDF)
            document.save(outputFile)
            return outputFile
        } finally {
            document.close()
        }
    }
}
