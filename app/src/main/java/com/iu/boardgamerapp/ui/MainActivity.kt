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
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val calendarEvents = mutableStateListOf<Pair<String, String>>() // Zustand für Kalenderereignisse

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(UserRepository(AppDatabaseHelper()), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Registrierung des Permission Request Launchers
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchCalendarEvents() // Holen Sie sich die Kalenderereignisse, wenn die Berechtigung erteilt ist
            } else {
                Log.d("MainActivity", "Calendar permission denied")
            }
        }

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

                // Check and request calendar permission
                LaunchedEffect(Unit) {
                    when {
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED -> {
                            fetchCalendarEvents()
                        }
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
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

    private fun fetchCalendarEvents() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            val contentResolver: ContentResolver = contentResolver
            val uri = CalendarContract.Events.CONTENT_URI

            val startMillis: Long = Calendar.getInstance().run {
                timeInMillis
            }

            val endMillis: Long = Calendar.getInstance().run {
                add(Calendar.YEAR, 1)
                timeInMillis
            }

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )

            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.TITLE} LIKE ?"
            val selectionArgs = arrayOf(startMillis.toString(), "%Brettspielabend%")

            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)

                calendarEvents.clear()

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val start = it.getLong(startIndex)

                    // Datum formatieren und direkt zur Liste hinzufügen
                    calendarEvents.add(
                        java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(start)) to title
                    )
                }
            }
        } else {
            Log.d("MainActivity", "Calendar permission not granted")
        }
    }
}
