package kaf.audiobookshelfwearos.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.data.Track
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ManualLoadView(item, itemId)
            }

            if (item?.id?.isNotEmpty() == true)
                item?.run {
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp)
                    ) {
                        item {
                            Text(
                                text = title, textAlign = TextAlign.Center, modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f),
                                thickness = 1.dp
                            )
                        }

                        item {
                            Text(
                                textAlign = TextAlign.Center,
                                text = "Chapters:", modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                            )
                        }

                        itemsIndexed(media.tracks) { index, _ ->
                            Track(media.tracks[index])
                            if (index != media.tracks.size - 1) {
                                Divider()
                            }
                        }

                    }
                }
        }
    }

    @Composable
    private fun ManualLoadView(item: LibraryItem?, itemId: String) {
        if (item?.id!!.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "There was some problem. Try again.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = {
                        viewModel.getItem(itemId)
                        Timber.d("click!")
                    }) {
                        Text(text = "LOAD")
                    }
                }
            }
        }
    }

    @Composable
    private fun Track(track: Track) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Timber
                    .tag("BookItem")
                    .d(track.title)
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
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = timeToString(track.startOffset),
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