package com.megaconverter.app.converter.converters

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileConverter
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.converter.FormatCategory
import com.megaconverter.app.util.FileUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Audio/video transcoding via ffmpeg-kit. Video -> audio targets also work
 * (e.g. mp4 -> mp3): ffmpeg is invoked with -vn to drop the video stream.
 */
class AudioVideoConverter : FileConverter {

    private val audioFormats = FileFormat.entries.filter { it.category == FormatCategory.AUDIO }.toSet()
    private val videoFormats = FileFormat.entries.filter { it.category == FormatCategory.VIDEO }.toSet()

    override fun canConvert(from: FileFormat, to: FileFormat): Boolean {
        val fromMedia = from.category == FormatCategory.AUDIO || from.category == FormatCategory.VIDEO
        val toMedia = to.category == FormatCategory.AUDIO || to.category == FormatCategory.VIDEO
        return fromMedia && toMedia && from != to
    }

    override fun possibleOutputs(from: FileFormat): Set<FileFormat> = when (from.category) {
        FormatCategory.AUDIO -> audioFormats - from
        FormatCategory.VIDEO -> (videoFormats + audioFormats) - from
        else -> emptySet()
    }

    override suspend fun convert(
        context: Context,
        input: ConversionInput,
        targetFormat: FileFormat,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        val outputFile = FileUtils.newOutputFile(context, input.displayName, targetFormat)
        val durationMs = probeDurationMs(input.sourceFile)
        val extractAudioOnly = input.format.category == FormatCategory.VIDEO &&
            targetFormat.category == FormatCategory.AUDIO
        val command = buildCommand(input.sourceFile, outputFile, extractAudioOnly)

        return suspendCancellableCoroutine { cont ->
            val session = FFmpegKit.executeAsync(
                command,
                { session ->
                    val returnCode = session.returnCode
                    val result = when {
                        ReturnCode.isSuccess(returnCode) -> ConversionResult.Success(outputFile, targetFormat)
                        ReturnCode.isCancel(returnCode) -> ConversionResult.Failure("Conversione annullata")
                        else -> ConversionResult.Failure(
                            "Conversione audio/video fallita (codice ${returnCode?.value})",
                        )
                    }
                    if (cont.isActive) cont.resume(result)
                },
                { /* ignore raw ffmpeg logs */ },
                { statistics ->
                    if (durationMs > 0) {
                        val progress = (statistics.time / durationMs.toFloat()).coerceIn(0f, 0.99f)
                        onProgress(progress)
                    }
                },
            )
            cont.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
        }
    }

    private fun buildCommand(input: File, output: File, extractAudioOnly: Boolean): String {
        val vnFlag = if (extractAudioOnly) "-vn " else ""
        return "-y -i \"${input.absolutePath}\" $vnFlag\"${output.absolutePath}\""
    }

    private fun probeDurationMs(file: File): Long = try {
        val info = FFprobeKit.getMediaInformation(file.absolutePath).mediaInformation
        val seconds = info?.duration?.toDoubleOrNull()
        if (seconds != null) (seconds * 1000).toLong() else -1L
    } catch (e: Exception) {
        -1L
    }
}
