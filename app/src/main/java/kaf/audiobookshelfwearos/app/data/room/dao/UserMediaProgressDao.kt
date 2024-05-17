package kaf.audiobookshelfwearos.app.data.room.dao
import androidx.room.*
import kaf.audiobookshelfwearos.app.data.UserMediaProgress

@Dao
interface UserMediaProgressDao {
    @Transaction
    @Query("SELECT * FROM user_media_progress WHERE id = :id")
    suspend fun getUserMediaProgressById(id: String): UserMediaProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMediaProgress(userMediaProgress: UserMediaProgress)

    @Delete
    suspend fun deleteUserMediaProgress(userMediaProgress: UserMediaProgress)
}