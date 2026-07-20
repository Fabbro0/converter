package com.megaconverter.app.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.megaconverter.app.converter.ConversionEngine
import com.megaconverter.app.converter.ConversionInput
import com.megaconverter.app.converter.ConversionResult
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.converter.converters.MultiImageToCbz
import com.megaconverter.app.converter.converters.MultiImageToPdf
import com.megaconverter.app.converter.converters.PdfMerge
import com.megaconverter.app.library.LibraryStore
import com.megaconverter.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(engine: ConversionEngine, initialUri: Uri?, onOpenLibrary: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var uiState by remember { mutableStateOf<UiState>(UiState.Idle) }
    var progress by remember { mutableFloatStateOf(0f) }

    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch { uiState = loadFile(context, engine, uri) }
        }
    }

    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            progress = 0f
            uiState = UiState.BatchProcessing("UNIONE IN CORSO…", "${uris.size} immagini → un unico PDF")
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val files = uris.map { uri ->
                            val name = FileUtils.displayNameFromUri(context, uri)
                            FileUtils.copyToCache(context, uri, name)
                        }
                        val outputFile = MultiImageToPdf.combine(context, files, "immagini_unite") { p ->
                            mainHandler.post { progress = p }
                        }
                        ConversionResult.Success(outputFile, FileFormat.PDF)
                    } catch (e: Exception) {
                        ConversionResult.Failure(e.message ?: "Errore durante l'unione delle immagini")
                    }
                }
                uiState = when (result) {
                    is ConversionResult.Success -> UiState.Success(null, result.outputFile, result.format)
                    is ConversionResult.Failure -> UiState.Error(result.message)
                }
            }
        }
    }

    val multiImageToCbzPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            progress = 0f
            uiState = UiState.BatchProcessing("UNIONE IN CORSO…", "${uris.size} immagini → un unico CBZ")
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val files = uris.map { uri ->
                            val name = FileUtils.displayNameFromUri(context, uri)
                            FileUtils.copyToCache(context, uri, name)
                        }
                        val outputFile = MultiImageToCbz.combine(context, files, "fumetto") { p ->
                            mainHandler.post { progress = p }
                        }
                        ConversionResult.Success(outputFile, FileFormat.CBZ)
                    } catch (e: Exception) {
                        ConversionResult.Failure(e.message ?: "Errore durante la creazione del CBZ")
                    }
                }
                uiState = when (result) {
                    is ConversionResult.Success -> UiState.Success(null, result.outputFile, result.format)
                    is ConversionResult.Failure -> UiState.Error(result.message)
                }
            }
        }
    }

    val multiPdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.size >= 2) {
            progress = 0f
            uiState = UiState.BatchProcessing("UNIONE PDF IN CORSO…", "${uris.size} PDF → un unico PDF")
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val files = uris.map { uri ->
                            val name = FileUtils.displayNameFromUri(context, uri)
                            FileUtils.copyToCache(context, uri, name)
                        }
                        val outputFile = PdfMerge.merge(context, files, "pdf_unito")
                        ConversionResult.Success(outputFile, FileFormat.PDF)
                    } catch (e: Exception) {
                        ConversionResult.Failure(e.message ?: "Errore durante l'unione dei PDF")
                    }
                }
                uiState = when (result) {
                    is ConversionResult.Success -> UiState.Success(null, result.outputFile, result.format)
                    is ConversionResult.Failure -> UiState.Error(result.message)
                }
            }
        } else if (uris.isNotEmpty()) {
            uiState = UiState.Error("Seleziona almeno 2 PDF da unire")
        }
    }

    val batchPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.size >= 2) {
            scope.launch { uiState = loadBatch(context, engine, uris) }
        } else if (uris.isNotEmpty()) {
            uiState = UiState.Error("Seleziona almeno 2 file dello stesso formato")
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { destUri ->
        val current = uiState
        if (destUri != null && current is UiState.Success) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    current.outputFile.inputStream().use { it.copyTo(out) }
                }
            }
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { uiState = loadFile(context, engine, it) }
    }

    fun startConversion(input: ConversionInput, target: FileFormat) {
        progress = 0f
        uiState = UiState.Converting(input, target)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                engine.convert(context, input, target) { p ->
                    mainHandler.post { progress = p }
                }
            }
            uiState = when (result) {
                is ConversionResult.Success -> UiState.Success(input, result.outputFile, result.format)
                is ConversionResult.Failure -> UiState.Error(
                    result.message,
                    previous = UiState.FileSelected(input, target, engine.supportedTargets(input.format)),
                )
            }
        }
    }

    fun startBatchConversion(inputs: List<ConversionInput>, target: FileFormat) {
        progress = 0f
        val label = "CONVERSIONE MULTIPLA IN CORSO…"
        uiState = UiState.BatchProcessing(label, "0 / ${inputs.size} file → .${target.extension.uppercase()}")
        scope.launch {
            val outputFiles = mutableListOf<File>()
            var failureMessage: String? = null
            withContext(Dispatchers.IO) {
                for ((index, input) in inputs.withIndex()) {
                    mainHandler.post {
                        progress = index / inputs.size.toFloat()
                        uiState = UiState.BatchProcessing(
                            label,
                            "${index + 1} / ${inputs.size} file → .${target.extension.uppercase()}",
                        )
                    }
                    when (val result = engine.convert(context, input, target)) {
                        is ConversionResult.Success -> outputFiles.add(result.outputFile)
                        is ConversionResult.Failure -> {
                            failureMessage = "Conversione fallita su '${input.displayName}': ${result.message}"
                        }
                    }
                    if (failureMessage != null) break
                }
            }
            uiState = when {
                failureMessage != null -> UiState.Error(failureMessage!!)
                outputFiles.size == 1 -> UiState.Success(null, outputFiles.first(), target)
                else -> {
                    val zipFile = withContext(Dispatchers.IO) {
                        val zip = FileUtils.newOutputFile(context, "conversioni_multiple", "zip")
                        ZipOutputStream(zip.outputStream()).use { out ->
                            outputFiles.forEach { file ->
                                out.putNextEntry(ZipEntry(file.name))
                                file.inputStream().use { it.copyTo(out) }
                                out.closeEntry()
                            }
                        }
                        zip
                    }
                    UiState.Success(null, zipFile, target)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MEGA CONVERTER") },
                actions = { TextButton(onClick = onOpenLibrary) { Text("LIBRERIA") } },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val state = uiState) {
                    is UiState.Idle -> IdleContent(
                        onPick = { pickFileLauncher.launch(arrayOf("*/*")) },
                        onPickMultiImages = { multiImagePickerLauncher.launch(arrayOf("image/*")) },
                        onPickMultiImagesToCbz = { multiImageToCbzPickerLauncher.launch(arrayOf("image/*")) },
                        onPickMultiPdf = { multiPdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onPickBatch = { batchPickerLauncher.launch(arrayOf("*/*")) },
                    )
                    is UiState.FileSelected -> FileSelectedContent(
                        state = state,
                        onTargetChange = { target -> uiState = state.copy(targetFormat = target) },
                        onConvert = { startConversion(state.input, state.targetFormat) },
                        onPickAnother = { pickFileLauncher.launch(arrayOf("*/*")) },
                    )
                    is UiState.BatchSelected -> BatchSelectedContent(
                        state = state,
                        onTargetChange = { target -> uiState = state.copy(targetFormat = target) },
                        onConvert = { startBatchConversion(state.inputs, state.targetFormat) },
                        onPickAnother = { batchPickerLauncher.launch(arrayOf("*/*")) },
                    )
                    is UiState.Converting -> ConvertingContent(state = state, progress = progress)
                    is UiState.BatchProcessing -> BatchProcessingContent(state = state, progress = progress)
                    is UiState.Success -> SuccessContent(
                        state = state,
                        onOpen = { context.startActivity(FileUtils.openIntent(context, state.outputFile, FileUtils.guessMimeType(state.outputFile, state.outputFormat))) },
                        onShare = { context.startActivity(FileUtils.shareIntent(context, state.outputFile, FileUtils.guessMimeType(state.outputFile, state.outputFormat))) },
                        onSaveAs = { saveAsLauncher.launch(state.outputFile.name) },
                        onConvertAnother = { uiState = UiState.Idle },
                    )
                    is UiState.Error -> ErrorContent(
                        state = state,
                        onRetry = { uiState = state.previous ?: UiState.Idle },
                        onPickAnother = { pickFileLauncher.launch(arrayOf("*/*")) },
                    )
                }
            }
        }
    }
}

