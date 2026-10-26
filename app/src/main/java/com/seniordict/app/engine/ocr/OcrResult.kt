package com.seniordict.app.engine.ocr

import android.graphics.RectF

data class OcrCharacter(
    val char: String,
    val bounds: RectF, // Coordinates on original image
    val confidence: Float = 1.0f
)

data class OcrLine(
    val text: String,
    val bounds: RectF,
    val characters: List<OcrCharacter>
)

data class OcrPageResult(
    val characters: List<OcrCharacter>,
    val lines: List<OcrLine>,
    val imageWidth: Int,
    val imageHeight: Int
) {
    /**
     * Hit test: given a tap point (x, y) on the displayed view,
     * find the closest matching character within touch threshold.
     */
    fun findCharacterAt(tapX: Float, tapY: Float, viewWidth: Float, viewHeight: Float): OcrCharacter? {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return null

        val scaleX = viewWidth / imageWidth.toFloat()
        val scaleY = viewHeight / imageHeight.toFloat()
        
        // Touch padding for senior fingers (50dp equivalent in px)
        val touchPadding = 48f

        var bestMatch: OcrCharacter? = null
        var minDistance = Float.MAX_VALUE

        for (ch in characters) {
            val viewLeft = ch.bounds.left * scaleX
            val viewTop = ch.bounds.top * scaleY
            val viewRight = ch.bounds.right * scaleX
            val viewBottom = ch.bounds.bottom * scaleY

            val paddedRect = RectF(
                viewLeft - touchPadding,
                viewTop - touchPadding,
                viewRight + touchPadding,
                viewBottom + touchPadding
            )

            if (paddedRect.contains(tapX, tapY)) {
                // Calculate distance from tap point to center of character
                val centerX = (viewLeft + viewRight) / 2f
                val centerY = (viewTop + viewBottom) / 2f
                val dist = (tapX - centerX) * (tapX - centerX) + (tapY - centerY) * (tapY - centerY)
                if (dist < minDistance) {
                    minDistance = dist
                    bestMatch = ch
                }
            }
        }
        return bestMatch
    }
}
