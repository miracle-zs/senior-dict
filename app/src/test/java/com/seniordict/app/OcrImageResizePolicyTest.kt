package com.seniordict.app

import com.seniordict.app.engine.ocr.OcrImageResizePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrImageResizePolicyTest {

    @Test
    fun `calculateSampleSize returns 1 for images within 1280px`() {
        val sampleSizeSmall = OcrImageResizePolicy.calculateSampleSize(800, 600)
        assertEquals(1, sampleSizeSmall)

        val sampleSizeExact = OcrImageResizePolicy.calculateSampleSize(1280, 720)
        assertEquals(1, sampleSizeExact)
    }

    @Test
    fun `calculateSampleSize downsamples large camera images to prevent OOM`() {
        // Typical 12MP photo: 4032 x 3024
        val sampleSize12MP = OcrImageResizePolicy.calculateSampleSize(4032, 3024)
        assertTrue("Sample size should be at least 2 for 4032px", sampleSize12MP >= 2)

        // Typical 50MP photo: 8192 x 6144
        val sampleSize50MP = OcrImageResizePolicy.calculateSampleSize(8192, 6144)
        assertTrue("Sample size should be at least 4 for 8192px", sampleSize50MP >= 4)
    }

    @Test
    fun `calculateSampleSize handles portrait and landscape aspect ratios`() {
        val samplePortrait = OcrImageResizePolicy.calculateSampleSize(1080, 2400)
        assertTrue(samplePortrait >= 1)

        val sampleLandscape = OcrImageResizePolicy.calculateSampleSize(2400, 1080)
        assertEquals(samplePortrait, sampleLandscape)
    }
}
