package com.seniordict.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.ui.theme.*

@Composable
fun SplitCharDialog(
    onDismiss: () -> Unit,
    onQuery: (String) -> Unit,
    candidates: List<CharacterEntity>,
    onSelectChar: (CharacterEntity) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    val quickRadicals = listOf(
        "木" to "木字旁",
        "艹" to "草字头",
        "氵" to "三点水",
        "亻" to "单人旁",
        "扌" to "提手旁",
        "口" to "口字旁",
        "讠" to "言字旁",
        "钅" to "金字旁",
        "火" to "火字旁",
        "日" to "日字旁"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = SeniorSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "偏旁 / 拆字查生字",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input box
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        onQuery(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    placeholder = {
                        Text(
                            text = "例如：左边木右边白，或草字头加约",
                            style = MaterialTheme.typography.bodyLarge.copy(color = SeniorTextMuted)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { onQuery(inputText) }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(32.dp))
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Radical Helper Chips
                Text(
                    text = "快捷偏旁（点击直接填入）：",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickRadicals) { (char, label) ->
                        ElevatedFilterChip(
                            selected = false,
                            onClick = {
                                val nextText = if (inputText.isBlank()) char else "$inputText $char"
                                inputText = nextText
                                onQuery(nextText)
                            },
                            label = {
                                Text(
                                    text = "$char ($label)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results Header
                Text(
                    text = if (candidates.isEmpty()) "候选生字（输入部件即可快速反查）：" else "查到的生字（点击可看大字和读音）：",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Candidates Grid (Large Tiles)
                if (candidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (inputText.isNotBlank()) "未找到对应拆分的汉字\n可尝试简化输入，例如只输入两个偏旁" else "在上方输入偏旁或描述\n例如输入：木 白",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = SeniorTextMuted
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 80.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(candidates) { item ->
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SeniorBackground)
                                    .border(2.dp, SeniorBorder, RoundedCornerShape(16.dp))
                                    .clickable { onSelectChar(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = item.char,
                                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp)
                                    )
                                    Text(
                                        text = item.pinyin,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = SeniorPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
