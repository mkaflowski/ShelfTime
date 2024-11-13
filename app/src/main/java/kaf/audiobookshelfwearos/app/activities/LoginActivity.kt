package kaf.audiobookshelfwearos.app.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kaf.audiobookshelfwearos.R
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
import kaf.audiobookshelfwearos.databinding.ActivityLoginBinding
import timber.log.Timber
import java.net.UnknownHostException
import kotlin.reflect.KMutableProperty1


class LoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var protocol: EditText
    private lateinit var server: EditText
    private lateinit var login: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var userDataManager: UserDataManager
    private lateinit var offlineModeCheckBox: CheckBox

    private val viewModel: ApiViewModel by viewModels {
        ApiViewModel.ApiViewModelFactory(
            ApiHandler(
                this
            )
        )
    }

    private val map: Map<EditText, KMutableProperty1<UserDataManager, String>>
        get() {
            return mapOf(
                protocol to UserDataManager::protocol,
                server to UserDataManager::serverAddress,
                login to UserDataManager::login,
                password to UserDataManager::password
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        userDataManager = UserDataManager(this)
        viewModel.setShowErrorTaosts(!userDataManager.offlineMode)

        if (userDataManager.token.isNotEmpty()) {
            viewModel.login()
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        protocol = findViewById(R.id.protocol)
        server = findViewById(R.id.server)
        login = findViewById(R.id.login)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.buttonLogin)
        offlineModeCheckBox = findViewById(R.id.offlinemode)

        setSavedValues()

        val editTextMap = map

        // Listen for changes in EditText and save values to UserDataManager
        editTextMap.forEach { (editText, property) ->

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    property.set(userDataManager, editText.text.toString())
                }
            })
        }

        offlineModeCheckBox.isChecked = userDataManager.offlineMode
        offlineModeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            userDataManager.offlineMode = isChecked
        }

        viewModel.loginResult.observe(
            this
        ) { user ->
            Timber.d(user.token)
            userDataManager.token = user.token
            startActivity(Intent(this, BookListActivity::class.java))
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            Timber.d("Login clicked")
            Timber.d("isInternetAvailable() = " + isInternetAvailable())
            if (userDataManager.token.isNotEmpty() && !isInternetAvailable()) {
                startActivity(Intent(this, BookListActivity::class.java))
                startActivity(intent)
            } else viewModel.login()
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.LOCATION_HARDWARE)) requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
    }

    private fun isInternetAvailable(): Boolean {
        if (userDataManager.offlineMode)
            return false
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("" + actNw.transportInfo)
        }
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> false
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun setSavedValues() {
        userDataManager.apply {
            protocol.takeIf { it.isNotEmpty() }?.let { this@LoginActivity.protocol.setText(it) }
            serverAddress.takeIf { it.isNotEmpty() }?.let { this@LoginActivity.server.setText(it) }
            login.takeIf { it.isNotEmpty() }?.let { this@LoginActivity.login.setText(it) }
            password.takeIf { it.isNotEmpty() }?.let { this@LoginActivity.password.setText(it) }
        }
    }
}
