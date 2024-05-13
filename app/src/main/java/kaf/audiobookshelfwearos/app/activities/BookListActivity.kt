package kaf.audiobookshelfwearos.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import kaf.audiobookshelfwearos.app.ApiHandler
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
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    viewModel.getLibraries()
                }) {
                    Text(text = "TEST")
                }

                for (library in libraries!!) {
                    Text(text = "Library: ${library.name}")
                }
            }
        }
    }

    @Composable
    fun BookCard(title: String, author: String) {
        Column {
            Text(text = title)
            Text(text = author)
        }
    }

    @Preview
    @Composable
    fun PreviewBookCard() {
        BookCard(title = "Book of the year", author = "Tom Jones")
    }
}