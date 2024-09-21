package com.iu.boardgamerapp.ui

import GameScheduleScreen
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
    private val calendarEvents = mutableStateListOf<Pair<String, String>>() // State for storing calendar events

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(UserRepository(AppDatabaseHelper(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        MainScreen(viewModel = viewModel, navController = navController) {
                            // Hier kannst du die rotateHost-Methode aufrufen
                            rotateHost()
                        }
                    }
                    composable("game_schedule") {
                        GameScheduleScreen(
                            gameDates = calendarEvents,
                            navController = navController,
                            fetchCalendarEvents = { fetchCalendarEvents() }
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
