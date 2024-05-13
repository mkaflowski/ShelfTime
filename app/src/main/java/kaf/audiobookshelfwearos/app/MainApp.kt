package kaf.audiobookshelfwearos.app

import android.app.Application
import kaf.audiobookshelfwearos.BuildConfig
import timber.log.Timber

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }
    }

    internal class LineNumberDebugTree : Timber.DebugTree()
    {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
        }
    }
}