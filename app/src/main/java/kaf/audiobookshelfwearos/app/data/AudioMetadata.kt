package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AudioMetadata(
    val filename: String = "",
    val ext: String = "",
    val path: String = "",
    val relPath: String = "",
    val size: Long = 0L,
    val mtimeMs: Long = 0L,
    val ctimeMs: Long = 0L,
    val birthtimeMs: Long = 0L
)
