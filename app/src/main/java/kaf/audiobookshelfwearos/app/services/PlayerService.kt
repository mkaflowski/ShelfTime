package kaf.audiobookshelfwearos.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import kaf.audiobookshelfwearos.R
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.activities.PlayerActivity
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
    private val START_OFFSET_SECONDS = 5
    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: NotificationManagerCompat

    private var playbackStartTime: Long = 0
    private var totalPlaybackTime: Long = 0
    private var ONGOING_NOTIFICATION_ID: Int = 151
    private var CHANNEL_NAME: String = "Player"

    private var audiobook = LibraryItem()
    private lateinit var db: AppDatabase
    private lateinit var userDataManager: UserDataManager

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        userDataManager = UserDataManager(this)
        createChannel(this)
        notificationManager = NotificationManagerCompat.from(applicationContext)
        db = (applicationContext as MainApp).database
        createPlayer()
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    private fun createChannel(context: Context) {
        val mNotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // The id of the channel.
        // The user-visible name of the channel.
        val name: CharSequence = "Player"
        // The user-visible description of the channel.
        val description: String = "Player"
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(CHANNEL_NAME, name, importance)
        // Configure the notification channel.
        mChannel.description = description
        mChannel.setShowBadge(true)
        mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        mNotificationManager.createNotificationChannel(mChannel)
    }

    private fun generateOngoingActivityNotification() {
        if (!exoPlayer.isPlaying) {
            notificationManager.cancel(ONGOING_NOTIFICATION_ID)
            return
        }

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data (note, the main notification text comes from the parameter above).
        val titleText = getString(R.string.app_name)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText("Playing")
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, PlayerActivity::class.java)

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 4. Build and issue the notification.
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, CHANNEL_NAME)

        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText("Playing")
            .setSmallIcon(R.drawable.notification)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Makes Notification an Ongoing Notification (a Notification with a background task).
            .setOngoing(true)
            // For an Ongoing Activity, used to decide priority on the watch face.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Create an Ongoing Activity.
        val ongoingActivityStatus = Status.Builder()
            // Sets the text used across various surfaces.
            .addTemplate("Playing")
            .build()

        val ongoingActivity =
            OngoingActivity.Builder(
                applicationContext,
                ONGOING_NOTIFICATION_ID,
                notificationBuilder
            )
                // Sets icon that will appear on the watch face in active mode. If it isn't set,
                // the watch face will use the static icon in active mode.
                .setAnimatedIcon(R.drawable.notification)
                // Sets the icon that will appear on the watch face in ambient mode.
                // Falls back to Notification's smallIcon if not set. If neither is set,
                // an Exception is thrown.
                .setStaticIcon(R.drawable.notification)
                // Sets the tap/touch event, so users can re-enter your app from the
                // other surfaces.
                // Falls back to Notification's contentIntent if not set. If neither is set,
                // an Exception is thrown.
                .setTouchIntent(activityPendingIntent)
                // In our case, sets the text used for the Ongoing Activity (more options are
                // available for timers and stop watches).
                .setStatus(ongoingActivityStatus)
                .build()

        // Applies any Ongoing Activity updates to the notification builder.
        // This method should always be called right before you build your notification,
        // since an Ongoing Activity doesn't hold references to the context.
        ongoingActivity.apply(applicationContext)


        notificationManager.notify(ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    }

    @UnstableApi
    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateUIMetadata()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                Timber.d("mediaMetadata - " + mediaMetadata.trackNumber)
                mediaMetadata.trackNumber?.minus(1)
                updateUIMetadata()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                generateOngoingActivityNotification()
            }

            override fun onPlaybackStateChanged(state: Int) {
                generateOngoingActivityNotification()

                when (state) {
                    Player.STATE_BUFFERING -> {
                        Timber
                            .d("ExoPlayer is buffering")

                        val intent = Intent("$packageName.ACTION_BUFFERING")
                        sendBroadcast(intent)
                    }

                    Player.STATE_READY -> {
                        Timber.d("ExoPlayer is ready " + exoPlayer.currentPosition)
                        updateUIMetadata()
                    }

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
        Timber.d("progress " + audiobook.userProgress.progress)
        Timber.d("duration " + audiobook.userProgress.duration)
        Timber.d("episodeId " + audiobook.userProgress.episodeId)
        Timber.d("id " + audiobook.userProgress.id)
        Timber.d("currentTime " + audiobook.userProgress.currentTime)
        Timber.d("lastUpdate " + audiobook.userProgress.lastUpdate)

        audiobook.userProgress.lastUpdate = System.currentTimeMillis()
        audiobook.userProgress.currentTime = getCurrentTotalPositionInS()
        audiobook.userProgress.toUpload = true
        audiobook.userProgress.libraryItemId = audiobook.id
        scope.launch(Dispatchers.IO) {
            db.libraryItemDao().insertLibraryItem(audiobook)
            ApiHandler(this@PlayerService).updateProgress(audiobook.userProgress)
        }
    }

    fun updateUIMetadata() {
        if (exoPlayer.playbackState == Player.STATE_BUFFERING) {
            val intent = Intent("$packageName.ACTION_BUFFERING")
            sendBroadcast(intent)
        }

        val timeInS = getCurrentTotalPositionInS() + START_OFFSET_SECONDS + 1

        var currentChapter = Chapter()
        for (chapter in audiobook.media.chapters) {
            if (timeInS >= chapter.start && timeInS < chapter.end)
                currentChapter = chapter
        }

        val intent = Intent("$packageName.ACTION_UPDATE_METADATA").apply {
            putExtra("CHAPTER_TITLE", currentChapter.title)
        }
        sendBroadcast(intent)
        if (exoPlayer.isPlaying)
            sendBroadcast(Intent("$packageName.ACTION_PLAYING"))
    }

    private fun getCurrentTotalPositionInS(): Double {
        if (audiobook.media.tracks.isEmpty())
            return 0.0
        val track = audiobook.media.tracks[exoPlayer.currentMediaItemIndex]
        return track.startOffset + exoPlayer.currentPosition / 1000
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
            setSpeed(userDataManager.speed)
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

        intent?.getStringExtra("id")?.let { id ->
            GlobalScope.launch {
                db.libraryItemDao().getLibraryItemById(id)?.let {
                    withContext(Dispatchers.Main) {
                        var time = intent.getDoubleExtra("time", -1.0)
                        if (time < 0)
                            time = it.userProgress.currentTime

                        if (intent.getStringExtra("action").equals("continue")) {
                            if (audiobook.id.equals(id))
                                return@withContext
                        }

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

    fun getSpeed(): Float {
        return exoPlayer.playbackParameters.speed
    }

    fun setSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        userDataManager.speed = speed
    }

    companion object {
        fun setAudiobook(
            context: Context,
            item: LibraryItem,
            time: Double = -1.0,
            action: String = "default"
        ) {
            val serviceIntent = Intent(context, PlayerService::class.java).apply {
                putExtra(
                    "id",
                    item.id
                )
                putExtra(
                    "time",
                    time
                )
                putExtra(
                    "action",
                    action
                )
            }
            context.startForegroundService(serviceIntent)
        }
    }

}