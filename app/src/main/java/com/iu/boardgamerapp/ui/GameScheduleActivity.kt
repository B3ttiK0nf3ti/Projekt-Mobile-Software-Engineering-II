package com.iu.boardgamerapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent

class GameScheduleActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var appDatabaseHelper: AppDatabaseHelper

    // MutableStateList für die Kalenderereignisse
    private val calendarEvents = mutableStateListOf<CalendarEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Instanziiere den AppDatabaseHelper
        appDatabaseHelper = AppDatabaseHelper()

        // Registrierung des ActivityResultLaunchers für die Kalenderberechtigung
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("GameScheduleActivity", "Calendar permission granted")
                fetchCalendarEventsFromFirestore(calendarEvents) // Abrufen der Ereignisse
            } else {
                Log.d("GameScheduleActivity", "Calendar permission denied")
            }
        }

        setContent {
            GameScheduleScreen(calendarEvents) // calendarEvents wird hier übergeben
        }
    }

    @Composable
    fun GameScheduleScreen(calendarEvents: MutableList<CalendarEvent>) {
        var isRefreshing by remember { mutableStateOf(false) }
        val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            // Kalenderereignisse beim ersten Rendern abrufen
            checkAndFetchCalendarEvents(calendarEvents)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE0E0E0) // Hellgrauer Hintergrund
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch(Dispatchers.IO) {
                        // Kalenderereignisse abrufen (im Hintergrund)
                        fetchCalendarEvents(calendarEvents) // Kalenderereignisse abrufen
                        isRefreshing = false
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zurück-Button - Schließt die Activity
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = Color.Gray
                        )
                    }

                    // Überschrift
                    Text(
                        text = "Spielplan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF318DFF) // Blau wie in ChatActivity
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gerätekalender öffnen Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_CALENDAR) // Öffnet die Kalender-App
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(text = "Gerätekalender öffnen", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Spieltermine in einer LazyColumn anzeigen
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        items(calendarEvents.size) { index ->
                            val event = calendarEvents[index]
                            ScheduleItem(date = formatDate(event.startTime), location = event.location)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button, um ein Ereignis hinzuzufügen
                    Button(
                        onClick = {
                            val newEvent = CalendarEvent(
                                title = "Brettspielabend",
                                startTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // In einem Tag
                                endTime = System.currentTimeMillis() + 26 * 60 * 60 * 1000, // In einem Tag und 2 Stunden
                                location = "Bei Alex"
                            )

                            // Ereignis in den lokalen Kalender hinzufügen
                            addEventToCalendar(newEvent)

                            // Ereignis in Firestore hinzufügen
                            appDatabaseHelper.addCalendarEvent(newEvent) { success ->
                                if (success) {
                                    Log.d("GameScheduleActivity", "Kalenderereignis erfolgreich hinzugefügt.")
                                    calendarEvents.add(newEvent) // Aktualisiere die UI mit dem CalendarEvent
                                } else {
                                    Log.e("GameScheduleActivity", "Fehler beim Hinzufügen des Kalenderereignisses.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(text = "Termin hinzufügen", color = Color.White)
                    }
                }
            }
        }
    }

    private fun checkAndFetchCalendarEvents(calendarEvents: MutableList<CalendarEvent>) {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Berechtigung erteilt, Kalenderdaten abrufen
            fetchCalendarEventsFromFirestore(calendarEvents)
        } else {
            // Berechtigung anfordern
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun fetchCalendarEventsFromFirestore(calendarEvents: MutableList<CalendarEvent>) {
        appDatabaseHelper.fetchCalendarEvents { events ->
            // UI mit den abgerufenen Firestore-Ereignissen aktualisieren
            calendarEvents.clear()
            calendarEvents.addAll(events)
        }
    }

    private fun addEventToCalendar(event: CalendarEvent) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTime)
        }
        startActivity(intent)
    }

    @SuppressLint("Range")
    private fun fetchCalendarEvents(calendarEvents: MutableList<CalendarEvent>) {
        val contentResolver: ContentResolver = contentResolver
        val uri = CalendarContract.Events.CONTENT_URI

        val startMillis: Long = Calendar.getInstance().run {
            timeInMillis
        }

        val endMillis: Long = Calendar.getInstance().run {
            add(Calendar.YEAR, 1)
            timeInMillis
        }

        // Alle notwendigen Spalten angeben
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION // Standort hinzufügen, falls benötigt
        )

        val selection =
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.TITLE} LIKE ?"
        val selectionArgs = arrayOf(startMillis.toString(), "%Brettspielabend%")

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        if (cursor != null) { // Überprüfen, ob der Cursor nicht null ist
            val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
            val titleIndex = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            val startIndex = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIndex = cursor.getColumnIndex(CalendarContract.Events.DTEND) // Endzeit-Index hinzufügen

            // Kalenderereignisse in der Liste speichern
            calendarEvents.clear()

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex)
                val start = cursor.getLong(startIndex)
                val end = cursor.getLong(endIndex) // Endzeit abrufen

                // Ereignis als CalendarEvent hinzufügen
                calendarEvents.add(
                    CalendarEvent(
                        title = title,
                        startTime = start,
                        endTime = end, // Endzeit verwenden
                        location = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)) ?: "" // Standort optional
                    )
                )
            }
            cursor.close() // Cursor schließen
        } else {
            Log.d("GameScheduleActivity", "Cursor ist null, keine Kalenderereignisse gefunden.")
        }
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    @Composable
    fun ScheduleItem(date: String, location: String) {
        Column(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Datum: $date",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF318DFF) // Blau für Titel
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ort: $location",
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}
