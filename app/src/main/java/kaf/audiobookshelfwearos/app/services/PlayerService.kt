package kaf.audiobookshelfwearos.app.services

import android.content.Context
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
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService.buildAddDownloadIntent
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.data.Chapter
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.data.room.AppDatabase
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class PlayerService : MediaSessionService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val binder = LocalBinder()
    private lateinit var exoPlayer: ExoPlayer
    private val CHANNEL_ID = "PlayerServiceChannel"
    private val START_OFFSET_SECONDS = 5
    private var mediaSession: MediaSession? = null

    private var playbackStartTime: Long = 0
    private var totalPlaybackTime: Long = 0

    private lateinit var audiobook: LibraryItem
    private lateinit var db: AppDatabase

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        db = (applicationContext as MainApp).database
        createPlayer()
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                Timber.d("mediaMetadata - " + mediaMetadata.trackNumber)
                mediaMetadata.trackNumber?.minus(1)
                updateUIMetadata()
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> Timber
                        .d("ExoPlayer is buffering")

                    Player.STATE_READY -> Timber.d("ExoPlayer is ready")
                    Player.STATE_ENDED -> {
                        Timber.d("ExoPlayer has ended")
                        sendBroadcast(Intent("$packageName.ACTION_TRACK_ENDED"))
                    }

                    Player.STATE_IDLE -> {
                        Timber.d("ExoPlayer in idle")
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
                    saveProgress()
                    sendBroadcast(Intent("$packageName.ACTION_PAUSE"))
                    val currentTime = System.currentTimeMillis()
                    totalPlaybackTime += currentTime - playbackStartTime
                    Timber.tag("PlayerService")
                        .d("%s seconds", "Total playback time: ${totalPlaybackTime / 1000}")
                }
            }
        })
    }

    private fun saveProgress() {
        Timber.d("getCurrentTotalPositionInS " + getCurrentTotalPositionInS())
        Timber.d("progress " + audiobook.userMediaProgress.progress)
        Timber.d("duration " + audiobook.userMediaProgress.duration)
        Timber.d("episodeId " + audiobook.userMediaProgress.episodeId)
        Timber.d("id " + audiobook.userMediaProgress.id)
        Timber.d("currentTime " + audiobook.userMediaProgress.currentTime)
        Timber.d("lastUpdate " + audiobook.userMediaProgress.lastUpdate)

        audiobook.userMediaProgress.lastUpdate = System.currentTimeMillis()
        audiobook.userMediaProgress.currentTime = getCurrentTotalPositionInS()
        audiobook.userMediaProgress.toUpload = true
        scope.launch(Dispatchers.IO) {
            db.libraryItemDao().insertLibraryItem(audiobook)
            ApiHandler(this@PlayerService).updateProgress(audiobook.userMediaProgress)
        }
    }

    private fun updateUIMetadata() {
        val timeInS = getCurrentTotalPositionInS()

        var currentChapter = Chapter()
        for (chapter in audiobook.media.chapters) {
            if (timeInS >= chapter.start && timeInS < chapter.end)
                currentChapter = chapter
        }

        val intent = Intent("$packageName.ACTION_UPDATE_METADATA").apply {
            putExtra("CHAPTER_TITLE", currentChapter.title)
        }
        sendBroadcast(intent)
    }

    private fun getCurrentTotalPositionInS(): Double {
        val track = audiobook.media.tracks[exoPlayer.currentMediaItemIndex]
        val timeInS = track.startOffset + exoPlayer.currentPosition / 1000
        return timeInS
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
        ) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    @OptIn(UnstableApi::class)
    private fun setAudiobook(audiobook: LibraryItem, userTotalTime: Double) {
        this.audiobook = audiobook
        exoPlayer.clearMediaItems()
        val userDataManager = UserDataManager(this)


        //getting chapter by time
        var totalDuration = 0.0
        var trackIndex = 0
        for (track in audiobook.media.tracks) {
            totalDuration += track.duration
            if (totalDuration > userTotalTime)
                break
            trackIndex++
        }

        val userTrackTime = userTotalTime - audiobook.media.tracks[trackIndex].startOffset

        val headers = hashMapOf<String, String>()
        headers["Authorization"] = "Bearer " + userDataManager.token;
        // Create a factory for HTTP data sources with the OkHttpClient instance
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        val sources = arrayListOf<MediaSource>()
        for (track in audiobook.media.tracks) {
            val url =
                userDataManager.getCompleteAddress() + track.contentUrl

            val mediaItem =
                MediaItem.Builder()
                    .setMediaId("track-index-" + track.index)
                    .setUri(url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setArtist(audiobook.media.metadata.authorName)
                            .setTitle(audiobook.media.metadata.title)
                            .build()
                    )
                    .build()

            // Build a track source using the data source factory
            val downloaded = track.isDownloaded(this)
            Timber.d("${track.index} downloaded = $downloaded")

            val downloadCache = MyDownloadService.getDownloadCache(this)

            // Create a read-only cache data source factory using the download cache.
            val cacheDataSourceFactory: DataSource.Factory =
                CacheDataSource.Factory()
                    .setCache(downloadCache)
                    .setUpstreamDataSourceFactory(dataSourceFactory)
                    .setCacheWriteDataSinkFactory(null) // Disable writing.

            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(mediaItem)
            sources.add(mediaSource)

            val concatenatingMediaSource = ConcatenatingMediaSource()
            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        exoPlayer.run {
            setMediaSources(sources)
            seekToDefaultPosition(trackIndex)
            seekTo(trackIndex, (userTrackTime.toLong() - START_OFFSET_SECONDS) * 1000)
            prepare()
            playWhenReady = true
        }
    }

    fun getTotalPlaybackTime(): Long {
        return totalPlaybackTime
    }

    @OptIn(UnstableApi::class)
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
                db.libraryItemDao().getLibraryItemById(it)?.let {
                    withContext(Dispatchers.Main) {
                        var time = intent.getDoubleExtra("time", -1.0)
                        if (time < 0)
                            time = it.userMediaProgress.currentTime
                        setAudiobook(it, time)
                    }
                }
            }
        }

        return START_STICKY
    }

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
        job.cancel()
        super.onDestroy()
    }

    companion object {
        fun setAudiobook(context: Context, item: LibraryItem, time: Double = -1.0) {
            val serviceIntent = Intent(context, PlayerService::class.java).apply {
                putExtra(
                    "id",
                    item.id
                )
                putExtra(
                    "time",
                    time
                )
            }
            context.startService(serviceIntent)
        }
    }

}