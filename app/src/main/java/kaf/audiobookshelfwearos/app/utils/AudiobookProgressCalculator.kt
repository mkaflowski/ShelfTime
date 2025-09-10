package kaf.audiobookshelfwearos.app.utils

import kaf.audiobookshelfwearos.app.data.AudiobookDownloadProgress
import kaf.audiobookshelfwearos.app.data.DownloadProgress
import kaf.audiobookshelfwearos.app.data.LibraryItem

object AudiobookProgressCalculator {
    
    fun calculateAudiobookProgress(
        libraryItem: LibraryItem,
        trackProgresses: List<DownloadProgress>
    ): AudiobookDownloadProgress {
        val totalTracks = libraryItem.media.tracks.size
        
        if (trackProgresses.isEmpty()) {
            return AudiobookDownloadProgress(
                audiobookId = libraryItem.id,
                trackProgresses = emptyList(),
                overallProgress = 0f,
                totalBytesDownloaded = 0L,
                totalBytes = 0L,
                averageDownloadSpeed = 0L,
                estimatedTimeRemaining = Long.MAX_VALUE
            )
        }
        
        val totalBytesDownloaded = trackProgresses.sumOf { it.bytesDownloaded }
        val totalBytes = trackProgresses.sumOf { it.totalBytes }
        
        val overallProgress = if (totalBytes > 0) {
            (totalBytesDownloaded.toFloat() / totalBytes.toFloat()) * 100f
        } else {
            // Fallback to track count based progress
            val completedTracks = trackProgresses.count { it.percentComplete >= 100f }
            (completedTracks.toFloat() / totalTracks.toFloat()) * 100f
        }
        
        // Use weighted average for more stable speed calculation
        val activeDownloads = trackProgresses.filter { it.downloadSpeed > 1000 } // Only consider reasonable speeds
        val averageDownloadSpeed = if (activeDownloads.isNotEmpty()) {
            // Weight by bytes downloaded to give more weight to larger/more active downloads
            val totalWeightedSpeed = activeDownloads.sumOf { 
                it.downloadSpeed * (it.bytesDownloaded.coerceAtLeast(1L))
            }
            val totalWeight = activeDownloads.sumOf { it.bytesDownloaded.coerceAtLeast(1L) }
            
            if (totalWeight > 0) {
                (totalWeightedSpeed / totalWeight).coerceAtLeast(0L)
            } else {
                activeDownloads.sumOf { it.downloadSpeed } / activeDownloads.size
            }
        } else {
            0L
        }
        
        val remainingBytes = totalBytes - totalBytesDownloaded
        val estimatedTimeRemaining = if (averageDownloadSpeed > 1000 && remainingBytes > 0) {
            val estimatedSeconds = remainingBytes / averageDownloadSpeed
            // Add 10% buffer for more conservative estimates and cap at reasonable values
            (estimatedSeconds * 1.1).toLong().coerceIn(1L, 86400L)
        } else {
            Long.MAX_VALUE
        }
        
        timber.log.Timber.d("Audiobook progress calculation: " +
                "tracks=${trackProgresses.size}, " +
                "totalBytes=$totalBytes, " +
                "downloaded=$totalBytesDownloaded, " +
                "progress=$overallProgress%, " +
                "speed=$averageDownloadSpeed, " +
                "eta=${estimatedTimeRemaining}s")
        
        return AudiobookDownloadProgress(
            audiobookId = libraryItem.id,
            trackProgresses = trackProgresses,
            overallProgress = overallProgress,
            totalBytesDownloaded = totalBytesDownloaded,
            totalBytes = totalBytes,
            averageDownloadSpeed = averageDownloadSpeed,
            estimatedTimeRemaining = estimatedTimeRemaining
        )
    }
}
