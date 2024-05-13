package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Folder(
    val id: String = "",
    val fullPath: String = "",
    val libraryId: String = "",
    val addedAt: Long = 0
)
