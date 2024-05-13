package kaf.audiobookshelfwearos.app

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kaf.audiobookshelfwearos.app.data.Library
import kaf.audiobookshelfwearos.app.data.User
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
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

    fun getAllLibraries(callback : (List<Library>) -> Unit) {
        val request = getRequest("/api/libraries")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.message?.let { showToast(it) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    // Extract token from the JSON response
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val libraries = jsonResponse?.getJSONArray("libraries")
                    val librariesMapped: List<Library> = jacksonMapper.readValue(libraries.toString())
                    callback(librariesMapped)
                } else {
                    showToast(response.code.toString())
                }
            }
        })
    }

    fun login(callback: (User) -> Unit) {
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.tag("Login").e("Failed to login: %s", e.message)
                showToast("Failed to login: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Timber.tag("Login").d("Login successful: %s", responseBody)

                    // Extract token from the JSON response
                    val jsonResponse = JSONObject(responseBody)
                    val user = jsonResponse.getJSONObject("user")
                    val userMapped: User =
                        jacksonMapper.readValue(user.toString())
                    callback(userMapped)

                    // Handle successful login
                } else {
                    Timber.tag("Login").e("Login failed: %s", response.code)
                    showToast("Login error: ${response.code}")
                }
            }
        })
    }

    private fun showToast(text: String) {
        if (context is Activity)
            (context as Activity).runOnUiThread {
                Toast.makeText(
                    context,
                    text,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
