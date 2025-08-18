package kaf.audiobookshelfwearos.app.data.room.dao

import androidx.room.*
import kaf.audiobookshelfwearos.app.data.LibraryItem

@Dao
interface LibraryItemDao {
    @Transaction
    @Query("SELECT * FROM library_item WHERE id = :id")
    suspend fun getLibraryItemById(id: String): LibraryItem?

    @Transaction
    @Query("SELECT * FROM library_item WHERE progress_isFinished = false ORDER BY progress_lastUpdate DESC")
    suspend fun getAllLibraryItems(): List<LibraryItem>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItemInternal(libraryItem: LibraryItem)

    @Transaction
    @Delete
    suspend fun deleteLibraryItem(libraryItem: LibraryItem)

    // New methods for pending sync management
    @Transaction
    @Query("SELECT * FROM library_item WHERE progress_toUpload = 1 ORDER BY progress_lastUpdate DESC")
    suspend fun getItemsWithPendingSync(): List<LibraryItem>
    
    @Transaction
    @Query("UPDATE library_item SET progress_toUpload = 0 WHERE id = :itemId")
    suspend fun markProgressAsSynced(itemId: String)
    
    @Transaction
    @Query("SELECT COUNT(*) FROM library_item WHERE progress_toUpload = 1")
    suspend fun getPendingSyncCount(): Int
    
    @Transaction
    @Query("SELECT * FROM library_item WHERE progress_toUpload = 1 AND progress_lastUpdate > :since")
    suspend fun getRecentPendingSync(since: Long): List<LibraryItem>

    // Enhanced insertLibraryItem with better conflict resolution
    suspend fun insertLibraryItem(libraryItem: LibraryItem) {
        val existingItem = getLibraryItemById(libraryItem.id)
        when {
            existingItem == null -> {
                // New item, insert directly
                insertLibraryItemInternal(libraryItem)
            }
            existingItem.userProgress.lastUpdate < libraryItem.userProgress.lastUpdate -> {
                // Incoming item is newer, replace
                insertLibraryItemInternal(libraryItem)
            }
            existingItem.userProgress.lastUpdate == libraryItem.userProgress.lastUpdate -> {
                // Same timestamp, prefer the one with toUpload = true
                if (libraryItem.userProgress.toUpload && !existingItem.userProgress.toUpload) {
                    insertLibraryItemInternal(libraryItem)
                }
            }
            // Existing item is newer, don't update
        }
    }
}