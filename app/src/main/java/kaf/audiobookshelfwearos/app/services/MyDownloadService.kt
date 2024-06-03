package kaf.audiobookshelfwearos.app.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import kaf.audiobookshelfwearos.R
import kaf.audiobookshelfwearos.app.data.Track
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executor


const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
private const val JOB_ID = 1
private const val FOREGROUND_NOTIFICATION_ID = 1


@OptIn(UnstableApi::class)
class MyDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.exo_download_notification_channel_name,
    /* channelDescriptionResourceId= */ 0
) {
    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        super.onCreate()
        downloadNotificationHelper =
            DownloadNotificationHelper(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID);
    }

    override fun getDownloadManager(): DownloadManager {
        return getDownloadManager(this)
    }


    override fun getScheduler(): Scheduler? {
        return if (Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(
        downloads: List<Download>,
        @Requirements.RequirementFlags notMetRequirements: Int
    ): Notification {
        return downloadNotificationHelper
            .buildProgressNotification(
                /* context= */ this,
                R.drawable.splash_icon,
                /* contentIntent= */ null,
                /* message= */ null,
                downloads,
                notMetRequirements
            )
    }

    companion object {
        private lateinit var downloadManager: DownloadManager
        private lateinit var databaseProvider: StandaloneDatabaseProvider
        private lateinit var downloadCache: SimpleCache
        private val downloadManagerLock = Any()
        private val databaseProviderLock = Any()
        private val downloadCacheLock = Any()

        fun getDownloadCache(context: Context): Cache {
            synchronized(downloadCacheLock) {
                if (::downloadCache.isInitialized) {
                    return downloadCache
                }
                val downloadContentDirectory =
                    File(getDownloadDirectory(context.applicationContext), "downloads")
                downloadCache =
                    SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider)
                return downloadCache
            }
        }

        fun sendRemoveDownload(context: Context, track: Track) {
            val userDataManager = UserDataManager(context)
            val url = userDataManager.getCompleteAddress() + track.contentUrl
            Timber.d("Removing url = $url")
            Timber.d("Downloaded = " + track.isDownloaded(context))

            val downloadIntent: Intent =
                buildRemoveDownloadIntent(
                    context,
                    MyDownloadService::class.java,
                    track.id,
                    false
                )
            context.startService(downloadIntent)
            Timber.d("Downloaded = " + track.isDownloaded(context))

        }

        fun sendAddDownload(context: Context, track: Track) {
            val userDataManager = UserDataManager(context)
            val url = userDataManager.getCompleteAddress() + track.contentUrl
            Timber.d("Downloading url = $url")
            Timber.d("Downloaded = " + track.isDownloaded(context))
            val downloadRequest = DownloadRequest.Builder(
                track.id,
                Uri.parse(url)
            ).build()

            val downloadIntent: Intent =
                buildAddDownloadIntent(
                    context,
                    MyDownloadService::class.java,
                    downloadRequest,
                    false
                )
            context.startService(downloadIntent)
        }

        fun getDownloadManager(context: Context): DownloadManager {
            synchronized(downloadManagerLock) {
                if (::downloadManager.isInitialized) {
                    return downloadManager
                }

                databaseProvider = getDatabaseProvider(context.applicationContext)
                val downloadCache = getDownloadCache(context)

                val dataSourceFactory = DefaultHttpDataSource.Factory()
                val headers = hashMapOf<String, String>()
                val userDataManager = UserDataManager(context.applicationContext)
                headers["Authorization"] = "Bearer " + userDataManager.token
                dataSourceFactory.setDefaultRequestProperties(headers)

                val downloadExecutor = Executor(Runnable::run)

                downloadManager = DownloadManager(
                    context,
                    databaseProvider,
                    downloadCache,
                    dataSourceFactory,
                    downloadExecutor
                )

                downloadManager.maxParallelDownloads = 3
                downloadManager.addListener(object : DownloadManager.Listener {
                    override fun onInitialized(downloadManager: DownloadManager) {
                        super.onInitialized(downloadManager)
                        Timber.d("onInitialized")
                    }

                    override fun onDownloadsPausedChanged(
                        downloadManager: DownloadManager,
                        downloadsPaused: Boolean
                    ) {
                        super.onDownloadsPausedChanged(downloadManager, downloadsPaused)
                        Timber.d("onDownloadsPausedChanged")
                    }

                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?
                    ) {
                        super.onDownloadChanged(downloadManager, download, finalException)
                        Timber.d("onDownloadChanged ${download.state}")
                        if (download.state == Download.STATE_COMPLETED) {
                            Timber.i("Download completed: " + download.request.id)
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download
                    ) {
                        super.onDownloadRemoved(downloadManager, download)
                        Timber.d("onDownloadRemoved")
                    }

                    override fun onIdle(downloadManager: DownloadManager) {
                        super.onIdle(downloadManager)
                        Timber.d("onIdle")
                    }

                    override fun onRequirementsStateChanged(
                        downloadManager: DownloadManager,
                        requirements: Requirements,
                        notMetRequirements: Int
                    ) {
                        super.onRequirementsStateChanged(
                            downloadManager,
                            requirements,
                            notMetRequirements
                        )
                        Timber.d("onRequirementsStateChanged")
                    }

                    override fun onWaitingForRequirementsChanged(
                        downloadManager: DownloadManager,
                        waitingForRequirements: Boolean
                    ) {
                        super.onWaitingForRequirementsChanged(
                            downloadManager,
                            waitingForRequirements
                        )
                        Timber.d("onWaitingForRequirementsChanged")
                    }
                })

                return downloadManager
            }
        }

        private fun getDatabaseProvider(context: Context): StandaloneDatabaseProvider {
            synchronized(databaseProviderLock) {
                if (::databaseProvider.isInitialized) {
                    return databaseProvider
                }
                databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                return databaseProvider
            }
        }

        private fun getDownloadDirectory(context: Context): File? {
            var downloadDirectory = context.getExternalFilesDir(null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
            return downloadDirectory
        }
    }

}