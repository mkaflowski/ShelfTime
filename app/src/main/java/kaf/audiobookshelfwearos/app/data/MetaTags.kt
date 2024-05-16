package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetaTags(
    val tagAlbum: String = "",
    val tagArtist: String = "",
    val tagGenre: String = "",
    val tagTitle: String = "",
    val tagTrack: String = "",
    val tagAlbumArtist: String = "",
    val tagComposer: String = ""
)
