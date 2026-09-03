package com.p2p.fileshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.p2p.fileshare.client.FileDownloader
import com.p2p.fileshare.model.*
import com.p2p.fileshare.qr.QrGenerator
import com.p2p.fileshare.qr.QrPayload
import com.p2p.fileshare.server.NetworkUtils
import com.p2p.fileshare.server.P2PHttpServer
import com.p2p.fileshare.service.TransferForegroundService
import com.p2p.fileshare.ui.screens.*
import com.p2p.fileshare.ui.theme.P2PFileShareTheme
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen {
    HOME,
    SEND,
    QR_DISPLAY,
    QR_SCANNER,
    TRANSFER_PROGRESS,
    HISTORY
}

class MainActivity : ComponentActivity(), P2PHttpServer.ServerEventListener, FileDownloader.DownloadEventListener {

    private var currentScreen by mutableStateOf(AppScreen.HOME)
    private var localIp by mutableStateOf("127.0.0.1")
    private var wifiSsid by mutableStateOf("Wi-Fi")

    // Selected files for sending
    private val selectedFiles = mutableStateListOf<SharedItem>()
    private var shareQrBitmap by mutableStateOf<Bitmap?>(null)
    private var activeShareUrl by mutableStateOf("")
    private var connectedPeersCount by mutableStateOf(0)

    // Receiver state
    private var peerBaseUrl by mutableStateOf("")

    // Active transfer progress
    private var activeProgress by mutableStateOf<TransferProgress?>(null)
    private val historyList = mutableStateListOf<HistoryItem>()

