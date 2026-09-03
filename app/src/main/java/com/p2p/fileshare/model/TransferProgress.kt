package com.p2p.fileshare.model

import java.io.File

enum class TransferStatus {
    IDLE,
    CONNECTING,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class TransferProgress(
    val isSending: Boolean,
    val fileId: String,
    val fileName: String,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speedBps: Double,
    val etaSeconds: Long,
    val percent: Int,
    val status: TransferStatus = TransferStatus.TRANSFERRING,
    val errorMessage: String? = null,
    val localFile: File? = null
) {
    val formattedSpeed: String
        get() = "${SharedItem.formatFileSize(speedBps.toLong())}/s"

    val formattedTransferred: String
        get() = "${SharedItem.formatFileSize(transferredBytes)} / ${SharedItem.formatFileSize(totalBytes)}"

    val formattedEta: String
        get() = when {
            status == TransferStatus.COMPLETED -> "Done"
            etaSeconds <= 0 -> "--"
            etaSeconds < 60 -> "${etaSeconds}s remaining"
            etaSeconds < 3600 -> "${etaSeconds / 60}m ${etaSeconds % 60}s remaining"
            else -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m remaining"
        }
}

data class HistoryItem(
    val id: String,
    val name: String,
    val size: Long,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReceived: Boolean = true,
    val mimeType: String = "*/*"
) {
    val formattedSize: String
        get() = SharedItem.formatFileSize(size)
}
