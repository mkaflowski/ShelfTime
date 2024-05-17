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
    suspend fun insertLibraryItem(libraryItem: LibraryItem)

    @Transaction
    @Delete
    suspend fun deleteLibraryItem(libraryItem: LibraryItem)
}