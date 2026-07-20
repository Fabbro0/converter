package com.megaconverter.app.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
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
import com.megaconverter.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(engine: ConversionEngine, initialUri: Uri?) {
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mega Converter", fontWeight = FontWeight.SemiBold) })
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
                    is UiState.Idle -> IdleContent(onPick = { pickFileLauncher.launch(arrayOf("*/*")) })
                    is UiState.FileSelected -> FileSelectedContent(
                        state = state,
                        onTargetChange = { target -> uiState = state.copy(targetFormat = target) },
                        onConvert = { startConversion(state.input, state.targetFormat) },
                        onPickAnother = { pickFileLauncher.launch(arrayOf("*/*")) },
                    )
                    is UiState.Converting -> ConvertingContent(state = state, progress = progress)
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

@Composable
private fun IdleContent(onPick: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Icon(
        imageVector = Icons.Filled.UploadFile,
        contentDescription = null,
        modifier = Modifier.size(96.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(24.dp))
    Text(
        "Converti qualsiasi file",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Immagini, documenti, audio e video: scegli un file e scegli in cosa trasformarlo.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onPick) {
        Icon(Icons.Filled.FileOpen, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Scegli file")
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

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("File selezionato", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(state.input.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                "${FileUtils.humanFileSize(state.input.sourceFile.length())} · .${state.input.format.extension.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text("Converti in", style = MaterialTheme.typography.labelLarge)
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
        Text("Converti in .${state.targetFormat.extension.uppercase()}")
    }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onPickAnother) { Text("Scegli un altro file") }
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
        "Conversione in corso…",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
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
private fun SuccessContent(
    state: UiState.Success,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSaveAs: () -> Unit,
    onConvertAnother: () -> Unit,
) {
    Spacer(Modifier.height(48.dp))
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(16.dp))
    Text("Conversione completata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            Text("Apri")
        }
        OutlinedButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Condividi")
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onSaveAs) { Text("Salva con nome…") }
    Spacer(Modifier.height(20.dp))
    Button(onClick = onConvertAnother) { Text("Converti un altro file") }
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
    Text("Qualcosa è andato storto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        state.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    if (state.previous != null) {
        Button(onClick = onRetry) { Text("Riprova") }
        Spacer(Modifier.height(12.dp))
    }
    TextButton(onClick = onPickAnother) { Text("Scegli un altro file") }
}
