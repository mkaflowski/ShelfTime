package kaf.audiobookshelfwearos.app.workers

import android.content.Context
import androidx.work.*
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.MainApp
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val apiHandler = ApiHandler(applicationContext)
            val db = (applicationContext as MainApp).database
            
            val pendingItems = db.libraryItemDao().getItemsWithPendingSync()
            Timber.d("Background sync: Found ${pendingItems.size} items to sync")
            
            if (pendingItems.isEmpty()) {
                return Result.success()
            }
            
            var successCount = 0
            for (item in pendingItems) {
                val success = apiHandler.updateProgress(item.userProgress)
                if (success) {
                    successCount++
                    // Mark as synced in database
                    db.libraryItemDao().markProgressAsSynced(item.id)
                }
            }
            
            Timber.d("Background sync completed: $successCount/${pendingItems.size} successful")
            
            if (successCount == pendingItems.size) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Background sync failed")
            Result.retry()
        }
    }
    
    companion object {
        private const val WORK_NAME = "sync_progress"
        
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
        }
        
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
