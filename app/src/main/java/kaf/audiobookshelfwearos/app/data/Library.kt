package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Library(
    val id: String = "",
    val oldLibraryId: String? = "",
    val name: String = "",
    val folders: List<Folder> = emptyList(),
    val displayOrder: Int = 0,
    val icon: String = "",
    val mediaType: String = "",
    val provider: String = "",
    val settings: Settings = Settings(),
    val lastScan: Long = 0,
    val lastScanVersion: String = "",
    val createdAt: Long = 0,
    val lastUpdate: Long = 0,
    val libraryItems: ArrayList<LibraryItem> = arrayListOf()
)
