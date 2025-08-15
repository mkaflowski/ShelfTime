package kaf.audiobookshelfwearos.app.activities

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import coil.compose.AsyncImage
import kaf.audiobookshelfwearos.R
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
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

    private fun launchRemoteSearchInput(
        launcher: ActivityResultLauncher<Intent>,
        remoteInputs: List<RemoteInput>
    ) {
        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        launcher.launch(intent)
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
            val filteredLibraries by viewModel.filteredLibraries.collectAsState()
            val isSearchActive by viewModel.isSearchActive.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()

            val inputTextKey = "input_text"
            val remoteInputs: List<RemoteInput> = remember {
                listOf(
                    RemoteInput.Builder(inputTextKey)
                        .setLabel("Search")
                        .wearableExtender {
                            setEmojisAllowed(true)
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }.build()
                )
            }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.let { data ->
                    val resultsBundle: Bundle = RemoteInput.getResultsFromIntent(data)
                    val newInputText: CharSequence? = resultsBundle.getCharSequence(inputTextKey)
                    val userInput = newInputText?.toString() ?: ""
                    viewModel.updateSearchQuery(userInput)
                }
            }
            
            val displayLibraries = if (isSearchActive) filteredLibraries else libraries
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Search header at the top
                SearchHeader(
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchToggle = {
                        if (!isSearchActive) {
                            launchRemoteSearchInput(launcher, remoteInputs)
                        }
                        viewModel.toggleSearch()
                    }
                )
                
                // Main content
                ManualLoadView(displayLibraries, isSearchActive)
                Libraries(displayLibraries)
            }

        }
    }

    @Composable
    private fun SearchHeader(
        isSearchActive: Boolean,
        searchQuery: String,
        onSearchToggle: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSearchActive) 72.dp else 48.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSearchActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // Back button centered
                    Button(
                        onClick = onSearchToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    // Search query display below the button
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Search: $searchQuery" else "Enter search...",
                        style = MaterialTheme.typography.body2,
                        color = if (searchQuery.isNotEmpty())
                            MaterialTheme.colors.onSurface
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            } else {
                // Search button centered
                Button(
                    onClick = onSearchToggle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            }
        }
    }

    @Composable
    private fun ManualLoadView(libraries: List<Library>?, isSearchActive: Boolean = false) {
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
                        text = if (isSearchActive) "No results found" else "There was some problem. Try again.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!isSearchActive) {
                        Button(onClick = {
                            viewModel.getLibraries(this@BookListActivity, true, UserDataManager(this@BookListActivity).offlineMode)
                        }) {
                            Text(text = "LOAD")
                        }
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
                                Timber.d(item.title)
                                BookItem(item)
                                val showDivider =
                                    (index != library.libraryItems.size - 1 || libIndex != libraries.size - 1)
                                if (showDivider) {
                                    HorizontalDivider()
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
