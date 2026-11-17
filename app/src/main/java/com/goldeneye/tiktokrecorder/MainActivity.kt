package com.goldeneye.tiktokrecorder

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.goldeneye.tiktokrecorder.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayer()

        binding.btnCheckAndRecord.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a valid TikTok username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkAndStartLive(username)
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().also {
            binding.playerView.player = it
        }
    }

    private fun checkAndStartLive(username: String) {
        binding.tvStatus.text = "Checking if @$username is online..."
        activityScope.launch {
            val streamUrl = TikTokApiHelper.getLiveStreamUrl(username)
            if (streamUrl != null) {
                binding.tvStatus.text = "User is ONLINE! Starting recording & preview..."
                
                // Play live preview
                val mediaItem = MediaItem.fromUri(streamUrl)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()

                // Start timer
                binding.tvTimer.base = SystemClock.elapsedRealtime()
                binding.tvTimer.start()

                // Start Foreground Service for file saving
                val serviceIntent = Intent(this@MainActivity, LiveRecordService::class.java).apply {
                    putExtra("STREAM_URL", streamUrl)
                    putExtra("USERNAME", username)
                }
                startService(serviceIntent)
            } else {
                binding.tvStatus.text = "@$username is currently OFFLINE."
                Toast.makeText(this@MainActivity, "User is not live right now.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        activityScope.cancel()
    }
}
