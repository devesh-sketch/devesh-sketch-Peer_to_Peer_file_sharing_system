package com.p2p.fileshare.server

import android.content.Context
import android.os.Build
import android.util.Log
import com.p2p.fileshare.model.SharedItem
import com.p2p.fileshare.model.TransferProgress
import com.p2p.fileshare.model.TransferStatus
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

class P2PHttpServer(
    private val context: Context,
    private val port: Int = 8080,
    private val listener: ServerEventListener
) {
    interface ServerEventListener {
        fun onServerStarted(ip: String, port: Int)
        fun onPeerConnected(peerAddress: String)
        fun onProgressUpdate(progress: TransferProgress)
        fun onTransferComplete(fileName: String, peerAddress: String)
        fun onServerError(error: String)
        fun onServerStopped()
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    @Volatile private var isRunning = false
    private val sharedFiles = mutableListOf<SharedItem>()
    private val activePeers = ConcurrentHashMap<String, Long>()
    private val BUFFER_SIZE = 1024 * 1024 // 1 MB high-speed streaming chunk

    fun start(files: List<SharedItem>) {
        if (isRunning) return
        sharedFiles.clear()
        sharedFiles.addAll(files)

        scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                isRunning = true
                val localIp = NetworkUtils.getLocalIpAddress()
                
                withContext(Dispatchers.Main) {
                    listener.onServerStarted(localIp, port)
                }

                while (isRunning && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e("P2PHttpServer", "Accept error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("P2PHttpServer", "Server error: ${e.message}")
                withContext(Dispatchers.Main) {
                    listener.onServerError("Failed to start server on port $port: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        scope.cancel()
        listener.onServerStopped()
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientIp = socket.inetAddress.hostAddress ?: "Unknown"
        activePeers[clientIp] = System.currentTimeMillis()

        withContext(Dispatchers.Main) {
            listener.onPeerConnected(clientIp)
        }

        try {
            socket.tcpNoDelay = true
            socket.sendBufferSize = BUFFER_SIZE
            val inputStream = BufferedInputStream(socket.getInputStream())
            val outputStream = BufferedOutputStream(socket.getOutputStream())

            val reader = BufferedReader(InputStreamReader(inputStream))
            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            while (reader.readLine().also { headerLine = it } != null) {
                if (headerLine.isNullOrBlank()) break
                val colonIdx = headerLine!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = headerLine!!.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            when {
                path == "/" || path == "/web" || path == "/index.html" -> {
                    serveWebPortal(outputStream)
                }
                path == "/api/info" -> {
                    serveApiInfo(outputStream)
                }
                path.startsWith("/download") -> {
                    val fileId = extractQueryParam(path, "id")
                    serveFileDownload(fileId, headers, outputStream, clientIp)
                }
                else -> {
                    sendNotFound(outputStream)
                }
            }
        } catch (e: Exception) {
            Log.e("P2PHttpServer", "Client handling error: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun serveWebPortal(out: OutputStream) {
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val html = WebPortalHtml.generatePortalHtml(deviceName, sharedFiles)
        val htmlBytes = html.toByteArray(Charsets.UTF_8)

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${htmlBytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(htmlBytes)
        out.flush()
    }

    private fun serveApiInfo(out: OutputStream) {
        val jsonArray = JSONArray()
        for (file in sharedFiles) {
            val obj = JSONObject().apply {
                put("id", file.id)
                put("name", file.name)
                put("size", file.size)
                put("mime", file.mimeType)
                put("category", file.category.name)
            }
            jsonArray.put(obj)
        }

        val response = JSONObject().apply {
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("files", jsonArray)
        }.toString()

        val bytes = response.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private suspend fun serveFileDownload(
        fileId: String?,
        headers: Map<String, String>,
        out: OutputStream,
        clientIp: String
    ) {
        val file = sharedFiles.find { it.id == fileId } ?: sharedFiles.firstOrNull()
        if (file == null) {
            sendNotFound(out)
            return
        }

        val totalLength = file.size
        var startOffset = 0L
        var endOffset = totalLength - 1
        var isPartial = false

        // Check for HTTP 206 Range header (Crucial for large movie resumes and video seeking)
        val rangeHeader = headers["range"]
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val rangeVal = rangeHeader.substring(6).trim()
            val dashIdx = rangeVal.indexOf("-")
            if (dashIdx != -1) {
                val startStr = rangeVal.substring(0, dashIdx).trim()
                val endStr = rangeVal.substring(dashIdx + 1).trim()
                if (startStr.isNotEmpty()) {
                    startOffset = startStr.toLongOrNull() ?: 0L
                }
                if (endStr.isNotEmpty()) {
                    endOffset = endStr.toLongOrNull() ?: (totalLength - 1)
                }
                isPartial = true
            }
        }

        val contentLength = endOffset - startOffset + 1
        val statusLine = if (isPartial) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
        
        val headerBuilder = StringBuilder().apply {
            append("$statusLine\r\n")
            append("Content-Type: ${file.mimeType}\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Content-Disposition: attachment; filename=\"${file.name.replace("\"", "")}\"\r\n")
            append("Content-Length: $contentLength\r\n")
            if (isPartial) {
                append("Content-Range: bytes $startOffset-$endOffset/$totalLength\r\n")
            }
            append("Connection: close\r\n\r\n")
        }

        out.write(headerBuilder.toString().toByteArray(Charsets.UTF_8))
        out.flush()

        // Stream binary data directly from Android ContentResolver (Zero RAM overhead)
        context.contentResolver.openInputStream(file.uri)?.use { inStream ->
            // Skip to starting byte offset for partial requests
            if (startOffset > 0) {
                var skipped = 0L
                while (skipped < startOffset) {
                    val s = inStream.skip(startOffset - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRemaining = contentLength
            var totalSentForThisSession = 0L
            val startTime = System.currentTimeMillis()

            while (bytesRemaining > 0) {
                val toRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                val read = inStream.read(buffer, 0, toRead)
                if (read == -1) break

                out.write(buffer, 0, read)
                out.flush()

                bytesRemaining -= read
                totalSentForThisSession += read
                val currentTotalSent = startOffset + totalSentForThisSession

                val elapsed = maxOf(1L, System.currentTimeMillis() - startTime)
                val speedBps = (totalSentForThisSession * 1000.0) / elapsed
                val percent = if (totalLength > 0) ((currentTotalSent * 100) / totalLength).toInt() else 0
                val eta = if (speedBps > 0) ((totalLength - currentTotalSent) / speedBps).toLong() else 0L

                withContext(Dispatchers.Main) {
                    listener.onProgressUpdate(
                        TransferProgress(
                            isSending = true,
                            fileId = file.id,
                            fileName = file.name,
                            totalBytes = totalLength,
                            transferredBytes = currentTotalSent,
                            speedBps = speedBps,
                            etaSeconds = eta,
                            percent = percent,
                            status = if (currentTotalSent >= totalLength) TransferStatus.COMPLETED else TransferStatus.TRANSFERRING
                        )
                    )
                }
            }

            if (startOffset + totalSentForThisSession >= totalLength) {
                withContext(Dispatchers.Main) {
                    listener.onTransferComplete(file.name, clientIp)
                }
            }
        }
    }

    private fun sendNotFound(out: OutputStream) {
        val msg = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        out.write(msg.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun extractQueryParam(path: String, key: String): String? {
        val qIdx = path.indexOf("?")
        if (qIdx == -1) return null
        val query = path.substring(qIdx + 1)
        for (pair in query.split("&")) {
            val kv = pair.split("=")
            if (kv.size == 2 && kv[0] == key) {
                return URLDecoder.decode(kv[1], "UTF-8")
            }
        }
        return null
    }
}
