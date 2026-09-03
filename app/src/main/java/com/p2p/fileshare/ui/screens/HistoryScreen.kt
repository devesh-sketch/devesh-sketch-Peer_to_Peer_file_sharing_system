package com.p2p.fileshare.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.p2p.fileshare.model.HistoryItem
import com.p2p.fileshare.ui.theme.*
import com.p2p.fileshare.util.FileOpener
import java.io.File

@Composable
fun HistoryScreen(
    historyItems: List<HistoryItem>,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Transfer History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (historyItems.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = AccentRose)
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
        }

        if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📜", fontSize = 48.sp)
                    Text("No transfer history yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Received files will appear here", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyItems) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isMovie = item.name.endsWith(".mp4") || item.name.endsWith(".mkv") || item.name.endsWith(".avi")
                            val isPhoto = item.name.endsWith(".jpg") || item.name.endsWith(".jpeg") || item.name.endsWith(".png")
                            val icon = when {
                                isMovie -> "🎬"
                                isPhoto -> "📸"
                                item.name.endsWith(".apk") -> "📦"
                                else -> "📄"
                            }
                            Text(icon, fontSize = 24.sp)

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
                                Text("${item.formattedSize} • ${if (item.isReceived) "Received" else "Sent"}", fontSize = 12.sp, color = TextSecondary)
                            }

                            val file = File(item.filePath)
                            IconButton(onClick = {
                                FileOpener.openFile(context, file)
                            }) {
                                Icon(Icons.Default.FileOpen, contentDescription = "Open", tint = AccentCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    P2PFileShareTheme {
        HistoryScreen(
            historyItems = listOf(
                HistoryItem(
                    id = "1",
                    name = "Epic_Action_Movie.mp4",
                    size = 1024 * 1024 * 550,
                    filePath = "/storage/emulated/0/Download/Movie.mp4",
                    isReceived = true
                ),
                HistoryItem(
                    id = "2",
                    name = "Summer_Vacation.jpg",
                    size = 1024 * 500,
                    filePath = "/storage/emulated/0/Download/Photo.jpg",
                    isReceived = false
                ),
                HistoryItem(
                    id = "3",
                    name = "Game_Installer.apk",
                    size = 1024 * 1024 * 45,
                    filePath = "/storage/emulated/0/Download/App.apk",
                    isReceived = true
                ),
                HistoryItem(
                    id = "4",
                    name = "Document.pdf",
                    size = 1024 * 100,
                    filePath = "/storage/emulated/0/Download/Doc.pdf",
                    isReceived = false
                )
            ),
            onClearHistory = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty History")
@Composable
fun HistoryScreenEmptyPreview() {
    P2PFileShareTheme {
        HistoryScreen(
            historyItems = emptyList(),
            onClearHistory = {},
            onBack = {}
        )
    }
}
