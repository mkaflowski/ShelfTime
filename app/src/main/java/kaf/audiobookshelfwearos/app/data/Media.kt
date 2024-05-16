package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Media(
    val libraryItemId: String = "",
    val tracks: List<Track> = emptyList(),
    val audioFiles: List<AudioFile> = emptyList(),
    val metadata: Metadata = Metadata(),
    val coverPath: String = "",
    val tags: List<String> = emptyList(),
    val numTracks: Int = 0,
    val numAudioFiles: Int = 0,
    val numChapters: Int = 0,
    val duration: Double = 0.0,
    val size: Long = 0,
    val ebookFileFormat: String? = ""
)
