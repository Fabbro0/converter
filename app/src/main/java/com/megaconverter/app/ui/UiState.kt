package com.megaconverter.app.ui

import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.FileFormat
import java.io.File

sealed interface UiState {
    data object Idle : UiState
    data class FileSelected(
        val input: ConversionInput,
        val targetFormat: FileFormat,
        val availableTargets: List<FileFormat>,
    ) : UiState
    data class Converting(val input: ConversionInput, val targetFormat: FileFormat) : UiState
    data class BatchSelected(
        val inputs: List<ConversionInput>,
        val targetFormat: FileFormat,
        val availableTargets: List<FileFormat>,
    ) : UiState
    /** Covers every "many files in, one operation, one result" flow: image merge, PDF
     * merge, batch conversion. [label] and [detail] are the two lines shown while it runs. */
    data class BatchProcessing(val label: String, val detail: String) : UiState
    data class Signing(val input: ConversionInput) : UiState
    data class Success(val input: ConversionInput?, val outputFile: File, val outputFormat: FileFormat) : UiState
    data class Error(val message: String, val previous: FileSelected? = null) : UiState
}
