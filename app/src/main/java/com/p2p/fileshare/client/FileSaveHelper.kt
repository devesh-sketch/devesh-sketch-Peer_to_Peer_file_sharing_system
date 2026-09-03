package com.p2p.fileshare.client

import android.content.Context
import android.os.Environment
import com.p2p.fileshare.util.FileOpener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileSaveHelper {

    fun getDownloadOutputStream(context: Context, rawFileName: String, mimeType: String): Pair<OutputStream?, File?> {
        val fileName = sanitizeFileName(rawFileName)

        // 1. Primary: Direct Public Storage / FlashShare Directory
        try {
            val isVideo = mimeType.startsWith("video/") || fileName.endsWith(".mp4") || fileName.endsWith(".mkv")
            val isImage = mimeType.startsWith("image/") || fileName.endsWith(".jpg") || fileName.endsWith(".png")

            val baseDir = when {
                isVideo -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                isImage -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }

            val flashDir = File(baseDir, "FlashShare")
            if (!flashDir.exists()) flashDir.mkdirs()

            val targetFile = getUniqueFile(flashDir, fileName)
            val outputStream = FileOutputStream(targetFile)
            return Pair(outputStream, targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback: Public Downloads Folder
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val flashDir = File(downloadsDir, "FlashShare")
            if (!flashDir.exists()) flashDir.mkdirs()

            val targetFile = getUniqueFile(flashDir, fileName)
            val outputStream = FileOutputStream(targetFile)
            return Pair(outputStream, targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Guaranteed Fallback: App External Storage (Always writable without special permissions)
        try {
            val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val flashDir = File(appDir, "FlashShare")
            if (!flashDir.exists()) flashDir.mkdirs()

            val targetFile = getUniqueFile(flashDir, fileName)
            val outputStream = FileOutputStream(targetFile)
            return Pair(outputStream, targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(null, null)
    }

    fun finishPendingFile(context: Context, file: File?, mimeType: String) {
        if (file != null && file.exists()) {
            FileOpener.scanMediaFile(context, file, mimeType)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }

    private fun getUniqueFile(dir: File, fileName: String): File {
        var file = File(dir, fileName)
        if (!file.exists()) return file

        val nameWithoutExt = file.nameWithoutExtension
        val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
        var count = 1

        while (file.exists()) {
            file = File(dir, "$nameWithoutExt ($count)$ext")
            count++
        }
        return file
    }
}
