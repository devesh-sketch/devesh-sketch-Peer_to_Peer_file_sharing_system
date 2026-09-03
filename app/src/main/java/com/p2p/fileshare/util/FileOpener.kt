package com.p2p.fileshare.util

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileOpener {

    fun scanMediaFile(context: Context, file: File, mimeType: String? = null) {
        try {
            val resolvedMime = mimeType ?: detectMimeType(file.name)
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf(resolvedMime)
            ) { path, uri ->
                // Media scan complete: Available in Gallery/Photos
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Scan with system media provider so Photos/Gallery recognize it
        scanMediaFile(context, file)

        val mimeType = detectMimeType(file.name)

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open ${file.name} with"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: try opening with generic */*
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Open file with"))
            } catch (fallbackError: Exception) {
                Toast.makeText(
                    context,
                    "No app found to open this file. Saved to: ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun shareFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = detectMimeType(file.name)

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share ${file.name} via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun detectMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotEmpty()) {
            val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (fromMap != null) return fromMap
        }

        val name = fileName.lowercase()
        return when {
            name.endsWith(".mp4") -> "video/mp4"
            name.endsWith(".mkv") -> "video/x-matroska"
            name.endsWith(".avi") -> "video/x-msvideo"
            name.endsWith(".mov") -> "video/quicktime"
            name.endsWith(".wmv") -> "video/x-ms-wmv"
            name.endsWith(".flv") -> "video/x-flv"
            name.endsWith(".webm") -> "video/webm"
            name.endsWith(".m4v") -> "video/x-m4v"
            name.endsWith(".3gp") -> "video/3gpp"

            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".png") -> "image/png"
            name.endsWith(".gif") -> "image/gif"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".bmp") -> "image/bmp"
            name.endsWith(".svg") -> "image/svg+xml"

            name.endsWith(".mp3") -> "audio/mpeg"
            name.endsWith(".m4a") -> "audio/mp4"
            name.endsWith(".wav") -> "audio/wav"
            name.endsWith(".flac") -> "audio/flac"
            name.endsWith(".aac") -> "audio/aac"
            name.endsWith(".ogg") -> "audio/ogg"

            name.endsWith(".apk") -> "application/vnd.android.package-archive"
            name.endsWith(".pdf") -> "application/pdf"
            name.endsWith(".docx") || name.endsWith(".doc") -> "application/msword"
            name.endsWith(".xlsx") || name.endsWith(".xls") -> "application/vnd.ms-excel"
            name.endsWith(".pptx") || name.endsWith(".ppt") -> "application/vnd.ms-powerpoint"
            name.endsWith(".txt") -> "text/plain"
            name.endsWith(".zip") -> "application/zip"
            name.endsWith(".rar") -> "application/x-rar-compressed"
            name.endsWith(".7z") -> "application/x-7z-compressed"
            name.endsWith(".iso") -> "application/x-iso9660-image"

            else -> "*/*"
        }
    }
}
