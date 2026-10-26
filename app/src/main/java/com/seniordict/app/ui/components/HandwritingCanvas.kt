package com.seniordict.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HandwritingCanvasDialog(
    onDismiss: () -> Unit,
    onRecognize: suspend (Bitmap) -> List<CharacterEntity>,
    onSelectChar: (CharacterEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val paths = remember { mutableStateListOf<Path>() }
    var drawTick by remember { mutableLongStateOf(0L) }
    var lastStrokeTime by remember { mutableLongStateOf(0L) }

    var candidates by remember { mutableStateOf<List<CharacterEntity>>(emptyList()) }
    var isRecognizing by remember { mutableStateOf(false) }
    var hasRecognizedOnce by remember { mutableStateOf(false) }

    // Automatic debounce recognition: triggers 700ms after user finishes a stroke
    LaunchedEffect(lastStrokeTime) {
        if (lastStrokeTime > 0L && paths.isNotEmpty()) {
            delay(700)
            if (!isRecognizing && paths.isNotEmpty()) {
                isRecognizing = true
                hasRecognizedOnce = true
                val bmp = rasterizeCanvas(paths)
                try {
                    candidates = onRecognize(bmp)
                } finally {
                    isRecognizing = false
                    if (!bmp.isRecycled) {
                        bmp.recycle()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = SeniorSurface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "手写查字",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = JadeDeep
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(26.dp),
                            tint = SeniorDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tianzige / Mizige Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SeniorBackground)
                        .border(2.5.dp, JadePrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                val path = Path().apply {
                                    moveTo(down.position.x, down.position.y)
                                    lineTo(down.position.x + 0.5f, down.position.y + 0.5f)
                                }
                                paths.add(path)
                                drawTick++

                                val pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) {
                                        lastStrokeTime = System.currentTimeMillis()
                                        break
                                    }
                                    change.consume()
                                    path.lineTo(change.position.x, change.position.y)
                                    drawTick++
                                }
                            }
                        }
                ) {
                    // Traditional Rice/Field Grid (米字格) Guides
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val guideColor = CinnabarRed.copy(alpha = 0.22f)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

                        // Center Horizontal line
                        drawLine(
                            color = guideColor,
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 2f,
                            pathEffect = dashEffect
                        )
                        // Center Vertical line
                        drawLine(
                            color = guideColor,
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 2f,
                            pathEffect = dashEffect
                        )
                        // Diagonal 1: Top-Left to Bottom-Right
                        drawLine(
                            color = guideColor.copy(alpha = 0.15f),
                            start = Offset(0f, 0f),
                            end = Offset(w, h),
                            strokeWidth = 1.5f,
                            pathEffect = dashEffect
                        )
                        // Diagonal 2: Bottom-Left to Top-Right
                        drawLine(
                            color = guideColor.copy(alpha = 0.15f),
                            start = Offset(0f, h),
                            end = Offset(w, 0f),
                            strokeWidth = 1.5f,
                            pathEffect = dashEffect
                        )
                    }

                    // User Stroke Rendering
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (drawTick >= 0) {
                            for (p in paths) {
                                drawPath(
                                    path = p.asComposePath(),
                                    color = SeniorDark,
                                    style = Stroke(
                                        width = 20f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    if (paths.isEmpty()) {
                        Text(
                            text = "在此手写汉字",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = SeniorTextMuted.copy(alpha = 0.8f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Candidate Characters Section
                if (candidates.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "候选字",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = JadePrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(candidates) { entity ->
                                Surface(
                                    modifier = Modifier
                                        .width(74.dp)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(2.dp, JadePrimary, RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectChar(entity)
                                        },
                                    color = JadeLight.copy(alpha = 0.35f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = entity.pinyin.ifEmpty { " " },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = JadePrimary
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = entity.char,
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = JadeDeep
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (hasRecognizedOnce && !isRecognizing) {
                    Text(
                        text = "未识别到汉字，请写大一些重试",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WarmAmber,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action Bar: Undo, Clear & Recognize
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo last stroke
                    OutlinedButton(
                        onClick = {
                            if (paths.isNotEmpty()) {
                                paths.removeAt(paths.lastIndex)
                                drawTick++
                                candidates = emptyList()
                                hasRecognizedOnce = false
                                if (paths.isNotEmpty()) {
                                    lastStrokeTime = System.currentTimeMillis()
                                }
                            }
                        },
                        enabled = paths.isNotEmpty() && !isRecognizing,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, if (paths.isNotEmpty()) SeniorBorder else SeniorBorder.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤销",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "撤销",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Clear all
                    OutlinedButton(
                        onClick = {
                            paths.clear()
                            drawTick++
                            candidates = emptyList()
                            hasRecognizedOnce = false
                            lastStrokeTime = 0L
                        },
                        enabled = paths.isNotEmpty() && !isRecognizing,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, if (paths.isNotEmpty()) SeniorBorder else SeniorBorder.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "重写",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "重写",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Recognize button
                    Button(
                        onClick = {
                            if (paths.isEmpty() || isRecognizing) return@Button
                            isRecognizing = true
                            hasRecognizedOnce = true
                            val bmp = rasterizeCanvas(paths)
                            coroutineScope.launch {
                                try {
                                    candidates = onRecognize(bmp)
                                } finally {
                                    isRecognizing = false
                                    if (!bmp.isRecycled) {
                                        bmp.recycle()
                                    }
                                }
                            }
                        },
                        enabled = paths.isNotEmpty() && !isRecognizing,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JadePrimary,
                            disabledContainerColor = JadePrimary.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isRecognizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "识别中...",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "识别查字",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "识别查字",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Intelligent handwriting rasterizer:
 * Centers and scales the drawn strokes with an optimal ~18% white margin into a 480x480 square bitmap.
 * This guarantees the character is fully visible, well-proportioned, and ideal for PP-OCRv6 DBNet & CRNN recognition.
 */
@Suppress("DEPRECATION")
private fun rasterizeCanvas(paths: List<Path>, targetSize: Int = 480): Bitmap {
    val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    if (paths.isEmpty()) return bitmap

    val totalPath = Path()
    for (p in paths) {
        totalPath.addPath(p)
    }

    val bounds = RectF()
    totalPath.computeBounds(bounds, true)

    val bw = bounds.width()
    val bh = bounds.height()

    val paint = Paint().apply {
        color = AndroidColor.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    if (bw <= 0.01f || bh <= 0.01f) {
        paint.strokeWidth = 22f
        for (p in paths) {
            canvas.drawPath(p, paint)
        }
        return bitmap
    }

    // Leave a clean ~18% white margin around the character
    val padding = targetSize * 0.18f
    val availSize = targetSize - 2 * padding
    val maxDim = maxOf(bw, bh)
    val scale = (availSize / maxDim).coerceAtMost(3.0f)

    // Ensure strokes maintain an ideal ~22px thickness on target bitmap
    paint.strokeWidth = (22f / scale).coerceIn(12f, 32f)

    canvas.save()
    // Center the character inside the target square bitmap
    canvas.translate(targetSize / 2f, targetSize / 2f)
    canvas.scale(scale, scale)
    canvas.translate(-bounds.centerX(), -bounds.centerY())

    for (p in paths) {
        canvas.drawPath(p, paint)
    }
    canvas.restore()

    return bitmap
}
