package kaf.audiobookshelfwearos.app.data

data class DownloadProgress(
    val trackId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentComplete: Float,
    val downloadSpeed: Long, // bytes per second
    val estimatedTimeRemaining: Long, // seconds
    val state: DownloadState
)

enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class AudiobookDownloadProgress(
    val audiobookId: String,
    val trackProgresses: List<DownloadProgress>,
    val overallProgress: Float,
    val totalBytesDownloaded: Long,
    val totalBytes: Long,
    val averageDownloadSpeed: Long,
    val estimatedTimeRemaining: Long
)
