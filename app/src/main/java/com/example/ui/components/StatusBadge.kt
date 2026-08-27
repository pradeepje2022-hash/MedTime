package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoseStatus
import com.example.ui.theme.MedErrorContainer
import com.example.ui.theme.MedErrorRed
import com.example.ui.theme.MedOceanSecondary
import com.example.ui.theme.MedOceanSecondaryContainer
import com.example.ui.theme.MedSuccessGreen
import com.example.ui.theme.MedSuccessGreenContainer
import com.example.ui.theme.MedWarningAmber
import com.example.ui.theme.MedWarningContainer

@Composable
fun StatusBadge(
    status: DoseStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, icon, text) = when (status) {
        DoseStatus.TAKEN -> Quad(
            MedSuccessGreenContainer,
            MedSuccessGreen,
            Icons.Default.CheckCircle,
            "Taken"
        )
        DoseStatus.UPCOMING -> Quad(
            MedOceanSecondaryContainer,
            MedOceanSecondary,
            Icons.Default.Schedule,
            "Upcoming"
        )
        DoseStatus.MISSED -> Quad(
            MedErrorContainer,
            MedErrorRed,
            Icons.Default.Warning,
            "Missed"
        )
        DoseStatus.SKIPPED -> Quad(
            Color(0xFFE2E8F0),
            Color(0xFF64748B),
            Icons.Default.Close,
            "Skipped"
        )
        DoseStatus.SNOOZED -> Quad(
            MedWarningContainer,
            MedWarningAmber,
            Icons.Default.HourglassTop,
            "Snoozed"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
