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

    suspend fun insertLibraryItem(libraryItem: LibraryItem) {
        val existingItem = getLibraryItemById(libraryItem.id)
        if (existingItem == null || existingItem.userProgress.lastUpdate <= libraryItem.userProgress.lastUpdate) {
            insertLibraryItemInternal(libraryItem)
        }
    }
}