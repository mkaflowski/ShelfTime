package kaf.audiobookshelfwearos.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import coil.compose.AsyncImage
import kaf.audiobookshelfwearos.R
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class BookListActivity : ComponentActivity() {
    private val viewModel: ApiViewModel by viewModels {
        ApiViewModel.ApiViewModelFactory(
            ApiHandler(
                this
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on while this activity is in the foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.loginResult.observe(
            this
        ) { user ->
            Timber.d(user.token)
        }

        viewModel.getLibraries(this, true, UserDataManager(this).offlineMode)

        setContent {
            val libraries by viewModel.libraries.observeAsState()

            ManualLoadView(libraries)
            Libraries(libraries)

        }
    }

    @Composable
    private fun ManualLoadView(libraries: List<Library>?) {
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
                    indicatorColor = MaterialTheme.colors.secondary,
                    trackColor = MaterialTheme.colors.onBackground.copy(
                        alpha = 0.1f
                    ),
                    strokeWidth = 8.dp
                )
            }
        }

        if (libraries?.isEmpty() == true && !isLoading) {
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
                        viewModel.getLibraries(this@BookListActivity, true, UserDataManager(this@BookListActivity).offlineMode)
                    }) {
                        Text(text = "LOAD")
                    }
                }
            }

        }
    }

    @Composable
    private fun Libraries(libraries: List<Library>?) {
        val scalingLazyListState = rememberScalingLazyListState(0)
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        val scrollSpeedMultiplier = 3.0f // Adjust this value to control scroll speed

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
                modifier = Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent() {
                        coroutineScope.launch {
                            scalingLazyListState.scrollBy(it.verticalScrollPixels * scrollSpeedMultiplier)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                libraries?.let {
                    for ((libIndex, library) in libraries.withIndex()) {
                        itemsIndexed(library.libraryItems) { index, item ->
                            Column {
                                Timber.d(item.title)
                                BookItem(item)
                                val showDivider =
                                    (index != library.libraryItems.size - 1 || libIndex != libraries.size - 1)
                                if (showDivider) {
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    @Composable
    private fun BookItem(item: LibraryItem) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(this, ChapterListActivity::class.java).apply {
                    putExtra(
                        "id",
                        item.id
                    )
                }
                startActivity(intent)
            }
            .padding(16.dp)) {
            CoverImage(itemId = item.id)
            Text(
                text = item.title,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
            )
            Text(
                text = item.author,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    private fun CoverImage(itemId: String) {
        val coverUrls by viewModel.coverImages.observeAsState()
        viewModel.getCoverImage(itemId, this)

        AsyncImage(
            model = coverUrls?.get(itemId) ?: "",
            contentDescription = null,
            placeholder = painterResource(R.drawable.placeholder),
            error = painterResource(R.drawable.placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(100.dp) // Adjusted height for better display
        )
    }
}
