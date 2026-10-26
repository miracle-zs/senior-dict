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

data class DisplayTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val displayedWidth: Float,
    val displayedHeight: Float
)

data class OcrPageResult(
    val characters: List<OcrCharacter>,
    val lines: List<OcrLine>,
    val imageWidth: Int,
    val imageHeight: Int
) {
    /**
     * Calculate exact ContentScale.Fit geometric transformation for mapping
     * between image pixel coordinates and screen view coordinates.
     */
    fun getDisplayTransform(viewWidth: Float, viewHeight: Float): DisplayTransform {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            return DisplayTransform(1f, 0f, 0f, viewWidth, viewHeight)
        }
        val scale = minOf(viewWidth / imageWidth.toFloat(), viewHeight / imageHeight.toFloat())
        val dw = imageWidth * scale
        val dh = imageHeight * scale
        val ox = (viewWidth - dw) / 2f
        val oy = (viewHeight - dh) / 2f
        return DisplayTransform(scale, ox, oy, dw, dh)
    }

    /**
     * Hit test: given a tap point (x, y) on the displayed view,
     * find the closest matching character within touch threshold using exact
     * ContentScale.Fit inverse transformation.
     */
    fun findCharacterAt(tapX: Float, tapY: Float, viewWidth: Float, viewHeight: Float): OcrCharacter? {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0 || characters.isEmpty()) {
            return null
        }

        val transform = getDisplayTransform(viewWidth, viewHeight)
        // Convert touch point into image coordinates
        val imgX = (tapX - transform.offsetX) / transform.scale
        val imgY = (tapY - transform.offsetY) / transform.scale

        // Touch padding for senior fingers in image coordinate space (approx 44dp)
        val touchPadding = 48f / transform.scale

        var bestMatch: OcrCharacter? = null
        var minDistance = Float.MAX_VALUE

        for (ch in characters) {
            val paddedRect = RectF(
                ch.bounds.left - touchPadding,
                ch.bounds.top - touchPadding,
                ch.bounds.right + touchPadding,
                ch.bounds.bottom + touchPadding
            )

            if (paddedRect.contains(imgX, imgY)) {
                val centerX = ch.bounds.centerX()
                val centerY = ch.bounds.centerY()
                val dist = (imgX - centerX) * (imgX - centerX) + (imgY - centerY) * (imgY - centerY)
                if (dist < minDistance) {
                    minDistance = dist
                    bestMatch = ch
                }
            }
        }
        return bestMatch
    }
}
