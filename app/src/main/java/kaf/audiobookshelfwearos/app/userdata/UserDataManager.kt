package kaf.audiobookshelfwearos.app.userdata

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class UserDataManager(context: Context) {

    companion object {
        private const val PREF_NAME = "user_data"
        private const val KEY_SPEED = "speed"
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_LOGIN = "login"
        private const val KEY_PASSWORD = "password"
        private const val KEY_PROTOCOL = "protocol"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERID = "userid"
        private const val KEY_OFFLINEMODE = "offlinemode"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var serverAddress: String
        get() = sharedPreferences.getString(KEY_SERVER_ADDRESS, null) ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_SERVER_ADDRESS, value).apply()

    var login: String
        get() = sharedPreferences.getString(KEY_LOGIN, null) ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_LOGIN, value).apply()

    var password: String
        get() = sharedPreferences.getString(KEY_PASSWORD, null) ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_PASSWORD, value).apply()

    var userId: String
        get() = sharedPreferences.getString(KEY_USERID, null) ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_USERID, value).apply()

    var token: String
        get() = sharedPreferences.getString(KEY_TOKEN, null) ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_TOKEN, value).apply()

    var speed: Float
        get() = sharedPreferences.getFloat(KEY_SPEED, 1f)
        set(value) = sharedPreferences.edit().putFloat(KEY_SPEED, value).apply()

    var protocol: String
        get() = sharedPreferences.getString(KEY_PROTOCOL, null) ?: "https"
        set(value) = sharedPreferences.edit().putString(KEY_PROTOCOL, value).apply()

    var offlineMode: Boolean
        get() = sharedPreferences.getBoolean(KEY_OFFLINEMODE, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_OFFLINEMODE, value).apply()

    fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }

    fun getCompleteAddress(): String {
        return "$protocol://$serverAddress"
    }
}