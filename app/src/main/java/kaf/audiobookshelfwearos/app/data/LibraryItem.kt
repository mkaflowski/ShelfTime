package kaf.audiobookshelfwearos.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(tableName = "library_item")
data class LibraryItem(
    @PrimaryKey val id: String = "",
    val ino: String = "",
    val libraryId: String = "",
    val folderId: String = "",
    val path: String = "",
    val relPath: String = "",
    val isFile: Boolean = false,
    val mtimeMs: Long = 0,
    val ctimeMs: Long = 0,
    val birthtimeMs: Long = 0,
    val addedAt: Long = 0,
    val updatedAt: Long = 0,
    val isMissing: Boolean = false,
    val isInvalid: Boolean = false,
    val mediaType: String = "",
    val media: Media = Media(),
    val numFiles: Int = 0,
    val size: Long = 0,
    val collapsedSeries: CollapsedSeries = CollapsedSeries(),
    @Embedded(prefix = "progress_")
    var userMediaProgress: UserMediaProgress = UserMediaProgress(),
    val userMediaProgressId: String? = null,
    val progressLastUpdate: Long = 0
) {
    val title: String
        get() = media.metadata.title
}
