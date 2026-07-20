package com.megaconverter.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

/** A finger-drawing pad; "APPLICA" hands the caller a transparent-background Bitmap
 * with the drawn strokes, rendered natively (not captured from the Compose canvas) so
 * it can be embedded straight into a PDF via PdfSigner. */
@Composable
fun SignaturePadContent(onApply: (Bitmap) -> Unit, onCancel: () -> Unit) {
    val completedStrokes = remember { mutableStateListOf<List<Offset>>() }
    val currentStroke = remember { mutableStateListOf<Offset>() }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var strokeCount by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("FIRMA CON IL DITO", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Verrà applicata in basso a destra dell'ultima pagina del PDF.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.White)
                .onSizeChanged { size ->
                    canvasWidth = size.width
                    canvasHeight = size.height
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke.clear()
                            currentStroke.add(offset)
                        },
                        onDrag = { change, _ -> currentStroke.add(change.position) },
                        onDragEnd = {
                            if (currentStroke.size > 1) {
                                completedStrokes.add(currentStroke.toList())
                                strokeCount = completedStrokes.size
                            }
                            currentStroke.clear()
                        },
                    )
                },
        ) {
            (completedStrokes + listOf(currentStroke.toList())).forEach { points ->
                if (points.size > 1) {
                    val path = Path()
                    path.moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { path.lineTo(it.x, it.y) }
                    drawPath(
                        path,
                        color = Color.Black,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                completedStrokes.clear()
                currentStroke.clear()
                strokeCount = 0
            }) { Text("CANCELLA") }
            OutlinedButton(onClick = onCancel) { Text("ANNULLA") }
            Button(
                onClick = {
                    if (canvasWidth > 0 && canvasHeight > 0) {
                        onApply(renderSignatureBitmap(completedStrokes, canvasWidth, canvasHeight))
                    }
                },
                enabled = strokeCount > 0,
            ) { Text("APPLICA") }
        }
    }
}

private fun renderSignatureBitmap(strokes: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = AndroidPaint().apply {
        color = AndroidColor.BLACK
        strokeWidth = 6f
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
        isAntiAlias = true
    }
    strokes.forEach { points ->
        if (points.size > 1) {
            val path = AndroidPath()
            path.moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
