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
import com.google.firebase.firestore.EventListener
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import com.iu.boardgamerapp.ui.datamodel.User
import kotlinx.coroutines.delay
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

        // Laden der Benutzerliste sicherstellen
        viewModel.loadUsers()

        setContent {
            HostRotationScreen(viewModel)
        }

        // Überwachung der Änderungen in der Firestore-Sammlung
        listenForCalendarEventChanges()
    }

    private fun listenForCalendarEventChanges() {
        firestore.collection("calendarEvents")
            .addSnapshotListener(EventListener { snapshot, e ->
                if (e != null) {
                    Log.e("HostRotationActivity", "Fehler beim Abrufen von Ereignissen: ${e.message}", e)
                    return@EventListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                val removedEvent = change.document.toObject(CalendarEvent::class.java)
                                removedEvent.id = change.document.id
                                Log.d("HostRotationActivity", "Ereignis entfernt: ${removedEvent.title}")
                                // Gastgeberwechsel, wenn das Ereignis entfernt wird
                                rotateHostForRemovedEvent(removedEvent)
                            }
                            com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                Log.d("HostRotationActivity", "Ereignis hinzugefügt: ${change.document.id}")
                                // Optional: Sie können hier Logik hinzufügen, wenn ein Ereignis hinzugefügt wird.
                            }
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                Log.d("HostRotationActivity", "Ereignis geändert: ${change.document.id}")
                                // Optional: Sie können hier Logik hinzufügen, wenn ein Ereignis geändert wird.
                            }
                        }
                    }
                } else {
                    Log.w("HostRotationActivity", "Keine Ereignisse gefunden.")
                }
            })
    }

    private fun rotateHostForRemovedEvent(event: CalendarEvent) {
        viewModel.userList.observe(this) { userList ->
            if (userList.isNotEmpty()) {
                val currentHost = userList.find { it.isHost }

                if (currentHost != null) {
                    val filteredList = userList.filter { it.name != currentHost.name }

                    if (filteredList.isNotEmpty()) {
                        val newHost = filteredList.random()
                        event.title = "${event.title} - Hosted by ${newHost.name}"

                        // Vor dem Update prüfen, ob das Dokument existiert
                        firestore.collection("calendarEvents").document(event.id).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    firestore.collection("calendarEvents").document(event.id)
                                        .update("title", event.title, "currentHost", newHost.name)
                                        .addOnSuccessListener {
                                            Log.d("HostRotationActivity", "Firestore erfolgreich aktualisiert")
                                            viewModel.changeHost(newHost.name) {
                                                viewModel.loadCurrentHost()
                                                setResult(Activity.RESULT_OK)
                                                finish()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("HostRotationActivity", "Fehler beim Aktualisieren von Firestore: ${e.message}")
                                        }
                                } else {
                                    Log.w("HostRotationActivity", "Das Dokument existiert nicht, daher kann es nicht aktualisiert werden.")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("HostRotationActivity", "Fehler beim Abrufen des Dokuments: ${e.message}")
                            }
                    } else {
                        Log.w("HostRotationActivity", "Keine anderen Benutzer verfügbar, um den Gastgeber zu wechseln.")
                    }
                } else {
                    Log.w("HostRotationActivity", "Aktueller Gastgeber konnte nicht gefunden werden.")
                }
            } else {
                Log.w("HostRotationActivity", "Benutzerliste ist leer, kein Gastgeberwechsel möglich.")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class) // Suppress experimental API warning
    @Composable
    fun HostRotationScreen(viewModel: MainViewModel) {
        val userList by viewModel.userList.observeAsState(emptyList())
        val currentHost by viewModel.currentHost.observeAsState("Lade Gastgeber...")
        val snackbarHostState = remember { SnackbarHostState() }

        // Snackbar anzeigen, wenn eine Nachricht vorhanden ist
        val snackbarMessage by viewModel.snackbarMessage.observeAsState("")

        LaunchedEffect(snackbarMessage) {
            if (snackbarMessage.isNotEmpty()) {
                snackbarHostState.showSnackbar(snackbarMessage)
                delay(2000) // Verzögerung hinzufügen, um die Snackbar länger anzuzeigen
                viewModel.setSnackbarMessage("") // Nachricht nach Verzögerung zurücksetzen
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE0E0E0)
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
                            Text(
                                text = "Gastgeberwechsel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF318DFF),
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
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aktueller Gastgeber: $currentHost",
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
                                viewModel.changeHost(selectedUser.name) {
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // SnackbarHost
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(snackbarData = data)
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