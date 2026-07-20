package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import java.io.File

/**
 * Merges several existing PDFs into one, in list order, preserving real page content
 * (text/vectors) rather than rasterizing — PdfBox-Android's PDFMergerUtility copies the
 * actual page trees, unlike android.graphics.pdf which can only draw new bitmap content.
 */
object PdfMerge {

    fun merge(context: Context, files: List<File>, baseName: String): File {
        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.PDF)
        val merger = PDFMergerUtility()
        files.forEach { merger.addSource(it) }
        merger.setDestinationFileName(outputFile.absolutePath)
        merger.mergeDocuments(null)
        return outputFile
    }
}
