package kaf.audiobookshelfwearos.app.viewmodels

import android.graphics.Bitmap
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.data.User
import kotlinx.coroutines.launch

class ApiViewModel(private val apiHandler: ApiHandler) : ViewModel() {
    private val _loginResult = MutableLiveData<User>()
    val loginResult: LiveData<User> = _loginResult

    private val _libraries = MutableLiveData<List<Library>>(listOf())
    val libraries: LiveData<List<Library>> = _libraries

    private val _item = MutableLiveData(LibraryItem())
    val item: LiveData<LibraryItem> = _item

    private val _coverImages = MutableLiveData<Map<String, Bitmap>>()
    val coverImages: LiveData<Map<String, Bitmap>> get() = _coverImages

    fun login() {
        viewModelScope.launch{
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

    fun getItem(itemId : String){
        viewModelScope.launch{
            val item = apiHandler.getItem(itemId)
            _item.postValue(item)
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