private suspend fun loadFile(context: Context, engine: ConversionEngine, uri: Uri): UiState =
    withContext(Dispatchers.IO) {
        try {
            val displayName = FileUtils.displayNameFromUri(context, uri)
            val format = FileUtils.detectFormat(context, uri, displayName)
                ?: return@withContext UiState.Error("Formato non riconosciuto: $displayName")
            val file = FileUtils.copyToCache(context, uri, displayName)
            val input = ConversionInput(file, displayName, format)
            val targets = engine.supportedTargets(format)
            if (targets.isEmpty()) {
                return@withContext UiState.Error(
                    "Nessuna conversione disponibile al momento per .${format.extension}",
                )
            }
            UiState.FileSelected(input, targets.first(), targets)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Errore durante la selezione del file")
        }
    }

private suspend fun loadBatch(context: Context, engine: ConversionEngine, uris: List<Uri>): UiState =
    withContext(Dispatchers.IO) {
        try {
            val names = uris.map { FileUtils.displayNameFromUri(context, it) }
            val formats = uris.mapIndexed { i, uri -> FileUtils.detectFormat(context, uri, names[i]) }
            if (formats.any { it == null }) {
                return@withContext UiState.Error("Formato non riconosciuto per uno dei file selezionati")
            }
            val distinctFormats = formats.filterNotNull().distinct()
            if (distinctFormats.size > 1) {
                return@withContext UiState.Error(
                    "Per la conversione multipla scegli file dello stesso formato " +
                        "(trovati: ${distinctFormats.joinToString { it.extension.uppercase() }})",
                )
            }
            val format = distinctFormats.first()
            val targets = engine.supportedTargets(format)
            if (targets.isEmpty()) {
                return@withContext UiState.Error("Nessuna conversione disponibile al momento per .${format.extension}")
            }
            val inputs = uris.mapIndexed { i, uri ->
                val file = FileUtils.copyToCache(context, uri, names[i])
                ConversionInput(file, names[i], format)
            }
            UiState.BatchSelected(inputs, targets.first(), targets)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Errore durante la selezione dei file")
        }
    }

