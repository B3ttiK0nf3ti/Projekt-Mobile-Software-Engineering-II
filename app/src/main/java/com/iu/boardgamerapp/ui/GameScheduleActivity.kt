package com.iu.boardgamerapp.ui

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
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
import com.iu.boardgamerapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.border
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import java.util.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text

class GameScheduleActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val calendarEvents = mutableStateListOf<CalendarEvent>()
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
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            var isRefreshing by remember { mutableStateOf(false) }
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
            val coroutineScope = rememberCoroutineScope()
            var selectedDateStart by remember { mutableStateOf(Calendar.getInstance()) }
            var selectedDateEnd by remember {
                mutableStateOf(
                    Calendar.getInstance().apply { add(Calendar.HOUR, 2) })
            }

            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                isLoading = true
                fetchCalendarEvents(calendarEvents)
                isLoading = false
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
                                contentDescription = stringResource(R.string.back),
                                tint = Color.Gray
                            )
                        }

                        Text(
                            text = stringResource(R.string.schedule_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF318DFF)
                        )

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

                        // Dynamische Eingabefelder für Titel und Ort
                        var title by remember { mutableStateOf("") }
                        var location by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Ereignistitel") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text(stringResource(R.string.event_location)) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Buttons to pick start and end date/time
                        DatePickerButton(selectedDateStart) { selectedDateStart = it }
                        DatePickerButton(selectedDateEnd) { selectedDateEnd = it }

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = {
                                val newEvent = CalendarEvent(
                                    id = "", // Initially empty; will be set in saveEventToFirestore
                                    title = title,
                                    location = location,
                                    startTime = Timestamp(selectedDateStart.time),
                                    endTime = Timestamp(selectedDateEnd.time)
                                )

                                addEventToCalendar(newEvent) // Save the event to the calendar

                                // Save the event to Firestore
                                saveEventToFirestore(newEvent, calendarEvents) { eventId ->
                                    Log.d(
                                        "GameScheduleActivity",
                                        getString(R.string.event_successfully_added, eventId)
                                    )
                                    newEvent.id = eventId
                                    calendarEvents.add(newEvent)
                                    fetchCalendarEvents(calendarEvents) // Refresh calendar events

                                    // Eingabefelder zurücksetzen
                                    title = ""
                                    location = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(text = stringResource(R.string.add_event), color = Color.White)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DatePickerButton(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
        val context = LocalContext.current
        val openDialog = remember { mutableStateOf(false) }

        // Zeige den Dialog an, wenn openDialog.value true ist
        if (openDialog.value) {
            // Initialisiere den Dialog außerhalb von LaunchedEffect
            val datePickerDialog = remember {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        // Setze das Datum im Calendar-Objekt
                        selectedDate.set(year, month, dayOfMonth)

                        // Erstelle den TimePickerDialog
                        val timePickerDialog = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                // Setze die Uhrzeit im Calendar-Objekt
                                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                selectedDate.set(Calendar.MINUTE, minute)

                                // Rufe die onDateSelected Callback-Funktion auf
                                onDateSelected(selectedDate)
                                openDialog.value = false // Schließen des Dialogs nach Auswahl
                            },
                            selectedDate.get(Calendar.HOUR_OF_DAY),
                            selectedDate.get(Calendar.MINUTE),
                            true // 24-Stunden-Format
                        )
                        timePickerDialog.show() // Zeige den TimePickerDialog an
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
                )
            }

            DisposableEffect(Unit) {
                datePickerDialog.show()
                onDispose {
                    openDialog.value =
                        false // Schließe den Dialog, wenn der Composable entfernt wird
                }
            }
        }

        Button(
            onClick = { openDialog.value = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
            modifier = Modifier
                .wrapContentWidth()
                .padding(8.dp)
        ) {
            Text(
                text = if (selectedDate.timeInMillis == Calendar.getInstance().timeInMillis) {
                    "Startdatum und -uhrzeit wählen"
                } else {
                    SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        Locale.getDefault()
                    ).format(selectedDate.time)

                }
            )
        }
    }


    private fun deleteEvent(event: CalendarEvent) {
        val contentResolver: ContentResolver = contentResolver
        val uri = CalendarContract.Events.CONTENT_URI

        // Verwende die Kalender-ID, um das Ereignis zu löschen
        val selection = "${CalendarContract.Events._ID} = ?"
        val selectionArgs = arrayOf(event.id) // Überprüfen, ob diese ID wirklich die Kalender-ID ist

        Log.d("GameScheduleActivity", "Attempting to delete event with ID: ${event.id}")

        // Zuerst das Ereignis aus Firestore löschen, ID des Ereignisses übergeben
        deleteEventFromFirestore(event.id) // Hier die ID des Ereignisses verwenden

        // Dann versuche, das Ereignis aus dem Kalender zu löschen
        val deletedRows = contentResolver.delete(uri, selection, selectionArgs)
        if (deletedRows > 0) {
            Toast.makeText(this, getString(R.string.event_successfully_deleted), Toast.LENGTH_SHORT).show()
            Log.d("GameScheduleActivity", "Successfully deleted $deletedRows rows from calendar")
            calendarEvents.remove(event) // Update local list
        } else {
            Toast.makeText(this, getString(R.string.no_event_found), Toast.LENGTH_SHORT).show()
            Log.d("GameScheduleActivity", "No rows were deleted from the calendar")
        }
    }

    fun deleteEventFromFirestore(eventId: String) {
        Log.d("GameScheduleActivity", "Deleting Firestore event with ID: $eventId")

        val documentRef = firestore.collection("calendarEvents").document(eventId)
        documentRef.delete()
            .addOnSuccessListener {
                Log.d("GameScheduleActivity", "Event successfully deleted from Firestore: $eventId")
            }
            .addOnFailureListener { e ->
                Log.e("GameScheduleActivity", "Error deleting event: $e")
            }
    }

    private fun addEventToCalendar(event: CalendarEvent) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime.toDate().time)
            put(CalendarContract.EXTRA_EVENT_END_TIME, event.endTime.toDate().time)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri != null) {
            // Hole die ID des neu hinzugefügten Ereignisses
            val eventId = uri.lastPathSegment ?: run {
                Log.e("GameScheduleActivity", "Event ID is null")
                return
            }
            event.id = eventId // Setze die ID des Ereignisses auf die des Kalenders
            Log.d("GameScheduleActivity", "Event successfully added to calendar: $event")
            Toast.makeText(this, "Event successfully added to calendar", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("GameScheduleActivity", "Error adding event to calendar")
        }
    }

    private fun saveEventToFirestore(
        event: CalendarEvent,
        calendarEvents: MutableList<CalendarEvent>,
        onSuccess: (String) -> Unit
    ) {
        Log.d("Firestore", "Speichere Ereignis: $event")

        // Generiere eine neue Firestore-Dokumentreferenz
        val documentRef = firestore.collection("calendarEvents").document()
        val eventId = documentRef.id // Die neu generierte ID

        // We assign the Firestore ID to the event
        event.id = documentRef.id

        // Speichere das Ereignis in Firestore
        documentRef.set(event)
            .addOnSuccessListener {
                Log.d("Firestore", getString(R.string.event_successfully_added, eventId))
                calendarEvents.add(event) // Lokale Liste mit dem Ereignis aktualisieren
                onSuccess(eventId) // Erfolg mit der neuen Ereignis-ID melden
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", getString(R.string.error_adding_event, e.message), e)
            }
    }

    private fun fetchCalendarEvents(calendarEvents: MutableList<CalendarEvent>) {
        firestore.collection("calendarEvents")
            .get()
            .addOnSuccessListener { documents ->
                calendarEvents.clear() // Clear the list first

                for (document in documents) {
                    val event =
                        document.toObject(CalendarEvent::class.java) // Convert to CalendarEvent
                    event.id = document.id // Assign document ID to the event
                    calendarEvents.add(event) // Add event to the list
                }

                // Sortiere die Ereignisse nach Startzeit
                calendarEvents.sortBy { it.startTime.toDate() }

                Log.d(
                    "GameScheduleActivity",
                    "Successfully fetched ${calendarEvents.size} events from Firestore"
                )
            }
            .addOnFailureListener { e ->
                Log.w(
                    "GameScheduleActivity",
                    getString(R.string.error_fetching_events, e.message),
                    e
                )
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
                .background(Color.White, shape = RoundedCornerShape(8.dp)) // Weißer Hintergrund
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.event_date, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(event.startTime.toDate())),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF318DFF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.event_title_display, event.title),
                fontSize = 14.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.event_location_display, event.location),
                fontSize = 14.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(id = R.string.remove_event), color = Color.White)
            }
    }
}