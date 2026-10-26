package com.seniordict.app

import com.seniordict.app.engine.ocr.OcrPageResult
import org.junit.Assert.*
import org.junit.Test

class OcrGeometryAndNmsTest {

    @Test
    fun testDisplayTransformContentScaleFit() {
        // Image is 960 x 1280 (aspect ratio 3:4 = 0.75)
        // View is 1080 x 2340 (tall screen, aspect ratio 0.46)
        val ocrResult = OcrPageResult(
            characters = emptyList(),
            lines = emptyList(),
            imageWidth = 960,
            imageHeight = 1280
        )

        val transform = ocrResult.getDisplayTransform(1080f, 2340f)

        // Scale should be 1080 / 960 = 1.125
        assertEquals(1.125f, transform.scale, 0.001f)
        assertEquals(1080f, transform.displayedWidth, 0.01f)
        assertEquals(1440f, transform.displayedHeight, 0.01f)
        // Offset X should be 0, Offset Y should be (2340 - 1440) / 2 = 450
        assertEquals(0f, transform.offsetX, 0.01f)
        assertEquals(450f, transform.offsetY, 0.01f)
    }

    @Test
    fun testDisplayTransformLandscapeImage() {
        // Landscape photo 1280 x 720 on 1080 x 1920 phone
        val ocrResult = OcrPageResult(
            characters = emptyList(),
            lines = emptyList(),
            imageWidth = 1280,
            imageHeight = 720
        )

        val transform = ocrResult.getDisplayTransform(1080f, 1920f)

        // Scale should be 1080 / 1280 = 0.84375
        assertEquals(0.84375f, transform.scale, 0.001f)
        assertEquals(1080f, transform.displayedWidth, 0.01f)
        val expectedHeight = 720 * 0.84375f
        assertEquals(expectedHeight, transform.displayedHeight, 0.01f)
        assertEquals(0f, transform.offsetX, 0.01f)
        assertEquals((1920f - expectedHeight) / 2f, transform.offsetY, 0.01f)
    }
}
