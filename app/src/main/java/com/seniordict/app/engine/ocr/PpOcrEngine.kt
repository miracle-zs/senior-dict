package com.seniordict.app.engine.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import com.yuanshi.moldocr.recognition.NativeOcr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min

class PpOcrEngine(private val context: Context) : AutoCloseable {

    private val appContext = context.applicationContext
    private val modelDirectory = File(appContext.filesDir, "ocr-models")
    private var nativeHandle = 0L
    private val isInitialized = AtomicBoolean(false)
    private val inferenceLock = ReentrantLock()
    private var reusableBuffer: ByteBuffer? = null

    init {
        // Asynchronously initialize and warm up model on IO thread
        Thread {
            try {
                ensureInitialized()
                warmUp()
            } catch (t: Throwable) {
                Log.e("PpOcrEngine", "Failed to initialize native OCR engine", t)
            }
        }.start()
    }

    /**
     * Process bitmap using the real PP-OCRv6 deep learning engine
     */
    suspend fun processImage(bitmap: Bitmap): OcrPageResult = withContext(Dispatchers.Default) {
        check(!bitmap.isRecycled) { "Bitmap already recycled" }

        val width = bitmap.width
        val height = bitmap.height

        // Run Native PP-OCRv6 inference
        val nativeResult = tryRunNativeOcr(bitmap)
        if (nativeResult != null) {
            return@withContext nativeResult
        }

        OcrPageResult(emptyList(), emptyList(), width, height)
    }

