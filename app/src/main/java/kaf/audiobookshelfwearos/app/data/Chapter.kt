package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Chapter(
    val id: Int = 0,
    val start: Double = 0.0,
    val end: Double = 0.0,
    val title: String = ""
)
