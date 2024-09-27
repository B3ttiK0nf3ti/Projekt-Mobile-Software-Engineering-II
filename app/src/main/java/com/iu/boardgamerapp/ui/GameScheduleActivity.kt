package com.iu.boardgamerapp.ui

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import java.util.*

class GameScheduleActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("GameScheduleActivity", "Calendar permission granted")
            checkAndFetchCalendarEvents(mutableListOf())
        } else {
            Log.d("GameScheduleActivity", "Calendar permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isRefreshing by remember { mutableStateOf(false) }
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
            val coroutineScope = rememberCoroutineScope()

            // Kalenderereignisse als MutableStateList definieren
            val calendarEvents = remember { mutableStateListOf<CalendarEvent>() }

            LaunchedEffect(Unit) {
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
                            fetchCalendarEvents(calendarEvents)
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

                        /// Gerätekalender öffnen Button
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
                                ScheduleItem(event = event)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button, um ein Ereignis hinzuzufügen
                        Button(
                            onClick = {
                                val startTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 1 Tag in der Zukunft
                                val endTime = startTime + 2 * 60 * 60 * 1000 // 2 Stunden später
                                val newEvent = CalendarEvent(
                                    title = "Brettspielabend", // Titel des Ereignisses
                                    location = "Bei Alex",     // Ort des Ereignisses
                                    startTime = startTime,     // Startzeit des Ereignisses
                                    endTime = endTime          // Endzeit des Ereignisses
                                )
                                // Ereignis zum Kalender hinzufügen
                                addEventToCalendar(newEvent)
                                // Ereignis in Firestore speichern
                                saveEventToFirestore(newEvent) { date ->
                                    calendarEvents.add(newEvent) // Hier wird das neue Ereignis direkt hinzugefügt
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

    private fun saveEventToFirestore(event: CalendarEvent, onSuccess: (String) -> Unit) {
        Log.d("Firestore", "Saving event: $event") // Debugging-Log
        firestore.collection("calendarEvents")
            .add(event)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Event added with ID: ${documentReference.id}")
                val dateFormatted = java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(event.startTime))
                onSuccess(dateFormatted) // Geben Sie das formatierte Datum zurück
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding event: ${e.message}", e) // Verbessert die Fehlermeldung
            }
    }

    private fun checkAndFetchCalendarEvents(calendarEvents: MutableList<CalendarEvent>) {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCalendarEvents(calendarEvents)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

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

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION // Include location in projection
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.TITLE} LIKE ?"
        val selectionArgs = arrayOf(startMillis.toString(), "%spielabend%")

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
            val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
            val locationIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION) // Get index for location

            // Clear existing calendar events
            calendarEvents.clear()

            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val start = it.getLong(startIndex)
                val end = it.getLong(endIndex)
                val location = it.getString(locationIndex) ?: "" // Fetch location; use empty string if null

                // Create a CalendarEvent and add it
                val event = CalendarEvent(
                    title = title,
                    location = location, // Use the fetched location
                    startTime = start,
                    endTime = end
                )
                calendarEvents.add(event)
            }
        } ?: run {
            Log.w("GameScheduleActivity", "Cursor is null, no events fetched.")
        }
    }
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
            text = "Datum: ${java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(event.startTime))}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF318DFF) // Blau für Titel
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Titel: ${event.title}",
            fontSize = 14.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ort: ${event.location}",
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}