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
        appDatabaseHelper = AppDatabaseHelper(this)

        // Registrierung des ActivityResultLaunchers für die Kalenderberechtigung
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("GameScheduleActivity", "Kalenderberechtigung erteilt")
                fetchCalendarEvents() // Abrufen der Ereignisse
            } else {
                Log.d("GameScheduleActivity", "Kalenderberechtigung abgelehnt")
            }
        }

        setContent {
            GameScheduleScreen(calendarEvents)
        }
        // Überprüfen und Abrufen der Kalenderereignisse
        checkAndFetchCalendarEvents(calendarEvents)
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
                        fetchCalendarEvents() // Kalenderereignisse abrufen
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

                    Button(
                        onClick = {
                            updateCalendarEvents()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(text = "Kalender aktualisieren", color = Color.White)
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
                            ScheduleItem(event = event) // Zeige jedes Ereignis an
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button, um ein Ereignis hinzuzufügen
                    Button(
                        onClick = {
                            addNewEvent()
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
            fetchCalendarEvents()
        } else {
            // Berechtigung anfordern
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun fetchCalendarEvents() {
        fetchCalendarEventsFromFirestore(calendarEvents) // Ruft die Ereignisse aus Firestore ab
    }

    private fun fetchCalendarEventsFromFirestore(calendarEvents: MutableList<CalendarEvent>) {
        appDatabaseHelper.fetchCalendarEvents { events ->
            // UI mit den abgerufenen Firestore-Ereignissen aktualisieren
            calendarEvents.clear()
            calendarEvents.addAll(events)

            // An dieser Stelle brauchst du nicht mehr zu löschen
            Log.d("GameScheduleActivity", "Ereignisse aus Firestore erfolgreich abgerufen.")
        }
    }

    private fun addNewEvent() {
        // Neuen Termin erstellen (falls es sich um ein Spielabend handelt)
        val newEvent = CalendarEvent(
            id = UUID.randomUUID().toString(),
            title = "Brettspielabend",
            startTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // In einem Tag
            endTime = System.currentTimeMillis() + 26 * 60 * 60 * 1000, // In einem Tag und 2 Stunden
            location = "Bei Alex"
        )

        // Ereignis in den lokalen Kalender hinzufügen
        addEventToCalendar(newEvent)

        // Ereignis in Firestore hinzufügen, wenn es sich um ein „spielabend“ handelt
        if (newEvent.title.contains("spielabend", ignoreCase = true)) {
            appDatabaseHelper.addCalendarEvent(newEvent) { success ->
                if (success) {
                    Log.d("GameScheduleActivity", "Kalenderereignis erfolgreich hinzugefügt.")
                    calendarEvents.add(newEvent) // Aktualisiere die UI mit dem CalendarEvent
                } else {
                    Log.e("GameScheduleActivity", "Fehler beim Hinzufügen des Kalenderereignisses.")
                }
            }
        } else {
            Log.e("GameScheduleActivity", "Das Ereignis ist kein Spielabend.")
        }
    }

    private fun addEventToCalendar(event: CalendarEvent) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTime)
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        startActivity(intent)
    }

    @SuppressLint("Recycle", "Range")
    private fun updateCalendarEvents() {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val startMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val selectionArgs = arrayOf(startMillis.toString())

        val cursor = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        if (cursor != null && cursor.moveToFirst()) {
            calendarEvents.clear() // Leere die aktuelle Liste

            // Überprüfen, ob die benötigten Spalten vorhanden sind
            val titleIndex = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            val startIndex = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIndex = cursor.getColumnIndex(CalendarContract.Events.DTEND)
            val locationIndex = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

            // Überprüfen, ob die Spaltenindizes gültig sind
            if (titleIndex == -1 || startIndex == -1 || endIndex == -1) {
                Log.e("GameScheduleActivity", "Eine oder mehrere Spalten wurden nicht gefunden.")
                cursor.close()
                return // Beende die Funktion, wenn Spalten nicht gefunden wurden
            }

            do {
                val title = cursor.getString(titleIndex)
                if (title.contains("spielabend", ignoreCase = true)) { // Nur Spielabende
                    val id = cursor.getString(cursor.getColumnIndex(CalendarContract.Events._ID))
                    val startTime = cursor.getLong(startIndex)
                    val endTime = cursor.getLong(endIndex)
                    val location = if (locationIndex != -1) cursor.getString(locationIndex) else ""

                    val event = CalendarEvent(id, title, startTime, endTime, location)
                    calendarEvents.add(event) // Hinzufügen des Ereignisses zur Liste
                }
            } while (cursor.moveToNext())
            cursor.close() // Cursor schließen
        } else {
            Log.e("GameScheduleActivity", "Cursor ist null oder leer.")
        }
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    @Composable
    fun ScheduleItem(event: CalendarEvent) {
        Column(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Titel: ${event.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF318DFF) // Blau für Titel
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Datum: ${formatDate(event.startTime)}",
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = "Ort: ${event.location}",
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}
