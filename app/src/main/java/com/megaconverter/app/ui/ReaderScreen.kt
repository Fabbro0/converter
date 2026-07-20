package com.megaconverter.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.megaconverter.app.converter.FileFormat
import com.megaconverter.app.converter.FormatCategory
import com.megaconverter.app.converter.converters.DocxReader
import com.megaconverter.app.converter.converters.EpubReader
import com.megaconverter.app.converter.converters.NaturalSort
import com.megaconverter.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(file: File, format: FileFormat, displayName: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("INDIETRO") } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                format.category == FormatCategory.IMAGE -> ImageReaderContent(file)
                format == FileFormat.PDF -> PdfReaderContent(file)
                format == FileFormat.CBZ -> CbzReaderContent(file)
                format == FileFormat.TXT -> TextFileReaderContent(file)
                format == FileFormat.DOCX -> ExtractedTextReaderContent(file, DocxReader::extractText)
                format == FileFormat.EPUB -> ExtractedTextReaderContent(file, EpubReader::extractText)
                else -> UnsupportedReaderContent(file, format)
            }
        }
    }
}

@Composable
private fun TextFileReaderContent(file: File) {
    var text by remember(file) { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        text = withContext(Dispatchers.IO) { file.readText(Charsets.UTF_8) }
    }
    ReaderTextBody(text)
}

@Composable
private fun ExtractedTextReaderContent(file: File, extract: (File) -> String) {
    var text by remember(file) { mutableStateOf<String?>(null) }
    var error by remember(file) { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        try {
            text = withContext(Dispatchers.IO) { extract(file) }
        } catch (e: Exception) {
            error = e.message ?: "Impossibile leggere il file"
        }
    }
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        }
    } else {
        ReaderTextBody(text)
    }
}

@Composable
private fun ReaderTextBody(text: String?) {
    if (text == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Text(
            text,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PdfReaderContent(file: File) {
    var pageCount by remember(file) { mutableStateOf(0) }
    var loadError by remember(file) { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        try {
            pageCount = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { it.pageCount }
                }
            }
        } catch (e: Exception) {
            loadError = e.message ?: "PDF non leggibile"
        }
    }

    val error = loadError
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        }
        return
    }
    if (pageCount == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val pagerState = rememberPagerState(pageCount = { pageCount })
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            PdfPageImage(file = file, pageIndex = pageIndex)
        }
        Text(
            "${pagerState.currentPage + 1} / $pageCount",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun PdfPageImage(file: File, pageIndex: Int) {
    var bitmap by remember(file, pageIndex) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, pageIndex) {
        bitmap = withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.openPage(pageIndex).use { page ->
                        val scale = 2f
                        val bmp = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

private val CBZ_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")

@Composable
private fun CbzReaderContent(file: File) {
    var entryNames by remember(file) { mutableStateOf<List<String>>(emptyList()) }
    var loadError by remember(file) { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        try {
            entryNames = withContext(Dispatchers.IO) {
                ZipFile(file).use { zip ->
                    zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.substringAfterLast('.', "").lowercase() in CBZ_IMAGE_EXTENSIONS }
                        .map { it.name }
                        .sortedWith(NaturalSort.comparator)
                        .toList()
                }
            }
        } catch (e: Exception) {
            loadError = e.message ?: "CBZ non leggibile"
        }
    }

    val error = loadError
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        }
        return
    }
    if (entryNames.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val pagerState = rememberPagerState(pageCount = { entryNames.size })
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            CbzPageImage(file = file, entryName = entryNames[pageIndex])
        }
        Text(
            "${pagerState.currentPage + 1} / ${entryNames.size}",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun CbzPageImage(file: File, entryName: String) {
    var bitmap by remember(file, entryName) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, entryName) {
        bitmap = withContext(Dispatchers.IO) {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(entryName)
                if (entry != null) {
                    zip.getInputStream(entry).use { input -> BitmapFactory.decodeStream(input) }
                } else {
                    null
                }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ImageReaderContent(file: File) {
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file) {
        bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun UnsupportedReaderContent(file: File, format: FileFormat) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Anteprima non disponibile per .${format.extension.uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                context.startActivity(FileUtils.openIntent(context, file, FileUtils.guessMimeType(file, format)))
            }) {
                Text("APRI CON UN'ALTRA APP")
            }
        }
    }
}
