package com.p2p.fileshare.qr

import org.json.JSONObject

data class QrPayload(
    val url: String,
    val deviceName: String,
    val fileCount: Int,
    val totalSize: Long,
    val token: String = ""
) {
    fun serialize(): String {
        return try {
            val json = JSONObject().apply {
                put("url", url)
                put("name", deviceName)
                put("count", fileCount)
                put("size", totalSize)
                put("token", token)
            }
            "FLASHSHARE:$json"
        } catch (e: Exception) {
            url
        }
    }

    companion object {
        fun parse(raw: String): QrPayload? {
            return try {
                if (raw.startsWith("FLASHSHARE:")) {
                    val jsonStr = raw.substring("FLASHSHARE:".length)
                    val json = JSONObject(jsonStr)
                    QrPayload(
                        url = json.getString("url"),
                        deviceName = json.optString("name", "P2P Peer"),
                        fileCount = json.optInt("count", 1),
                        totalSize = json.optLong("size", 0L),
                        token = json.optString("token", "")
                    )
                } else if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    val url = if (raw.contains("/web")) raw.substringBefore("/web") else raw
                    QrPayload(
                        url = url,
                        deviceName = "Web Share",
                        fileCount = 1,
                        totalSize = 0L
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
