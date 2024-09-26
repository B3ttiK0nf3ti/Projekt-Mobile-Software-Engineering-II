package com.iu.boardgamerapp.ui

import GameScheduleActivity
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
import java.util.*

class MainActivity : ComponentActivity() {
    private val calendarEvents = mutableStateListOf<Pair<String, String>>() // Zustand für Kalenderereignisse

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(UserRepository(AppDatabaseHelper()), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observer für die Benutzerliste
        viewModel.loadUsers() // Lade die Benutzerliste
        viewModel.userList.observe(this) { users ->
            Log.d("MainActivity", "Benutzerliste aus dem ViewModel: ${users.joinToString()}")
            if (users.isEmpty()) {
                Toast.makeText(this, "Keine Benutzer zur Auswahl", Toast.LENGTH_SHORT).show()
            } else {
                // Logik zum Anzeigen der Benutzer
            }
        }

        // Setze den Inhalt mit NavHost und MainScreen
        setContent {
            BoardGamerAppTheme {
                val navController = rememberNavController()

                // Setup NavHost mit dem navController
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        MainScreen(
                            viewModel = viewModel,
                            navController = navController,
                            onShowUserListDialog = { showUserListDialog() },
                            onRotateHost = { rotateHost() },
                            onNavigateToGameSchedule = {
                                // Navigiere zu GameScheduleActivity
                                startActivity(Intent(this@MainActivity, GameScheduleActivity::class.java))
                            }
                        )
                    }
                }


            }
        }
    }

    private fun rotateHost() {
        viewModel.rotateHost() // Aufruf der Methode im ViewModel
    }

    private fun showUserListDialog() {
        // Benutzerliste abrufen und in ein Array umwandeln
        val userList = viewModel.userList.value?.map { it.first }?.toTypedArray() ?: arrayOf()
        Log.d("UserList", "Benutzerliste: ${userList.joinToString()}")

        // Überprüfen, ob die Benutzerliste leer ist
        if (userList.isEmpty()) {
            Toast.makeText(this, "Keine Benutzer zur Auswahl", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Wähle einen neuen Gastgeber")

        dialog.setItems(userList) { _, which ->
            val selectedUser = viewModel.userList.value?.get(which)
            selectedUser?.let {
                viewModel.changeHost(it.first) // Ändere den Gastgeber mit dem Benutzernamen
            }
        }

        dialog.setNegativeButton("Abbrechen", null)
        dialog.show()
    }


    }

