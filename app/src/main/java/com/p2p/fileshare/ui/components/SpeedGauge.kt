package com.p2p.fileshare.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.p2p.fileshare.model.TransferProgress
import com.p2p.fileshare.model.TransferStatus
import com.p2p.fileshare.ui.theme.*

@Composable
fun SpeedGauge(
    progress: TransferProgress,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Percent Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when (progress.status) {
                                TransferStatus.TRANSFERRING -> AccentCyan.copy(alpha = pulseAlpha)
                                TransferStatus.COMPLETED -> AccentEmerald
                                TransferStatus.ERROR -> AccentRose
                                else -> AccentAmber
                            },
                            CircleShape
                        )
                )
                Text(
                    text = when (progress.status) {
                        TransferStatus.TRANSFERRING -> if (progress.isSending) "Uploading to peer..." else "Downloading from peer..."
                        TransferStatus.COMPLETED -> "Transfer Complete ✓"
                        TransferStatus.ERROR -> "Transfer Failed"
                        TransferStatus.CONNECTING -> "Connecting..."
                        TransferStatus.PAUSED -> "Paused"
                        else -> "Ready"
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "${progress.percent}%",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = when (progress.status) {
                    TransferStatus.COMPLETED -> AccentEmerald
                    TransferStatus.ERROR -> AccentRose
                    else -> AccentCyan
                }
            )
        }

        // File Title
        Text(
            text = progress.fileName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 2
        )

        // Gradient Progress Bar
        LinearProgressIndicator(
            progress = { progress.percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = if (progress.status == TransferStatus.COMPLETED) AccentEmerald else AccentPrimary,
            trackColor = BgSecondary
        )

        // Metrics Grid (Transferred, Speed, ETA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Transferred", fontSize = 11.sp, color = TextMuted)
                Text(progress.formattedTransferred, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Speed", fontSize = 11.sp, color = TextMuted)
                Text(
                    text = if (progress.status == TransferStatus.TRANSFERRING) progress.formattedSpeed else "--",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("ETA", fontSize = 11.sp, color = TextMuted)
                Text(
                    text = progress.formattedEta,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress.status == TransferStatus.COMPLETED) AccentEmerald else AccentAmber
                )
            }
        }
    }
}
