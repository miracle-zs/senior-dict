package com.seniordict.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.data.db.FavoriteItem
import com.seniordict.app.data.db.HistoryItem
import com.seniordict.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAndFavoritesSheet(
    favorites: List<FavoriteItem>,
    history: List<HistoryItem>,
    onSelectChar: (String) -> Unit,
    onSpeakChar: (String, String?, String?) -> Unit,
    onDeleteFavorite: (String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Favorites, 1: History
    var showClearConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SeniorSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SeniorBorder,
                width = 56.dp,
                height = 6.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "温习生字",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeniorDark
                    )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(28.dp))
                }
            }

            // Big Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SeniorBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab 0: 生字本
                Button(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) SeniorPrimary else Color.Transparent,
                        contentColor = if (selectedTab == 0) Color.White else SeniorDark
                    ),
                    elevation = if (selectedTab == 0) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "生字本",
                        modifier = Modifier.size(20.dp),
                        tint = if (selectedTab == 0) Color.White else SeniorPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "我的生字本 (${favorites.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp)
                    )
                }

                // Tab 1: 查字历史
                Button(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) SeniorPrimary else Color.Transparent,
                        contentColor = if (selectedTab == 1) Color.White else SeniorDark
                    ),
                    elevation = if (selectedTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text(
                        text = "查字历史 (${history.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            if (selectedTab == 0) {
                // Favorites List
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⭐️ 生字本还是空的\n在查字卡片上点击【存入生字本】\n就能随时在这里复习啦！",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = SeniorTextMuted,
                                fontSize = 18.sp,
                                lineHeight = 26.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(favorites, key = { it.char }) { item ->
                            CharacterListItemCard(
                                char = item.char,
                                pinyin = item.pinyin ?: "",
                                definition = item.definition ?: "常用汉字",
                                examples = item.examples,
                                onCardClick = { onSelectChar(item.char) },
                                onSpeakClick = { onSpeakChar(item.char, item.pinyin, item.examples) },
                                onDeleteClick = { onDeleteFavorite(item.char) }
                            )
                        }
                    }
                }
            } else {
                // History List
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🕒 暂无查字历史\n拍照识字或拆字反查后\n会自动记录在这里方便回看",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = SeniorTextMuted,
                                fontSize = 18.sp,
                                lineHeight = 26.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showClearConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "清空", modifier = Modifier.size(18.dp), tint = SeniorTextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("清空全部历史", color = SeniorTextMuted, fontSize = 14.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(history, key = { it.id }) { item ->
                                CharacterListItemCard(
                                    char = item.char,
                                    pinyin = item.pinyin ?: "",
                                    definition = item.definition ?: "常用汉字",
                                    examples = item.examples,
                                    onCardClick = { onSelectChar(item.char) },
                                    onSpeakClick = { onSpeakChar(item.char, item.pinyin, item.examples) },
                                    onDeleteClick = { onDeleteHistoryItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(text = "确认清空查字历史？", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(text = "清空后将无法找回历史记录，您的生字本收藏不受影响。", style = MaterialTheme.typography.bodyLarge)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CharacterListItemCard(
    char: String,
    pinyin: String,
    definition: String,
    examples: String?,
    onCardClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SeniorBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Character Tile
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SeniorSurface)
                    .border(2.dp, SeniorBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeniorDark
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Pinyin & Definition
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pinyin,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeniorPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = SeniorDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!examples.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "组词：${examples.split("、", ",").take(2).joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = SeniorTextMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Loudspeaker Button (Green, 48dp)
            IconButton(
                onClick = onSpeakClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SeniorSuccess.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "朗读发音",
                    tint = SeniorSuccess,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete Button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = SeniorTextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
