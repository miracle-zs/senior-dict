package com.yuanshi.moldocr.recognition

import java.nio.ByteBuffer

/** Thin JNI declaration for the single-image ONNX Runtime PP-OCRv6 adapter. */
object NativeOcr {
    init {
        try {
            System.loadLibrary("c++_shared")
        } catch (t: Throwable) {
            android.util.Log.w("NativeOcr", "libc++_shared preload: ${t.message}")
        }
        try {
            System.loadLibrary("MNN")
            System.loadLibrary("MNN_CL")
        } catch (t: Throwable) {
            android.util.Log.w("NativeOcr", "MNN/MNN_CL library preload: ${t.message}")
        }
        try {
            System.loadLibrary("onnxruntime")
            System.loadLibrary("moldocr_native")
        } catch (t: Throwable) {
            android.util.Log.e("NativeOcr", "Failed to load OCR native libraries: ${t.message}", t)
        }
    }

    @JvmStatic
    external fun nativeCreate(
        detModelPath: String,
        clsModelPath: String,
        recModelPath: String,
        configPath: String,
        dictPath: String,
        cpuThreadNum: Int,
        cpuPowerMode: String,
    ): Long

    @JvmStatic
    external fun nativeRecognize(
        handle: Long,
        rgbaBuffer: ByteBuffer,
        width: Int,
        height: Int,
    ): String

    @JvmStatic
    external fun nativeWarmUp(handle: Long): Boolean

    @JvmStatic
    external fun nativeRelease(handle: Long): Boolean
}
