package com.megaconverter.app.converter

import android.content.Context
import java.io.File

data class ConversionInput(
    val sourceFile: File,
    val displayName: String,
    val format: FileFormat,
)

sealed interface ConversionResult {
    data class Success(val outputFile: File, val format: FileFormat) : ConversionResult
    data class Failure(val message: String, val cause: Throwable? = null) : ConversionResult
}

/**
 * A single conversion capability. Implementations declare which (from -> to) pairs
 * they support; the [ConversionEngine] picks the first matching converter at runtime.
 */
interface FileConverter {
    fun canConvert(from: FileFormat, to: FileFormat): Boolean

    fun possibleOutputs(from: FileFormat): Set<FileFormat>

    /**
     * Runs the conversion and returns the produced file. Converters create their own
     * output file(s) via [com.megaconverter.app.util.FileUtils.newOutputFile] since some
     * conversions (e.g. a multi-page PDF exported as images) don't map to a single file
     * matching [targetFormat]'s extension (a .zip is produced instead).
     */
    suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult
}
