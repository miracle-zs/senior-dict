package com.seniordict.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordDetailSheet(
    character: CharacterEntity,
    isFavorite: Boolean = false,
    contextCharacters: List<String> = emptyList(),
    onSelectCharacter: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSpeak: (CharacterEntity) -> Unit,
    onSpeakWord: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {}
) {
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 0. Multi-Character Switcher Strip (if scanning sentence or word phrase)
            if (contextCharacters.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "识别文字:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeniorTextMuted
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(contextCharacters) { c ->
                            val isCurrent = c == character.char
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) JadePrimary else SeniorBackground,
                                border = BorderStroke(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) JadePrimaryDark else SeniorBorder
                                ),
                                modifier = Modifier.clickable {
                                    if (!isCurrent) onSelectCharacter(c)
                                }
                            ) {
                                Text(
                                    text = c,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else SeniorDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Header Row: Tianzige Character Tile + Pinyin + Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Giant Character Tile (96dp x 96dp) with Calligraphy Tianzige Grid
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SeniorBackground)
                        .border(2.dp, SeniorBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val gridColor = Color(0x33DC2626)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                        drawRect(color = gridColor, style = Stroke(width = 1.5f))
                        drawLine(color = gridColor, start = Offset(w / 2f, 0f), end = Offset(w / 2f, h), strokeWidth = 1.2f, pathEffect = dashEffect)
                        drawLine(color = gridColor, start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = 1.2f, pathEffect = dashEffect)
                    }

                    Text(
                        text = character.char,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Pinyin & Metadata Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = character.pinyin,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        color = SeniorPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!character.radicals.isNullOrBlank()) {
                            BadgeChip(label = "部首: ${character.radicals}")
                        }
                        if (character.strokeCount > 0) {
                            BadgeChip(label = "${character.strokeCount} 画")
                        }
                    }
                }

                // Header Top-Right Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SeniorBackground)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = SeniorTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Action Row: 朗读 + 收藏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Loudspeaker Button
                Button(
                    onClick = { onSpeak(character) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SeniorSuccess),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "朗读发音",
                        modifier = Modifier.size(26.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "朗读",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                // Bookmark / Favorite Button
                FilledTonalButton(
                    onClick = { onToggleFavorite(character.char) },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFavorite) SeniorWarning.copy(alpha = 0.15f) else SeniorBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (isFavorite) SeniorWarning else SeniorBorder
                    )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "收藏生字",
                        tint = if (isFavorite) SeniorWarning else SeniorDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFavorite) "已收藏" else "收藏",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFavorite) SeniorWarning else SeniorDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Section: 字义解释
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "字义解释",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                    color = SeniorDark
                )
                if (!character.definition.isNullOrBlank()) {
                    IconButton(
                        onClick = { onSpeakWord(character.definition) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "朗读解释",
                            tint = SeniorSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SeniorBackground)
            ) {
                Text(
                    text = character.definition ?: "常用汉字",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 4. Section: 常用组词
            val wordList = remember(character.examples) {
                character.examples?.split("、", ",", "，", " ")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            }
            if (wordList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "常用组词",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                    color = SeniorDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wordList.forEach { word ->
                        ElevatedAssistChip(
                            onClick = { onSpeakWord(word) },
                            label = {
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SeniorDark
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "朗读 $word",
                                    modifier = Modifier.size(18.dp),
                                    tint = SeniorPrimary
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.elevatedAssistChipColors(containerColor = SeniorBackground),
                            elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 1.dp, pressedElevation = 3.dp),
                            border = BorderStroke(1.dp, SeniorBorder)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SeniorBorder)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = SeniorDark
        )
    }
}
