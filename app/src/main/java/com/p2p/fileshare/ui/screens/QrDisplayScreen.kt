package com.p2p.fileshare.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import com.p2p.fileshare.model.FileCategory
import com.p2p.fileshare.model.SharedItem
import com.p2p.fileshare.model.TransferProgress
import com.p2p.fileshare.model.TransferStatus
import com.p2p.fileshare.ui.components.SpeedGauge
import com.p2p.fileshare.ui.theme.*

@Composable
fun QrDisplayScreen(
    qrBitmap: Bitmap?,
    shareUrl: String,
    files: List<SharedItem>,
    connectedPeersCount: Int,
    activeUploadProgress: TransferProgress?,
    onStopShare: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val totalBytes = files.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onStopShare) {
                Icon(Icons.Default.Close, contentDescription = "Stop", tint = TextPrimary)
            }
            Text("Scan to Download", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(
                color = AccentEmerald.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(AccentEmerald, androidx.compose.foundation.shape.CircleShape))
                    Text(
                        text = if (connectedPeersCount > 0) "$connectedPeersCount Connected" else "Ready",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald
                    )
                }
            }
        }

        // QR Code Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Share QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            }
        }

        Text(
            text = "Point receiver phone's camera at this QR code",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        // Web URL Copy Strip
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Direct Browser Download Link", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "$shareUrl/web",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan,
                        maxLines = 1
                    )
                }

                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString("$shareUrl/web"))
                    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary)
                }
            }
        }

        // Active Upload Stats
        if (activeUploadProgress != null) {
            SpeedGauge(progress = activeUploadProgress)
        }

        // Files Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sharing ${files.size} file(s)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(SharedItem.formatFileSize(totalBytes), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                }
                files.take(3).forEach { file ->
                    Text("• ${file.name} (${file.formattedSize})", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                }
                if (files.size > 3) {
                    Text("+ ${files.size - 3} more file(s)", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stop Sharing Button
        OutlinedButton(
            onClick = onStopShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentRose.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop Sharing Server", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F111A)
@Composable
fun QrDisplayScreenPreview() {
    P2PFileShareTheme {
        QrDisplayScreen(
            qrBitmap = null,
            shareUrl = "http://192.168.43.1:8080",
            files = listOf(
                SharedItem("1", Uri.EMPTY, "Avatar_2022_4K.mkv", 14500000000L, "video/x-matroska", FileCategory.MOVIE),
                SharedItem("2", Uri.EMPTY, "Vacation_Photos.zip", 450000000L, "application/zip", FileCategory.DOCUMENT),
                SharedItem("3", Uri.EMPTY, "Music_Collection.rar", 120000000L, "application/x-rar-compressed", FileCategory.DOCUMENT),
                SharedItem("4", Uri.EMPTY, "App_Backup.apk", 85000000L, "application/vnd.android.package-archive", FileCategory.APP)
            ),
            connectedPeersCount = 2,
            activeUploadProgress = TransferProgress(
                isSending = true,
                fileId = "1",
                fileName = "Avatar_2022_4K.mkv",
                totalBytes = 14500000000L,
                transferredBytes = 4350000000L,
                speedBps = 85000000.0,
                etaSeconds = 120,
                percent = 30,
                status = TransferStatus.TRANSFERRING
            ),
            onStopShare = {}
        )
    }
}
