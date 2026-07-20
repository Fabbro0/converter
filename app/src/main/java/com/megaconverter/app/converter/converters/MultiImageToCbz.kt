package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.util.FileUtils
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Zips several images (in list order) into a .cbz, one page per image, no re-encoding. */
object MultiImageToCbz {

    fun combine(context: Context, files: List<File>, baseName: String, onProgress: (Float) -> Unit): File {
        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.CBZ)
        val digits = files.size.toString().length
        ZipOutputStream(outputFile.outputStream()).use { zip ->
            files.forEachIndexed { index, file ->
                val ext = FileUtils.extensionOf(file.name) ?: "jpg"
                zip.putNextEntry(ZipEntry("page_${(index + 1).toString().padStart(digits, '0')}.$ext"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                onProgress((index + 1f) / files.size * 0.95f)
            }
        }
        onProgress(1f)
        return outputFile
    }
}
