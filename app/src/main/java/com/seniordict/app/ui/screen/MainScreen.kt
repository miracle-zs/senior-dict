package com.seniordict.app.ui.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.seniordict.app.engine.ocr.OcrImageResizePolicy
import com.seniordict.app.ui.components.*
import com.seniordict.app.ui.theme.*
import com.seniordict.app.ui.viewmodel.AppScreenMode
import com.seniordict.app.ui.viewmodel.MainViewModel
import java.util.concurrent.Executors

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()

    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            viewModel.openCamera()
        } else {
            Toast.makeText(context, "拍照识字需要相机权限，请在手机设置中开启", Toast.LENGTH_LONG).show()
        }
    }

    // Photo Gallery Launcher for WeChat screenshots / medication box photos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    val sampleSize = OcrImageResizePolicy.calculateSampleSize(options.outWidth, options.outHeight)
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    if (bmp != null) {
                        val safeBmp = OcrImageResizePolicy.resizeBitmapIfNeeded(bmp)
                        viewModel.onPhotoCaptured(safeBmp, source = "GALLERY")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to load gallery image", e)
            }
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Toast message handler
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    var lastBackPressTime by remember { mutableStateOf(0L) }

    // Intercept Back button: Layered protective flow for elderly users
    BackHandler {
        when {
            uiState.isDetailSheetVisible -> {
                viewModel.dismissDetailSheet()
            }
            uiState.isSplitDialogVisible -> {
                viewModel.closeSplitDialog()
            }
            uiState.isHandwritingVisible -> {
                viewModel.closeHandwriting()
            }
            uiState.isHistorySheetVisible -> {
                viewModel.closeHistorySheet()
            }
            uiState.isSettingsDialogVisible -> {
                viewModel.closeSettingsDialog()
            }
            uiState.isFrozen -> {
                viewModel.unfreezeCamera()
            }
            uiState.currentScreen == AppScreenMode.CAMERA -> {
                viewModel.closeCamera()
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "再按一次退出银发字典", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Screen Routing: HOME vs CAMERA
    when (uiState.currentScreen) {
        AppScreenMode.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                favorites = favorites,
                history = history,
                onOpenCamera = {
                    if (hasCameraPermission) {
                        viewModel.openCamera()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }

        AppScreenMode.CAMERA -> {
            if (hasCameraPermission) {
                CameraScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    favorites = favorites,
                    onBackToHome = { viewModel.closeCamera() },
                    onLaunchGallery = { galleryLauncher.launch("image/*") },
                    imageCaptureRef = { capture -> imageCapture = capture },
                    onCaptureClick = {
                        capturePhoto(
                            imageCapture = imageCapture,
                            executor = cameraExecutor,
                            onSuccess = { bmp -> viewModel.onPhotoCaptured(bmp, source = "CAMERA") },
                            onError = { Log.e("MainScreen", "Capture error") }
                        )
                    }
                )
            } else {
                // Permission Fallback Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SeniorBackground)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SeniorSurface,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = JadePrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "需要相机权限",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JadeDeep
                                )
                            )
                            Text(
                                text = "“拍照看字”需要开启相机才能对准报纸、药盒放大看字。",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    color = SeniorTextMuted
                                ),
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.closeCamera() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("返回书房", fontSize = 16.sp)
                                }
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = JadePrimary)
                                ) {
                                    Text("开启权限", fontSize = 16.sp, color = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Word Detail Sheet
    if (uiState.isDetailSheetVisible && uiState.selectedEntity != null) {
        val contextChars = remember(uiState.ocrResult, uiState.selectedEntity) {
            val fromOcr = uiState.ocrResult?.characters?.map { it.char }?.distinct() ?: emptyList()
            if (fromOcr.isNotEmpty() && uiState.selectedEntity?.char in fromOcr) {
                fromOcr
            } else {
                emptyList()
            }
        }

        WordDetailSheet(
            character = uiState.selectedEntity!!,
            isFavorite = uiState.isSelectedCharFavorite,
            contextCharacters = contextChars,
            onSelectCharacter = { charStr -> viewModel.selectScannedCharacter(charStr) },
            onDismiss = { viewModel.dismissDetailSheet() },
            onSpeak = { viewModel.speakCurrentCharacter() },
            onSpeakWord = { word -> viewModel.speakWord(word) },
            onToggleFavorite = { char -> viewModel.toggleFavorite(char) }
        )
    }

    // Split Character Query Dialog
    if (uiState.isSplitDialogVisible) {
        SplitCharDialog(
            onDismiss = { viewModel.closeSplitDialog() },
            onQuery = { query -> viewModel.onSplitQuery(query) },
            candidates = uiState.splitCandidates,
            onSelectChar = { charEntity -> viewModel.onDirectLookup(charEntity.char, "SPLIT") }
        )
    }

    // Handwriting Canvas Dialog with Real OCR Recognition
    if (uiState.isHandwritingVisible) {
        HandwritingCanvasDialog(
            onDismiss = { viewModel.closeHandwriting() },
            onRecognize = { bmp -> viewModel.recognizeHandwriting(bmp) },
            onSelectChar = { charEntity -> viewModel.onDirectLookup(charEntity.char, "HANDWRITING") }
        )
    }

    // History and Favorites Sheet
    if (uiState.isHistorySheetVisible) {
        HistoryAndFavoritesSheet(
            favorites = favorites,
            history = history,
            onSelectChar = { char -> viewModel.onDirectLookup(char, "FAVORITES") },
            onSpeakChar = { char, pinyin, examples -> viewModel.speakCharacterByInfo(char, pinyin, examples) },
            onDeleteFavorite = { char -> viewModel.toggleFavorite(char) },
            onDeleteHistoryItem = { id -> viewModel.deleteHistoryItem(id) },
            onClearHistory = { viewModel.clearAllHistory() },
            onDismiss = { viewModel.closeHistorySheet() }
        )
    }

    // Senior Settings Dialog
    if (uiState.isSettingsDialogVisible) {
        SeniorSettingsDialog(
            currentSpeechRate = uiState.speechRate,
            isMnemonicEnabled = uiState.isMnemonicEnabled,
            isHapticEnabled = uiState.isHapticEnabled,
            onSpeechRateChange = { viewModel.setSpeechRate(it) },
            onMnemonicToggle = { viewModel.setMnemonicEnabled(it) },
            onHapticToggle = { viewModel.setHapticEnabled(it) },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }
}
