package kaf.audiobookshelfwearos.app.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
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
import kaf.audiobookshelfwearos.app.data.DownloadProgress
import kaf.audiobookshelfwearos.app.data.DownloadState
import kaf.audiobookshelfwearos.app.data.Track
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kaf.audiobookshelfwearos.app.utils.DownloadProgressCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
        
        // Progress flow for emitting download progress updates
        private val progressUpdateFlow = MutableSharedFlow<DownloadProgress>(
            replay = 1,
            extraBufferCapacity = 10
        )
        
        fun getProgressFlow(): SharedFlow<DownloadProgress> = progressUpdateFlow.asSharedFlow()

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

                downloadManager.maxParallelDownloads = 2
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
                        
                        // Calculate and emit progress
                        val progress = calculateProgress(download)
                        Timber.d("Calculated progress for ${download.request.id}: ${progress.percentComplete}%, speed: ${progress.downloadSpeed}")
                        
                        val emitted = progressUpdateFlow.tryEmit(progress)
                        Timber.d("Progress emission result: $emitted")
                        
                        if (download.state == Download.STATE_COMPLETED) {
                            Timber.i("Download completed: " + download.request.id)
                            // Clear speed history for completed downloads
                            DownloadProgressCalculator.clearSpeedHistory(download.request.id)
                        }

                        Timber.d("onDownloadChanged ${download.state}")
                        if (download.percentDownloaded != C.PERCENTAGE_UNSET.toFloat()) {
                            Timber.d("Download progress: ${download.percentDownloaded}%")
                        } else {
                            Timber.d("Download progress: Not available")
                        }
                        if (download.state == Download.STATE_COMPLETED) {
                            Timber.i("Download completed: ${download.request.id}")
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download
                    ) {
                        super.onDownloadRemoved(downloadManager, download)
                        Timber.d("onDownloadRemoved")
                        // Clear speed history for removed downloads
                        DownloadProgressCalculator.clearSpeedHistory(download.request.id)
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
        
        private fun calculateProgress(download: Download): DownloadProgress {
            val percentComplete = if (download.percentDownloaded != C.PERCENTAGE_UNSET.toFloat()) {
                download.percentDownloaded
            } else {
                0f
            }
            
            val bytesDownloaded = download.bytesDownloaded
            val totalBytes = if (download.contentLength != -1L) {
                download.contentLength
            } else {
                // Estimate total bytes based on current progress if available
                if (percentComplete > 0) {
                    (bytesDownloaded / (percentComplete / 100f)).toLong()
                } else {
                    0L
                }
            }
            
            val downloadSpeed = DownloadProgressCalculator.calculateDownloadSpeed(
                download.request.id, 
                bytesDownloaded
            )
            val remainingBytes = totalBytes - bytesDownloaded
            val estimatedTime = DownloadProgressCalculator.calculateEstimatedTime(
                remainingBytes, 
                downloadSpeed
            )
            
            Timber.d("Progress calculation for ${download.request.id}: " +
                    "percent=$percentComplete%, bytes=$bytesDownloaded/$totalBytes, " +
                    "speed=$downloadSpeed, eta=$estimatedTime")
            
            return DownloadProgress(
                trackId = download.request.id,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                percentComplete = percentComplete,
                downloadSpeed = downloadSpeed,
                estimatedTimeRemaining = estimatedTime,
                state = mapDownloadState(download.state)
            )
        }
        
        private fun mapDownloadState(downloadState: Int): DownloadState {
            return when (downloadState) {
                Download.STATE_QUEUED -> DownloadState.QUEUED
                Download.STATE_DOWNLOADING -> DownloadState.DOWNLOADING
                Download.STATE_COMPLETED -> DownloadState.COMPLETED
                Download.STATE_FAILED -> DownloadState.FAILED
                Download.STATE_REMOVING -> DownloadState.CANCELLED
                Download.STATE_RESTARTING -> DownloadState.DOWNLOADING
                else -> DownloadState.QUEUED
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