package kaf.audiobookshelfwearos.app.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun login() {
        viewModelScope.launch {
            val user = apiHandler.login()
            if (user.token.isNotEmpty())
                _loginResult.postValue(user)
        }
    }

    fun setShowErrorTaosts(show: Boolean) {
        apiHandler.shouldShowErrorToast = show
    }

    fun getCoverImage(itemId: String, context: Context) {
        val currentImages = _coverImages.value ?: mapOf()

        if (currentImages.containsKey(itemId)) {
            _coverImages.postValue(currentImages)
            return  // If the image is already loaded, do nothing.
        }

        val cachedCover = loadBitmapFromCache(context,itemId)
        if(cachedCover!=null){
            val updatedImages = currentImages.toMutableMap()
            updatedImages[itemId] = cachedCover
            _coverImages.postValue(updatedImages)
            return
        }

        viewModelScope.launch {
            val bitmap = apiHandler.getCover(itemId)
            bitmap?.let {
                // Post new state with updated image.
                saveBitmapToCache(context, bitmap, itemId)
                val updatedImages = currentImages.toMutableMap()
                updatedImages[itemId] = it
                _coverImages.postValue(updatedImages)
            }
        }
    }

    private fun loadBitmapFromCache(context: Context, filename: String): Bitmap? {
        val file = File(context.cacheDir, "$filename.jpg")
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String): File? {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "$filename.jpg")

        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return file
    }


    fun getItem(context: Context, itemId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val db = (context.applicationContext as MainApp).database
            val libraryItem = db.libraryItemDao().getLibraryItemById(itemId)
            libraryItem?.let {
                _isLoading.value = false
                _item.postValue(libraryItem)
            }

            val item = apiHandler.getItem(itemId)
            item?.let {
                if (libraryItem == null || libraryItem.userProgress.lastUpdate <= item.userProgress.lastUpdate) {
                    Timber.d("Post server version")
                    libraryItem?.let {
                        if (libraryItem.userProgress.lastUpdate >= item.userProgress.lastUpdate) {
                            Timber.d("Local progress is newer or the same")
                            item.userProgress = libraryItem.userProgress
                        }
                    }
                    _isLoading.value = false
                    _item.postValue(item)
                }
            }
        }
    }

    fun sync(item: LibraryItem) {
        _isSyncing.value = true
        viewModelScope.launch {
            val newItem =
                item.copy(userMediaProgress = item.userProgress.copy(toUpload = !item.userProgress.toUpload))
            val updated = apiHandler.updateProgress(newItem.userProgress)
            if (updated) {
                _item.postValue(newItem)
            }
            _isSyncing.value = false
        }
    }

    fun getLibraries(
        context: Context,
        includeLocalProgress: Boolean = true,
        onlyDownloaded: Boolean = false
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            var localItems = listOf<LibraryItem>()
            var allLibraries = arrayListOf<Library>()
            if (includeLocalProgress) {
                val db = (context.applicationContext as MainApp).database
                localItems = db.libraryItemDao().getAllLibraryItems()
                val localLibrary = Library(libraryItems = localItems.toCollection(ArrayList()))
                if (onlyDownloaded) {
                    localLibrary.libraryItems.retainAll { it.isDownloaded(context) }
                }
                allLibraries.add(localLibrary)
                _libraries.postValue(listOf(localLibrary))
                _isLoading.value = false
            }

            val res = loadLibraries(onlyDownloaded, context)
            for (library in res) {
                library.libraryItems.removeAll { item2 -> localItems.any { item1 -> item1.id == item2.id } }
                allLibraries.add(library)
            }
            _isLoading.value = false
            _libraries.postValue(allLibraries)
        }
    }

    private suspend fun loadLibraries(onlyDownloaded: Boolean, context: Context) = coroutineScope {
        val libraries = apiHandler.getAllLibraries()
        Timber.d("onlyDownloaded $onlyDownloaded")

        val deferredLibraries = libraries.map { library ->
            async {
                Timber.d("onlyDownloaded $onlyDownloaded")
                val libraryItems = apiHandler.getLibraryItems(library.id)
                val filteredItems = if (onlyDownloaded) {
                    libraryItems.filter { it.isDownloaded(context) }
                } else libraryItems
                library.libraryItems.addAll(filteredItems)
                library
            }
        }

        deferredLibraries.awaitAll()
    }

    class ApiViewModelFactory(private val apiHandler: ApiHandler) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApiViewModel(apiHandler = apiHandler) as T
        }
    }
}