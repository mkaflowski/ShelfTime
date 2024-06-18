package kaf.audiobookshelfwearos.app.activities

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import kaf.audiobookshelfwearos.app.services.PlayerService
import kotlinx.coroutines.delay
import timber.log.Timber

class PlayerActivity : ComponentActivity() {
    private var playerService: PlayerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            isBound = true
            playerService!!.updateUIMetadata()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the PlayerService
        val intent = Intent(this, PlayerService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            PlaybackControls()
        }
    }

    @Composable
    fun PlaybackControls(isPreview: Boolean = false) {
        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var chapterTitle by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            while (true) {
                if (isBound) {
                    currentPosition = playerService?.getCurrentPosition() ?: 0L
                    playerService?.getDuration()?.let {
                        if (it > 0)
                            duration = it
                    }
                }
                delay(1000)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chapterTitle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val intent = Intent(this@PlayerActivity, PlayerService::class.java)
                        intent.action = "ACTION_REWIND"
                        startService(intent)
                    }) {
                        Icon(
                            tint = Color.White,
                            imageVector = Icons.Filled.FastRewind,
                            contentDescription = "Rewind"
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        var progressBar = 0f
                        if (duration > 0)
                            progressBar = currentPosition.toFloat() / duration
                        Timber.d("duration $duration")
                        Timber.d("currentPosition $currentPosition")
                        Timber.d("progressBar $progressBar")
                        CircularProgressIndicator(
                            progress = progressBar,
                            startAngle = 0f,
                            indicatorColor = MaterialTheme.colors.secondary,
                            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                            strokeWidth = 3.dp
                        )
                        IconButton(onClick = {
                            val intent = Intent(this@PlayerActivity, PlayerService::class.java)
                            intent.action = "ACTION_PLAY_PAUSE"
                            startService(intent)
                            isPlaying = !isPlaying
                        }) {
                            Icon(
                                tint = Color.White,
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                        }
                    }

                    IconButton(onClick = {
                        val intent = Intent(this@PlayerActivity, PlayerService::class.java)
                        intent.action = "ACTION_FAST_FORWARD"
                        startService(intent)
                    }) {
                        Icon(
                            tint = Color.White,
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = "Fast Forward"
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 10.dp)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${timeToString(currentPosition / 1000)} / ${timeToString(duration / 1000)}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }

        if (!isPreview)
            DisposableEffect(Unit) {
                val playerReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            "$packageName.ACTION_PLAYING" -> {
                                isPlaying = true // Update the UI state
                            }

                            "$packageName.ACTION_PAUSED" -> {
                                isPlaying = false // Update the UI state
                            }

                            "$packageName.ACTION_UPDATE_METADATA" -> {
                                intent.getStringExtra("CHAPTER_TITLE")?.let {
                                    chapterTitle = it
                                    Timber.d("chapterTitle = " + chapterTitle)
                                }
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction("$packageName.ACTION_PLAYING")
                    addAction("$packageName.ACTION_PAUSED")
                    addAction("$packageName.ACTION_UPDATE_METADATA")
                }
                this@PlayerActivity.registerReceiver(playerReceiver, filter)
                playerService?.updateUIMetadata()

                onDispose {
                    this@PlayerActivity.unregisterReceiver(playerReceiver)
                }
            }

    }

    @Preview(showBackground = true, widthDp = 250, heightDp = 250)
    @Composable
    fun PlaybackControlsPreview() {
        PlaybackControls(isPreview = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        isBound = false
    }

    private fun timeToString(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        val timeString = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
        return timeString
    }
}