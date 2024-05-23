package kaf.audiobookshelfwearos.app.services

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class PlayerService : MediaSessionService() {

    private val binder = LocalBinder()
    private lateinit var exoPlayer: ExoPlayer
    private val CHANNEL_ID = "PlayerServiceChannel"
    private var mediaSession: MediaSession? = null

    private var playbackStartTime: Long = 0
    private var totalPlaybackTime: Long = 0

    private lateinit var audiobook: LibraryItem

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createPlayer()
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

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

                    Player.STATE_IDLE -> {
                        Timber.tag("PlayerService").d("ExoPlayer in idle")
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Timber.tag("PlayerService").d("ExoPlayer is playing")
                    playbackStartTime = System.currentTimeMillis()
                    sendBroadcast(Intent("$packageName.ACTION_PLAYING"))
                } else {
                    Timber.tag("PlayerService").d("ExoPlayer is paused")
                    sendBroadcast(Intent("$packageName.ACTION_PAUSE"))
                    val currentTime = System.currentTimeMillis()
                    totalPlaybackTime += currentTime - playbackStartTime
                    Timber.tag("PlayerService")
                        .d("%s seconds", "Total playback time: ${totalPlaybackTime / 1000}")
                }
            }
        })
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    @OptIn(UnstableApi::class)
    private fun setAudiobook(audiobook: LibraryItem) {
        this.audiobook = audiobook
        exoPlayer.clearMediaItems()
        val userDataManager = UserDataManager(this)
        val url =
            userDataManager.getCompleteAddress() + this.audiobook.media.tracks[0].contentUrl

        val headers = hashMapOf<String, String>()
        headers["Authorization"] = "Bearer " + userDataManager.token;
        // Create a factory for HTTP data sources with the OkHttpClient instance
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        // Build a media source using the data source factory
        val mediaItem =
            MediaItem.Builder()
                .setMediaId("media-1")
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtist(audiobook.title)
                        .setTitle(audiobook.title)
                        .build()
                )
                .build()

        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        exoPlayer.run {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    fun getTotalPlaybackTime(): Long {
        return totalPlaybackTime
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
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

        intent?.getStringExtra("id")?.let {
            GlobalScope.launch {
                val db = (applicationContext as MainApp).database
                db.libraryItemDao().getLibraryItemById(it)?.let {
                    withContext(Dispatchers.Main) {
                        setAudiobook(it)
                    }
                }
            }
        }

//        startForeground(1, createNotification())
        return START_STICKY
    }

//    private fun createNotification(): Notification {
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            "Player Service Channel",
//            NotificationManager.IMPORTANCE_DEFAULT
//        )
//        val manager = getSystemService(NotificationManager::class.java)
//        manager.createNotificationChannel(channel)
//
//        val playPauseIntent = PendingIntent.getService(
//            this, 0, Intent(this, PlayerService::class.java).apply {
//                action = "ACTION_PLAY_PAUSE"
//            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val rewindIntent = PendingIntent.getService(
//            this, 0, Intent(this, PlayerService::class.java).apply {
//                action = "ACTION_REWIND"
//            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val fastForwardIntent = PendingIntent.getService(
//            this, 0, Intent(this, PlayerService::class.java).apply {
//                action = "ACTION_FAST_FORWARD"
//            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Player Service")
//            .setContentText("Playing audio")
//            .setSmallIcon(R.drawable.placeholder)
//            .addAction(R.drawable.placeholder, "Rewind", rewindIntent)
//            .addAction(R.drawable.placeholder, "Play/Pause", playPauseIntent)
//            .addAction(R.drawable.placeholder, "Fast Forward", fastForwardIntent)
//            .build()
//    }

    fun getCurrentPosition(): Long {
        return exoPlayer.currentPosition
    }

    fun getDuration(): Long {
        return exoPlayer.duration
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

}