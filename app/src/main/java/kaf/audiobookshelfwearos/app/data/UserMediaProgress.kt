package kaf.audiobookshelfwearos.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(tableName = "user_media_progress")
data class UserMediaProgress(
    @PrimaryKey val id: String = "",
    val libraryItemId: String = "",
    val episodeId: String? = null,
    val duration: Double = 0.0,
    val progress: Double = 0.0,
    var currentTime: Double = 0.0,
    val isFinished: Boolean = false,
    val hideFromContinueListening: Boolean = false,
    var lastUpdate: Long = 0L,
    val startedAt: Long = 0L,
    val finishedAt: Long? = null,
    var toUpload: Boolean = false
)
