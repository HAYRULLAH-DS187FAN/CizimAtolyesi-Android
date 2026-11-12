package com.example.cizimatolyesi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.calculatePan
import androidx.compose.ui.input.pointer.calculateZoom
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(18, 20, 34)
        window.navigationBarColor = android.graphics.Color.rgb(18, 20, 34)
        setContent {
            MaterialTheme {
                DrawingStudioApp()
            }
        }
    }
}

private enum class DrawingTool { BRUSH, ERASER }

private data class InkStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val eraser: Boolean
)

private data class DrawingLayer(
    val id: Long,
    val name: String,
    val strokes: List<InkStroke> = emptyList(),
    val visible: Boolean = true,
    val opacity: Float = 1f
)

private data class HistoryEntry(val layerId: Long, val stroke: InkStroke)

@Composable
private fun DrawingStudioApp() {
    var layers by remember { mutableStateOf(listOf(DrawingLayer(id = 1L, name = "Katman 1"))) }
    var selectedLayerId by remember { mutableStateOf(1L) }
    var nextLayerId by remember { mutableStateOf(2L) }

    var selectedTool by remember { mutableStateOf(DrawingTool.BRUSH) }
    var selectedColor by remember { mutableStateOf(Color(0xFF20202A)) }
    var brushSize by remember { mutableStateOf(8f) }
    var activeStroke by remember { mutableStateOf<InkStroke?>(null) }
    var activeStrokeLayerId by remember { mutableStateOf<Long?>(null) }

    var cameraScale by remember { mutableStateOf(1f) }
    var cameraOffset by remember { mutableStateOf(Offset.Zero) }
    val scaleState = rememberUpdatedState(cameraScale)
    val offsetState = rememberUpdatedState(cameraOffset)

    val undoStack = remember { mutableStateListOf<HistoryEntry>() }
    val redoStack = remember { mutableStateListOf<HistoryEntry>() }
    val currentLayer = layers.firstOrNull { it.id == selectedLayerId }

    fun updateLayer(layerId: Long, transform: (DrawingLayer) -> DrawingLayer) {
        layers = layers.map { layer -> if (layer.id == layerId) transform(layer) else layer }
    }

    fun commitStroke(layerId: Long, stroke: InkStroke) {
        updateLayer(layerId) { it.copy(strokes = it.strokes + stroke) }
        undoStack.add(HistoryEntry(layerId, stroke))
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val entry = undoStack.removeAt(undoStack.lastIndex)
        updateLayer(entry.layerId) { layer -> layer.copy(strokes = layer.strokes.dropLast(1)) }
        redoStack.add(entry)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val entry = redoStack.removeAt(redoStack.lastIndex)
        updateLayer(entry.layerId) { layer -> layer.copy(strokes = layer.strokes + entry.stroke) }
        undoStack.add(entry)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121422)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Çizim Atölyesi", color = Color.White, fontSize = 20.sp)
                    Text("İlk çizim motoru · v0.1", color = Color(0xFF9BA3BD), fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Text("↶", fontSize = 24.sp, color = Color.White)
                    }
                    TextButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Text("↷", fontSize = 24.sp, color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Katmanlar", color = Color(0xFFD6DBEE), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        val id = nextLayerId
                        nextLayerId += 1L
                        layers = layers + DrawingLayer(id, "Katman ${layers.size + 1}")
                        selectedLayerId = id
                    }
                ) { Text("+ Katman") }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                layers.forEach { layer ->
                    FilterChip(
                        selected = selectedLayerId == layer.id,
                        onClick = { selectedLayerId = layer.id },
                        label = { Text(if (layer.visible) layer.name else "◌ ${layer.name}") }
                    )
                    TextButton(onClick = {
                        updateLayer(layer.id) { it.copy(visible = !it.visible) }
                    }) { Text(if (layer.visible) "◉" else "○", color = Color.White) }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF242838), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF3B4259), RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF202433), RoundedCornerShape(12.dp))
                        .pointerInput(selectedTool, selectedColor, brushSize, selectedLayerId) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startScale = scaleState.value
                                val startOffset = offsetState.value
                                val startPoint = (down.position - startOffset) / startScale
                                val layerIdAtStart = selectedLayerId
                                var points = listOf(startPoint)
                                var transforming = false
                                activeStrokeLayerId = layerIdAtStart
                                activeStroke = InkStroke(
                                    points = points,
                                    color = selectedColor,
                                    width = brushSize,
                                    eraser = selectedTool == DrawingTool.ERASER
                                )
                                down.consume()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }

                                    if (pressedCount >= 2) {
                                        transforming = true
                                        activeStroke = null
                                        activeStrokeLayerId = null
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        cameraScale = (scaleState.value * zoom).coerceIn(0.35f, 6f)
                                        cameraOffset = offsetState.value + pan
                                        event.changes.forEach { it.consume() }
                                        while (true) {
                                            val transformEvent: PointerEvent = awaitPointerEvent()
                                            if (transformEvent.changes.count { it.pressed } < 2) {
                                                transformEvent.changes.forEach { it.consume() }
                                                break
                                            }
                                            val nextZoom = transformEvent.calculateZoom()
                                            val nextPan = transformEvent.calculatePan()
                                            cameraScale = (scaleState.value * nextZoom).coerceIn(0.35f, 6f)
                                            cameraOffset = offsetState.value + nextPan
                                            transformEvent.changes.forEach { it.consume() }
                                        }
                                        break
                                    }

                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        if (!transforming) {
                                            val finished = activeStroke
                                            val targetLayer = activeStrokeLayerId
                                            if (finished != null && targetLayer != null) {
                                                commitStroke(targetLayer, finished)
                                            }
                                        }
                                        activeStroke = null
                                        activeStrokeLayerId = null
                                        change?.consume()
                                        break
                                    }

                                    val point = (change.position - offsetState.value) / scaleState.value
                                    points = points + point
                                    activeStroke = activeStroke?.copy(points = points)
                                    change.consume()
                                }
                            }
                        }
                ) {
                    // Checkerboard is only a transparency preview; it is not part of the artwork.
                    val cell = 18.dp.toPx()
                    val columns = (size.width / cell).toInt() + 1
                    val rows = (size.height / cell).toInt() + 1
                    for (row in 0..rows) {
                        for (column in 0..columns) {
                            val shade = if ((row + column) % 2 == 0) Color(0xFF343A4C) else Color(0xFF292F40)
                            drawRect(
                                color = shade,
                                topLeft = Offset(column * cell, row * cell),
                                size = androidx.compose.ui.geometry.Size(cell, cell)
                            )
                        }
                    }

                    withTransform({
                        translate(left = cameraOffset.x, top = cameraOffset.y)
                        scale(scaleX = cameraScale, scaleY = cameraScale, pivot = Offset.Zero)
                    }) {
                        layers.filter { it.visible }.forEach { layer ->
                            val bounds = Rect(0f, 0f, size.width, size.height)
                            val saveCount = drawContext.canvas.saveLayer(
                                bounds,
                                Paint().apply { alpha = layer.opacity }
                            )
                            layer.strokes.forEach { ink -> drawInkStroke(ink) }
                            if (activeStrokeLayerId == layer.id) {
                                activeStroke?.let { drawInkStroke(it) }
                            }
                            drawContext.canvas.restoreToCount(saveCount)
                        }
                    }
                }
                Text(
                    "Zemin şeffaf · iki parmakla yakınlaştır/kaydır",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 7.dp)
                        .background(Color(0xAA171B29), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color(0xFFCFD5E8),
                    fontSize = 10.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton("✎ Fırça", selectedTool == DrawingTool.BRUSH) {
                    selectedTool = DrawingTool.BRUSH
                }
                ToolButton("⌫ Silgi", selectedTool == DrawingTool.ERASER) {
                    selectedTool = DrawingTool.ERASER
                }
                Text("${brushSize.toInt()} px", color = Color(0xFFD6DBEE), fontSize = 12.sp)
            }

            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 2f..48f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Color(0xFF20202A), Color(0xFFE74C3C), Color(0xFFF1C40F),
                    Color(0xFF2ECC71), Color(0xFF3498DB), Color(0xFF9B59B6), Color.White
                ).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(swatch, CircleShape)
                            .border(
                                width = if (selectedColor == swatch) 3.dp else 1.dp,
                                color = if (selectedColor == swatch) Color(0xFF8EA8FF) else Color(0xFF69718A),
                                shape = CircleShape
                            )
                            .clickable { selectedColor = swatch }
                    )
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    layers = layers.map { it.copy(strokes = emptyList()) }
                    undoStack.clear()
                    redoStack.clear()
                    activeStroke = null
                }) { Text("Tuvali temizle") }
            }

            Text(
                "Aktif: ${currentLayer?.name ?: "Katman yok"}  •  Ölçek: ${(cameraScale * 100).toInt()}%",
                color = Color(0xFF8992AC),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ToolButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF5368C8) else Color(0xFF292F43),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInkStroke(stroke: InkStroke) {
    if (stroke.points.isEmpty()) return
    val blend = if (stroke.eraser) BlendMode.Clear else BlendMode.SrcOver
    if (stroke.points.size == 1) {
        drawCircle(
            color = stroke.color,
            radius = max(1f, stroke.width / 2f),
            center = stroke.points.first(),
            blendMode = blend
        )
        return
    }
    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = stroke.color,
        style = DrawStroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        blendMode = blend
    )
}
