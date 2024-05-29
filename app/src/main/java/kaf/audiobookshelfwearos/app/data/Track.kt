package kaf.audiobookshelfwearos.app.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kaf.audiobookshelfwearos.app.services.MyDownloadService
import kaf.audiobookshelfwearos.app.userdata.UserDataManager

@JsonIgnoreProperties(ignoreUnknown = true)
data class Track(
    val index: Int = 0,
    val startOffset: Double = 0.0,
    val duration: Double = 0.0,
    val title: String = "",
    val contentUrl: String = "",
    val mimeType: String = "",
    val metadata: AudioMetadata = AudioMetadata()
) {
    val id: String
        get() = contentUrl

    @OptIn(UnstableApi::class)
    fun isDownloaded(context: Context): Boolean {
        val downloadManager: DownloadManager = MyDownloadService.getDownloadManager(context)
        val download: Download? = downloadManager.downloadIndex.getDownload(contentUrl)
        return download != null && download.state == Download.STATE_COMPLETED
    }
}