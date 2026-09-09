# ⚡ FlashShare — High-Speed Offline P2P File & Movie Sharing

![Android](https://img.shields.io/badge/Android-35-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Live Showcase Demo](https://img.shields.io/badge/🌐_LIVE_WEB_DEMO-SHOWCASE-2563EB?style=for-the-badge)

👉 **[Launch Interactive Live Web Showcase Demo](https://devesh-sketch.github.io/devesh-sketch-Peer_to_Peer_file_sharing_system/)**

**FlashShare** is a high-speed, offline peer-to-peer (P2P) file and movie sharing Android app built with **Jetpack Compose Material 3**, **CameraX + ML Kit QR Scanner**, and an **Embedded HTTP Web Streaming Engine**.

It allows users to transfer 4K movies, heavy videos, apps (APKs), photos, and documents between devices at local Wi-Fi speeds (**50–100+ MB/s**) without requiring internet or cloud servers.

---

## 🎨 User Interface Showcase

| **Home Screen (Send & Receive)** | **Category Picker & File Selection** | **Speed & Transfer Progress Gauge** |
| :---: | :---: | :---: |
| Clean Light UI with Blue (Send) & Green (Receive) | Quick filters for Movies, Audio, Photos, APKs, Docs | Live MB/s speed gauge, ETA counter, and progress bar |

---

## ✨ Features

* ⚡ **Blazing Fast Offline Sharing**: Share files locally at maximum Wi-Fi/Hotspot bandwidth (50–100+ MB/s).
* 📤 **Send Files (Blue Action)**: Easily pick movies, APKs, documents, and folders to share via dynamic QR code.
* 📥 **Receive Files (Green Action)**: Fast CameraX QR code scanner with instant connection and auto-download.
* 🌐 **Cross-Platform Browser Access**: iPhone, iPad, Mac, Windows, Linux, and Smart TV users can receive/download files directly in their browser without installing any app.
* 📊 **Live Speed & ETA Gauge**: Real-time bandwidth gauge, MB/s counter, and remaining time calculation.
* 🎬 **Integrated File Opener**: Play movies/videos, view photos, or install APKs directly after transfer completion.
* 📜 **Transfer History**: Complete history log of sent and received items.

---

## 🛠️ How It Works

### 1️⃣ Sender
- Select files to share and tap **Send Files**.
- FlashShare launches an embedded lightweight HTTP server and displays a dynamic QR code (`http://<IP>:<PORT>`).

### 2️⃣ Receiver (Android)
- Tap **Receive Files** and scan the sender's QR code using the built-in CameraX scanner.
- Transfers start instantly at full network speed.

### 3️⃣ Cross-Platform (iPhone / PC / Mac)
- Connect to the same Wi-Fi or Hotspot network.
- Scan the QR code or visit the displayed URL in any web browser to stream or download files.

---

## 🏗️ Architecture & Tech Stack

```
com.p2p.fileshare
├── client/           # File downloader & HTTP client (OkHttp)
├── model/            # SharedItem, TransferProgress data models
├── qr/               # Dynamic QR generator & payload encoding
├── server/           # Embedded P2P HTTP Server & Web Portal HTML
├── service/          # Transfer Foreground Service
├── ui/
│   ├── components/   # SpeedGauge, CategoryPicker
│   ├── screens/      # HomeScreen, SendScreen, QrDisplayScreen, QrScannerScreen, HistoryScreen, TransferProgressScreen
│   └── theme/        # Color, Theme, Typography
└── util/             # FileOpener & Save Helpers
```

- **UI Framework**: Jetpack Compose with Material 3 (Light Theme)
- **Scanning**: CameraX + Google ML Kit Barcode Scanning
- **Networking**: Local HTTP Web Server & OkHttp
- **Asynchronous**: Kotlin Coroutines & StateFlow

---

## 📦 Building the App

To build the APK locally:

```bash
# Clone the repository
git clone https://github.com/devesh-sketch/devesh-sketch-Peer_to_Peer_file_sharing_system.git
cd devesh-sketch-Peer_to_Peer_file_sharing_system

# Build Debug APK
./gradlew app:assembleDebug
```

The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
