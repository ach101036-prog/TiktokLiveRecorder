package com.goldeneye.tiktokrecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
IBinder? = null
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class LiveRecordService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRecording = false
    private var streamUrl: String? = null
    private var outputFile: File? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        streamUrl = intent?.getStringExtra("STREAM_URL")
        val username = intent?.getStringExtra("USERNAME") ?: "tiktok_user"

        if (streamUrl != null) {
            startForeground(1, createNotification("Recording $username live stream..."))
            startRecording(username)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startRecording(username: String) {
        isRecording = true
        serviceScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val appDir = File(dir, "TikTokRecords").apply { if (!exists()) mkdirs() }
                outputFile = File(appDir, "${username}_$timestamp.mp4")

                val connection = URL(streamUrl).openConnection()
                connection.getInputStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (isRecording && input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                showCompletionNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }
    }

    private fun createNotification(message: String): android.app.Notification {
        val channelId = "tiktok_record_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "TikTok Recorder", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TikTok Live Recorder")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    private fun showCompletionNotification() {
        val channelId = "tiktok_record_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Recording Saved! 🎉")
            .setContentText("Saved to Movies/TikTokRecords/${outputFile?.name}")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        manager.notify(2, notification)
    }

    override fun onDestroy() {
        isRecording = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
