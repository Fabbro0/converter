package com.megaconverter.app.converter.converters

import android.content.Context
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.library.LibraryStore
import com.megaconverter.app.util.FileUtils
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Generic archive tool: zip arbitrary files together, or unpack a zip. */
object ZipTool {

    fun createZip(context: Context, files: List<File>, baseName: String, onProgress: (Float) -> Unit): File {
        val outputFile = FileUtils.newOutputFile(context, baseName, FileFormat.ZIP)
        ZipOutputStream(outputFile.outputStream()).use { zip ->
            files.forEachIndexed { index, file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                onProgress((index + 1f) / files.size * 0.95f)
            }
        }
        onProgress(1f)
        return outputFile
    }

    /**
     * Extracts entries with a recognized [FileFormat] straight into the library. A
     * zip's contents are inherently a set of possibly-mixed-type files, which doesn't
     * fit the single-file result screen the rest of the app uses for a conversion — the
     * library (already the place for "several saved files") is the natural landing
     * spot. Returns (added, skipped) counts; entries with an unrecognized extension are
     * skipped rather than guessed at.
     */
    fun extractToLibrary(context: Context, zipFile: File): Pair<Int, Int> {
        var added = 0
        var skipped = 0
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val name = File(entry.name).name
                val format = FileUtils.extensionOf(name)?.let { FileFormat.fromExtension(it) }
                if (format == null) {
                    skipped++
                    return@forEach
                }
                val tempFile = File(context.cacheDir, "zip_extract_${System.nanoTime()}_$name")
                zip.getInputStream(entry).use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                LibraryStore.addItem(context, tempFile, name, format)
                tempFile.delete()
                added++
            }
        }
        return added to skipped
    }
}
