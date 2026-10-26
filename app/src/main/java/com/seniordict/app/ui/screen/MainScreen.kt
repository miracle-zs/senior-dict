package com.seniordict.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
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
import com.seniordict.app.ui.components.*
import com.seniordict.app.ui.theme.*
import com.seniordict.app.ui.viewmodel.MainViewModel
import java.util.concurrent.Executors

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
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

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = SeniorDark,
        topBar = {
            // Top Bar with Big Title and Direct Shortcuts
            Surface(
                color = SeniorBackground,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "银发字典",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SeniorDark
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // "偏旁拆字" Button
                        FilledTonalButton(
                            onClick = { viewModel.openSplitDialog() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = "拆字", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("偏旁拆字", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                        }

                        // "手写板" Button
                        FilledTonalButton(
                            onClick = { viewModel.openHandwriting() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Create, contentDescription = "手写", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("手写", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
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

                        // Top Tip Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .align(Alignment.TopCenter),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = SeniorPrimary,
                                shadowElevation = 6.dp
                            ) {
                                Text(
                                    text = "👆 请用手指轻点屏幕上的黄色字块查看读音",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                        }
                    } else {
                        // Live Camera Viewfinder with Touch-to-Focus
                        CameraViewfinder(
                            imageCaptureRef = { capture -> imageCapture = capture }
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
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isFrozen) {
                        // "重新拍照" Button
                        Button(
                            onClick = { viewModel.unfreezeCamera() },
                            modifier = Modifier
                                .height(64.dp)
                                .padding(horizontal = 32.dp),
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
                                        onSuccess = { bmp -> viewModel.onPhotoCaptured(bmp) },
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
                    }
                }
            }
        }
    }

    // Modal Word Detail Sheet
    if (uiState.isDetailSheetVisible && uiState.selectedEntity != null) {
        WordDetailSheet(
            character = uiState.selectedEntity!!,
            onDismiss = { viewModel.dismissDetailSheet() },
            onSpeak = { viewModel.speakCurrentCharacter() }
        )
    }

    // Split Character Query Dialog
    if (uiState.isSplitDialogVisible) {
        SplitCharDialog(
            onDismiss = { viewModel.closeSplitDialog() },
            onQuery = { query -> viewModel.onSplitQuery(query) },
            candidates = uiState.splitCandidates,
            onSelectChar = { charEntity -> viewModel.onDirectLookup(charEntity.char) }
        )
    }

    // Handwriting Canvas Dialog
    if (uiState.isHandwritingVisible) {
        HandwritingCanvasDialog(
            onDismiss = { viewModel.closeHandwriting() },
            onSelectChar = { charEntity -> viewModel.onDirectLookup(charEntity.char) }
        )
    }
}
