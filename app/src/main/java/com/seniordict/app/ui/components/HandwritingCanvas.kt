package com.seniordict.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

import kotlinx.coroutines.launch

@Composable
fun HandwritingCanvasDialog(
    onDismiss: () -> Unit,
    onRecognize: suspend (Bitmap) -> List<CharacterEntity>,
    onSelectChar: (CharacterEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var redrawKey by remember { mutableStateOf(0) }

    var candidates by remember { mutableStateOf<List<CharacterEntity>>(emptyList()) }
    var isRecognizing by remember { mutableStateOf(false) }
    var hasRecognizedOnce by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = SeniorSurface
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
                        text = "大字手写板",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SeniorDark
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SeniorBackground)
                        .border(3.dp, SeniorBorder, RoundedCornerShape(16.dp))
                        .pointerInput(redrawKey) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val p = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = p
                                    paths.add(p)
                                },
                                onDrag = { change, _ ->
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    redrawKey++
                                },
                                onDragEnd = {
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    // Tianzige (田字格) background guides
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Dotted cross lines
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 2f
                        )
                    }

                    // Stroke Rendering
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (p in paths) {
                            drawPath(
                                path = p.asComposePath(),
                                color = SeniorDark,
                                style = Stroke(
                                    width = 18f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (paths.isEmpty()) {
                        Text(
                            text = "请在此用手指写字\n字迹尽量写大、居中",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = SeniorTextMuted,
                                fontSize = 20.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 30.sp
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
                            text = "👆 请轻点您写的字：",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(candidates) { entity ->
                                Surface(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(2.5.dp, SeniorPrimary, RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectChar(entity)
                                        },
                                    color = SeniorPrimary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = entity.char,
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SeniorDark
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
                        text = "未识别出清晰汉字，请写大、写清楚一点再试哦",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Action Bar: Clear & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            paths.clear()
                            currentPath = null
                            candidates = emptyList()
                            hasRecognizedOnce = false
                            redrawKey++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, SeniorBorder)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "重写", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "重写", style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp))
                    }

                    Button(
                        onClick = {
                            if (paths.isEmpty() || isRecognizing) return@Button
                            isRecognizing = true
                            hasRecognizedOnce = true
                            val bmp = rasterizeCanvas(paths)
                            coroutineScope.launch {
                                try {
                                    val result = onRecognize(bmp)
                                    candidates = result
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
                            .weight(1.3f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SeniorPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isRecognizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "识别中...", style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "识别", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "识别查字", style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp))
                        }
                    }
                }
            }
        }
    }
}

private fun rasterizeCanvas(paths: List<Path>, width: Int = 360, height: Int = 360): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint().apply {
        color = AndroidColor.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    for (p in paths) {
        canvas.drawPath(p, paint)
    }
    return bitmap
}
