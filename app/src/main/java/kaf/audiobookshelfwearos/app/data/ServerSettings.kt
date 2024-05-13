package kaf.audiobookshelfwearos.app.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServerSettings(
    val id: String = "",
    val scannerFindCovers: Boolean = false,
    val scannerCoverProvider: String = "",
    val scannerParseSubtitle: Boolean = false,
    val scannerPreferMatchedMetadata: Boolean = false,
    val scannerDisableWatcher: Boolean = false,
    val storeCoverWithItem: Boolean = false,
    val storeMetadataWithItem: Boolean = false,
    val metadataFileFormat: String = "",
    val rateLimitLoginRequests: Int = 0,
    val rateLimitLoginWindow: Int = 0,
    val backupSchedule: String = "",
    val backupsToKeep: Int = 0,
    val maxBackupSize: Int = 0,
    val loggerDailyLogsToKeep: Int = 0,
    val loggerScannerLogsToKeep: Int = 0,
    val homeBookshelfView: Int = 0,
    val bookshelfView: Int = 0,
    val sortingIgnorePrefix: Boolean = false,
    val sortingPrefixes: List<String> = listOf(),
    val chromecastEnabled: Boolean = false,
    val dateFormat: String = "",
    val language: String = "",
    val logLevel: Int = 0,
    val version: String = ""
)
