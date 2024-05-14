package kaf.audiobookshelfwearos.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userDataManager = UserDataManager(this)

        viewModel.loginResult.observe(
            this
        ) { user ->
            Timber.d(user.token)
        }

        setContent {
            val libraries by viewModel.libraries.observeAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (libraries!!.isEmpty())
                    Button(onClick = {
                        viewModel.getLibraries()
                    }) {
                        Text(text = "LOAD")
                    }

            }
            for (library in libraries!!) {
                Library(library)
            }
        }
    }

    @Composable
    private fun Library(library: Library) {
//        Text(text = "Library: ${library.name}")
        LazyColumn(
            Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(library.libraryItems) { index, d ->
                BookItem(library.libraryItems[index])
                if (index != library.libraryItems.size - 1) {
                    Divider()
                }
            }
        }
    }

    @Composable
    private fun BookItem(item: LibraryItem) {
        CoverImage(itemId = item.id)
        Text(
            text = item.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f), thickness = 1.dp
        )
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