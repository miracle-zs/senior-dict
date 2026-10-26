package com.seniordict.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Giant Character Box with Calligraphy Tianzige + Pinyin + Metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Giant Character Tile (104dp x 104dp) with Chinese Calligraphy Tianzige
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SeniorBackground)
                        .border(2.dp, SeniorBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Tianzige (田字格) Calligraphy Watermark Grid
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val gridColor = Color(0x33DC2626) // Traditional cinnabar red watermark
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                        // Outer border
                        drawRect(
                            color = gridColor,
                            style = Stroke(width = 1.5f)
                        )
                        // Vertical center dashed line
                        drawLine(
                            color = gridColor,
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 1.2f,
                            pathEffect = dashEffect
                        )
                        // Horizontal center dashed line
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 1.2f,
                            pathEffect = dashEffect
                        )
                    }

                    Text(
                        text = character.char,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 68.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Pinyin & Metadata Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = character.pinyin,
                        style = MaterialTheme.typography.headlineLarge,
                        color = SeniorPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!character.radicals.isNullOrBlank()) {
                            BadgeChip(label = "部首: ${character.radicals}")
                        }
                        if (character.strokeCount > 0) {
                            BadgeChip(label = "${character.strokeCount} 画")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Loudspeaker Button + Favorites Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Big Loudspeaker Button
                Button(
                    onClick = { onSpeak(character) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SeniorSuccess
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "朗读发音",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "大声朗读",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp),
                        color = Color.White
                    )
                }

                // Bookmark / Favorite Button
                FilledTonalButton(
                    onClick = { onToggleFavorite(character.char) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFavorite) SeniorWarning.copy(alpha = 0.2f) else SeniorBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isFavorite) SeniorWarning else SeniorBorder
                    )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "收藏生字",
                        tint = if (isFavorite) SeniorWarning else SeniorDark,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFavorite) "已存入" else "存生字本",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFavorite) SeniorWarning else SeniorDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Plain Chinese Explanation (生活大白话解释)
            Text(
                text = "【常用大白话解释】",
                style = MaterialTheme.typography.titleLarge,
                color = SeniorDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SeniorBackground)
            ) {
                Text(
                    text = character.definition ?: "常用汉字",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }

            // Section 2: Common Everyday Words (常见生活组词 - 点击可单独发音)
            val wordList = remember(character.examples) {
                character.examples?.split("、", ",", "，", " ")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            }
            if (wordList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "【常见生活组词】（点击词语听发音）",
                    style = MaterialTheme.typography.titleLarge,
                    color = SeniorDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    wordList.forEach { word ->
                        ElevatedAssistChip(
                            onClick = { onSpeakWord(word) },
                            label = {
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SeniorDark
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "朗读 $word",
                                    modifier = Modifier.size(20.dp),
                                    tint = SeniorPrimary
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = SeniorBackground
                            ),
                            elevation = AssistChipDefaults.elevatedAssistChipElevation(
                                elevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SeniorBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Big Close Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, SeniorBorder)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "关闭卡片",
                    style = MaterialTheme.typography.labelLarge
                )
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
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = SeniorDark
        )
    }
}
