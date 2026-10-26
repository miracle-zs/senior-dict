package com.seniordict.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.seniordict.app.engine.ocr.OcrImageResizePolicy
import com.seniordict.app.ui.components.*
import com.seniordict.app.ui.theme.*
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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = SeniorDark,
        topBar = {
            // Top Bar with Big Title and Direct Senior Shortcuts
            Surface(
                color = SeniorBackground,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Brand Title
                    Text(
                        text = "银发字典",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SeniorDark
                        )
                    )

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "偏旁拆字" Button
                        FilledTonalButton(
                            onClick = { viewModel.openSplitDialog() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = "拆字", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("拆字", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                        }

                        // "手写板" Button
                        FilledTonalButton(
                            onClick = { viewModel.openHandwriting() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Create, contentDescription = "手写", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("手写", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                        }

                        // "生字本" Button
                        FilledTonalButton(
                            onClick = { viewModel.openHistorySheet() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (favorites.isNotEmpty()) SeniorWarning.copy(alpha = 0.25f) else SeniorBorder
                            )
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "生字本",
                                modifier = Modifier.size(18.dp),
                                tint = if (favorites.isNotEmpty()) SeniorWarning else SeniorDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("生字本", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                        }

                        // "设置" Gear Button
                        IconButton(
                            onClick = { viewModel.openSettingsDialog() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(22.dp), tint = SeniorDark)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                // Permission Fallback Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "需要相机权限来帮您拍照识字",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("开启相机权限", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                // Viewfinder / Frozen Canvas Area
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isFrozen && uiState.capturedBitmap != null) {
                        // Display Frozen Image
                        Image(
                            bitmap = uiState.capturedBitmap!!.asImageBitmap(),
                            contentDescription = "定格画面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )

                        // Highlight Overlay for Character Tap
                        OcrOverlayCanvas(
                            ocrResult = uiState.ocrResult,
                            selectedChar = uiState.selectedOcrChar,
                            onCharTapped = { viewModel.onCharacterTapped(it) }
                        )

                        // Loading State Overlay
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x66000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = SeniorSurface,
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = SeniorPrimary,
                                            strokeWidth = 3.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "🔍 正在仔细看字...",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SeniorDark
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // Top Tip or Empty State Banner
                            val characters = uiState.ocrResult?.characters ?: emptyList()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                    .align(Alignment.TopCenter),
                                contentAlignment = Alignment.Center
                            ) {
                                if (characters.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = SeniorPrimary,
                                        shadowElevation = 6.dp
                                    ) {
                                        Text(
                                            text = "👆 请轻点屏幕上的黄色字块听发音",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                        )
                                    }
                                } else {
                                    // Empty State Advice
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = SeniorSurface,
                                        shadowElevation = 6.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🧐 没看清画面里的字",
                                                color = SeniorDark,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 19.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "请将镜头靠近一点，或在下方开灯后再拍一次",
                                                color = SeniorTextMuted,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Live Camera Viewfinder with Touch-to-Focus and Torch
                        CameraViewfinder(
                            imageCaptureRef = { capture -> imageCapture = capture },
                            isTorchOn = uiState.isTorchOn,
                            onTorchToggle = { viewModel.toggleTorch(it) }
                        )

                        // Central Alignment Framing Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(220.dp)
                                .align(Alignment.Center)
                                .border(2.dp, Color(0x88FFFFFF), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = "把不认识的字放在框内拍照",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            )
                        }
                    }
                }

                // Bottom Control Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isFrozen) {
                        // "重新拍照" Button
                        Button(
                            onClick = { viewModel.unfreezeCamera() },
                            modifier = Modifier
                                .height(64.dp)
                                .padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SeniorSurface)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重拍",
                                modifier = Modifier.size(28.dp),
                                tint = SeniorDark
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "重新拍照",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 22.sp,
                                    color = SeniorDark
                                )
                            )
                        }
                    } else {
                        // Live Camera Bottom Controls: Gallery Import (Left) + Big Shutter (Center)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery / WeChat Screenshot Import Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                            ) {
                                Surface(
                                    modifier = Modifier.size(60.dp),
                                    shape = CircleShape,
                                    color = Color(0x66000000),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.8f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PhotoLibrary,
                                            contentDescription = "相册导入",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "相册图片",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Big Shutter Button (88dp diameter)
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        capturePhoto(
                                            imageCapture = imageCapture,
                                            executor = cameraExecutor,
                                            onSuccess = { bmp -> viewModel.onPhotoCaptured(bmp, source = "CAMERA") },
                                            onError = { /* Handle error */ }
                                        )
                                    },
                                    modifier = Modifier.size(76.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "拍照识字",
                                        tint = SeniorDark,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // Placeholder / Spacer to balance the layout
                            Spacer(modifier = Modifier.size(60.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Word Detail Sheet
    if (uiState.isDetailSheetVisible && uiState.selectedEntity != null) {
        WordDetailSheet(
            character = uiState.selectedEntity!!,
            isFavorite = uiState.isSelectedCharFavorite,
            onDismiss = { viewModel.dismissDetailSheet() },
            onSpeak = { viewModel.speakCurrentCharacter() },
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
