package com.iu.boardgamerapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.components.MainScreen
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val databaseHelper = AppDatabaseHelper()
        val userRepository = UserRepository(databaseHelper)
        MainViewModelFactory(userRepository, databaseHelper, this)
    }

    private lateinit var hostRotationActivityResultLauncher: ActivityResultLauncher<Intent>
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Überprüfe, ob der Nutzer eingeloggt ist
        checkUserLoggedIn()
        scheduleExpiredEventWorker()
        listenForEventChanges()

        // Initialisiere den ActivityResultLauncher für die HostRotationActivity
        hostRotationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.loadCurrentHost()
                Toast.makeText(this, "Gastgeber erfolgreich gewechselt!", Toast.LENGTH_SHORT).show()
            }
        }

        // Observer für die Benutzerliste
        viewModel.loadUsers()
        viewModel.userList.observe(this) { users ->
            Log.d("MainActivity", "Benutzerliste aus dem ViewModel: ${users.joinToString()}")
            if (users.isEmpty()) {
                Toast.makeText(this, "Keine Benutzer zur Auswahl", Toast.LENGTH_SHORT).show()
            }
        }

        // Setze den Inhalt mit NavHost und MainScreen
        setContent {
            BoardGamerAppTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        MainScreen(
                            viewModel = viewModel,
                            onNavigateToGameSchedule = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        GameScheduleActivity::class.java
                                    )
                                )
                            },
                            onNavigateToHostRotation = {
                                hostRotationActivityResultLauncher.launch(
                                    Intent(
                                        this@MainActivity,
                                        HostRotationActivity::class.java
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun listenForEventChanges() {
        firestore.collection("calendarEvents")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MainActivity", "Fehler beim Abrufen von Ereignissen: $e")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    for (document in snapshot.documents) {
                        val event = document.toObject(CalendarEvent::class.java)
                        // Hier könnte man eine Logik implementieren, die prüft, ob das Ereignis abgelaufen ist
                        if (event != null && event.endTime.toDate().time < System.currentTimeMillis()) {
                            Log.d("MainActivity", "Ereignis abgelaufen: ${event.title}")
                            // Hier wird nichts mehr getan, das Löschen wird durch den Worker behandelt
                        }
                    }
                }
            }
    }

    private fun scheduleExpiredEventWorker() {
        // OneTimeWorkRequest für sofortige Ausführung
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ExpiredEventWorker>().build()
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)

        // PeriodicWorkRequest für regelmäßige Ausführung alle 15 Minuten
        val periodicWorkRequest = PeriodicWorkRequestBuilder<ExpiredEventWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(periodicWorkRequest)
    }

        private fun checkUserLoggedIn() {
        val databaseHelper = AppDatabaseHelper()
        databaseHelper.getUserWithFirebaseID { username ->
            if (username == null) {
                // Nutzer ist nicht eingeloggt, starte LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Beende MainActivity, damit der Nutzer nicht zurückkehren kann
            } else {
                // Setze den Nutzernamen im ViewModel
                viewModel.setUserName(username)
            }
        }
    }
}
