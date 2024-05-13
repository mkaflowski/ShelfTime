package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MediaProgress(
    val id: String = "",
    val libraryItemId: String = "",
    val episodeId: String = "",
    val duration: Double = 0.0,
    val progress: Double = 0.0,
    val currentTime: Double = 0.0,
    val isFinished: Boolean = false,
    val hideFromContinueListening: Boolean = false,
    val lastUpdate: Long = 0,
    val startedAt: Long = 0,
    val finishedAt: Long? = 0
)
