package kaf.audiobookshelfwearos.app

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.LibraryItem
import kaf.audiobookshelfwearos.app.data.User
import kaf.audiobookshelfwearos.app.data.UserMediaProgress
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException


class ApiHandler(private val context: Context) {
    private var client = OkHttpClient()
    private var jacksonMapper =
        ObjectMapper().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
    private val userDataManager = UserDataManager(context)
    private val db = (context.applicationContext as MainApp).database

    private fun getRequest(endPoint: String) = Request.Builder()
        .url(userDataManager.getCompleteAddress() + endPoint)
        .addHeader("Authorization", "Bearer ${userDataManager.token}")
        .build()

    suspend fun getAllLibraries(): List<Library> {
        return withContext(Dispatchers.IO) {
            val request = getRequest("/api/libraries")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) showToast(response.code.toString())
                val responseBody = response.body?.string()
                // Extract token from the JSON response
                val jsonResponse = responseBody?.let { JSONObject(it) }
                val libraries = jsonResponse?.getJSONArray("libraries")
                return@use jacksonMapper.readValue(libraries.toString())
            }
        }

    }

    suspend fun getCover(id: String): Bitmap {
        return withContext(Dispatchers.IO) {
            val request = getRequest("/api/items/$id/cover")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) showToast(response.code.toString())
                return@use BitmapFactory.decodeStream(response.body!!.byteStream())
            }
        }
    }

    suspend fun getItem(id: String): LibraryItem {
        return withContext(Dispatchers.IO) {
            val request = getRequest("/api/items/$id?expanded=1&include=progress")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) showToast(response.code.toString())
                val responseBody = response.body?.string()
                val libraryItem = jacksonMapper.readValue<LibraryItem>(responseBody.toString())
                val localLibraryItem = db.libraryItemDao().getLibraryItemById(id)
                localLibraryItem?.let {
                    if (it.userMediaProgress.lastUpdate > libraryItem.userMediaProgress.lastUpdate)
                        libraryItem.userMediaProgress = localLibraryItem.userMediaProgress
                }

                return@use libraryItem
            }
        }
    }

    suspend fun getLibraryItems(id: String): List<LibraryItem> {
        return withContext(Dispatchers.IO) {
            val request = getRequest("/api/libraries/$id/items?sort=updatedAt")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) showToast(response.code.toString())
                val responseBody = response.body?.string()
                // Extract token from the JSON response
                val jsonResponse = responseBody?.let { JSONObject(it) }
                val results = jsonResponse?.getJSONArray("results")
                Timber.d(results?.length().toString())
                val items: List<LibraryItem> =
                    jacksonMapper.readValue<List<LibraryItem>>(results.toString()).reversed()
                return@use items
            }
        }
    }

    fun updateProgress(userMediaProgress: UserMediaProgress) {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)

        scope.launch {
            try {
                val serverItem = getItem(userMediaProgress.libraryItemId)

                if (serverItem.userMediaProgress.lastUpdate > userMediaProgress.lastUpdate) {
                    Timber.d("Progress on server is more recent. Not uploading")
                    userMediaProgress.toUpload = false
                    db.userMediaProgressDao().insertUserMediaProgress(userMediaProgress)
                    return@launch
                }

                Timber.d("Uploading progress...")
                uploadProgress(userMediaProgress)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update progress")
            } finally {
                job.cancel()
            }
        }
    }

    private suspend fun uploadProgress(userMediaProgress: UserMediaProgress) {
        val jsonBody = JSONObject().apply {
            put("currentTime", userMediaProgress.currentTime)
            put("lastUpdate", userMediaProgress.lastUpdate)
        }

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
        val url = userDataManager.getCompleteAddress() + "/api/me/progress/" + userMediaProgress.libraryItemId
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .addHeader("Authorization", "Bearer ${userDataManager.token}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.w("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        userMediaProgress.toUpload = false
                        CoroutineScope(Dispatchers.IO).launch {
                            db.userMediaProgressDao().insertUserMediaProgress(userMediaProgress)
                        }
                        Timber.d("Progress uploaded. Response: $responseBody")
                    } else {
                        Timber.w("Error: ${response.code}")
                    }
                }
            }
        })
    }

    suspend fun login(): User {
        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("username", userDataManager.login)
                put("password", userDataManager.password)
            }

            val requestBody =
                RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())

            val request = Request.Builder()
                .url(userDataManager.getCompleteAddress() + "/login")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) showToast(response.code.toString())
                val responseBody = response.body?.string()
                val jsonResponse = responseBody?.let { JSONObject(it) }
                val user = jsonResponse?.getJSONObject("user")
                return@use jacksonMapper.readValue<User>(user.toString())
            }
        }
    }

    private fun showToast(text: String) {
        if (context is Activity)
            context.runOnUiThread {
                Toast.makeText(
                    context,
                    text,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
