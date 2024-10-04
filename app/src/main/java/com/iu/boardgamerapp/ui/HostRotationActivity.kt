package com.iu.boardgamerapp.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import com.iu.boardgamerapp.ui.datamodel.User
import java.util.Calendar

class HostRotationActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere ViewModel
        val databaseHelper = AppDatabaseHelper(this)
        val repository = UserRepository(databaseHelper)
        val factory = MainViewModelFactory(repository, databaseHelper, this)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            HostRotationScreen(viewModel)
        }

        // Sofortige Überprüfung der Ereignisse
        checkAndRotateHosts()
    }
    private fun checkAndRotateHosts() {
        firestore.collection("calendarEvents")
            .get()
            .addOnSuccessListener { documents ->
                val currentTime = Calendar.getInstance().time

                for (document in documents) {
                    val event = document.toObject(CalendarEvent::class.java)
                    event.id = document.id // ID aus Firestore-Dokument

                    // Überprüfen, ob das Ereignis in der Vergangenheit liegt
                    if (event.endTime.toDate().before(currentTime)) {
                        rotateHostForEvent(event)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HostRotationActivity", "Fehler beim Laden der Ereignisse: ${e.message}", e)
            }
    }

    private fun rotateHostForEvent(event: CalendarEvent) {
        // Benutzerliste aus Firestore oder ViewModel abrufen
        viewModel.userList.observe(this) { userList ->
            if (userList.isNotEmpty()) {
                // Beispiel: Wähle einen neuen Gastgeber zufällig aus
                val newHost = userList.random() // Wähle einen zufälligen Benutzer als neuen Gastgeber

                // Aktualisiere den Titel des Ereignisses mit dem neuen Gastgeber
                event.title = "${event.title} - Hosted by ${newHost.name}" // Verwende den Namen des ausgewählten Benutzers

                // Aktualisiere das Ereignis in Firestore
                firestore.collection("calendarEvents").document(event.id)
                    .update("title", event.title, "currentHost", newHost.name) // Aktualisiere den Gastgeber im Dokument
                    .addOnSuccessListener {
                        Log.d("HostRotationActivity", "Host erfolgreich aktualisiert für Ereignis: ${event.id} zu ${newHost.name}")
                        // Benutzerfeedback geben
                        showHostChangedSnackbar("Gastgeber gewechselt zu: ${newHost.name}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HostRotationActivity", "Fehler beim Aktualisieren des Gastgebers: $e")
                    }
            }
        }
    }
    private fun showHostChangedSnackbar(message: String) {
        // Hier ist eine einfache Snackbar-Implementierung
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(message) {
                snackbarHostState.showSnackbar(message)
            }

            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class) // Suppress experimental API warning
    @Composable
    fun HostRotationScreen(viewModel: MainViewModel) {
        // Abrufen der Benutzerliste aus dem ViewModel
        val userList by viewModel.userList.observeAsState(emptyList())
        // Abrufen des aktuellen Gastgebers aus dem ViewModel
        val currentHost by viewModel.currentHost.observeAsState("Lade Gastgeber...") // Standardtext bei Ladezustand

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE0E0E0) // Hellgrauer Hintergrund
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
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
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Display the current host
                            Text(
                                text = "Gastgeberwechsel", // Setzen Sie hier Ihren Titel ein
                                fontSize = 18.sp, // Angepasste Schriftgröße
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF318DFF), // Angepasste Farbe
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aktuellen Gastgeber anzeigen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp), // Padding auf beiden Seiten
                        contentAlignment = Alignment.Center // Zentriert den Inhalt
                    ) {
                        Text(
                            text = "Aktueller Gastgeber: $currentHost", // Zeigt den aktuellen Gastgeber an
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Benutzerliste in einer LazyColumn anzeigen
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        items(userList.size) { index ->
                            val user = userList[index]
                            UserItem(user) { selectedUser ->
                                // Gastgeber wechseln und zur Startseite zurückkehren
                                viewModel.changeHost(selectedUser.name) {
                                    // Callback, um den Hostwechsel zu bestätigen
                                    setResult(Activity.RESULT_OK) // Setze das Ergebnis
                                    finish()  // Schließt die Activity
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp)) // Fügt Platz zwischen den Benutzer-Items hinzu
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }


    @Composable
    fun UserItem(user: User, onClick: (User) -> Unit) {
        Column(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
                .fillMaxWidth()
                .clickable { onClick(user) }
        ) {
            Text(
                text = user.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF318DFF) // Blue for title
            )
        }
    }
}