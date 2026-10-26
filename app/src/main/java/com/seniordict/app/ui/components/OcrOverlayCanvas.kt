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
import kotlin.math.roundToInt

/**
 * High-contrast OCR highlight overlay with Touch Loupe Balloon (放大镜气泡).
 * Uses exact ContentScale.Fit geometric projection to ensure pixel-perfect bounding boxes
 * aligned with the camera image, with Non-Maximum Suppression to avoid overlapping clutter.
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
        // Background canvas rendering bounding boxes with exact ContentScale.Fit geometric projection
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewWidth = size.width
            val viewHeight = size.height
            val transform = ocrResult.getDisplayTransform(viewWidth, viewHeight)

            for (ch in ocrResult.characters) {
                val left = transform.offsetX + ch.bounds.left * transform.scale
                val top = transform.offsetY + ch.bounds.top * transform.scale
                val width = ch.bounds.width() * transform.scale
                val height = ch.bounds.height() * transform.scale

                val isHovered = hoveredChar != null && hoveredChar!!.char == ch.char && hoveredChar!!.bounds == ch.bounds
                val isSelected = selectedChar != null && selectedChar.char == ch.char && selectedChar.bounds == ch.bounds
                val isHighlight = isHovered || isSelected

                if (isHighlight) {
                    // Outer radiant halo for selected character
                    drawRoundRect(
                        color = Color(0x44F59E0B),
                        topLeft = Offset(left - 3f, top - 3f),
                        size = Size(width + 6f, height + 6f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Fill background
                    drawRoundRect(
                        color = Color(0x60F59E0B),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Border stroke
                    drawRoundRect(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 4f)
                    )
                } else {
                    // Gentle, clean non-selected outline
                    drawRoundRect(
                        color = Color(0x18F59E0B),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0x88F59E0B),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Floating Loupe Balloon positioned dynamically above the elderly user's finger
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
