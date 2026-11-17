package com.goldeneye.tiktokrecorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object TikTokApiHelper {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    suspend fun getLiveStreamUrl(username: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.tiktok.com/@$username/live"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null

                // Look for live room status and stream URL payload in web hydration data
                if (html.contains("\"status\":4") || html.contains("The live broadcast has ended")) {
                    return@withContext null // User is offline
                }

                // Extract stream URL regex match for flv/hls feed
                val regex = "\"hls_pull_url\":\"([^\"]+)\"".toRegex()
                val matchResult = regex.find(html)
                return@withContext matchResult?.groupValues?.get(1)?.replace("\\u002F", "/")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
