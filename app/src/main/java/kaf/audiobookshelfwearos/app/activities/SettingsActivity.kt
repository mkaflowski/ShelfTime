package kaf.audiobookshelfwearos.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import kaf.audiobookshelfwearos.app.theme.AudiobookshelfWearOSTheme
import kaf.audiobookshelfwearos.app.userdata.UserDataManager

class SettingsActivity : ComponentActivity() {
    private lateinit var userDataManager: UserDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userDataManager = UserDataManager(this)

        setContent {
            AudiobookshelfWearOSTheme {
                SettingsScreen()
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val listState = rememberScalingLazyListState()
        var smartDeleteEnabled by remember { mutableStateOf(userDataManager.smartDeleteEnabled) }
        var maxDownloads by remember { mutableStateOf(userDataManager.smartDeleteMaxDownloads) }

        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                ToggleChip(
                    checked = smartDeleteEnabled,
                    onCheckedChange = { enabled ->
                        smartDeleteEnabled = enabled
                        userDataManager.smartDeleteEnabled = enabled
                    },
                    label = {
                        Text("Smart Delete")
                    },
                    toggleControl = {
                        Switch(
                            checked = smartDeleteEnabled,
                            onCheckedChange = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (smartDeleteEnabled) {
                item {
                    Text(
                        text = "Old audiobooks will be deleted once this amount is reached:",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Text(
                        text = "Maximum Downloads",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        // Minus button
                        Button(
                            onClick = {
                                if (maxDownloads > 1) {
                                    maxDownloads--
                                    userDataManager.smartDeleteMaxDownloads = maxDownloads
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            enabled = maxDownloads > 1
                        ) {
                            Text("-")
                        }

                        // Current value
                        Text(
                            text = "$maxDownloads",
                            style = MaterialTheme.typography.title1,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )

                        // Plus button
                        Button(
                            onClick = {
                                if (maxDownloads < 20) {
                                    maxDownloads++
                                    userDataManager.smartDeleteMaxDownloads = maxDownloads
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            enabled = maxDownloads < 20
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }
    }
}
