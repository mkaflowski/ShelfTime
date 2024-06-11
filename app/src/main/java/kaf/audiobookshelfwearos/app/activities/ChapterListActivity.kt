package kaf.audiobookshelfwearos.app.activities

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                                if (index != media.chapters.size - 1) {
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


        LaunchedEffect(isDownloading) {
            while (isDownloading) {
                delay(1000L)
                isDownloading = libraryItem.media.tracks.any { track ->
                    track.isDownloading(this@ChapterListActivity)
                }
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
                IconButton(onClick = {
                    if (isDownloaded) {
                        isDownloaded = false
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
                        isDownloading = true
                    }
                }) {
                    Icon(
                        tint = Color.Gray,
                        imageVector = if (isDownloading) Icons.Filled.Downloading else (if (isDownloaded) Icons.Filled.Delete else Icons.Filled.Download),
                        contentDescription = if (isDownloaded) "Download" else "Delete"
                    )
                }

                IconButton(onClick = {
                    viewModel.sync(libraryItem)
                }) {
                    Icon(
                        tint = if (!libraryItem.userMediaProgress.toUpload || isSyncing) Color.Gray else Color.Yellow,
                        imageVector = if (isSyncing) Icons.Outlined.CloudSync else if (libraryItem.userMediaProgress.toUpload) Icons.Outlined.CloudUpload else Icons.Filled.Done,
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
        Button(
            onClick = {
                saveAudiobookToDB(item)
                // Start the PlayerService
                PlayerService.setAudiobook(this, item)
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
                Text(text = "Continue", fontSize = 16.sp)
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
                color = if (track.start > audiobook.userMediaProgress.currentTime) Color.Green else
                    if (track.end > audiobook.userMediaProgress.currentTime) Color.Cyan else Color.Gray,
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