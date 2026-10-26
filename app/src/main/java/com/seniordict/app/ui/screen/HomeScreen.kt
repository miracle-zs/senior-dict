package com.seniordict.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.data.db.FavoriteItem
import com.seniordict.app.data.db.HistoryItem
import com.seniordict.app.ui.theme.*
import com.seniordict.app.ui.viewmodel.DailyWord
import com.seniordict.app.ui.viewmodel.MainViewModel
import java.util.Calendar

/**
 * 经典辞书 · 典雅书房首页 (HomeScreen)
 * 养生雅致翠竹青调，大字高对比，温润防眩光。
 * 启动时完全不打开摄像头，护眼省电无打扰。
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    favorites: List<FavoriteItem>,
    history: List<HistoryItem>,
    onOpenCamera: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "早上好，长辈"
            in 12..13 -> "中午好，长辈"
            in 14..18 -> "下午好，长辈"
            else -> "晚上好，长辈"
        }
    }

    Scaffold(
        containerColor = SeniorBackground,
        topBar = {
            Surface(
                color = SeniorSurface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Brand & Greeting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Classical Seal "字" Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(JadePrimaryDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "字",
                                color = WarmAmber,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Serif
                            )
                        }

                        Column {
                            Text(
                                text = "银发字典",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = JadeDeep
                                )
                            )
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = SeniorTextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Settings & Help Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sound volume assurance indicator
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = JadeContainer,
                            modifier = Modifier.clickable {
                                viewModel.speakWord("大音量发音正常")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "大音量",
                                    tint = JadePrimaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "大音量",
                                    color = JadeDeep,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.openSettingsDialog() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = JadeDeep,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 大字搜索栏
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SeniorSurface,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JadeLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = JadePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = {
                                Text(
                                    text = "输入汉字或拼音 (如：福、fu)",
                                    fontSize = 17.sp,
                                    color = SeniorTextMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    viewModel.submitSearch(uiState.searchQuery)
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = SeniorDark,
                                unfocusedTextColor = SeniorDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清空", tint = SeniorTextMuted)
                            }
                        }
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.submitSearch(uiState.searchQuery)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JadePrimary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "查字",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. 核心主英雄位：拍照查字 (全宽尊老黄金卡片)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCamera() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(JadePrimaryDark, JadePrimary)
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = WarmAmberBorder,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // 推荐标签
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = WarmAmberLight
                                ) {
                                    Text(
                                        text = "★ 适老专属 · 一拍即查",
                                        color = WarmAmber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "拍照看字",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "看药盒、说明书、报纸说明，指哪读哪",
                                    fontSize = 15.sp,
                                    color = JadeLight,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WarmAmber,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "开启大字相机",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Camera Decorative Icon
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(2.dp, WarmAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    tint = WarmAmberLight,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. 另外两大查字法：田字格手写 + 偏旁拆字
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 手写查字卡片
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SeniorSurface,
                        shadowElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, JadeLight),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.openHandwriting() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(JadeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = "手写",
                                    tint = JadePrimaryDark,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "田字格手写",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorDark
                            )
                            Text(
                                text = "大格书写 · 拼音提示",
                                fontSize = 13.sp,
                                color = SeniorTextMuted
                            )
                        }
                    }

                    // 偏旁拆字卡片
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SeniorSurface,
                        shadowElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmAmberLight),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.openSplitDialog() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmAmberLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = "拆字",
                                    tint = WarmAmber,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "偏旁拆字",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorDark
                            )
                            Text(
                                text = "如：左边木右边白",
                                fontSize = 13.sp,
                                color = SeniorTextMuted
                            )
                        }
                    }
                }
            }

            // 4. 我的生字本入口
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SeniorSurface,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SeniorBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openHistorySheet() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmAmberLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "生字本",
                                    tint = WarmAmber,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "我的生字本",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SeniorDark
                                )
                                Text(
                                    text = if (favorites.isEmpty()) "点击红心收藏重点字，随时查阅温习" else "已收藏 ${favorites.size} 个生字，点击进入温习",
                                    fontSize = 14.sp,
                                    color = SeniorTextMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "进入",
                            tint = SeniorTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 5. 今日一字 · 每日吉字赏析卡片
            uiState.dailyWord?.let { daily ->
                item {
                    DailyWordCard(
                        dailyWord = daily,
                        onClickWord = {
                            viewModel.onDirectLookup(daily.char, source = "DAILY_CARD")
                        },
                        onSpeakWord = {
                            viewModel.speakWord("${daily.char}，${daily.pinyin}。${daily.blessing}")
                        }
                    )
                }
            }

            // 6. 最近查字足迹 (若有历史)
            if (history.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最近查过的字",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JadeDeep
                            )
                            TextButton(onClick = { viewModel.clearAllHistory() }) {
                                Text("清空记录", color = SeniorTextMuted, fontSize = 14.sp)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(history.take(12)) { item ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SeniorSurface,
                                    shadowElevation = 2.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, JadeLight),
                                    modifier = Modifier.clickable {
                                        viewModel.onDirectLookup(item.char, source = "HISTORY_TILE")
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = item.pinyin ?: "",
                                            fontSize = 12.sp,
                                            color = SeniorTextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = item.char,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SeniorDark,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. 贴心助老底纹文案
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌿 银发字典 · 字字清晰 · 纯离线保护长辈隐私",
                        fontSize = 13.sp,
                        color = SeniorTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * 每日吉字卡 (典雅朱砂田字格与生活化解说)
 */
@Composable
private fun DailyWordCard(
    dailyWord: DailyWord,
    onClickWord: () -> Unit,
    onSpeakWord: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SeniorSurface,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CinnabarRed.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickWord() }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: 今日吉字标签 + 祝福语
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CinnabarRedLight
                ) {
                    Text(
                        text = "今日吉字",
                        color = CinnabarRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = dailyWord.blessing,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JadeDeep
                )
            }

            // Center: 朱砂田字格汉字 + 发音与解释
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calligraphy Tianzige Box (82dp x 82dp)
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBF7))
                        .border(1.5.dp, CinnabarRed.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val strokeColor = CinnabarRed.copy(alpha = 0.25f)
                            // Cross lines
                            drawLine(strokeColor, Offset(w / 2f, 0f), Offset(w / 2f, h), 1.5f)
                            drawLine(strokeColor, Offset(0f, h / 2f), Offset(w, h / 2f), 1.5f)
                            // Diagonal dotted hints
                            drawLine(strokeColor, Offset(0f, 0f), Offset(w, h), 1f)
                            drawLine(strokeColor, Offset(w, 0f), Offset(0f, h), 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dailyWord.char,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CinnabarRed,
                        fontFamily = FontFamily.Serif
                    )
                }

                // Pinyin + Audio Button + Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = dailyWord.pinyin,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = JadeDeep
                        )

                        // Big Speaker Button
                        Button(
                            onClick = onSpeakWord,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SeniorSuccess),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "读音",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "听发音",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = dailyWord.lifeMeaning,
                        fontSize = 15.sp,
                        color = SeniorDark,
                        lineHeight = 21.sp
                    )
                }
            }

            // Footer hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "点击查看笔顺、部首与完整大白话组词 →",
                    fontSize = 13.sp,
                    color = JadePrimaryDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
