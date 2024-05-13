package kaf.audiobookshelfwearos.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.User

class ApiViewModel(private val apiHandler: ApiHandler) : ViewModel() {
    private val _loginResult = MutableLiveData<User>()
    val loginResult: LiveData<User> = _loginResult

    private val _libraries = MutableLiveData<List<Library>>(listOf())
    val libraries: LiveData<List<Library>> = _libraries

    fun login() {
        apiHandler.login { _loginResult.postValue(it) }
    }

    fun getLibraries(){
        apiHandler.getAllLibraries { _libraries.postValue(it) }
    }

    class ApiViewModelFactory(private val apiHandler: ApiHandler) :ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApiViewModel(apiHandler = apiHandler) as T
        }
    }
}