package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AudioFile(
    val index: Int = 0,
    val ino: String = "",
    val metadata: AudioMetadata = AudioMetadata(),
    val addedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val trackNumFromMeta: Int = 0,
    val discNumFromMeta: Int? = null,
    val trackNumFromFilename: Int = 0,
    val discNumFromFilename: Int? = null,
    val manuallyVerified: Boolean = false,
    val exclude: Boolean = false,
    val error: String? = null,
    val format: String = "",
    val duration: Double = 0.0,
    val bitRate: Int = 0,
    val language: String? = null,
    val codec: String = "",
    val timeBase: String = "",
    val channels: Int = 0,
    val channelLayout: String = "",
    val chapters: List<Chapter> = emptyList(),
    val embeddedCoverArt: String? = null,
    val metaTags: MetaTags = MetaTags(),
    val mimeType: String = ""
)
