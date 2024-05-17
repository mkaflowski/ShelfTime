package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Track(
    val index: Int = 0,
    val startOffset: Double = 0.0,
    val duration: Double = 0.0,
    val title: String = "",
    val contentUrl: String = "",
    val mimeType: String = "",
    val metadata: AudioMetadata = AudioMetadata()
)
