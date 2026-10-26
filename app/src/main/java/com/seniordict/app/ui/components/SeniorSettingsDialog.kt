package com.seniordict.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seniordict.app.data.preferences.SeniorPreferences
import com.seniordict.app.ui.theme.*

@Composable
fun SeniorSettingsDialog(
    currentSpeechRate: Float,
    isMnemonicEnabled: Boolean,
    isHapticEnabled: Boolean,
    onSpeechRateChange: (Float) -> Unit,
    onMnemonicToggle: (Boolean) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = SeniorSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "长辈设置",
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

                Spacer(modifier = Modifier.height(20.dp))

                // Item 1: Speech Rate
                Text(
                    text = "朗读语速",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeniorDark
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "根据听力习惯选择适合的发音速度",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SeniorTextMuted,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rates = listOf(
                        Triple("极慢 0.6x", SeniorPreferences.RATE_VERY_SLOW, "高龄"),
                        Triple("适中 0.8x", SeniorPreferences.RATE_MODERATE, "推荐"),
                        Triple("标准 1.0x", SeniorPreferences.RATE_NORMAL, "正常")
                    )

                    for ((label, rate, sub) in rates) {
                        val isSelected = kotlin.math.abs(currentSpeechRate - rate) < 0.05f
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) SeniorPrimary else SeniorBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSpeechRateChange(rate) },
                            color = if (isSelected) SeniorPrimary.copy(alpha = 0.12f) else SeniorBackground
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SeniorPrimary else SeniorDark
                                    )
                                )
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = if (isSelected) SeniorPrimary else SeniorTextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Item 2: Mnemonic Word Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SeniorBackground)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "朗读带生活组词",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorDark
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "例如读出：“柏，柏树的柏”",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SeniorTextMuted,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Switch(
                        checked = isMnemonicEnabled,
                        onCheckedChange = onMnemonicToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SeniorSuccess
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item 3: Haptic Feedback Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SeniorBackground)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "点字震动提示",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SeniorDark
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "手指点到字时手机轻微震动",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SeniorTextMuted,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Switch(
                        checked = isHapticEnabled,
                        onCheckedChange = onHapticToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SeniorSuccess
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section: App info & Commitment
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SeniorBorder.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔒 纯净适老承诺",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SeniorDark
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• 100% 离线可用：不消耗流量，无网络也能查\n• 零广告零弹窗：专为长辈长久安心使用打造\n• 权威字库：内置 1.48 万规范汉字与大白话解释",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = SeniorDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SeniorPrimary)
                ) {
                    Text(
                        text = "完成设置",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp)
                    )
                }
            }
        }
    }
}
