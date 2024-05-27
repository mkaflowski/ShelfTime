package kaf.audiobookshelfwearos.app.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kaf.audiobookshelfwearos.R
import kaf.audiobookshelfwearos.databinding.ActivityLoginBinding
import kaf.audiobookshelfwearos.app.ApiHandler
import kaf.audiobookshelfwearos.app.userdata.UserDataManager
import kaf.audiobookshelfwearos.app.viewmodels.ApiViewModel
import timber.log.Timber
import kotlin.reflect.KMutableProperty1


class LoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var protocol: EditText
    private lateinit var server: EditText
    private lateinit var login: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var userDataManager: UserDataManager

    private val viewModel : ApiViewModel by viewModels { ApiViewModel.ApiViewModelFactory(ApiHandler(this))}

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
        if(userDataManager.token.isNotEmpty()){
            viewModel.login()
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        protocol = findViewById(R.id.protocol)
        server = findViewById(R.id.server)
        login = findViewById(R.id.login)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.buttonLogin)

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

        viewModel.loginResult.observe(this
        ) { user ->
            Timber.d(user.token)
            userDataManager.token = user.token
//            startActivity(Intent(this, BookListActivity::class.java))
            val intent = Intent(this, ChapterListActivity::class.java).apply {
                putExtra(
                    "id",
                    "5ba58a9e-b39b-4a3e-bd22-228be1c89499" //drive - 50c6c74b-dc71-4b32-8ba8-c5bfad56d6ee
                )
            }
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            viewModel.login()
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
