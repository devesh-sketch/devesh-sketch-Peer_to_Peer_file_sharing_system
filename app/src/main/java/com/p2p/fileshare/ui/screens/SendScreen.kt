package com.p2p.fileshare.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.p2p.fileshare.model.FileCategory
import com.p2p.fileshare.model.SharedItem
import com.p2p.fileshare.ui.components.CategoryPicker
import com.p2p.fileshare.ui.theme.*

@Composable
fun SendScreen(
    selectedFiles: List<SharedItem>,
    onPickFiles: () -> Unit,
    onRemoveFile: (SharedItem) -> Unit,
    onStartShare: () -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }

    val filteredFiles = remember(selectedFiles, selectedCategory) {
        if (selectedCategory == FileCategory.ALL) selectedFiles
        else selectedFiles.filter { it.category == selectedCategory }
    }

    val totalBytes = remember(selectedFiles) {
        selectedFiles.sumOf { it.size }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Select Files to Share", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Box(modifier = Modifier.size(48.dp))
        }

        // Category Filter
        CategoryPicker(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        // Add File Action Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Select Any Files or Movies", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("No file size limits (4GB+ supported)", fontSize = 12.sp, color = TextMuted)
                }

                Button(
                    onClick = onPickFiles,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Files")
                }
            }
        }

        // Selected Files Summary Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected (${selectedFiles.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (selectedFiles.isNotEmpty()) {
                Text(
                    text = "Total: ${SharedItem.formatFileSize(totalBytes)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentCyan
                )
            }
        }

        // Files List
        if (selectedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📂", fontSize = 48.sp)
                    Text("No files selected yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Tap 'Add Files' to select movies, videos, or documents", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val icon = when (item.category) {
                                FileCategory.MOVIE -> "🎬"
                                FileCategory.AUDIO -> "🎵"
                                FileCategory.IMAGE -> "📸"
                                FileCategory.APP -> "📦"
                                FileCategory.DOCUMENT -> "📄"
                                else -> "📁"
                            }
                            Text(icon, fontSize = 24.sp)

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${item.formattedSize} • ${item.category.label}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            IconButton(onClick = { onRemoveFile(item) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = AccentRose)
                            }
                        }
                    }
                }
            }
        }

        // Share Action Button
        Button(
            onClick = onStartShare,
            enabled = selectedFiles.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Generate QR & Share (${selectedFiles.size} files)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SendScreenPreview() {
    val sampleSharedItems = listOf(
        SharedItem(
            id = "1",
            uri = Uri.EMPTY,
            name = "Big_Buck_Bunny.mp4",
            size = 1024L * 1024 * 750,
            mimeType = "video/mp4",
            category = FileCategory.MOVIE
        ),
        SharedItem(
            id = "2",
            uri = Uri.EMPTY,
            name = "Inception_Soundtrack.mp3",
            size = 1024L * 1024 * 12,
            mimeType = "audio/mpeg",
            category = FileCategory.AUDIO
        ),
        SharedItem(
            id = "3",
            uri = Uri.EMPTY,
            name = "Landscape_Photo.jpg",
            size = 1024L * 1024 * 3,
            mimeType = "image/jpeg",
            category = FileCategory.IMAGE
        ),
        SharedItem(
            id = "4",
            uri = Uri.EMPTY,
            name = "Social_App_v2.apk",
            size = 1024L * 1024 * 45,
            mimeType = "application/vnd.android.package-archive",
            category = FileCategory.APP
        ),
        SharedItem(
            id = "5",
            uri = Uri.EMPTY,
            name = "Project_Proposal.pdf",
            size = 1024L * 500,
            mimeType = "application/pdf",
            category = FileCategory.DOCUMENT
        )
    )

    P2PFileShareTheme {
        SendScreen(
            selectedFiles = sampleSharedItems,
            onPickFiles = {},
            onRemoveFile = {},
            onStartShare = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty Selection")
@Composable
fun SendScreenEmptyPreview() {
    P2PFileShareTheme {
        SendScreen(
            selectedFiles = emptyList(),
            onPickFiles = {},
            onRemoveFile = {},
            onStartShare = {},
            onBack = {}
        )
    }
}
