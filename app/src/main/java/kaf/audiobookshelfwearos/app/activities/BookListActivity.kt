package kaf.audiobookshelfwearos.app.activities

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
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

        viewModel.loginResult.observe(
            this
        ) { user ->
            Timber.d(user.token)
        }

        viewModel.getLibraries(this, true)

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
                        viewModel.getLibraries(this@BookListActivity, true)
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
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                libraries?.let {
                    for ((libIndex, library) in libraries.withIndex()) {
                        itemsIndexed(library.libraryItems) { index, item ->
                            Column {
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
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    private fun CoverImage(itemId: String) {
        val coverUrls by viewModel.coverImages.observeAsState()
        viewModel.getCoverImage(itemId)

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