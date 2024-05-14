package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonData(
    val libraryItems: List<LibraryItem> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val page: Int = 0,
    val sortBy: String = "",
    val sortDesc: Boolean = false,
    val filterBy: String = "",
    val mediaType: String = "",
    val minified: Boolean = false,
    val collapseseries: Boolean = false,
    val include: String = ""
)
