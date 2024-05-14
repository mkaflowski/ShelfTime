package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kaf.audiobookshelfwearos.app.userdata.UserDataManager

@JsonIgnoreProperties(ignoreUnknown = true)
data class LibraryItem(
    val id: String = "",
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
    val progressLastUpdate: Long = 0
) {
    val cover: String
        get() = media.coverPath
    val title: String
        get() = media.metadata.title
}
