package kaf.audiobookshelfwearos.app.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.MainApp
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ApiViewModel(private val apiHandler: ApiHandler) : ViewModel() {
    private val _loginResult = MutableLiveData<User>()
    val loginResult: LiveData<User> = _loginResult

    private val _libraries = MutableLiveData<List<Library>>(listOf())
    val libraries: LiveData<List<Library>> = _libraries

    private val _item = MutableLiveData(LibraryItem())
    val item: LiveData<LibraryItem> = _item

    private val _coverImages = MutableLiveData<Map<String, Bitmap>>()
    val coverImages: LiveData<Map<String, Bitmap>> get() = _coverImages

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    fun login() {
        viewModelScope.launch {
            val user = apiHandler.login()
            _loginResult.postValue(user)
        }
    }

    fun getCoverImage(itemId: String) {
        val currentImages = _coverImages.value ?: mapOf()

        if (currentImages.containsKey(itemId)) {
            _coverImages.postValue(currentImages)
            return  // If the image is already loaded, do nothing.
        }

        viewModelScope.launch {
            val bitmap = apiHandler.getCover(itemId)
            bitmap.let {
                // Post new state with updated image.
                val updatedImages = currentImages.toMutableMap()
                updatedImages[itemId] = it
                _coverImages.postValue(updatedImages)
            }
        }
    }


    fun getItem(context: Context, itemId: String) {
        viewModelScope.launch {
            val db = (context.applicationContext as MainApp).database
            val libraryItem = db.libraryItemDao().getLibraryItemById(itemId)
            libraryItem?.let {
                _item.postValue(libraryItem)
            }

            val item = apiHandler.getItem(itemId)
            if (libraryItem == null || libraryItem.userMediaProgress.lastUpdate <= item.userMediaProgress.lastUpdate) {
                Timber.d("Post server version")
                libraryItem?.let {
                    if (libraryItem.userMediaProgress.lastUpdate >= item.userMediaProgress.lastUpdate) {
                        Timber.d("Local progress is newer or the same")
                        item.userMediaProgress = libraryItem.userMediaProgress
                    }
                }
                _item.postValue(item)
            }
        }
    }

    fun sync(item: LibraryItem) {
        _isSyncing.value = true
        viewModelScope.launch {
            val debugValue = item.userMediaProgress.toUpload
            val newItem = item.copy(userMediaProgress = item.userMediaProgress.copy(toUpload = !debugValue))
            val updated = apiHandler.updateProgress(newItem.userMediaProgress)
            Timber.d("updated = "+updated)
            if (updated) {
                Timber.w("toupload = "+newItem.userMediaProgress.toUpload)
                _item.postValue(newItem)
            }
            _isSyncing.value = false
        }
    }

    fun getLibraries() {
        viewModelScope.launch {
            val libraries = apiHandler.getAllLibraries()
            val totalLibraries = libraries.size
            var completedLibraries = 0

            for (library in libraries) {
                viewModelScope.launch {
                    val libraryItems = apiHandler.getLibraryItems(library.id)
                    library.libraryItems.addAll(libraryItems)
                    completedLibraries++

                    if (completedLibraries == totalLibraries) {
                        _libraries.postValue(libraries)
                    }
                }
            }

            // If there are no libraries, post the empty list immediately
            if (totalLibraries == 0) {
                _libraries.postValue(libraries)
            }
        }
    }


    class ApiViewModelFactory(private val apiHandler: ApiHandler) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApiViewModel(apiHandler = apiHandler) as T
        }
    }
}