    private fun tryRunNativeOcr(bitmap: Bitmap): OcrPageResult? {
        return try {
            inferenceLock.withLock {
                ensureInitialized()
                if (nativeHandle == 0L) return null

                // Ensure bitmap is ARGB_8888
                val targetBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                    bitmap
                } else {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                }

                val width = targetBitmap.width
                val height = targetBitmap.height
                val requiredCapacity = width * height * 4

                val buffer = reusableBuffer?.takeIf { it.capacity() >= requiredCapacity }?.apply {
                    clear()
                } ?: ByteBuffer.allocateDirect(requiredCapacity).order(ByteOrder.nativeOrder()).also {
                    reusableBuffer = it
                }

                targetBitmap.copyPixelsToBuffer(buffer)
                buffer.rewind()

                val t0 = SystemClock.elapsedRealtime()
                val rawJson = NativeOcr.nativeRecognize(nativeHandle, buffer, width, height)
                val elapsed = SystemClock.elapsedRealtime() - t0
                Log.i("PpOcrEngine", "Native PP-OCRv6 finished in ${elapsed}ms: $rawJson")

                parseOcrJson(rawJson, width, height)
            }
        } catch (t: Throwable) {
            Log.e("PpOcrEngine", "Native recognition failed", t)
            null
        }
    }

    /**
     * Parse structured JSON from C++ pipeline:
     * {"items": [{"text": "...", "score": 0.95, "box": [[x1, y1], [x2, y2], [x3, y3], [x4, y4]]}]}
     */
    private fun parseOcrJson(jsonStr: String, imgWidth: Int, imgHeight: Int): OcrPageResult {
        val root = JSONObject(jsonStr)
        val items = root.optJSONArray("items") ?: return OcrPageResult(emptyList(), emptyList(), imgWidth, imgHeight)

        val lines = mutableListOf<OcrLine>()
        val characters = mutableListOf<OcrCharacter>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val text = item.optString("text", "").trim()
            val score = item.optDouble("score", 1.0).toFloat()
            val box = item.optJSONArray("box")

            if (text.isEmpty() || box == null || box.length() < 4) continue

            // Parse normalized coordinates [0.0 ~ 1.0]
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (p in 0 until box.length()) {
                val point = box.getJSONArray(p)
                val normX = point.optDouble(0).toFloat()
                val normY = point.optDouble(1).toFloat()
                val px = normX * imgWidth
                val py = normY * imgHeight
                minX = min(minX, px)
                minY = min(minY, py)
                maxX = max(maxX, px)
                maxY = max(maxY, py)
            }

            // Apply 10% ROI Padding to protect complex radicals (草字头、走之底)
            val padX = (maxX - minX) * 0.05f
            val padY = (maxY - minY) * 0.08f
            val lineBounds = RectF(
                max(0f, minX - padX),
                max(0f, minY - padY),
                min(imgWidth.toFloat(), maxX + padX),
                min(imgHeight.toFloat(), maxY + padY)
            )

            // Segment line into individual square Chinese characters
            val charList = segmentLineToCharacters(text, lineBounds, score)
            lines.add(OcrLine(text = text, bounds = lineBounds, characters = charList))
            characters.addAll(charList)
        }

        return OcrPageResult(characters = characters, lines = lines, imageWidth = imgWidth, imageHeight = imgHeight)
    }

    /**
     * Segment text lines into square Chinese character touch areas.
     * Only actual Chinese characters get interactive touch targets,
     * filtering out English units, numbers, and punctuation.
     */
    fun segmentLineToCharacters(lineText: String, lineBounds: RectF, confidence: Float): List<OcrCharacter> {
        val chars = lineText.trim()
        if (chars.isEmpty()) return emptyList()

        val results = mutableListOf<OcrCharacter>()
        val totalChars = chars.length
        val charWidth = (lineBounds.right - lineBounds.left) / totalChars.toFloat()

        for (i in 0 until totalChars) {
            val ch = chars[i]
            val isChinese = (ch in '\u4e00'..'\u9fa5') || (ch in '\u3400'..'\u4dbf')
            if (isChinese) {
                val left = lineBounds.left + i * charWidth
                val right = left + charWidth
                val bounds = RectF(left, lineBounds.top, right, lineBounds.bottom)
                results.add(OcrCharacter(char = ch.toString(), bounds = bounds, confidence = confidence))
            }
        }
        return results
    }

    private fun ensureInitialized() {
        if (nativeHandle != 0L) return
        modelDirectory.mkdirs()

        val detModel = materializeAsset("models/PP-OCRv6_medium_det.mnn")
        val recModel = materializeAsset("models/PP-OCRv6_medium_rec.onnx")
        val clsModel = ""
        val config = materializeAsset("config_v6.txt")
        val dictionary = materializeAsset("labels/ppocr_keys_v6.txt")

        nativeHandle = NativeOcr.nativeCreate(
            detModelPath = detModel.absolutePath,
            clsModelPath = clsModel,
            recModelPath = recModel.absolutePath,
            configPath = config.absolutePath,
            dictPath = dictionary.absolutePath,
            cpuThreadNum = 4,
            cpuPowerMode = "LITE_POWER_HIGH"
        )
        if (nativeHandle != 0L) {
            isInitialized.set(true)
            Log.i("PpOcrEngine", "PP-OCRv6 Medium native engine initialized successfully: handle=$nativeHandle")
        } else {
            Log.e("PpOcrEngine", "Failed to create native OCR pipeline")
        }
    }

    private fun warmUp() {
        if (nativeHandle != 0L) {
            val start = SystemClock.elapsedRealtime()
            NativeOcr.nativeWarmUp(nativeHandle)
            Log.i("PpOcrEngine", "Native warmup finished in ${SystemClock.elapsedRealtime() - start}ms")
        }
    }

    private fun materializeAsset(assetPath: String): File {
        val destFile = File(modelDirectory, assetPath.replace('/', '_'))
        if (!destFile.exists() || destFile.length() == 0L) {
            Log.i("PpOcrEngine", "Extracting asset $assetPath to ${destFile.absolutePath}...")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return destFile
    }

    override fun close() {
        inferenceLock.withLock {
            if (nativeHandle != 0L) {
                NativeOcr.nativeRelease(nativeHandle)
                nativeHandle = 0L
            }
            reusableBuffer = null
        }
    }
}
