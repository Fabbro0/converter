package com.megaconverter.app.converter.converters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.converter.FormatCategory
import com.megaconverter.app.util.FileUtils
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Image -> PDF (one page) and PDF -> image (one file per page, zipped if there's more than one). */
class ImagePdfConverter : FileConverter {

    private val imageFormats = FileFormat.entries.filter { it.category == FormatCategory.IMAGE }.toSet()

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        (from.category == FormatCategory.IMAGE && to == FileFormat.PDF) ||
            (from == FileFormat.PDF && to.category == FormatCategory.IMAGE)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> = when {
        from.category == FormatCategory.IMAGE -> setOf(FileFormat.PDF)
        from == FileFormat.PDF -> imageFormats
        else -> emptySet()
    }

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult = if (input.format == FileFormat.PDF) {
        pdfToImages(context, input, targetFormat, onProgress)
    } else {
        imageToPdf(context, input, onProgress)
    }

    private fun imageToPdf(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        onProgress(0.1f)
        val bitmap = BitmapFactory.decodeFile(input.sourceFile.absolutePath)
            ?: return ConversionResult.Failure("Impossibile leggere l'immagine sorgente")

        val renderDpi = 150f
        val pageWidth = (bitmap.width * 72f / renderDpi).toInt().coerceAtLeast(1)
        val pageHeight = (bitmap.height * 72f / renderDpi).toInt().coerceAtLeast(1)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, null, Rect(0, 0, pageWidth, pageHeight), null)
        pdfDocument.finishPage(page)
        onProgress(0.7f)

        val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.PDF)
        outputFile.outputStream().use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        bitmap.recycle()
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.PDF)
    }

    private fun pdfToImages(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        val pfd = ParcelFileDescriptor.open(input.sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        if (pageCount == 0) {
            renderer.close()
            pfd.close()
            return ConversionResult.Failure("Il PDF non contiene pagine")
        }

        val targetDpi = 200f
        val baseName = input.displayName.substringBeforeLast('.', input.displayName)
        val pageFiles = mutableListOf<File>()

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val scale = targetDpi / 72f
            val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
            val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val pageBaseName = if (pageCount > 1) "${baseName}_page${i + 1}" else baseName
            val pageFile = FileUtils.newOutputFile(context, pageBaseName, targetFormat)
            val ok = pageFile.outputStream().use { out -> ImageEncoder.write(bitmap, targetFormat, out) }
            bitmap.recycle()
            if (!ok) {
                renderer.close()
                pfd.close()
                return ConversionResult.Failure("Formato immagine di destinazione non supportato")
            }
            pageFiles.add(pageFile)
            onProgress((i + 1f) / pageCount * 0.9f)
        }
        renderer.close()
        pfd.close()

        if (pageFiles.size == 1) {
            onProgress(1f)
            return ConversionResult.Success(pageFiles.first(), targetFormat)
        }

        val zipFile = FileUtils.newOutputFile(context, baseName, "zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            pageFiles.forEach { pageFile ->
                zip.putNextEntry(ZipEntry(pageFile.name))
                pageFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                pageFile.delete()
            }
        }
        onProgress(1f)
        return ConversionResult.Success(zipFile, targetFormat)
    }
}
