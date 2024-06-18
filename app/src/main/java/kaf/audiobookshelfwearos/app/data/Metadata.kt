package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Metadata(
    var title: String = "",
    val titleIgnorePrefix: String? = "",
    val subtitle: String? = "",
    var authorName: String = "",
    val narratorName: String? = "",
    val seriesName: String? = "",
    val genres: List<String> = emptyList(),
    val publishedYear: String? = "",
    val publishedDate: String? = "",
    val publisher: String? = "",
    val description: String = "",
    val isbn: String? = "",
    val asin: String? = "",
    val language: String? = "",
    val explicit: Boolean = false
)
