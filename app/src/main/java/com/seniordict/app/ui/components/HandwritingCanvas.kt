package com.seniordict.app.ui.components

import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.ui.theme.*

@Composable
fun HandwritingCanvasDialog(
    onDismiss: () -> Unit,
    onSelectChar: (CharacterEntity) -> Unit
) {
    // Current drawing paths
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var redrawKey by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
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
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            color = Color(0xFFE2E8F0),
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFFE2E8F0),
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
                                    width = 16f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (paths.isEmpty()) {
                        Text(
                            text = "请在此用手指写字\n字迹大、笔顺随意",
                            style = MaterialTheme.typography.bodyLarge.copy(color = SeniorTextMuted),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar: Clear & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            paths.clear()
                            currentPath = null
                            redrawKey++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "重写", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "清空重写", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = {
                            // On handwriting completed
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SeniorPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "识别查字", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
