package com.seniordict.app.engine.ocr

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * 图像内存安全缩放与降采样策略
 * 避免手机 5000万/1亿像素原图直接解码造成 OOM 闪退，
 * 并大幅降低端侧 OCR 推理延迟。
 */
object OcrImageResizePolicy {
    const val MAX_IMAGE_SIDE = 1280

    fun calculateSampleSize(width: Int, height: Int, maxSide: Int = MAX_IMAGE_SIDE): Int {
        require(width > 0 && height > 0) { "图片尺寸必须为正数" }
        require(maxSide > 0) { "最大边长必须为正数" }

        var sample = 1
        while (width / (sample * 2) > maxSide || height / (sample * 2) > maxSide) {
            sample *= 2
        }
        if (sample == 1 && (width > maxSide || height > maxSide)) sample = 2
        return sample
    }

    fun fitWithin(width: Int, height: Int, maxSide: Int = MAX_IMAGE_SIDE): ImageSize {
        require(width > 0 && height > 0) { "图片尺寸必须为正数" }
        require(maxSide > 0) { "最大边长必须为正数" }
        if (width <= maxSide && height <= maxSide) return ImageSize(width, height)

        return if (width >= height) {
            ImageSize(
                width = maxSide,
                height = (height * maxSide.toDouble() / width).roundToInt().coerceAtLeast(1)
            )
        } else {
            ImageSize(
                width = (width * maxSide.toDouble() / height).roundToInt().coerceAtLeast(1),
                height = maxSide
            )
        }
    }

    fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSide: Int = MAX_IMAGE_SIDE): Bitmap {
        if (bitmap.isRecycled) return bitmap
        val targetSize = fitWithin(bitmap.width, bitmap.height, maxSide)
        return if (targetSize.width != bitmap.width || targetSize.height != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, targetSize.width, targetSize.height, true)
        } else {
            bitmap
        }
    }

    data class ImageSize(val width: Int, val height: Int)
}
