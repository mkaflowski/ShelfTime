package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    val id: String = "",
    val username: String = "",
    val type: String = "",
    val token: String = "",
    val mediaProgress: List<MediaProgress> = listOf(),
    val seriesHideFromContinueListening: List<Any> = listOf(),
    val bookmarks: List<Any> = listOf(),
    val isActive: Boolean = false,
    val isLocked: Boolean = false,
    val lastSeen: Long = 0,
    val createdAt: Long = 0,
    val permissions: Permissions = Permissions(),
    val librariesAccessible: List<Any> = listOf(),
    val itemTagsAccessible: List<Any> = listOf(),
    val userDefaultLibraryId: String = "",
    val serverSettings: ServerSettings = ServerSettings()
)
