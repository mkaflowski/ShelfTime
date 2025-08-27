package kaf.audiobookshelfwearos.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import kaf.audiobookshelfwearos.app.data.AudiobookDownloadProgress
import kaf.audiobookshelfwearos.app.data.DownloadProgress
import kaf.audiobookshelfwearos.app.utils.AudiobookProgressCalculator
import kaf.audiobookshelfwearos.app.utils.DownloadProgressCalculator
import androidx.media3.exoplayer.offline.Download
import androidx.media3.common.C
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.data.Chapter
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.services.MyDownloadService
import kaf.audiobookshelfwearos.app.services.PlayerService
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.floor

class ChapterListActivity : ComponentActivity() {
    var itemId: String = ""

    private val viewModel: ApiViewModel by viewModels {
        ApiViewModel.ApiViewModelFactory(
            ApiHandler(
                this
            )
        )
    }

    private var isOnlineMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isOnlineMode = !UserDataManager(this).offlineMode
        viewModel.setShowErrorTaosts(isOnlineMode)

        // Keep the screen on while this activity is in the foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.loginResult.observe(
            this
        ) { user ->
            Timber.d(user.token)
        }

        itemId = intent.getStringExtra("id") ?: ""

        Timber.i("itemId = $itemId")

        setContent {
            val libraryItem by viewModel.item.observeAsState()

            ManualLoadView(libraryItem, itemId)
            val scalingLazyListState = rememberScalingLazyListState(0)

            if (libraryItem?.id?.isNotEmpty() == true)
                libraryItem?.run {
                    Scaffold(
                        modifier = Modifier.onGloballyPositioned {},
                        positionIndicator = {
                            PositionIndicator(scalingLazyListState = scalingLazyListState)
                        },
                        vignette = {
                            Vignette(vignettePosition = VignettePosition.TopAndBottom)
                        }
                    ) {
                        ScalingLazyColumn(
                            state = scalingLazyListState,
                            scalingParams = ScalingLazyColumnDefaults.scalingParams(
                                edgeScale = 0.5f,
                                minTransitionArea = 0.5f,
                                maxTransitionArea = 0.5f
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(0.dp)
                        ) {
                            item {
                                AudiobookInfo(this@run)
                            }

                            item {
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Chapters:", modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth()
                                )
                            }

                            itemsIndexed(media.chapters) { index, _ ->
                                Chapter(this@run, media.chapters[index])
                                if (index != media.chapters.size) {
                                    Divider()
                                }
                            }

                        }
                    }
                }
        }
    }

    @Composable
    private fun AudiobookInfo(
        libraryItem: LibraryItem,
    ) {
        // Collect download progress from service
        val downloadProgressFlow = MyDownloadService.getProgressFlow()
        var trackProgresses by remember { mutableStateOf(mutableStateMapOf<String, DownloadProgress>()) }
        var audiobookProgress by remember { mutableStateOf<AudiobookDownloadProgress?>(null) }

        var isDownloaded by remember {
            mutableStateOf(
                libraryItem.media.tracks.all { track -> track.isDownloaded(this) }
            )
        }

        var isDownloading by remember {
            mutableStateOf(
                libraryItem.media.tracks.any { track -> track.isDownloading(this) }
            )
        }

        var downloadedCount by remember {
            mutableStateOf(libraryItem.media.tracks.count { track -> track.isDownloaded(this) })
        }
        val totalTracks = libraryItem.media.tracks.size

        // Listen for progress updates
        LaunchedEffect(libraryItem.id) {
            try {
                Timber.d("Starting to collect download progress for audiobook: ${libraryItem.id}")
                downloadProgressFlow.collect { trackProgress ->
                    Timber.d("Received progress update for track: ${trackProgress.trackId}, progress: ${trackProgress.percentComplete}%")
                    
                    // Debug: Log all track IDs for this audiobook
                    val audiobookTrackIds = libraryItem.media.tracks.map { it.id }
                    Timber.d("Audiobook track IDs: $audiobookTrackIds")
                    
                    // Check if this progress update is for one of our tracks
                    if (libraryItem.media.tracks.any { it.id == trackProgress.trackId }) {
                        Timber.d("Progress update matches one of our tracks!")
                        trackProgresses[trackProgress.trackId] = trackProgress
                        
                        // Calculate overall audiobook progress
                        val currentProgresses = trackProgresses.values.toList()
                        if (currentProgresses.isNotEmpty()) {
                            audiobookProgress = AudiobookProgressCalculator.calculateAudiobookProgress(
                                libraryItem, 
                                currentProgresses
                            )
                            Timber.d("Updated audiobook progress: ${audiobookProgress?.overallProgress}%")
                        }
                        
                        // Update download states
                        downloadedCount = libraryItem.media.tracks.count { track -> 
                            track.isDownloaded(this@ChapterListActivity) 
                        }
                        isDownloading = libraryItem.media.tracks.any { track ->
                            track.isDownloading(this@ChapterListActivity)
                        }
                        isDownloaded = libraryItem.media.tracks.all { track -> 
                            track.isDownloaded(this@ChapterListActivity) 
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error collecting download progress")
            }
        }

        // Periodic progress checker for more frequent updates
        LaunchedEffect(isDownloading) {
            while (isDownloading) {
                try {
                    // Check progress of all tracks manually
                    val downloadManager = MyDownloadService.getDownloadManager(this@ChapterListActivity)
                    var hasActiveDownloads = false
                    
                    for (track in libraryItem.media.tracks) {
                        val download = downloadManager.downloadIndex.getDownload(track.id)
                        if (download != null && download.state == Download.STATE_DOWNLOADING) {
                            hasActiveDownloads = true
                            
                            // Calculate progress manually
                            val percentComplete = if (download.percentDownloaded != C.PERCENTAGE_UNSET.toFloat()) {
                                download.percentDownloaded
                            } else {
                                0f
                            }
                            
                            val bytesDownloaded = download.bytesDownloaded
                            val totalBytes = if (download.contentLength != -1L) {
                                download.contentLength
                            } else {
                                if (percentComplete > 0) {
                                    (bytesDownloaded / (percentComplete / 100f)).toLong()
                                } else {
                                    0L
                                }
                            }
                            
                            val downloadSpeed = DownloadProgressCalculator.calculateDownloadSpeed(
                                track.id, 
                                bytesDownloaded
                            )
                            val remainingBytes = totalBytes - bytesDownloaded
                            val estimatedTime = DownloadProgressCalculator.calculateEstimatedTime(
                                remainingBytes, 
                                downloadSpeed
                            )
                            
                            val manualProgress = DownloadProgress(
                                trackId = track.id,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                percentComplete = percentComplete,
                                downloadSpeed = downloadSpeed,
                                estimatedTimeRemaining = estimatedTime,
                                state = kaf.audiobookshelfwearos.app.data.DownloadState.DOWNLOADING
                            )
                            
                            trackProgresses[track.id] = manualProgress
                            Timber.d("Manual progress check for ${track.id}: ${percentComplete}%")
                        }
                    }
                    
                    // Update audiobook progress if we have active downloads
                    if (hasActiveDownloads && trackProgresses.isNotEmpty()) {
                        val currentProgresses = trackProgresses.values.toList()
                        audiobookProgress = AudiobookProgressCalculator.calculateAudiobookProgress(
                            libraryItem, 
                            currentProgresses
                        )
                        Timber.d("Manual audiobook progress update: ${audiobookProgress?.overallProgress}%")
                    }
                    
                    // Update download states
                    downloadedCount = libraryItem.media.tracks.count { track -> 
                        track.isDownloaded(this@ChapterListActivity) 
                    }
                    isDownloading = libraryItem.media.tracks.any { track ->
                        track.isDownloading(this@ChapterListActivity)
                    }
                    isDownloaded = libraryItem.media.tracks.all { track -> 
                        track.isDownloaded(this@ChapterListActivity) 
                    }
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error in manual progress check")
                }
                
                delay(2000L) // Check every 2 seconds
            }
        }

        LaunchedEffect(isDownloading) {
            while (isDownloading) {
                downloadedCount =
                    libraryItem.media.tracks.count { track -> track.isDownloaded(this@ChapterListActivity) }
                isDownloading = libraryItem.media.tracks.any { track ->
                    track.isDownloading(this@ChapterListActivity)
                }
                delay(1000L)
            }
            isDownloaded =
                libraryItem.media.tracks.all { track -> track.isDownloaded(this@ChapterListActivity) }
        }

        val isSyncing by viewModel.isSyncing.collectAsState()

        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = libraryItem.title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 15.dp, start = 30.dp, end = 30.dp)
                    .fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Download/Delete button
                IconButton(onClick = {
                    if (isDownloaded) {
                        Toast.makeText(
                            this@ChapterListActivity,
                            "Audiobook deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                        for (track in libraryItem.media.tracks) {
                            MyDownloadService.sendRemoveDownload(
                                this@ChapterListActivity,
                                track
                            )
                        }
                        // Clear progress tracking
                        trackProgresses.clear()
                        audiobookProgress = null
                    } else {
                        Toast.makeText(
                            this@ChapterListActivity,
                            "Downloading started",
                            Toast.LENGTH_SHORT
                        ).show()
                        for (track in libraryItem.media.tracks) {
                            saveAudiobookToDB(libraryItem)
                            MyDownloadService.sendAddDownload(
                                this@ChapterListActivity,
                                track
                            )
                        }
                    }
                }) {
                    Icon(
                        tint = Color.Gray,
                        imageVector = when {
                            isDownloading -> Icons.Filled.Downloading
                            isDownloaded -> Icons.Filled.Delete
                            else -> Icons.Filled.Download
                        },
                        contentDescription = if (isDownloaded) "Delete" else "Download"
                    )
                }

                // Progress information (optimized for circular screen)
                if (isDownloading) {
                    val currentProgress = audiobookProgress
                    if (currentProgress != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Circular progress indicator (fits well on round screens)
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = (currentProgress.overallProgress / 100f).coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 3.dp,
                                    color = Color.Green
                                )
                                Text(
                                    text = if (currentProgress.overallProgress < 0.1f) "<1%" else "${currentProgress.overallProgress.toInt()}%",
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            // Speed and time remaining (compact for small screens)
                            if (currentProgress.averageDownloadSpeed > 0) {
                                Text(
                                    text = "${DownloadProgressCalculator.formatBytes(currentProgress.averageDownloadSpeed)}/s",
                                    fontSize = 7.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            if (currentProgress.estimatedTimeRemaining != Long.MAX_VALUE && currentProgress.estimatedTimeRemaining > 0) {
                                Text(
                                    text = DownloadProgressCalculator.formatTime(currentProgress.estimatedTimeRemaining),
                                    fontSize = 7.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        // Fallback to simple track count display
                        Column {
                            Text(text = "Downloading...", fontSize = 8.sp, color = Color.Gray)
                            Text(text = "$downloadedCount / $totalTracks", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                IconButton(onClick = {
                    viewModel.sync(libraryItem)
                }) {
                    Icon(
                        tint = if (!libraryItem.userProgress.toUpload || isSyncing) Color.Gray else Color.Yellow,
                        imageVector = if (isSyncing) Icons.Outlined.CloudSync else if (libraryItem.userProgress.toUpload) Icons.Outlined.CloudUpload else Icons.Filled.Done,
                        contentDescription = "Sync"
                    )
                }
            }
            
            PlayButton(libraryItem)
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f),
                thickness = 1.dp
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getItem(this@ChapterListActivity, itemId)
    }

    @Composable
    fun PlayButton(item: LibraryItem) {
        val buttonText = if (item.userProgress.currentTime > 10) "Continue" else "Start"

        Button(
            onClick = {
                saveAudiobookToDB(item)
                // Start the PlayerService
                PlayerService.setAudiobook(this, item, action = "continue")
                val intent = Intent(this@ChapterListActivity, PlayerActivity::class.java)
                startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF086409))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = buttonText, fontSize = 16.sp)
            }
        }
    }

    private fun saveAudiobookToDB(item: LibraryItem) {
        GlobalScope.launch {
            val db = (applicationContext as MainApp).database
            db.libraryItemDao().insertLibraryItem(item)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AudiobookInfoPreview() {
        val libraryItem = LibraryItem()
        libraryItem.title = "Some Random Title"
        AudiobookInfo(libraryItem)
    }

    @Preview(showBackground = true)
    @Composable
    fun ChapterPreview() {
        Chapter(LibraryItem(), Chapter(0, 120.0, 260.0, "Chapter 1"))
    }

    @Composable
    private fun ManualLoadView(item: LibraryItem?, itemId: String) {
        val isLoading by viewModel.isLoading.collectAsState()

        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(
                    startAngle = 0f,
                    modifier = Modifier
                        .width(80.dp)
                        .height(80.dp),
                    indicatorColor = androidx.wear.compose.material.MaterialTheme.colors.secondary,
                    trackColor = androidx.wear.compose.material.MaterialTheme.colors.onBackground.copy(
                        alpha = 0.1f
                    ),
                    strokeWidth = 8.dp
                )
            }
        }

        if (item?.id!!.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center, modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text(
                        text = "There was some problem. Try again.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = {
                        viewModel.getItem(this@ChapterListActivity, itemId)
                    }) {
                        Text(text = "LOAD")
                    }
                }
            }

        }
    }

    @Composable
    private fun Chapter(audiobook: LibraryItem, track: Chapter) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                saveAudiobookToDB(audiobook)
                // Start the PlayerService
                PlayerService.setAudiobook(this, audiobook, track.start)
                val intent = Intent(this@ChapterListActivity, PlayerActivity::class.java)
                startActivity(intent)
            }
            .padding(16.dp)) {
            Text(
                text = track.title,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
            )
//            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = timeToString(track.start),
                fontSize = 10.sp,
                color = if (track.start > audiobook.userProgress.currentTime) Color.Green else
                    if (track.end > audiobook.userProgress.currentTime) Color.Cyan else Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    private fun timeToString(seconds: Double): String {
        val totalSeconds = floor(seconds).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        val timeString = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
        return timeString
    }
}