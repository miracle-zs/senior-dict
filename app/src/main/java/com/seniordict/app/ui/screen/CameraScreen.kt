package com.seniordict.app.ui.screen

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.data.db.FavoriteItem
import com.seniordict.app.ui.components.*
import com.seniordict.app.ui.theme.*
import com.seniordict.app.ui.viewmodel.MainUiState
import com.seniordict.app.ui.viewmodel.MainViewModel

/**
 * 拍照识字屏幕 (CameraScreen)
 * 专业适老全屏取景、防抖放大、触控点读与重拍。
 */
@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    favorites: List<FavoriteItem>,
    onBackToHome: () -> Unit,
    onLaunchGallery: () -> Unit,
    imageCaptureRef: (ImageCapture?) -> Unit,
    onCaptureClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. 相机取景或定格画面
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isFrozen && uiState.capturedBitmap != null) {
                // 定格图片
                Image(
                    bitmap = uiState.capturedBitmap.asImageBitmap(),
                    contentDescription = "定格画面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // OCR 字块交互覆盖层
                OcrOverlayCanvas(
                    ocrResult = uiState.ocrResult,
                    selectedChar = uiState.selectedOcrChar,
                    onCharTapped = { ocrChar -> viewModel.onCharacterTapped(ocrChar) }
                )

                // 识别中加载提示
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
                                    color = JadePrimary,
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
                    // 顶部点字指引条
                    val characters = uiState.ocrResult?.characters ?: emptyList()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        if (characters.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = JadePrimary,
                                shadowElevation = 6.dp
                            ) {
                                Text(
                                    text = "轻点文字即可听发音",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                        } else {
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
                                        text = "未识别到文字",
                                        color = SeniorDark,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "请靠近一点，或开启上方补光灯再拍一次",
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
                // 实时相机画面 (带 1x / 2x / 3x 变焦与闪光灯控制)
                CameraViewfinder(
                    imageCaptureRef = imageCaptureRef,
                    isTorchOn = uiState.isTorchOn,
                    onTorchToggle = { viewModel.toggleTorch(it) }
                )

                // 中央专业金色取景框准星
                FramingReticleBox(modifier = Modifier.align(Alignment.Center))
            }
        }

        // 2. 顶部高对比导航栏 (返回书房 + 标题 + 手电筒)
        Surface(
            color = Color(0x99000000),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "返回书房" 大触控按钮
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = JadePrimary,
                    modifier = Modifier.clickable { onBackToHome() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "返回",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 标题
                Text(
                    text = if (uiState.isFrozen) "点选文字" else "拍照查字",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // 手电筒补光开关
                IconButton(
                    onClick = { viewModel.toggleTorch(!uiState.isTorchOn) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isTorchOn) WarmAmber else Color(0x44FFFFFF))
                ) {
                    Icon(
                        imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "补光灯",
                        tint = if (uiState.isTorchOn) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 3. 底部控制栏 (已识别大字列表 + 重拍 或 三点式拍照控制)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isFrozen) {
                val characters = uiState.ocrResult?.characters ?: emptyList()
                if (!uiState.isLoading && characters.isNotEmpty() && !uiState.isDetailSheetVisible) {
                    // 已识别大字快捷点读卡片栏 (不用费劲点画面上的小框)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xF21E293B), // High contrast deep slate
                        shadowElevation = 10.dp,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x66F59E0B))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已识别汉字",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "共${characters.size}字 · 点字听读音",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                items(characters) { ch ->
                                    val isSelected = uiState.selectedOcrChar?.let {
                                        it.char == ch.char && it.bounds == ch.bounds
                                    } ?: false

                                    Surface(
                                        modifier = Modifier
                                            .size(width = 54.dp, height = 60.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { viewModel.onCharacterTapped(ch) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color(0x77F59E0B)
                                        ),
                                        shadowElevation = if (isSelected) 6.dp else 2.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ch.char,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSelected) Color(0xFF0F172A) else Color.White
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // "重新拍照" 大按钮
                Button(
                    onClick = { viewModel.unfreezeCamera() },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SeniorSurface)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "重拍",
                        modifier = Modifier.size(26.dp),
                        tint = SeniorDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "重新拍照",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SeniorDark
                        )
                    )
                }
            } else {
                // 相机控制三点式：相册导入(左) + 拍照大圆钮(中) + 生字本(右)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 相册导入
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onLaunchGallery() }
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
                            text = "相册选图",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // 88dp 拍照大圆钮
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0x55FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onCaptureClick,
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "拍照识字",
                                tint = JadeDeep,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // 生字本快捷入口
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.openHistorySheet() }
                    ) {
                        BadgedBox(
                            badge = {
                                if (favorites.isNotEmpty()) {
                                    Badge(
                                        containerColor = WarmAmber,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (favorites.size > 99) "99+" else "${favorites.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        ) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = CircleShape,
                                color = if (favorites.isNotEmpty()) WarmAmber.copy(alpha = 0.35f) else Color(0x66000000),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (favorites.isNotEmpty()) WarmAmber else Color.White.copy(alpha = 0.8f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "生字本",
                                        tint = if (favorites.isNotEmpty()) WarmAmber else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "生字本",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
