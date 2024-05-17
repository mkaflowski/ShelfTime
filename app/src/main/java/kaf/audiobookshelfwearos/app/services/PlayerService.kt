package kaf.audiobookshelfwearos.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kaf.audiobookshelfwearos.R
import timber.log.Timber

class PlayerService : Service() {

    private val binder = LocalBinder()
    private lateinit var exoPlayer: ExoPlayer
    private val url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" // Replace with your audio URL
    private val CHANNEL_ID = "PlayerServiceChannel"

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> Timber.tag("PlayerService")
                        .d("ExoPlayer is buffering")
                    Player.STATE_READY -> Timber.tag("PlayerService").d("ExoPlayer is ready")
                    Player.STATE_ENDED -> {
                        Timber.tag("PlayerService").d("ExoPlayer has ended")
                        sendBroadcast(Intent("$packageName.ACTION_TRACK_ENDED"))
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Timber.tag("PlayerService").d("ExoPlayer is playing")
                    sendBroadcast(Intent("$packageName.ACTION_PLAYING"))
                } else {
                    Timber.tag("PlayerService").d("ExoPlayer is paused")
                    sendBroadcast(Intent("$packageName.ACTION_PAUSE"))
                }
            }
        })

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY_PAUSE" -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }
            "ACTION_REWIND" -> {
                exoPlayer.seekTo(exoPlayer.currentPosition - 10000) // Rewind 10 seconds
            }
            "ACTION_FAST_FORWARD" -> {
                exoPlayer.seekTo(exoPlayer.currentPosition + 10000) // Fast forward 10 seconds
            }
        }

        startForeground(1, createNotification())
        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Player Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val playPauseIntent = PendingIntent.getService(
            this, 0, Intent(this, PlayerService::class.java).apply {
                action = "ACTION_PLAY_PAUSE"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewindIntent = PendingIntent.getService(
            this, 0, Intent(this, PlayerService::class.java).apply {
                action = "ACTION_REWIND"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fastForwardIntent = PendingIntent.getService(
            this, 0, Intent(this, PlayerService::class.java).apply {
                action = "ACTION_FAST_FORWARD"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Player Service")
            .setContentText("Playing audio")
            .setSmallIcon(R.drawable.placeholder)
            .addAction(R.drawable.placeholder, "Rewind", rewindIntent)
            .addAction(R.drawable.placeholder, "Play/Pause", playPauseIntent)
            .addAction(R.drawable.placeholder, "Fast Forward", fastForwardIntent)
            .build()
    }

    fun getCurrentPosition(): Long {
        return exoPlayer.currentPosition
    }

    override fun onDestroy() {
        exoPlayer.release()
        super.onDestroy()
    }

}