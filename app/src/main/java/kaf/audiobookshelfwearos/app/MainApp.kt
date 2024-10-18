package kaf.audiobookshelfwearos.app

import android.app.Application
import androidx.room.Room
import kaf.audiobookshelfwearos.BuildConfig
import kaf.audiobookshelfwearos.app.data.room.AppDatabase
import timber.log.Timber

class MainApp : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "library-item-db"
        ).fallbackToDestructiveMigration().build()
    }

    internal class LineNumberDebugTree : Timber.DebugTree()
    {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
        }
    }
}