package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.MedicineType

@Composable
fun MedicineTypeIcon(
    medicineType: MedicineType,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val baseColor = Color(colorHex)
    val containerColor = baseColor.copy(alpha = 0.16f)

    val iconVector: ImageVector = when (medicineType) {
        MedicineType.TABLET -> Icons.Default.Medication
        MedicineType.CAPSULE -> Icons.Default.Healing
        MedicineType.SYRUP -> Icons.Default.LocalDrink
        MedicineType.INJECTION -> Icons.Default.Vaccines
        MedicineType.DROPS -> Icons.Default.Opacity
        MedicineType.INHALER -> Icons.Default.Air
        MedicineType.TOPICAL -> Icons.Default.Biotech
        MedicineType.OTHER -> Icons.Default.LocalPharmacy
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = medicineType.displayName,
            tint = baseColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
