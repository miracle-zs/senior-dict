package com.seniordict.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.engine.ocr.OcrCharacter
import com.seniordict.app.engine.ocr.OcrPageResult
import com.seniordict.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * High-contrast OCR highlight overlay with Touch Loupe Balloon (放大镜气泡).
 * Overcomes thick-finger occlusion for elderly users by showing a magnified preview
 * floating right above the finger as they touch or drag across characters.
 */
@Composable
fun OcrOverlayCanvas(
    ocrResult: OcrPageResult?,
    selectedChar: OcrCharacter?,
    onCharTapped: (OcrCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (ocrResult == null || ocrResult.characters.isEmpty()) return

    var hoveredChar by remember { mutableStateOf<OcrCharacter?>(null) }
    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var isLoupeActive by remember { mutableStateOf(false) }

    var containerWidthPx by remember { mutableStateOf(1080f) }
    var containerHeightPx by remember { mutableStateOf(1920f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerWidthPx = coordinates.size.width.toFloat()
                containerHeightPx = coordinates.size.height.toFloat()
            }
            .pointerInput(ocrResult) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val viewW = size.width.toFloat()
                    val viewH = size.height.toFloat()

                    var currentOffset = down.position
                    var currentHovered = ocrResult.findCharacterAt(
                        tapX = currentOffset.x,
                        tapY = currentOffset.y,
                        viewWidth = viewW,
                        viewHeight = viewH
                    )

                    touchOffset = currentOffset
                    hoveredChar = currentHovered
                    isLoupeActive = currentHovered != null

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            currentOffset = pointer.position
                            val newHovered = ocrResult.findCharacterAt(
                                tapX = currentOffset.x,
                                tapY = currentOffset.y,
                                viewWidth = viewW,
                                viewHeight = viewH
                            )
                            touchOffset = currentOffset
                            if (newHovered != null) {
                                hoveredChar = newHovered
                                isLoupeActive = true
                            }
                        } else {
                            // Pointer up
                            if (hoveredChar != null) {
                                onCharTapped(hoveredChar!!)
                            }
                            break
                        }
                    }

                    isLoupeActive = false
                    hoveredChar = null
                    touchOffset = null
                }
            }
    ) {
        // Background canvas rendering bounding boxes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewWidth = size.width
            val viewHeight = size.height
            val scaleX = viewWidth / ocrResult.imageWidth.toFloat()
            val scaleY = viewHeight / ocrResult.imageHeight.toFloat()

            for (ch in ocrResult.characters) {
                val left = ch.bounds.left * scaleX
                val top = ch.bounds.top * scaleY
                val width = ch.bounds.width() * scaleX
                val height = ch.bounds.height() * scaleY

                val isHovered = hoveredChar != null && hoveredChar!!.char == ch.char && hoveredChar!!.bounds == ch.bounds
                val isSelected = selectedChar != null && selectedChar.char == ch.char && selectedChar.bounds == ch.bounds

                val isHighlight = isHovered || isSelected

                val fillColor = if (isHighlight) OcrBoxSelectedFill else OcrBoxFill
                val borderColor = if (isHighlight) OcrBoxSelectedBorder else OcrBoxBorder
                val strokeWidth = if (isHighlight) 4.5f else 2f

                // Draw character highlight background
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Draw character border
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Floating Loupe Balloon
        if (isLoupeActive && hoveredChar != null && touchOffset != null) {
            LoupeBalloon(
                character = hoveredChar!!.char,
                touchOffset = touchOffset!!,
                containerWidth = containerWidthPx
            )
        }
    }
}

/**
 * Floating Loupe Balloon positioned dynamically above the elderly user's finger.
 */
@Composable
private fun LoupeBalloon(
    character: String,
    touchOffset: Offset,
    containerWidth: Float
) {
    val density = LocalDensity.current
    val balloonWidthPx = with(density) { 82.dp.toPx() }
    val balloonHeightPx = with(density) { 82.dp.toPx() }
    val liftPx = with(density) { 84.dp.toPx() }

    val rawX = touchOffset.x - (balloonWidthPx / 2f)
    val clampedX = rawX.coerceIn(16f, (containerWidth - balloonWidthPx - 16f).coerceAtLeast(16f))

    // If too close to the top of the screen, flip below the finger
    val rawY = touchOffset.y - liftPx - balloonHeightPx
    val clampedY = if (rawY < 20f) {
        touchOffset.y + with(density) { 40.dp.toPx() }
    } else {
        rawY
    }

    Surface(
        modifier = Modifier
            .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
            .size(82.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A), // Deep dark high-contrast slate
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFF59E0B)) // Warm gold border
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = character,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
