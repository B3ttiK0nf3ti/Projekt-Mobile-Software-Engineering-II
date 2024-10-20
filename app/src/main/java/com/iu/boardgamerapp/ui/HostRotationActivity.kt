package com.iu.boardgamerapp.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.firebase.firestore.*
import com.iu.boardgamerapp.R

class HostRotationActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere ViewModel
        val databaseHelper = AppDatabaseHelper()
        val repository = UserRepository(databaseHelper)
        val factory = MainViewModelFactory(repository, databaseHelper, this)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            HostRotationScreen(viewModel)
        }

        // Überwache Änderungen in der Kalenderereignis-Sammlung
        monitorCalendarEvents()
    }

    private fun monitorCalendarEvents() {
        firestore.collection("calendarEvents")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HostRotationActivity", "Fehler beim Überwachen von Ereignissen: ${e.message}", e)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.REMOVED) {
                        val deletedEvent = change.document.toObject(CalendarEvent::class.java)
                        deletedEvent.id = change.document.id
                        Log.d("HostRotationActivity", "Ereignis gelöscht: ${deletedEvent.title}")

                        // Gastgeberrotation erst nach erfolgreichem Löschen des Events
                        rotateHostForDeletedEvent(deletedEvent)
                    }
                }
            }
    }

    private fun rotateHostForDeletedEvent(event: CalendarEvent) {
        viewModel.userList.observe(this) { userList ->
            if (userList.isNotEmpty()) {
                val currentHost = userList.find { it.isHost } // Finde den aktuellen Gastgeber
                val newHost = userList.random() // Wähle zufällig einen neuen Gastgeber aus

                // Firestore Batch, um mehrere Operationen in einer Transaktion durchzuführen
                val batch = firestore.batch()

                // Setze den aktuellen Gastgeber auf isHost = false
                currentHost?.let {
                    val currentHostRef = firestore.collection("users").document(it.firebaseInstallationId)
                    batch.update(currentHostRef, "isHost", false)
                }

                // Setze den neuen Gastgeber auf isHost = true
                val newHostRef = firestore.collection("users").document(newHost.firebaseInstallationId)
                batch.update(newHostRef, "isHost", true)

                // Führe das Batch-Update durch
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("HostRotationActivity", "Gastgeber gewechselt zu: ${newHost.name}")
                        viewModel.setSnackbarMessage(getString(R.string.host_changed_snackbar, newHost.name)) // Snackbar Text
                    }
                    .addOnFailureListener { e ->
                        Log.e("HostRotationActivity", "Fehler beim Ändern des Gastgebers: $e")
                        viewModel.setSnackbarMessage(getString(R.string.error_event_deletion, e.message)) // Fehler Nachricht
                    }
            } else {
                Log.w("HostRotationActivity", getString(R.string.user_list_empty_warning))
            }
        }
    }

    @Composable
    fun HostRotationScreen(viewModel: MainViewModel) {
        val userList by viewModel.userList.observeAsState(emptyList())
        val currentHost by viewModel.currentHost.observeAsState(getString(R.string.loading_host))
        val snackbarHostState = remember { SnackbarHostState() }

        // Snackbar anzeigen, wenn eine Nachricht vorhanden ist
        val snackbarMessage by viewModel.snackbarMessage.observeAsState("")

        LaunchedEffect(snackbarMessage) {
            if (snackbarMessage.isNotEmpty()) {
                snackbarHostState.showSnackbar(snackbarMessage)
                viewModel.setSnackbarMessage("") // Verwende die Methode aus dem ViewModel, um die Nachricht zurückzusetzen
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
                                contentDescription = getString(R.string.back_button),
                                tint = Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = getString(R.string.current_host, currentHost),
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