package kaf.audiobookshelfwearos.app.activities

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaf.audiobookshelfwearos.app.services.PlayerService
import kotlinx.coroutines.delay

class PlayerActivity : ComponentActivity() {
    private var playerService: PlayerService? = null
    private var isBound = false
    private lateinit var trackEndedReceiver: BroadcastReceiver

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            isBound = true
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
    fun PlaybackControls() {
        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableStateOf(0L) }
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            while (true) {
                if (isBound) {
                    currentPosition = playerService?.getCurrentPosition() ?: 0L
                }
                delay(1000)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Media info", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Playback position: ${currentPosition / 1000} sec", color = Color.White)
        }

        DisposableEffect(Unit) {
            val trackEndedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        "$packageName.ACTION_PLAYING" -> {
                            isPlaying = true // Update the UI state
                        }
                        "$packageName.ACTION_PAUSED" -> {
                            isPlaying = false // Update the UI state
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction("$packageName.ACTION_PLAYING")
                addAction("$packageName.ACTION_PAUSED")
            }
            this@PlayerActivity.registerReceiver(trackEndedReceiver, filter)

            onDispose {
                this@PlayerActivity.unregisterReceiver(trackEndedReceiver)
            }
        }

    }


    @Preview(showBackground = true)
    @Composable
    fun PlaybackControlsPreview() {
        PlaybackControls()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        isBound = false
    }
}