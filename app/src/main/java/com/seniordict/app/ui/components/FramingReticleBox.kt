package com.seniordict.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniordict.app.ui.theme.SeniorWarning

/**
 * Camera Viewfinder Framing Box with 4 Golden L-Shaped Reticle Brackets
 * Provides clear visual anchoring for elderly users aligning medicine labels.
 */
@Composable
fun FramingReticleBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .height(230.dp)
    ) {
        // Reticle Corner Brackets Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bracketLength = 32.dp.toPx()
            val bracketStroke = 4.5f
            val cornerRadius = 16.dp.toPx()
            val bracketColor = Color(0xFFF59E0B) // High-contrast warm gold

            // Background subtle rounded guideline
            drawRoundRect(
                color = Color(0x33FFFFFF),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )

            // Top-Left L-Bracket
            drawLine(
                color = bracketColor,
                start = Offset(cornerRadius, 0f),
                end = Offset(cornerRadius + bracketLength, 0f),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = bracketColor,
                start = Offset(0f, cornerRadius),
                end = Offset(0f, cornerRadius + bracketLength),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = bracketColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bracketStroke, cap = StrokeCap.Round)
            )

            // Top-Right L-Bracket
            drawLine(
                color = bracketColor,
                start = Offset(w - cornerRadius - bracketLength, 0f),
                end = Offset(w - cornerRadius, 0f),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = bracketColor,
                start = Offset(w, cornerRadius),
                end = Offset(w, cornerRadius + bracketLength),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = bracketColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w - cornerRadius * 2, 0f),
                size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bracketStroke, cap = StrokeCap.Round)
            )

            // Bottom-Left L-Bracket
            drawLine(
                color = bracketColor,
                start = Offset(0f, h - cornerRadius - bracketLength),
                end = Offset(0f, h - cornerRadius),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = bracketColor,
                start = Offset(cornerRadius, h),
                end = Offset(cornerRadius + bracketLength, h),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = bracketColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, h - cornerRadius * 2),
                size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bracketStroke, cap = StrokeCap.Round)
            )

            // Bottom-Right L-Bracket
            drawLine(
                color = bracketColor,
                start = Offset(w, h - cornerRadius - bracketLength),
                end = Offset(w, h - cornerRadius),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = bracketColor,
                start = Offset(w - cornerRadius - bracketLength, h),
                end = Offset(w - cornerRadius, h),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = bracketColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w - cornerRadius * 2, h - cornerRadius * 2),
                size = androidx.compose.ui.geometry.Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bracketStroke, cap = StrokeCap.Round)
            )
        }

        // Instructional Tip Pill (Dark frosted glass, guarantees readability against bright white medicine bottles)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xAA0F172A)
        ) {
            Text(
                text = "把不认识的字放在金角框内拍照",
                color = Color(0xFFF8FAFC),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
