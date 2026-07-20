package com.megaconverter.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.megaconverter.app.converter.ConversionEngine
import com.megaconverter.app.converter.FileFormat
import java.io.File

private sealed interface Screen {
    data object Converter : Screen
    data object Library : Screen
    data class Reader(val file: File, val format: FileFormat, val displayName: String) : Screen
}

/** Top-level screen switcher (Converter / Library / Reader). No Navigation-Compose
 * dependency: three screens and a linear back path don't need a back-stack library. */
@Composable
fun ConverterAppRoot(engine: ConversionEngine, initialUri: Uri?) {
    var screen by remember { mutableStateOf<Screen>(Screen.Converter) }
    val current = screen

    BackHandler(enabled = current !is Screen.Converter) {
        screen = if (current is Screen.Reader) Screen.Library else Screen.Converter
    }

    when (current) {
        is Screen.Converter -> ConverterScreen(
            engine = engine,
            initialUri = initialUri,
            onOpenLibrary = { screen = Screen.Library },
        )
        is Screen.Library -> LibraryScreen(
            onBack = { screen = Screen.Converter },
            onOpenReader = { file, format, name -> screen = Screen.Reader(file, format, name) },
        )
        is Screen.Reader -> ReaderScreen(
            file = current.file,
            format = current.format,
            displayName = current.displayName,
            onBack = { screen = Screen.Library },
        )
    }
}
