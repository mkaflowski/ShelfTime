package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserMediaProgress(
    val id: String = "",
    val libraryItemId: String = "",
    val episodeId: String? = null,
    val duration: Double = 0.0,
    val progress: Double = 0.0,
    val currentTime: Double = 0.0,
    val isFinished: Boolean = false,
    val hideFromContinueListening: Boolean = false,
    val lastUpdate: Long = 0L,
    val startedAt: Long = 0L,
    val finishedAt: Long? = null
)
