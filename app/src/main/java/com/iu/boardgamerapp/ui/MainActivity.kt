package com.iu.boardgamerapp.ui

import com.iu.boardgamerapp.ui.GameScheduleActivity
import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.components.MainScreen
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme
import androidx.core.content.ContextCompat
import com.iu.boardgamerapp.ui.HostRotationActivity
import com.iu.boardgamerapp.ui.MainViewModel
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val databaseHelper = AppDatabaseHelper(this)
        val userRepository = UserRepository(databaseHelper)
        MainViewModelFactory(userRepository, databaseHelper, this)
    }

    private lateinit var hostRotationActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Überprüfe, ob der Nutzer eingeloggt ist
        checkUserLoggedIn()

        // Initialisiere den ActivityResultLauncher für die HostRotationActivity
        hostRotationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.loadCurrentHost()
                Toast.makeText(this, "Gastgeber erfolgreich gewechselt!", Toast.LENGTH_SHORT).show()
            }
        }

        // Observer für die Benutzerliste
        viewModel.loadUsers()
        viewModel.userList.observe(this) { users ->
            Log.d("MainActivity", "Benutzerliste aus dem ViewModel: ${users.joinToString()}")
            if (users.isEmpty()) {
                Toast.makeText(this, "Keine Benutzer zur Auswahl", Toast.LENGTH_SHORT).show()
            }
        }

        // Setze den Inhalt mit NavHost und MainScreen
        setContent {
            BoardGamerAppTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        MainScreen(
                            viewModel = viewModel,
                            navController = navController,
                            onNavigateToGameSchedule = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        GameScheduleActivity::class.java
                                    )
                                )
                            },
                            onNavigateToHostRotation = {
                                hostRotationActivityResultLauncher.launch(
                                    Intent(
                                        this@MainActivity,
                                        HostRotationActivity::class.java
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkUserLoggedIn() {
        val databaseHelper = AppDatabaseHelper(this)
        databaseHelper.getUserWithFirebaseID { username ->
            if (username == null) {
                // Nutzer ist nicht eingeloggt, starte LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Beende MainActivity, damit der Nutzer nicht zurückkehren kann
            } else {
                // Setze den Nutzernamen im ViewModel
                viewModel.setUserName(username)
            }
        }
    }
}
