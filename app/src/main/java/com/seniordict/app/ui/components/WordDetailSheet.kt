package com.seniordict.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailSheet(
    character: CharacterEntity,
    onDismiss: () -> Unit,
    onSpeak: (CharacterEntity) -> Unit
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
            // Header Row: Giant Character Box + Pinyin + Big Speaker Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Giant Character Tile (96dp x 96dp)
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SeniorBackground)
                        .border(2.dp, SeniorBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
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

            // Big Loudspeaker Button (Min 64dp height for elderly thumb)
            Button(
                onClick = { onSpeak(character) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SeniorSuccess
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "朗读发音",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "大声朗读发音",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 22.sp),
                    color = Color.White
                )
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

            // Section 2: Common Everyday Words (常见生活组词)
            if (!character.examples.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "【常见生活组词】",
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
                        text = character.examples.replace("、", "   •   "),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = SeniorPrimary
                        ),
                        modifier = Modifier.padding(18.dp)
                    )
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
