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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.*

class GameScheduleActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val calendarEvents = mutableStateListOf<CalendarEvent>()
    private var nextEventId by mutableStateOf(1)

    // Register for permission requests
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isReadGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        val isWriteGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false

        if (isReadGranted && isWriteGranted) {
            Log.d("GameScheduleActivity", "Kalenderberechtigungen erteilt")
            fetchCalendarEvents(calendarEvents)
        } else {
            Log.d("GameScheduleActivity", "Kalenderberechtigungen abgelehnt")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isRefreshing by remember { mutableStateOf(false) }
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                checkAndRequestPermissions()
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFE0E0E0)
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

                        Text(
                            text = "Spielplan",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF318DFF)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(text = "Gerätekalender öffnen", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            items(calendarEvents.size) { index ->
                                val event = calendarEvents[index]
                                ScheduleItem(
                                    event = event,
                                    onDelete = { deleteEvent(event) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                // Aktuellen Zeitpunkt für den Start und das Ende des Ereignisses
                                val startTime = Timestamp.now()
                                val endTime = Timestamp.now().apply {
                                    val calendar = Calendar.getInstance()
                                    calendar.time = toDate() // Umwandeln in Date
                                    calendar.add(Calendar.HOUR, 2) // Zwei Stunden hinzufügen
                                    this.toDate().time + 2 * 60 * 60 * 1000 // 2 Stunden später
                                }

                                val newEvent = CalendarEvent(
                                    id = nextEventId.toString(),
                                    title = "Brettspielabend",
                                    location = "Bei Alex",
                                    startTime = startTime,
                                    endTime = endTime
                                )

                                addEventToCalendar(newEvent)

                                // Speichern des Ereignisses in Firestore und aktualisieren der Ereignisliste
                                saveEventToFirestore(newEvent, calendarEvents) { eventId ->
                                    // Hier rufen wir fetchCalendarEvents auf, um die neuesten Daten zu holen
                                    fetchCalendarEvents(calendarEvents)
                                    nextEventId++ // Update nextEventId
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(text = "Termin hinzufügen", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    private fun deleteEvent(event: CalendarEvent) {
        val contentResolver: ContentResolver = contentResolver
        val uri = CalendarContract.Events.CONTENT_URI

        event.id?.let { eventId ->
            val selection = "${CalendarContract.Events._ID} = ?"
            val selectionArgs = arrayOf(eventId)

            val deletedRows = contentResolver.delete(uri, selection, selectionArgs)
            if (deletedRows > 0) {
                Log.d("GameScheduleActivity", "Ereignis erfolgreich gelöscht: $event")
                deleteEventFromFirestore(event)
                calendarEvents.remove(event)
            } else {
                Log.w("GameScheduleActivity", "Kein Ereignis gefunden zum Löschen: $event")
            }
        } ?: run {
            Log.w("GameScheduleActivity", "Ereignis-ID ist null, kann nicht gelöscht werden.")
        }
    }

    private fun deleteEventFromFirestore(event: CalendarEvent) {
        event.id?.let { eventId ->
            firestore.collection("calendarEvents")
                .document(eventId)
                .delete()
                .addOnSuccessListener {
                    Log.d("Firestore", "Ereignis erfolgreich aus Firestore gelöscht: $eventId")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Fehler beim Löschen des Ereignisses: ${e.message}", e)
                }
        } ?: run {
            Log.w("Firestore", "Ereignis hat keine ID, kann nicht gelöscht werden.")
        }
    }

    private fun addEventToCalendar(event: CalendarEvent) {
        Log.d("GameScheduleActivity", "Bereite vor, Ereignis hinzuzufügen: $event")

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime.toDate().time) // Konvertieren in Millisekunden
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTime.toDate().time) // Konvertieren in Millisekunden
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            startActivity(intent)
            Log.d("GameScheduleActivity", "Ereignis erfolgreich zum Kalender hinzugefügt.")
        } catch (e: Exception) {
            Log.e("GameScheduleActivity", "Fehler beim Hinzufügen des Ereignisses zum Kalender: ${e.message}", e)
        }
    }

    private fun saveEventToFirestore(event: CalendarEvent, calendarEvents: MutableList<CalendarEvent>, onSuccess: (String) -> Unit) {
        Log.d("Firestore", "Speichere Ereignis: $event")

        // Verwenden Sie die aktuelle ID als Dokument-ID in Firestore
        val eventId = event.id // Benutzen Sie die manuell zugewiesene ID

        firestore.collection("calendarEvents")
            .document(eventId) // Verwenden Sie die benutzerdefinierte ID
            .set(event) // Speichern Sie direkt das CalendarEvent
            .addOnSuccessListener {
                Log.d("Firestore", "Ereignis hinzugefügt mit ID: $eventId")
                // Rufen Sie die ID zurück, um zu bestätigen, dass das Speichern erfolgreich war
                onSuccess(eventId)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Fehler beim Hinzufügen des Ereignisses: ${e.message}", e)
            }
    }

    private fun fetchCalendarEvents(calendarEvents: MutableList<CalendarEvent>) {
        firestore.collection("calendarEvents")
            .get()
            .addOnSuccessListener { documents ->
                calendarEvents.clear() // Zuerst die Liste leeren

                for (document in documents) {
                    val event = document.toObject(CalendarEvent::class.java) // Umwandeln des Dokuments in ein CalendarEvent
                    event.id = document.id // ID vom Dokument zuweisen
                    calendarEvents.add(event) // Hinzufügen des Ereignisses zur Liste
                }

                Log.d("GameScheduleActivity", "Erfolgreich ${calendarEvents.size} Ereignisse aus Firestore abgerufen")
            }
            .addOnFailureListener { e ->
                Log.w("GameScheduleActivity", "Fehler beim Abrufen der Ereignisse: ${e.message}", e)
            }
    }

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_CALENDAR
                    ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("GameScheduleActivity", "Kalenderberechtigungen bereits erteilt")
                fetchCalendarEvents(calendarEvents)
            }
            else -> {
                Log.d("GameScheduleActivity", "Anfordern von Kalenderberechtigungen")
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )
                )
            }
        }
    }
}

@Composable
fun ScheduleItem(event: CalendarEvent, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Datum: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(event.startTime.toDate())}",
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
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Entfernen", color = Color.White) // Button-Text auf Deutsch
        }
    }
}
