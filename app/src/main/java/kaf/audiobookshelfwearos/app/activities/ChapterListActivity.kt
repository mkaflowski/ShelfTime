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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.data.Chapter
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.services.MyDownloadService
import kaf.audiobookshelfwearos.app.services.PlayerService
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.floor

class ChapterListActivity : ComponentActivity() {
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

        val itemId = intent.getStringExtra("id") ?: ""

        Timber.i("itemId = $itemId")

        setContent {
            val item by viewModel.item.observeAsState()


            ManualLoadView(item, itemId)

            if (item?.id?.isNotEmpty() == true)
                item?.run {
                    LazyColumn(
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

    @Composable
    private fun AudiobookInfo(
        libraryItem: LibraryItem,
    ) {
        var isDownloaded by remember {
            mutableStateOf(
                libraryItem.media.tracks.all { track -> track.isDownloaded(this) }
            )
        }

        Column {
            Text(
                text = libraryItem.title, textAlign = TextAlign.Center, modifier = Modifier
                    .padding(top = 10.dp)
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
                        isDownloaded = true
                        Toast.makeText(
                            this@ChapterListActivity,
                            "Downloading started",
                            Toast.LENGTH_SHORT
                        ).show()
                        for (track in libraryItem.media.tracks) {
                            MyDownloadService.sendAddDownload(
                                this@ChapterListActivity,
                                track
                            )
                        }
                    }
                }) {
                    Icon(
                        tint = Color.Gray,
                        imageVector = if (isDownloaded) Icons.Filled.Delete else Icons.Filled.Download,
                        contentDescription = if (isDownloaded) "Download" else "Delete"
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

            val libraryItem = db.libraryItemDao().getLibraryItemById(item.id)
            Timber
                .tag("BookItem")
                .d(libraryItem?.title)
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
        if (item?.id!!.isEmpty()) {

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
                        viewModel.getItem(itemId)
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
                color = Color.Green,
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