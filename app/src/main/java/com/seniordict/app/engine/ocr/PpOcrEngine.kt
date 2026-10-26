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
import kotlin.math.abs
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
        // Pre-warm OCR engine in background after a gentle delay to allow smooth UI launch
        Thread {
            try {
                Thread.sleep(1500)
                ensureInitialized()
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

        val rawCharacters = mutableListOf<OcrCharacter>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val text = item.optString("text", "").trim()
            val score = item.optDouble("score", 1.0).toFloat()
            val box = item.optJSONArray("box")

            // Filter out low confidence detections (< 0.50) to prevent hallucinated garbage characters
            if (text.isEmpty() || score < 0.50f || box == null || box.length() < 4) continue

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

            // Filter out tiny noise boxes (e.g. dust, camera artifacts < 14px)
            if ((maxX - minX) < 14f || (maxY - minY) < 14f) continue

            // Apply 5% ROI Padding to protect complex radicals (草字头、走之底)
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
            rawCharacters.addAll(charList)
        }

        // Apply strict NMS and deduplication to eliminate overlapping yellow boxes
        val finalCharacters = applyNmsAndDeduplication(rawCharacters)

        // Construct clean non-overlapping lines
        val lines = buildLinesFromCharacters(finalCharacters)

        return OcrPageResult(characters = finalCharacters, lines = lines, imageWidth = imgWidth, imageHeight = imgHeight)
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

    /**
     * Strict Non-Maximum Suppression (NMS) to eliminate overlapping bounding boxes
     * and sort characters in natural top-to-bottom, left-to-right reading order.
     */
    private fun applyNmsAndDeduplication(characters: List<OcrCharacter>): List<OcrCharacter> {
        if (characters.size <= 1) return characters

        // 1. Sort by confidence descending
        val sorted = characters.sortedByDescending { it.confidence }
        val kept = mutableListOf<OcrCharacter>()

        for (candidate in sorted) {
            var overlaps = false
            for (selected in kept) {
                val iou = calculateIoU(candidate.bounds, selected.bounds)
                val overlapRatio = calculateOverlapRatio(candidate.bounds, selected.bounds)
                // Suppress if IoU > 0.30 or if one box contains > 50% of another
                if (iou > 0.30f || overlapRatio > 0.50f) {
                    overlaps = true
                    break
                }
            }
            if (!overlaps) {
                kept.add(candidate)
            }
        }

        // 2. Sort kept characters by natural reading order
        return sortInReadingOrder(kept)
    }

    private fun calculateIoU(a: RectF, b: RectF): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        if (interRight <= interLeft || interBottom <= interTop) return 0f

        val interArea = (interRight - interLeft) * (interBottom - interTop)
        val areaA = a.width() * a.height()
        val areaB = b.width() * b.height()
        val unionArea = areaA + areaB - interArea
        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    private fun calculateOverlapRatio(a: RectF, b: RectF): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        if (interRight <= interLeft || interBottom <= interTop) return 0f

        val interArea = (interRight - interLeft) * (interBottom - interTop)
        val minArea = min(a.width() * a.height(), b.width() * b.height())
        return if (minArea > 0f) interArea / minArea else 0f
    }

    private fun sortInReadingOrder(chars: List<OcrCharacter>): List<OcrCharacter> {
        if (chars.size <= 1) return chars
        val avgHeight = chars.map { it.bounds.height() }.average().toFloat().coerceAtLeast(12f)
        val lineThreshold = avgHeight * 0.65f

        val sortedByY = chars.sortedBy { it.bounds.centerY() }
        val lines = mutableListOf<MutableList<OcrCharacter>>()

        for (c in sortedByY) {
            val matchingLine = lines.find { l ->
                val lineCenterY = l.map { it.bounds.centerY() }.average().toFloat()
                abs(c.bounds.centerY() - lineCenterY) < lineThreshold
            }
            if (matchingLine != null) {
                matchingLine.add(c)
            } else {
                lines.add(mutableListOf(c))
            }
        }

        // Sort lines from top to bottom
        lines.sortBy { line -> line.map { it.bounds.top }.minOrNull() ?: 0f }
        val result = mutableListOf<OcrCharacter>()
        for (line in lines) {
            // Sort characters within a line from left to right
            line.sortBy { it.bounds.left }
            result.addAll(line)
        }
        return result
    }

    private fun buildLinesFromCharacters(chars: List<OcrCharacter>): List<OcrLine> {
        if (chars.isEmpty()) return emptyList()
        val avgHeight = chars.map { it.bounds.height() }.average().toFloat().coerceAtLeast(12f)
        val lineThreshold = avgHeight * 0.65f
        val lineGroups = mutableListOf<MutableList<OcrCharacter>>()

        for (c in chars) {
            val matching = lineGroups.find { l ->
                val lineCenterY = l.map { it.bounds.centerY() }.average().toFloat()
                abs(c.bounds.centerY() - lineCenterY) < lineThreshold
            }
            if (matching != null) {
                matching.add(c)
            } else {
                lineGroups.add(mutableListOf(c))
            }
        }

        val lines = mutableListOf<OcrLine>()
        for (group in lineGroups) {
            group.sortBy { it.bounds.left }
            val text = group.joinToString("") { it.char }
            val minX = group.map { it.bounds.left }.minOrNull() ?: 0f
            val minY = group.map { it.bounds.top }.minOrNull() ?: 0f
            val maxX = group.map { it.bounds.right }.maxOrNull() ?: 0f
            val maxY = group.map { it.bounds.bottom }.maxOrNull() ?: 0f
            lines.add(OcrLine(text, RectF(minX, minY, maxX, maxY), group))
        }
        lines.sortBy { it.bounds.top }
        return lines
    }

    private fun ensureInitialized() {
        if (nativeHandle != 0L) return
        modelDirectory.mkdirs()

        val detModel = materializeAsset("models/PP-OCRv6_medium_det.mnn")
        val recModel = materializeAsset("models/PP-OCRv6_medium_rec.onnx")
        val clsModel = detModel.absolutePath // direction classify disabled; pass valid file to prevent NULL pointer dereference
        val config = materializeAsset("config_v6.txt")
        val dictionary = materializeAsset("labels/ppocr_keys_v6.txt")

        nativeHandle = NativeOcr.nativeCreate(
            detModelPath = detModel.absolutePath,
            clsModelPath = clsModel,
            recModelPath = recModel.absolutePath,
            configPath = config.absolutePath,
            dictPath = dictionary.absolutePath,
            cpuThreadNum = 2,
            cpuPowerMode = ""
        )
        if (nativeHandle != 0L) {
            isInitialized.set(true)
            Log.i("PpOcrEngine", "PP-OCRv6 Medium native engine initialized successfully: handle=$nativeHandle")
        } else {
            Log.e("PpOcrEngine", "Failed to create native OCR pipeline")
        }
    }

    private fun materializeAsset(assetPath: String): File {
        val destFile = File(modelDirectory, assetPath.replace('/', '_'))
        val isConfig = assetPath.endsWith(".txt")
        var shouldCopy = !destFile.exists() || destFile.length() == 0L || isConfig
        if (!shouldCopy) {
            try {
                context.assets.open(assetPath).use { stream ->
                    val len = stream.available().toLong()
                    if (len > 0 && destFile.length() != len) {
                        shouldCopy = true
                    }
                }
            } catch (ignored: Exception) {}
        }

        if (shouldCopy) {
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
