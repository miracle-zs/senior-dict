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
 * 银发字典 · 首页 (HomeScreen)
 * 纯粹自然，大字高对比，温润防眩光。
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
            in 5..11 -> "早上好"
            in 12..13 -> "中午好"
            in 14..18 -> "下午好"
            else -> "晚上好"
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
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Brand & Greeting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(JadePrimaryDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "字",
                                color = WarmAmber,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Serif
                            )
                        }

                        Column {
                            Text(
                                text = "银发字典",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 23.sp,
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

                    // Settings Button
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
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JadeLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = JadePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = {
                                Text(
                                    text = "输入汉字或拼音查字",
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

            // 1.5 即时搜索候选 (拼音或汉字前缀联想)
            if (uiState.searchSuggestions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "候选汉字 (轻触查字与发音)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = JadePrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.searchSuggestions) { entity ->
                                Surface(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(82.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.5.dp, JadeLight, RoundedCornerShape(14.dp))
                                        .clickable {
                                            focusManager.clearFocus()
                                            viewModel.onDirectLookup(entity.char, source = "SUGGESTION")
                                        },
                                    color = SeniorSurface,
                                    shadowElevation = 2.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = entity.pinyin.ifEmpty { " " },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = JadePrimary
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = entity.char,
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = 34.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SeniorDark
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. 核心主入口：拍照看字
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    shadowElevation = 5.dp,
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
                                Text(
                                    text = "拍照看字",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "对准包装、药盒或书报拍照点读",
                                    fontSize = 15.sp,
                                    color = JadeLight,
                                    lineHeight = 20.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(2.dp, WarmAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    tint = WarmAmberLight,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. 另外两大查字法：手写查字 + 偏旁拆字
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 手写查字卡片
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SeniorSurface,
                        shadowElevation = 2.dp,
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(JadeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = "手写",
                                    tint = JadePrimaryDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "手写查字",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorDark
                            )
                            Text(
                                text = "屏幕手写，随写随认",
                                fontSize = 13.sp,
                                color = SeniorTextMuted
                            )
                        }
                    }

                    // 偏旁拆字卡片
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SeniorSurface,
                        shadowElevation = 2.dp,
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmAmberLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = "拆字",
                                    tint = WarmAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "偏旁拆字",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorDark
                            )
                            Text(
                                text = "按部件组合反查",
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmAmberLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "生字本",
                                    tint = WarmAmber,
                                    modifier = Modifier.size(24.dp)
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
                                    text = if (favorites.isEmpty()) "收藏难记生字，随时复习" else "已收藏 ${favorites.size} 个生字",
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
                            viewModel.speakWord("${daily.char}。${daily.blessing}")
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

            // 7. 纯离线隐私提示底纹
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "纯离线词典 · 零网络消耗 · 隐私保护",
                        fontSize = 13.sp,
                        color = SeniorTextMuted.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        shadowElevation = 3.dp,
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
                            drawLine(strokeColor, Offset(w / 2f, 0f), Offset(w / 2f, h), 1.5f)
                            drawLine(strokeColor, Offset(0f, h / 2f), Offset(w, h / 2f), 1.5f)
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
        }
    }
}
