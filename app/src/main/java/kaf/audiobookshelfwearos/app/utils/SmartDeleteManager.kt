package kaf.audiobookshelfwearos.app.utils

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.content.Context
import android.widget.Toast
import androidx.media3.common.util.Log
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.services.MyDownloadService
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class SmartDeleteManager(private val context: Context) {
    private val userDataManager = UserDataManager(context)
    private val database = (context.applicationContext as MainApp).database

    fun triggerSmartDeleteAfterDownload() {
        if (!userDataManager.smartDeleteEnabled) return

        CoroutineScope(Dispatchers.IO).launch {
            delay(3_000) // 3 second delay
            performSmartDelete()
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun performSmartDelete() {
        try {
            val downloadManager = MyDownloadService.getDownloadManager(context)
            val downloadedItems = database.libraryItemDao().getAllLibraryItems()
                .filter { it.isDownloaded(context) }
                
            // Get download completion order by checking download index
            val itemsWithDownloadInfo = downloadedItems.mapNotNull { item ->
                val firstTrack = item.media.tracks.firstOrNull()
                if (firstTrack != null) {
                    val download = downloadManager.downloadIndex.getDownload(firstTrack.contentUrl)
                    if (download != null) {
                        item to download.updateTimeMs
                    } else null
                } else null
            }.sortedBy { it.second } // Sort by download update time (oldest first)

            val maxDownloads = userDataManager.smartDeleteMaxDownloads
            Timber.d("Smart delete max count: ${maxDownloads}")
            val excessCount = itemsWithDownloadInfo.size - maxDownloads
            Timber.d("Smart delete excess count: ${excessCount}")

            if (excessCount > 0) {
                val itemsToDelete = itemsWithDownloadInfo.take(excessCount).map { it.first }
                
                for (item in itemsToDelete) {
                    // Remove downloads using the existing service
                    for (track in item.media.tracks) {
                        MyDownloadService.sendRemoveDownload(context, track)
                    }
                    
                    // Remove from database
                    database.libraryItemDao().deleteLibraryItem(item)
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Removed ${item.title} to make space",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    Timber.d("Smart delete removed: ${item.title}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during smart delete")
        }
    }
}