    // Core engines
    private lateinit var p2pServer: P2PHttpServer
    private lateinit var fileDownloader: FileDownloader

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            val item = resolveSharedItem(uri)
            if (item != null && selectedFiles.none { it.uri == item.uri }) {
                selectedFiles.add(item)
            }
        }
    }

    // Comprehensive permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted && currentScreen == AppScreen.HOME) {
            currentScreen = AppScreen.QR_SCANNER
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        p2pServer = P2PHttpServer(this, port = 8080, listener = this)
        fileDownloader = FileDownloader(this, listener = this)

        updateNetworkInfo()
        requestAllAppPermissions()

        setContent {
            P2PFileShareTheme {
                when (currentScreen) {
                    AppScreen.HOME -> {
                        HomeScreen(
                            localIp = localIp,
                            wifiSsid = wifiSsid,
                            historyCount = historyList.size,
                            onSendClick = {
                                updateNetworkInfo()
                                currentScreen = AppScreen.SEND
                            },
                            onReceiveClick = {
                                updateNetworkInfo()
                                checkCameraPermissionAndOpen()
                            },
                            onHistoryClick = {
                                currentScreen = AppScreen.HISTORY
                            }
                        )
                    }
                    AppScreen.SEND -> {
                        SendScreen(
                            selectedFiles = selectedFiles,
                            onPickFiles = {
                                filePickerLauncher.launch("*/*")
                            },
                            onRemoveFile = { item ->
                                selectedFiles.remove(item)
                            },
                            onStartShare = {
                                startServerAndGenerateQr()
                            },
                            onBack = {
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }
                    AppScreen.QR_DISPLAY -> {
                        QrDisplayScreen(
                            qrBitmap = shareQrBitmap,
                            shareUrl = activeShareUrl,
                            files = selectedFiles,
                            connectedPeersCount = connectedPeersCount,
                            activeUploadProgress = activeProgress,
                            onStopShare = {
                                p2pServer.stop()
                                stopForegroundService()
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }
                    AppScreen.QR_SCANNER -> {
                        QrScannerScreen(
                            onQrScanned = { rawQr ->
                                handleQrCodeScanned(rawQr)
                            },
                            onManualIpEntered = { manualUrl ->
                                connectToPeer(manualUrl)
                            },
                            onBack = {
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }
                    AppScreen.TRANSFER_PROGRESS -> {
                        TransferProgressScreen(
                            progress = activeProgress,
                            onCancel = {
                                fileDownloader.cancel()
                                stopForegroundService()
                                currentScreen = AppScreen.HOME
                            },
                            onDone = {
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }
                    AppScreen.HISTORY -> {
                        HistoryScreen(
                            historyItems = historyList,
                            onClearHistory = {
                                historyList.clear()
                            },
                            onBack = {
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }
                }
            }
        }
    }

    private fun updateNetworkInfo() {
        localIp = NetworkUtils.getLocalIpAddress()
        wifiSsid = NetworkUtils.getWifiSsid(this)
    }

    private fun requestAllAppPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            currentScreen = AppScreen.QR_SCANNER
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun startServerAndGenerateQr() {
        if (selectedFiles.isEmpty()) return

        updateNetworkInfo()
        val port = 8080
        val url = "http://$localIp:$port"
        activeShareUrl = url
        connectedPeersCount = 0

        p2pServer.start(selectedFiles)

        lifecycleScope.launch {
            val totalSize = selectedFiles.sumOf { it.size }
            val payload = QrPayload(
                url = url,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                fileCount = selectedFiles.size,
                totalSize = totalSize
            )
            shareQrBitmap = QrGenerator.generateQrBitmap(payload.serialize(), size = 700)
            currentScreen = AppScreen.QR_DISPLAY
        }
    }

    private fun handleQrCodeScanned(raw: String) {
        val parsed = QrPayload.parse(raw)
        val targetUrl = parsed?.url ?: if (raw.startsWith("http")) raw else "http://$raw:8080"
        connectToPeer(targetUrl)
    }

    private fun connectToPeer(targetUrl: String) {
        val cleanUrl = targetUrl.trimEnd('/')
        peerBaseUrl = cleanUrl
        currentScreen = AppScreen.TRANSFER_PROGRESS
        activeProgress = TransferProgress(
            isSending = false,
            fileId = "",
            fileName = "Connecting to peer...",
            totalBytes = 0L,
            transferredBytes = 0L,
            speedBps = 0.0,
            etaSeconds = 0L,
            percent = 0,
            status = TransferStatus.CONNECTING
        )
        fileDownloader.fetchServerFiles(cleanUrl)
    }

    private fun resolveSharedItem(uri: Uri): SharedItem? {
        return try {
            var name = "file_${System.currentTimeMillis()}"
            var size = 0L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) name = cursor.getString(nameIdx)
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
            val mime = contentResolver.getType(uri) ?: "*/*"
            val category = SharedItem.detectCategory(mime, name)
            SharedItem(
                id = "${System.currentTimeMillis()}_${(1000..9999).random()}",
                uri = uri,
                name = name,
                size = size,
                mimeType = mime,
                category = category
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // P2PHttpServer Callbacks
    override fun onServerStarted(ip: String, port: Int) {
        localIp = ip
        startForegroundService("Server Ready", 0, "Listening on port $port", isSending = true)
    }

    override fun onPeerConnected(peerAddress: String) {
        connectedPeersCount++
        Toast.makeText(this, "Peer connected: $peerAddress", Toast.LENGTH_SHORT).show()
    }

    override fun onProgressUpdate(progress: TransferProgress) {
        activeProgress = progress
        updateForegroundService(progress.fileName, progress.percent, progress.formattedSpeed, isSending = true)
    }

    override fun onTransferComplete(fileName: String, peerAddress: String) {
        Toast.makeText(this, "Finished sending $fileName", Toast.LENGTH_SHORT).show()
        val file = selectedFiles.find { it.name == fileName }
        if (file != null) {
            historyList.add(0, HistoryItem(id = file.id, name = file.name, size = file.size, filePath = file.uri.toString(), isReceived = false, mimeType = file.mimeType))
        }
    }

    override fun onServerError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    override fun onServerStopped() {
        stopForegroundService()
    }

    // FileDownloader Callbacks
    override fun onDiscoveredFiles(files: List<SharedItem>, deviceName: String, hostBaseUrl: String) {
        if (files.isNotEmpty()) {
            peerBaseUrl = hostBaseUrl
            Toast.makeText(this, "Connected to $deviceName (${files.size} file(s))", Toast.LENGTH_SHORT).show()
            fileDownloader.downloadFiles(hostBaseUrl, files)
        } else {
            Toast.makeText(this, "No files available on peer server", Toast.LENGTH_SHORT).show()
            currentScreen = AppScreen.HOME
        }
    }

    override fun onProgress(progress: TransferProgress) {
        activeProgress = progress
        updateForegroundService(progress.fileName, progress.percent, progress.formattedSpeed, isSending = false)
    }

    override fun onFileDownloaded(file: File, fileName: String, mimeType: String) {
        Toast.makeText(this, "Saved $fileName to Movies / Downloads!", Toast.LENGTH_LONG).show()
        historyList.add(0, HistoryItem(id = "${System.currentTimeMillis()}", name = fileName, size = file.length(), filePath = file.absolutePath, isReceived = true, mimeType = mimeType))
    }

    override fun onBatchCompleted(totalFiles: Int) {
        Toast.makeText(this, "All $totalFiles file(s) downloaded successfully!", Toast.LENGTH_LONG).show()
    }

    override fun onDownloadError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        activeProgress = activeProgress?.copy(status = TransferStatus.ERROR, errorMessage = error)
    }

    // Foreground service helpers
    private fun startForegroundService(fileName: String, progress: Int, speed: String, isSending: Boolean) {
        val intent = Intent(this, TransferForegroundService::class.java).apply {
            action = TransferForegroundService.ACTION_START
            putExtra(TransferForegroundService.EXTRA_FILE_NAME, fileName)
            putExtra(TransferForegroundService.EXTRA_PROGRESS, progress)
            putExtra(TransferForegroundService.EXTRA_SPEED, speed)
            putExtra(TransferForegroundService.EXTRA_IS_SENDING, isSending)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun updateForegroundService(fileName: String, progress: Int, speed: String, isSending: Boolean) {
        val intent = Intent(this, TransferForegroundService::class.java).apply {
            action = TransferForegroundService.ACTION_UPDATE
            putExtra(TransferForegroundService.EXTRA_FILE_NAME, fileName)
            putExtra(TransferForegroundService.EXTRA_PROGRESS, progress)
            putExtra(TransferForegroundService.EXTRA_SPEED, speed)
            putExtra(TransferForegroundService.EXTRA_IS_SENDING, isSending)
        }
        startService(intent)
    }

    private fun stopForegroundService() {
        val intent = Intent(this, TransferForegroundService::class.java).apply {
            action = TransferForegroundService.ACTION_STOP
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        p2pServer.stop()
        fileDownloader.cancel()
        stopForegroundService()
    }
}
