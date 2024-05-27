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
import kotlinx.coroutines.Dispatchers
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
                return@use jacksonMapper.readValue<LibraryItem>(responseBody.toString())
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
         val jsonBody= JSONObject()
         jsonBody.put("currentTime", userMediaProgress.currentTime)
         jsonBody.put("lastUpdate", userMediaProgress.lastUpdate)

         val requestBody =
             RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())

         val url =
             userDataManager.getCompleteAddress() + "/api/me/progress/" + userMediaProgress.libraryItemId

         Timber.d(url)
         val request = Request.Builder()
             .url(url)
             .patch(requestBody)
             .addHeader("Authorization", "Bearer ${userDataManager.token}")
             .build()

         // Make the network call asynchronously
         client.newCall(request).enqueue(object : Callback {
             override fun onFailure(call: Call, e: IOException) {
                 // Handle failure
                 e.printStackTrace()
             }

             override fun onResponse(call: Call, response: Response) {
                 // Handle success
                 if (response.isSuccessful) {
                     val responseBody = response.body?.string()
                     println("Response: $responseBody")
                 } else {
                     println("Error: ${response.code}")
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
