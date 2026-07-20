package com.megaconverter.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.library.LibraryItem
import com.megaconverter.app.library.LibraryStore
import com.megaconverter.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class LibrarySort(val label: String) {
    RECENT("Più recenti"),
    NAME("Nome (A-Z)"),
    SIZE("Dimensione"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit, onOpenReader: (File, FileFormat, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var tagDialogItem by remember { mutableStateOf<LibraryItem?>(null) }
    var addError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(LibrarySort.RECENT) }
    var sortExpanded by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch { items = withContext(Dispatchers.IO) { LibraryStore.loadAll(context) } }
    }

    val addFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val added = withContext(Dispatchers.IO) {
                    try {
                        val displayName = FileUtils.displayNameFromUri(context, uri)
                        val format = FileUtils.detectFormat(context, uri, displayName)
                        if (format == null) {
                            false
                        } else {
                            val cached = FileUtils.copyToCache(context, uri, displayName)
                            LibraryStore.addItem(context, cached, displayName, format)
                            true
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                if (added) {
                    addError = null
                    refresh()
                } else {
                    addError = "Formato non riconosciuto, impossibile aggiungere il file"
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val allTags = items.flatMap { it.tags }.distinct().sorted()
    val visibleItems = items
        .filter { selectedTag == null || selectedTag in it.tags }
        .filter { searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true) }
        .let { list ->
            when (sortMode) {
                LibrarySort.RECENT -> list.sortedByDescending { it.addedAt }
                LibrarySort.NAME -> list.sortedBy { it.displayName.lowercase() }
                LibrarySort.SIZE -> list.sortedByDescending { LibraryStore.fileFor(context, it).length() }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LIBRERIA") },
                navigationIcon = { TextButton(onClick = onBack) { Text("INDIETRO") } },
                actions = {
                    TextButton(onClick = { addFileLauncher.launch(arrayOf("*/*")) }) { Text("AGGIUNGI FILE") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            addError?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }
            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cerca per nome") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        OutlinedButton(onClick = { sortExpanded = true }) {
                            Text("ORDINA ⌄")
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            LibrarySort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        sortMode = sort
                                        sortExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagChip("TUTTI", selectedTag == null) { selectedTag = null }
                    allTags.forEach { tag ->
                        TagChip(tag.uppercase(), selectedTag == tag) { selectedTag = tag }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (visibleItems.isEmpty()) {
                Spacer(Modifier.height(48.dp))
                Text(
                    "Nessun file in libreria",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tocca \"AGGIUNGI FILE\" qui sopra, oppure salva un file convertito con " +
                        "\"SALVA IN LIBRERIA\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visibleItems, key = { it.id }) { item ->
                        LibraryItemCard(
                            item = item,
                            onOpen = {
                                val file = LibraryStore.fileFor(context, item)
                                onOpenReader(file, item.format, item.displayName)
                            },
                            onEditTags = { tagDialogItem = item },
                            onDelete = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { LibraryStore.deleteItem(context, item) }
                                    refresh()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    tagDialogItem?.let { item ->
        TagEditDialog(
            item = item,
            onDismiss = { tagDialogItem = null },
            onSave = { newTags ->
                scope.launch {
                    withContext(Dispatchers.IO) { LibraryStore.updateTags(context, item.id, newTags) }
                    tagDialogItem = null
                    refresh()
                }
            },
        )
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    onOpen: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                ".${item.format.extension.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.tags.forEach { tag ->
                        Text(
                            "#$tag",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEditTags) { Text("ETICHETTE") }
                TextButton(onClick = onDelete) { Text("ELIMINA") }
            }
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    if (selected) {
        Button(onClick = onClick, contentPadding = padding) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = padding) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TagEditDialog(item: LibraryItem, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var text by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ETICHETTE") },
        text = {
            Column {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Etichette, separate da virgola") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tags = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                onSave(tags)
            }) { Text("SALVA") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULLA") }
        },
    )
}
