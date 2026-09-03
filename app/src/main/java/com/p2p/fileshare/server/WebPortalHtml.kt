package com.p2p.fileshare.server

import com.p2p.fileshare.model.SharedItem

object WebPortalHtml {

    fun generatePortalHtml(deviceName: String, files: List<SharedItem>): String {
        val totalSize = files.sumOf { it.size }
        val formattedTotal = SharedItem.formatFileSize(totalSize)

        val fileListHtml = StringBuilder()
        for (file in files) {
            val isVideo = file.mimeType.startsWith("video/") || file.name.endsWith(".mp4") || file.name.endsWith(".mkv")
            val icon = when {
                isVideo -> "🎬"
                file.mimeType.startsWith("audio/") -> "🎵"
                file.mimeType.startsWith("image/") -> "📸"
                file.mimeType.contains("pdf") -> "📄"
                file.name.endsWith(".apk") -> "📦"
                else -> "📁"
            }

            fileListHtml.append("""
                <div class="file-card">
                    <div class="file-icon">$icon</div>
                    <div class="file-details">
                        <div class="file-name" title="${file.name}">${file.name}</div>
                        <div class="file-meta">${file.formattedSize} • ${file.mimeType}</div>
                        ${if (isVideo) """<video controls preload="metadata" style="max-width:100%; border-radius:8px; margin-top:8px; display:none;" id="vid-${file.id}"><source src="/download?id=${file.id}" type="${file.mimeType}">Your browser does not support video.</video><button class="preview-btn" onclick="toggleVideo('vid-${file.id}', this)">▶ Stream / Preview Movie</button>""" else ""}
                    </div>
                    <a class="btn-download" href="/download?id=${file.id}" download="${file.name}">
                        ⬇ Download
                    </a>
                </div>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>⚡ FlashShare P2P - Direct Download</title>
                <style>
                    :root {
                        --bg-color: #0d1117;
                        --card-bg: #161b22;
                        --card-border: #30363d;
                        --accent-blue: #58a6ff;
                        --accent-cyan: #39d353;
                        --text-main: #f0f6fc;
                        --text-muted: #8b949e;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                    body {
                        background: var(--bg-color);
                        color: var(--text-main);
                        padding: 24px 16px;
                        display: flex;
                        justify-content: center;
                    }
                    .container {
                        max-width: 680px;
                        width: 100%;
                    }
                    .header {
                        text-align: center;
                        padding: 24px 0 32px;
                    }
                    .badge {
                        display: inline-block;
                        background: rgba(57, 211, 83, 0.15);
                        color: var(--accent-cyan);
                        padding: 4px 12px;
                        border-radius: 20px;
                        font-size: 13px;
                        font-weight: 600;
                        margin-bottom: 12px;
                        border: 1px solid rgba(57, 211, 83, 0.3);
                    }
                    h1 {
                        font-size: 28px;
                        font-weight: 800;
                        letter-spacing: -0.5px;
                        margin-bottom: 8px;
                    }
                    .subtitle {
                        color: var(--text-muted);
                        font-size: 14px;
                    }
                    .stats-card {
                        background: var(--card-bg);
                        border: 1px solid var(--card-border);
                        border-radius: 16px;
                        padding: 18px 20px;
                        display: flex;
                        justify-content: space-around;
                        text-align: center;
                        margin-bottom: 24px;
                    }
                    .stat-value {
                        font-size: 20px;
                        font-weight: 700;
                        color: var(--accent-blue);
                    }
                    .stat-label {
                        font-size: 12px;
                        color: var(--text-muted);
                        margin-top: 4px;
                    }
                    .file-card {
                        background: var(--card-bg);
                        border: 1px solid var(--card-border);
                        border-radius: 14px;
                        padding: 16px;
                        display: flex;
                        align-items: center;
                        gap: 16px;
                        margin-bottom: 14px;
                        transition: border-color 0.2s;
                    }
                    .file-card:hover {
                        border-color: var(--accent-blue);
                    }
                    .file-icon {
                        font-size: 32px;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: rgba(88, 166, 255, 0.1);
                        border-radius: 10px;
                    }
                    .file-details {
                        flex: 1;
                        min-width: 0;
                    }
                    .file-name {
                        font-weight: 600;
                        font-size: 15px;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                    }
                    .file-meta {
                        color: var(--text-muted);
                        font-size: 13px;
                        margin-top: 3px;
                    }
                    .btn-download {
                        background: #238636;
                        color: white;
                        text-decoration: none;
                        padding: 10px 18px;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 14px;
                        white-space: nowrap;
                        transition: background 0.2s;
                    }
                    .btn-download:hover {
                        background: #2ea043;
                    }
                    .preview-btn {
                        background: transparent;
                        border: 1px solid var(--accent-blue);
                        color: var(--accent-blue);
                        padding: 4px 10px;
                        border-radius: 6px;
                        font-size: 12px;
                        cursor: pointer;
                        margin-top: 6px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 40px;
                        color: var(--text-muted);
                        font-size: 13px;
                    }
                </style>
                <script>
                    function toggleVideo(id, btn) {
                        var v = document.getElementById(id);
                        if (v.style.display === 'none') {
                            v.style.display = 'block';
                            btn.innerText = '⏹ Hide Player';
                            v.play();
                        } else {
                            v.pause();
                            v.style.display = 'none';
                            btn.innerText = '▶ Stream / Preview Movie';
                        }
                    }
                </script>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="badge">⚡ Direct Local P2P Stream</div>
                        <h1>FlashShare Files</h1>
                        <p class="subtitle">Shared from <strong>$deviceName</strong> over high-speed Wi-Fi</p>
                    </div>

                    <div class="stats-card">
                        <div>
                            <div class="stat-value">${files.size}</div>
                            <div class="stat-label">Files Shared</div>
                        </div>
                        <div>
                            <div class="stat-value">$formattedTotal</div>
                            <div class="stat-label">Total Size</div>
                        </div>
                        <div>
                            <div class="stat-value">50-100+ MB/s</div>
                            <div class="stat-label">Wi-Fi Direct Speed</div>
                        </div>
                    </div>

                    <div class="file-list">
                        $fileListHtml
                    </div>

                    <div class="footer">
                        ⚡ FlashShare P2P • High Performance Local File Transfer
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
