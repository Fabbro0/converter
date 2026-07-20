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
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private val CBZ_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")

/** CBZ (comic book zip) is just an ordered zip of page images. CBZ<->PDF and
 * image(s)->CBZ reuse the same page-rendering logic as PdfMerge/ImagePdfConverter. */
class CbzConverter : FileConverter {

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean =
        (from == FileFormat.CBZ && to == FileFormat.PDF) ||
            (from == FileFormat.PDF && to == FileFormat.CBZ) ||
            (from.category == FormatCategory.IMAGE && to == FileFormat.CBZ)

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> = when {
        from == FileFormat.CBZ -> setOf(FileFormat.PDF)
        from == FileFormat.PDF -> setOf(FileFormat.CBZ)
        from.category == FormatCategory.IMAGE -> setOf(FileFormat.CBZ)
        else -> emptySet()
    }

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult = when {
        input.format == FileFormat.CBZ && targetFormat == FileFormat.PDF -> cbzToPdf(context, input, onProgress)
        input.format == FileFormat.PDF && targetFormat == FileFormat.CBZ -> pdfToCbz(context, input, onProgress)
        input.format.category == FormatCategory.IMAGE && targetFormat == FileFormat.CBZ ->
            imageToCbz(context, input, onProgress)
        else -> ConversionResult.Failure("Conversione CBZ non supportata")
    }

    private fun cbzToPdf(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        val zip = ZipFile(input.sourceFile)
        val imageEntries = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.substringAfterLast('.', "").lowercase() in CBZ_IMAGE_EXTENSIONS }
            .sortedWith(compareBy(NaturalSort.comparator) { it.name })
            .toList()
        if (imageEntries.isEmpty()) {
            zip.close()
            return ConversionResult.Failure("Il file CBZ non contiene immagini")
        }

        val renderDpi = 150f
        val pdfDocument = PdfDocument()
        imageEntries.forEachIndexed { index, entry ->
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                val pageWidth = (bitmap.width * 72f / renderDpi).toInt().coerceAtLeast(1)
                val pageHeight = (bitmap.height * 72f / renderDpi).toInt().coerceAtLeast(1)
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, null, Rect(0, 0, pageWidth, pageHeight), null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }
            onProgress((index + 1f) / imageEntries.size * 0.95f)
        }
        zip.close()

        val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.PDF)
        outputFile.outputStream().use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.PDF)
    }

    private fun pdfToCbz(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        val pfd = ParcelFileDescriptor.open(input.sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        if (pageCount == 0) {
            renderer.close()
            pfd.close()
            return ConversionResult.Failure("Il PDF non contiene pagine")
        }

        val targetDpi = 200f
        val digits = pageCount.toString().length
        val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.CBZ)
        ZipOutputStream(outputFile.outputStream()).use { zip ->
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

                zip.putNextEntry(ZipEntry("page_${(i + 1).toString().padStart(digits, '0')}.jpg"))
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, zip)
                zip.closeEntry()
                bitmap.recycle()
                onProgress((i + 1f) / pageCount * 0.95f)
            }
        }
        renderer.close()
        pfd.close()
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.CBZ)
    }

    private fun imageToCbz(context: Context, input: ConversionInput, onProgress: (Float) -> Unit): ConversionResult {
        onProgress(0.2f)
        val outputFile = FileUtils.newOutputFile(context, input.displayName, FileFormat.CBZ)
        ZipOutputStream(outputFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("page_001.${input.format.extension}"))
            input.sourceFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
        onProgress(1f)
        return ConversionResult.Success(outputFile, FileFormat.CBZ)
    }
}
