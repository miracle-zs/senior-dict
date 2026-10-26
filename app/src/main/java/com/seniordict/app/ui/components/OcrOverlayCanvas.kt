package com.seniordict.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.seniordict.app.engine.ocr.OcrCharacter
import com.seniordict.app.engine.ocr.OcrPageResult
import com.seniordict.app.ui.theme.*

@Composable
fun OcrOverlayCanvas(
    ocrResult: OcrPageResult?,
    selectedChar: OcrCharacter?,
    onCharTapped: (OcrCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (ocrResult == null || ocrResult.characters.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(ocrResult) {
                detectTapGestures { offset ->
                    val hit = ocrResult.findCharacterAt(
                        tapX = offset.x,
                        tapY = offset.y,
                        viewWidth = size.width.toFloat(),
                        viewHeight = size.height.toFloat()
                    )
                    if (hit != null) {
                        onCharTapped(hit)
                    }
                }
            }
    ) {
        val viewWidth = size.width
        val viewHeight = size.height
        val scaleX = viewWidth / ocrResult.imageWidth.toFloat()
        val scaleY = viewHeight / ocrResult.imageHeight.toFloat()

        for (ch in ocrResult.characters) {
            val left = ch.bounds.left * scaleX
            val top = ch.bounds.top * scaleY
            val width = ch.bounds.width() * scaleX
            val height = ch.bounds.height() * scaleY

            val isSelected = selectedChar?.char == ch.char && selectedChar?.bounds == ch.bounds

            val fillColor = if (isSelected) OcrBoxSelectedFill else OcrBoxFill
            val borderColor = if (isSelected) OcrBoxSelectedBorder else OcrBoxBorder
            val strokeWidth = if (isSelected) 4f else 2f

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
}
