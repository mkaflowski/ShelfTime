package kaf.audiobookshelfwearos.app.data.room.dao

import androidx.room.*
import kaf.audiobookshelfwearos.app.data.LibraryItem

@Dao
interface LibraryItemDao {
    @Transaction
    @Query("SELECT * FROM library_item WHERE id = :id")
    suspend fun getLibraryItemById(id: String): LibraryItem?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItemInternal(libraryItem: LibraryItem)

    @Transaction
    @Delete
    suspend fun deleteLibraryItem(libraryItem: LibraryItem)

    suspend fun insertLibraryItem(libraryItem: LibraryItem) {
        val existingItem = getLibraryItemById(libraryItem.id)
        if (existingItem == null || existingItem.userMediaProgress.lastUpdate <= libraryItem.userMediaProgress.lastUpdate) {
            insertLibraryItemInternal(libraryItem)
        }
    }
}