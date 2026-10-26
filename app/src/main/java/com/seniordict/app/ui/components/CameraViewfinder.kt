package com.seniordict.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.seniordict.app.engine.ocr.OcrImageResizePolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraViewfinder(
    imageCaptureRef: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var focusIndicatorOffset by remember { mutableStateOf<Offset?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val pView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewView = pView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(pView.surfaceProvider)
                    }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCaptureRef(imageCapture)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraControl = camera.cameraControl

                        // Initial auto-focus in center
                        pView.post {
                            if (pView.width > 0 && pView.height > 0) {
                                val factory = pView.meteringPointFactory
                                val centerPoint = factory.createPoint(pView.width / 2f, pView.height / 2f)
                                val action = FocusMeteringAction.Builder(
                                    centerPoint,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                ).build()
                                cameraControl?.startFocusAndMetering(action)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraViewfinder", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                pView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pView = previewView ?: return@detectTapGestures
                        val control = cameraControl ?: return@detectTapGestures

                        // Tap-to-focus & metering (手指点选区域触发主动对焦和测光)
                        val factory = pView.meteringPointFactory
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(
                            point,
                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                        ).setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                        control.startFocusAndMetering(action)

                        // Show visual focus ring indicator
                        focusIndicatorOffset = offset
                        coroutineScope.launch {
                            delay(1200)
                            if (focusIndicatorOffset == offset) {
                                focusIndicatorOffset = null
                            }
                        }
                    }
                }
        )

        // Visual Focus Indicator Ring (触控对焦反馈圈)
        focusIndicatorOffset?.let { offset ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 42.dp.toPx(),
                    center = offset,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = offset
                )
            }
        }
    }
}

/**
 * Capture frame from CameraX ImageCapture with EXIF orientation correction
 * and memory-safe downsampling via OcrImageResizePolicy.
 */
fun capturePhoto(
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.Executor,
    onSuccess: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(IllegalStateException("ImageCapture is null"))
        return
    }

    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    // Step 1: Decode image with memory-safe inSampleSize calculation
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    val rawWidth = options.outWidth
                    val rawHeight = options.outHeight

                    val sampleSize = OcrImageResizePolicy.calculateSampleSize(rawWidth, rawHeight)
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                        ?: throw IllegalStateException("Bitmap decode failed")

                    // Step 2: EXIF hardware rotation correction (物理纠偏)
                    val orientedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        val rotated = Bitmap.createBitmap(
                            decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true
                        )
                        if (rotated !== decodedBitmap) decodedBitmap.recycle()
                        rotated
                    } else {
                        decodedBitmap
                    }

                    // Step 3: Ensure maximum side fits within MAX_IMAGE_SIDE (1280px)
                    val finalSafeBitmap = OcrImageResizePolicy.resizeBitmapIfNeeded(orientedBitmap)

                    onSuccess(finalSafeBitmap)
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    imageProxy.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
