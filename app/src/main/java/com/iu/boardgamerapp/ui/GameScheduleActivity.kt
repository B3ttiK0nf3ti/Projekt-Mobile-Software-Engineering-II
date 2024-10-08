package com.iu.boardgamerapp.ui

import kotlinx.coroutines.launch
import android.Manifest
import android.content.ContentResolver
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
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import com.google.firebase.Timestamp
import com.iu.boardgamerapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import java.util.*
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.lifecycleScope

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
            var selectedDateStart = Calendar.getInstance(TimeZone.getDefault())

            var isLoading by remember { mutableStateOf(true) }

            // Use LaunchedEffect to fetch calendar events safely within the Composable
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
                            fetchCalendarEvents(calendarEvents) // Lade die aktuellen Events
                            isRefreshing = false
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(Color.White)
                            ) {
                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Zurück",
                                        tint = Color.Gray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.schedule_title),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF318DFF),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.width(48.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp) // Padding an den Seiten
                            ) {
                                items(calendarEvents.size) { index ->
                                    val event = calendarEvents[index]
                                    ScheduleItem(
                                        event = event,
                                        onDelete = { deleteEvent(event) } // Event löschen und aktualisieren
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
                                label = { Text(stringResource(R.string.event_title)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                textStyle = TextStyle(color = Color.Black), // Textfarbe Schwarz für bessere Sichtbarkeit
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF318DFF),  // Dunklerer Rahmen, wenn das Feld fokussiert ist
                                    unfocusedBorderColor = Color.Gray,  // Dunklerer Rahmen, wenn das Feld nicht fokussiert ist
                                    cursorColor = Color.Black,  // Schwarzer Cursor für bessere Sichtbarkeit
                                    focusedLabelColor = Color(0xFF318DFF), // Label im Fokuszustand
                                    unfocusedLabelColor = Color.Gray  // Label im nicht fokussierten Zustand
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text(stringResource(R.string.event_location)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),

                                textStyle = TextStyle(color = Color.Black), // Textfarbe Schwarz für bessere Sichtbarkeit
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF318DFF),  // Dunklerer Rahmen, wenn das Feld fokussiert ist
                                    unfocusedBorderColor = Color.Gray,  // Dunklerer Rahmen, wenn das Feld nicht fokussiert ist
                                    cursorColor = Color.Black,  // Schwarzer Cursor für bessere Sichtbarkeit
                                    focusedLabelColor = Color(0xFF318DFF), // Label im Fokuszustand
                                    unfocusedLabelColor = Color.Gray  // Label im nicht fokussierten Zustand
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Buttons to pick start and end date/time
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp), // Padding an den Seiten
                                contentAlignment = Alignment.Center // Zentriert den Inhalt (Button)
                            ) {
                                DatePickerButton(selectedDateStart) { date ->
                                    selectedDateStart =
                                        date // Der date-Parameter ist jetzt das aktualisierte Datum
                                }
                            }

                            Spacer(modifier = Modifier.height(48.dp))

                            Button(
                                onClick = {
                                    // Check if the title or location is empty
                                    if (title.isBlank() || location.isBlank()) {
                                        Toast.makeText(
                                            this@GameScheduleActivity,
                                            "Titel und Ort dürfen nicht leer sein!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    // Check if the start date is in the future
                                    val currentTime = Calendar.getInstance().time
                                    if (selectedDateStart.time.before(currentTime)) {
                                        Toast.makeText(
                                            this@GameScheduleActivity,
                                            "Startzeit muss in der Zukunft liegen!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    Log.d(
                                        "GameScheduleActivity",
                                        "Start time (millis): ${selectedDateStart.timeInMillis}"
                                    )

                                    // Set the end time to be 2 hours later than the start time
                                    val selectedDateEnd = Calendar.getInstance().apply {
                                        timeInMillis = selectedDateStart.timeInMillis
                                        add(Calendar.HOUR_OF_DAY, 2) // Add 2 hours
                                    }

                                    val newEvent = CalendarEvent(
                                        id = "",
                                        title = title,
                                        location = location,
                                        startTime = Timestamp(selectedDateStart.time), // Die Zeit in der lokalen Zeitzone
                                        endTime = Timestamp(selectedDateEnd.time) // Benutze die berechnete Endzeit in der lokalen Zeitzone
                                    )

                                    // Save the event to Firestore
                                    saveEventToFirestore(newEvent, calendarEvents) { eventId ->
                                        Log.d(
                                            "GameScheduleActivity",
                                            "Event successfully added with ID: $eventId"
                                        )
                                        newEvent.id = eventId
                                        calendarEvents.add(newEvent)
                                        fetchCalendarEvents(calendarEvents)

                                        // Eingabefelder zurücksetzen
                                        title = ""
                                        location = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF318DFF
                                    )
                                ),
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
    }

    @Composable
    fun DatePickerButton(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
        val context = LocalContext.current
        val openDialog = remember { mutableStateOf(false) }

        // Button zum Öffnen des DatePickers
        Button(onClick = { openDialog.value = true }) {
            Text(text = "Datum auswählen")
        }

        // Zeige den Dialog an, wenn openDialog.value true ist
        if (openDialog.value) {
            // DatePickerDialog wird bei jedem Klick neu erstellt
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    // Setze das Datum im Calendar-Objekt
                    selectedDate.set(year, month, dayOfMonth)

                    // Zeige den TimePickerDialog an
                    val timePickerDialog = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedDate.set(Calendar.MINUTE, minute)

                            // Rufe die onDateSelected Callback-Funktion auf
                            onDateSelected(selectedDate)

                            // Dialog schließen
                            openDialog.value = false
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

            // Listener für den Fall, dass der Dialog abgebrochen wird
            datePickerDialog.setOnCancelListener {
                openDialog.value = false // Zustand zurücksetzen, wenn der Benutzer abbricht
            }

            // Zeige den DatePickerDialog an
            DisposableEffect(Unit) {
                datePickerDialog.show()
                onDispose {
                    openDialog.value = false // Schließe den Dialog, wenn der Composable entfernt wird
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
        // Zuerst das Ereignis lokal aus der Liste entfernen
        calendarEvents.remove(event)
        // Danach die UI aktualisieren, indem die neuen Events neu geladen werden
        fetchCalendarEvents(calendarEvents)

        val contentResolver: ContentResolver = contentResolver
        val uri = CalendarContract.Events.CONTENT_URI

        // Verwende die Kalender-ID, um das Ereignis zu löschen
        val selection = "${CalendarContract.Events._ID} = ?"
        val selectionArgs = arrayOf(event.id)

        Log.d("GameScheduleActivity", "Versuche, das Event mit ID: ${event.id} zu löschen")

        // Ereignis aus Firestore löschen
        deleteEventFromFirestore(event.id) // ID des Ereignisses übergeben

        // Das Ereignis aus dem Android-Kalender löschen
        lifecycleScope.launch(Dispatchers.IO) {
            val deletedRows = contentResolver.delete(uri, selection, selectionArgs)
            if (deletedRows > 0) {
                Log.d(
                    "GameScheduleActivity",
                    "Erfolgreich $deletedRows Zeilen aus dem Kalender gelöscht"
                )
            } else {
                Log.d("GameScheduleActivity", "Keine Zeilen wurden aus dem Kalender gelöscht")
            }
        }
    }

   private fun deleteEventFromFirestore(eventId: String) {
        Log.d("GameScheduleActivity", "Lösche Firestore Event mit ID: $eventId")
        val documentRef = firestore.collection("calendarEvents").document(eventId)
        documentRef.delete()
            .addOnSuccessListener {
                Log.d("GameScheduleActivity", "Event erfolgreich aus Firestore gelöscht: $eventId")
            }
            .addOnFailureListener { e ->
                Log.e("GameScheduleActivity", "Fehler beim Löschen des Events: $e")
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
            text = stringResource(R.string.event_date, SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(event.startTime.toDate())),
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