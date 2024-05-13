package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Settings(
    val coverAspectRatio: Int = 0,
    val disableWatcher: Boolean = false,
    val skipMatchingMediaWithAsin: Boolean = false,
    val skipMatchingMediaWithIsbn: Boolean = false,
    val autoScanCronExpression: String? = null,
    val audiobooksOnly: Boolean = false,
    val hideSingleBookSeries: Boolean = false,
    val onlyShowLaterBooksInContinueSeries: Boolean = false,
    val metadataPrecedence: List<String> = emptyList(),
    val podcastSearchRegion: String? = null
)
