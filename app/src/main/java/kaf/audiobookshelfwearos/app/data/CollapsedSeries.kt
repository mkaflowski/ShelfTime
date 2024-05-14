package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CollapsedSeries(
    val id: String = "",
    val name: String = "",
    val nameIgnorePrefix: String? = "",
    val numBooks: Int = 0
)
