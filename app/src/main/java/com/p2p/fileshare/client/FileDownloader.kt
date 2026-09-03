package com.p2p.fileshare.client

import android.content.Context
import android.util.Log
import com.p2p.fileshare.model.FileCategory
import com.p2p.fileshare.model.SharedItem
import com.p2p.fileshare.model.TransferProgress
import com.p2p.fileshare.model.TransferStatus
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FileDownloader(
    private val context: Context,
    private val listener: DownloadEventListener
) {
    interface DownloadEventListener {
        fun onDiscoveredFiles(files: List<SharedItem>, deviceName: String, hostBaseUrl: String)
        fun onProgress(progress: TransferProgress)
        fun onFileDownloaded(file: File, fileName: String, mimeType: String)
        fun onBatchCompleted(totalFiles: Int)
        fun onDownloadError(error: String)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var isCancelled = false
    private val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer for high throughput

    fun fetchServerFiles(baseUrl: String) {
        scope.launch {
            try {
                val cleanUrl = baseUrl.trimEnd('/')
                Log.d("FileDownloader", "Connecting to peer: $cleanUrl/api/info")

                val url = URL("$cleanUrl/api/info")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }

                val code = connection.responseCode
                if (code == 200) {
                    val reader = InputStreamReader(connection.inputStream)
                    val responseStr = reader.readText()
                    reader.close()

                    val json = JSONObject(responseStr)
                    val deviceName = json.optString("device", "P2P Peer")
                    val filesArray = json.getJSONArray("files")

                    val items = mutableListOf<SharedItem>()
                    for (i in 0 until filesArray.length()) {
                        val obj = filesArray.getJSONObject(i)
                        val id = obj.getString("id")
                        val name = obj.getString("name")
                        val size = obj.getLong("size")
                        val mime = obj.optString("mime", "*/*")
                        val catStr = obj.optString("category", "ALL")
                        val cat = try { FileCategory.valueOf(catStr) } catch (e: Exception) { FileCategory.ALL }

                        items.add(
                            SharedItem(
                                id = id,
                                uri = android.net.Uri.parse("$cleanUrl/download?id=$id"),
                                name = name,
                                size = size,
                                mimeType = mime,
                                category = cat
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        listener.onDiscoveredFiles(items, deviceName, cleanUrl)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        listener.onDownloadError("Failed to connect to peer (HTTP $code)")
                    }
                }
            } catch (e: Exception) {
                Log.e("FileDownloader", "Fetch files error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    listener.onDownloadError("Connection failed: Make sure both phones are on the same Wi-Fi/Hotspot. (${e.localizedMessage})")
                }
            }
        }
    }

    fun downloadFiles(baseUrl: String, items: List<SharedItem>) {
        isCancelled = false
        scope.launch {
            val cleanUrl = baseUrl.trimEnd('/')
            var downloadedCount = 0

            for (item in items) {
                if (isCancelled) break
                val success = downloadSingleFile(cleanUrl, item)
                if (success) {
                    downloadedCount++
                }
            }

            if (!isCancelled && downloadedCount > 0) {
                withContext(Dispatchers.Main) {
                    listener.onBatchCompleted(downloadedCount)
                }
            }
        }
    }

    private suspend fun downloadSingleFile(baseUrl: String, item: SharedItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = URL("$baseUrl/download?id=${item.id}")
            Log.d("FileDownloader", "Starting stream from: $downloadUrl")

            withContext(Dispatchers.Main) {
                listener.onProgress(
                    TransferProgress(
                        isSending = false,
                        fileId = item.id,
                        fileName = item.name,
                        totalBytes = item.size,
                        transferredBytes = 0L,
                        speedBps = 0.0,
                        etaSeconds = 0L,
                        percent = 0,
                        status = TransferStatus.CONNECTING
                    )
                )
            }

            val (outStream, localFile) = FileSaveHelper.getDownloadOutputStream(context, item.name, item.mimeType)
            if (outStream == null) {
                withContext(Dispatchers.Main) {
                    listener.onDownloadError("Could not create local storage destination for ${item.name}")
                }
                return@withContext false
            }

            val connection = (downloadUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
            }

            val responseCode = connection.responseCode
            if (responseCode != 200 && responseCode != 206) {
                outStream.close()
                withContext(Dispatchers.Main) {
                    listener.onDownloadError("Download failed with server code $responseCode")
                }
                return@withContext false
            }

            val totalSize = if (item.size > 0) item.size else connection.contentLengthLong
            val inStream = BufferedInputStream(connection.inputStream, BUFFER_SIZE)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalDownloaded = 0L
            val startTime = System.currentTimeMillis()

            outStream.use { out ->
                inStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled) {
                            break
                        }

                        out.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val elapsed = maxOf(1L, System.currentTimeMillis() - startTime)
                        val speedBps = (totalDownloaded * 1000.0) / elapsed
                        val percent = if (totalSize > 0) ((totalDownloaded * 100) / totalSize).toInt() else 0
                        val eta = if (speedBps > 0) ((totalSize - totalDownloaded) / speedBps).toLong() else 0L

                        withContext(Dispatchers.Main) {
                            listener.onProgress(
                                TransferProgress(
                                    isSending = false,
                                    fileId = item.id,
                                    fileName = item.name,
                                    totalBytes = totalSize,
                                    transferredBytes = totalDownloaded,
                                    speedBps = speedBps,
                                    etaSeconds = eta,
                                    percent = percent,
                                    status = TransferStatus.TRANSFERRING,
                                    localFile = localFile
                                )
                            )
                        }
                    }
                }
            }

            if (!isCancelled) {
                FileSaveHelper.finishPendingFile(context, localFile, item.mimeType)

                withContext(Dispatchers.Main) {
                    listener.onProgress(
                        TransferProgress(
                            isSending = false,
                            fileId = item.id,
                            fileName = item.name,
                            totalBytes = totalSize,
                            transferredBytes = totalDownloaded,
                            speedBps = 0.0,
                            etaSeconds = 0L,
                            percent = 100,
                            status = TransferStatus.COMPLETED,
                            localFile = localFile
                        )
                    )
                    if (localFile != null) {
                        listener.onFileDownloaded(localFile, item.name, item.mimeType)
                    }
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("FileDownloader", "Download error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                listener.onDownloadError("Download error on ${item.name}: ${e.localizedMessage}")
            }
            return@withContext false
        }
    }

    fun cancel() {
        isCancelled = true
        scope.cancel()
    }
}
