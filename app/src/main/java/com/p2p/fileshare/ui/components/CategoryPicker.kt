package com.p2p.fileshare.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.p2p.fileshare.model.FileCategory
import com.p2p.fileshare.ui.theme.*

@Composable
fun CategoryPicker(
    selectedCategory: FileCategory,
    onCategorySelected: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FileCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            val icon = when (category) {
                FileCategory.ALL -> "📁"
                FileCategory.MOVIE -> "🎬"
                FileCategory.AUDIO -> "🎵"
                FileCategory.IMAGE -> "📸"
                FileCategory.APP -> "📦"
                FileCategory.DOCUMENT -> "📄"
                FileCategory.OTHER -> "📦"
            }

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = "$icon ${category.label}",
                        fontSize = 13.sp,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else TextSecondary
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentPrimary,
                    containerColor = BgCard
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderCard,
                    selectedBorderColor = AccentPrimary,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}
