package com.iu.boardgamerapp.ui

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.components.MainScreen
import com.iu.boardgamerapp.ui.screens.GameScheduleScreen
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var dbHelper: AppDatabaseHelper

    private val calendarEvents = mutableStateListOf<Pair<String, String>>() // State for storing calendar events

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = AppDatabaseHelper(this)
        val userRepository = UserRepository(dbHelper)

        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(userRepository)
        }

        // Register permission request launcher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchCalendarEvents()
            } else {
                Log.d("MainActivity", "Calendar permission denied")
            }
        }

        setContent {
            BoardGamerAppTheme {
                val navController = rememberNavController()

                // Setup NavHost with the navController
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        MainScreen(viewModel = viewModel, navController = navController)
                    }
                    composable("game_schedule") {
                        GameScheduleScreen(
                            gameDates = calendarEvents, // Pass the calendar events here
                            navController = navController,
                            fetchCalendarEvents = { fetchCalendarEvents() } // Pass the fetchCalendarEvents callback
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

    private fun fetchCalendarEvents() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            val contentResolver: ContentResolver = contentResolver
            val uri = CalendarContract.Events.CONTENT_URI

            // Startzeit: aktuelles Datum
            val startMillis: Long = Calendar.getInstance().run {
                timeInMillis
            }

            // Endzeit: Ein Jahr ab jetzt (du kannst das nach Bedarf anpassen)
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

            // Auswahlkriterien für zukünftige Ereignisse
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.TITLE} LIKE ?"
            val selectionArgs = arrayOf(startMillis.toString(), "%Brettspielabend%")

            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC" // Sortierung nach Startdatum
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)

                calendarEvents.clear() // Vorherige Ereignisse löschen

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val start = it.getLong(startIndex)
                    val end = it.getLong(endIndex)

                    // Datum und Zeit formatieren
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(start))
                    val location = title

                    calendarEvents.add(date to location)
                }
            }
        } else {
            Log.d("MainActivity", "Calendar permission not granted")
        }
    }
}
