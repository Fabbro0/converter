package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File

/** OCR entry point: image or PDF -> a .txt with the recognized text. For PDFs, each
 * page is rasterized (same approach as CbzConverter/ImagePdfConverter) and OCR'd in
 * turn — this is for scanned/image-only PDFs, unlike TextPdfConverter's PDF->TXT which
 * reads a PDF's real embedded text layer and is far more accurate when one exists. */
object OcrTool {

    suspend fun recognizeToTextFile(
        context: Context,
        sourceFile: File,
        sourceFormat: FileFormat,
        baseName: String,
        onProgress: (Float) -> Unit,
    ): File {
        val text = if (sourceFormat == FileFormat.PDF) {
            recognizePdf(sourceFile, onProgress)
        } else {
            onProgress(0.2f)
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
                ?: error("Impossibile leggere l'immagine")
            val recognized = OcrEngine.recognizeText(bitmap)
            bitmap.recycle()
            onProgress(0.9f)
            recognized
        }
        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.TXT)
        outputFile.writeText(text, Charsets.UTF_8)
        onProgress(1f)
        return outputFile
    }

    private suspend fun recognizePdf(file: File, onProgress: (Float) -> Unit): String {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        val sb = StringBuilder()
        val targetDpi = 200f

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val scale = targetDpi / 72f
            val bitmap = Bitmap.createBitmap(
                (page.width * scale).toInt().coerceAtLeast(1),
                (page.height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            sb.append(OcrEngine.recognizeText(bitmap))
            sb.append("\n\n")
            bitmap.recycle()
            onProgress((i + 1f) / pageCount * 0.95f)
        }
        renderer.close()
        pfd.close()
        return sb.toString().trim()
    }
}
