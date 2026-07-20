package com.megaconverter.app.library

import com.megaconverter.app.converter.FileFormat

data class LibraryItem(
    val id: String,
    /** File name inside the app's private library storage directory. */
    val fileName: String,
    val displayName: String,
    val format: FileFormat,
    val tags: List<String>,
    val addedAt: Long,
)
