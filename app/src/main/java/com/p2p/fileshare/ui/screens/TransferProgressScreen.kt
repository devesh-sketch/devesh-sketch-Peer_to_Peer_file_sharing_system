package com.p2p.fileshare.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.p2p.fileshare.model.TransferProgress
import com.p2p.fileshare.model.TransferStatus
import com.p2p.fileshare.ui.components.SpeedGauge
import com.p2p.fileshare.ui.theme.*
import com.p2p.fileshare.util.FileOpener
import java.io.File

@Composable
fun TransferProgressScreen(
    progress: TransferProgress?,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
            }
            Text(
                text = if (progress?.isSending == true) "Sending Stream" else "Receiving Stream",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (progress != null) {
            SpeedGauge(progress = progress)

            // When Download is Completed -> Action Buttons
            if (progress.status == TransferStatus.COMPLETED && progress.localFile != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 File Saved & Ready!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald
                        )

                        val isMovie = progress.fileName.endsWith(".mp4") || progress.fileName.endsWith(".mkv") || 
                                     progress.fileName.endsWith(".avi") || progress.fileName.endsWith(".mov")
                        val isImage = progress.fileName.endsWith(".jpg") || progress.fileName.endsWith(".jpeg") || 
                                     progress.fileName.endsWith(".png") || progress.fileName.endsWith(".webp")

                        Button(
                            onClick = {
                                FileOpener.openFile(context, progress.localFile)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Icon(
                                when {
                                    isMovie -> Icons.Default.PlayArrow
                                    isImage -> Icons.Default.Image
                                    else -> Icons.Default.FileOpen
                                },
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isMovie -> "🎬 Play Movie / Video"
                                    isImage -> "📸 View Photo / Image"
                                    progress.fileName.endsWith(".apk") -> "📦 Install App (APK)"
                                    else -> "📂 Open File"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                FileOpener.shareFile(context, progress.localFile)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCard)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share / Send to Other Apps", color = TextPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (progress.status == TransferStatus.TRANSFERRING) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRose.copy(alpha = 0.4f))
                ) {
                    Text("Cancel Transfer")
                }
            } else {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Done")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransferProgressScreenTransferringPreview() {
    P2PFileShareTheme {
        TransferProgressScreen(
            progress = TransferProgress(
                isSending = true,
                fileId = "1",
                fileName = "Big_Buck_Bunny.mp4",
                totalBytes = 1024L * 1024 * 750,
                transferredBytes = 1024L * 1024 * 350,
                speedBps = 1024L * 1024 * 12.5,
                etaSeconds = 32,
                percent = 46,
                status = TransferStatus.TRANSFERRING
            ),
            onCancel = {},
            onDone = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransferProgressScreenCompletedPreview() {
    P2PFileShareTheme {
        TransferProgressScreen(
            progress = TransferProgress(
                isSending = false,
                fileId = "2",
                fileName = "Vacation_Photo.jpg",
                totalBytes = 1024L * 1024 * 5,
                transferredBytes = 1024L * 1024 * 5,
                speedBps = 0.0,
                etaSeconds = 0,
                percent = 100,
                status = TransferStatus.COMPLETED,
                localFile = File("dummy.jpg")
            ),
            onCancel = {},
            onDone = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransferProgressScreenLoadingPreview() {
    P2PFileShareTheme {
        TransferProgressScreen(
            progress = null,
            onCancel = {},
            onDone = {}
        )
    }
}
