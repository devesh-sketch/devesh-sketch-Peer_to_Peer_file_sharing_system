package com.p2p.fileshare.model

import android.net.Uri

enum class FileCategory(val label: String) {
    ALL("All Files"),
    MOVIE("Movies & Video"),
    AUDIO("Music"),
    IMAGE("Photos"),
    APP("Apps (APK)"),
    DOCUMENT("Documents"),
    OTHER("Other")
}

data class SharedItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val category: FileCategory = FileCategory.OTHER,
    val path: String? = null
) {
    val formattedSize: String
        get() = formatFileSize(size)

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val safeGroup = digitGroups.coerceIn(0, units.size - 1)
            return String.format("%.2f %s", bytes / Math.pow(1024.0, safeGroup.toDouble()), units[safeGroup])
        }

        fun detectCategory(mimeType: String?, fileName: String): FileCategory {
            val mime = mimeType?.lowercase() ?: ""
            val name = fileName.lowercase()

            return when {
                mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv") || 
                name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".wmv") || 
                name.endsWith(".flv") || name.endsWith(".webm") || name.endsWith(".m4v") -> FileCategory.MOVIE

                mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".flac") || 
                name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".aac") || 
                name.endsWith(".ogg") -> FileCategory.AUDIO

                mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp") || 
                name.endsWith(".bmp") -> FileCategory.IMAGE

                mime == "application/vnd.android.package-archive" || name.endsWith(".apk") || 
                name.endsWith(".xapk") || name.endsWith(".apks") -> FileCategory.APP

                mime.startsWith("text/") || mime.contains("pdf") || mime.contains("word") || 
                mime.contains("excel") || mime.contains("presentation") || name.endsWith(".pdf") || 
                name.endsWith(".docx") || name.endsWith(".doc") || name.endsWith(".xlsx") || 
                name.endsWith(".pptx") || name.endsWith(".txt") || name.endsWith(".zip") || 
                name.endsWith(".rar") || name.endsWith(".7z") || name.endsWith(".tar") || 
                name.endsWith(".gz") || name.endsWith(".iso") -> FileCategory.DOCUMENT

                else -> FileCategory.OTHER
            }
        }
    }
}
