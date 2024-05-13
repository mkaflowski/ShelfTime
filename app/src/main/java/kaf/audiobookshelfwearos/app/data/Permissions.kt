package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Permissions(
    val download: Boolean = false,
    val update: Boolean = false,
    val delete: Boolean = false,
    val upload: Boolean = false,
    val accessAllLibraries: Boolean = false,
    val accessAllTags: Boolean = false,
    val accessExplicitContent: Boolean = false
)