@Composable
private fun IdleContent(
    onPick: () -> Unit,
    onPickMultiImages: () -> Unit,
    onPickMultiImagesToCbz: () -> Unit,
    onPickMultiPdf: () -> Unit,
    onPickBatch: () -> Unit,
) {
    Spacer(Modifier.height(40.dp))
    Icon(
        imageVector = Icons.Filled.UploadFile,
        contentDescription = null,
        modifier = Modifier.size(88.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "CONVERTI QUALSIASI FILE",
        style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.height(12.dp))
    DotRow()
    Spacer(Modifier.height(12.dp))
    Text(
        "Immagini, documenti, ebook, audio e video: scegli un file e scegli in cosa trasformarlo.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.FileOpen, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("SCEGLI FILE")
    }
    Spacer(Modifier.height(12.dp))

    var toolsExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { toolsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("STRUMENTI ⌄")
        }
        DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Unisci più immagini in un PDF") },
                onClick = {
                    toolsExpanded = false
                    onPickMultiImages()
                },
            )
            DropdownMenuItem(
                text = { Text("Unisci più immagini in un CBZ") },
                onClick = {
                    toolsExpanded = false
                    onPickMultiImagesToCbz()
                },
            )
            DropdownMenuItem(
                text = { Text("Unisci più PDF in uno") },
                onClick = {
                    toolsExpanded = false
                    onPickMultiPdf()
                },
            )
            DropdownMenuItem(
                text = { Text("Converti più file insieme") },
                onClick = {
                    toolsExpanded = false
                    onPickBatch()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileSelectedContent(
    state: UiState.FileSelected,
    onTargetChange: (FileFormat) -> Unit,
    onConvert: () -> Unit,
    onPickAnother: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("FILE SELEZIONATO", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(state.input.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                "${FileUtils.humanFileSize(state.input.sourceFile.length())} · .${state.input.format.extension.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text("CONVERTI IN", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = ".${state.targetFormat.extension.uppercase()}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Formato di destinazione") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.availableTargets.groupBy { it.category.label }.forEach { (categoryLabel, formats) ->
                        formats.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(".${format.extension.uppercase()} · $categoryLabel") },
                                onClick = {
                                    onTargetChange(format)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onConvert, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("CONVERTI IN .${state.targetFormat.extension.uppercase()}")
    }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onPickAnother) { Text("SCEGLI UN ALTRO FILE") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchSelectedContent(
    state: UiState.BatchSelected,
    onTargetChange: (FileFormat) -> Unit,
    onConvert: () -> Unit,
    onPickAnother: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("FILE SELEZIONATI", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.inputs.size} file · .${state.inputs.first().format.extension.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(20.dp))
            Text("CONVERTI TUTTI IN", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = ".${state.targetFormat.extension.uppercase()}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Formato di destinazione") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.availableTargets.groupBy { it.category.label }.forEach { (categoryLabel, formats) ->
                        formats.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(".${format.extension.uppercase()} · $categoryLabel") },
                                onClick = {
                                    onTargetChange(format)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onConvert, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("CONVERTI ${state.inputs.size} FILE IN .${state.targetFormat.extension.uppercase()}")
    }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onPickAnother) { Text("SCEGLI ALTRI FILE") }
}

@Composable
private fun ConvertingContent(state: UiState.Converting, progress: Float) {
    Spacer(Modifier.height(64.dp))
    if (progress > 0f) {
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(72.dp))
    } else {
        CircularProgressIndicator(modifier = Modifier.size(72.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text(
        "CONVERSIONE IN CORSO…",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "${state.input.displayName} → .${state.targetFormat.extension.uppercase()}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (progress > 0f) {
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BatchProcessingContent(state: UiState.BatchProcessing, progress: Float) {
    Spacer(Modifier.height(64.dp))
    if (progress > 0f) {
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(72.dp))
    } else {
        CircularProgressIndicator(modifier = Modifier.size(72.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text(state.label, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        state.detail,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (progress > 0f) {
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SuccessContent(
    state: UiState.Success,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSaveAs: () -> Unit,
    onConvertAnother: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savedToLibrary by remember(state) { mutableStateOf(false) }

    Spacer(Modifier.height(48.dp))
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(16.dp))
    Text("CONVERSIONE COMPLETATA", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    DotRow()
    Spacer(Modifier.height(8.dp))
    Text(
        "${state.outputFile.name} · ${FileUtils.humanFileSize(state.outputFile.length())}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onOpen) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("APRI")
        }
        OutlinedButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("CONDIVIDI")
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onSaveAs) { Text("SALVA CON NOME…") }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    LibraryStore.addItem(context, state.outputFile, state.outputFile.name, state.outputFormat)
                }
                savedToLibrary = true
            }
        },
        enabled = !savedToLibrary,
    ) {
        Text(if (savedToLibrary) "SALVATO IN LIBRERIA ✓" else "SALVA IN LIBRERIA")
    }
    Spacer(Modifier.height(20.dp))
    Button(onClick = onConvertAnother) { Text("CONVERTI UN ALTRO FILE") }
}

@Composable
private fun ErrorContent(state: UiState.Error, onRetry: () -> Unit, onPickAnother: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Icon(
        imageVector = Icons.Filled.Error,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(16.dp))
    Text("QUALCOSA È ANDATO STORTO", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        state.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    if (state.previous != null) {
        Button(onClick = onRetry) { Text("RIPROVA") }
        Spacer(Modifier.height(12.dp))
    }
    TextButton(onClick = onPickAnother) { Text("SCEGLI UN ALTRO FILE") }
}

/** Small square dot-matrix accent row, echoing Nothing's pixel-grid glyph language. */
@Composable
private fun DotRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) { index ->
            val color = if (index % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Box(modifier = Modifier.size(4.dp).background(color))
        }
    }
}
