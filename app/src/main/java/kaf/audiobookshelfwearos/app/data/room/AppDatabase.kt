package kaf.audiobookshelfwearos.app.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.data.UserMediaProgress
import kaf.audiobookshelfwearos.app.data.room.dao.LibraryItemDao

@Database(entities = [LibraryItem::class, UserMediaProgress::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryItemDao(): LibraryItemDao
}