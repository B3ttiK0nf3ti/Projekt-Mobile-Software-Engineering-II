package com.iu.boardgamerapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Rating(
    val hostName: String = "",  // Name des Gastgebers
    val hostRating: Int = 0,    // Bewertung für den Gastgeber (0-5)
    val foodRating: Int = 0,     // Bewertung für das Essen (0-5)
    val eveningRating: Int = 0   // Bewertung für den Abend (0-5)
)

class RatingActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance() // Firestore Instanz
    private val auth = FirebaseAuth.getInstance() // Firebase Auth Instanz

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hole den aktuellen Gastgeber aus Firestore
        setContent {
            var currentHost by remember { mutableStateOf("Lade...") } // Standardwert beim Laden
            LaunchedEffect(Unit) {
                currentHost = fetchCurrentHost() // Hole den aktuellen Gastgeber
            }

            RatingScreen(currentHost) { hostRating, foodRating, eveningRating ->
                // Speichere die Bewertung in Firestore
                saveRating(currentHost, hostRating, foodRating, eveningRating)
            }
        }
    }

    private fun saveRating(hostName: String, hostRating: Int, foodRating: Int, eveningRating: Int) {
        val rating = Rating(hostName, hostRating, foodRating, eveningRating)

        db.collection("ratings") // Hier wird die Collection definiert, wo die Bewertungen gespeichert werden
            .add(rating)
            .addOnSuccessListener {
                // Erfolg
                Toast.makeText(this, "Bewertung erfolgreich gespeichert!", Toast.LENGTH_SHORT)
                    .show()
                finish() // Optional: Schließe die Activity nach dem Speichern
            }
            .addOnFailureListener { e ->
                // Fehlerbehandlung
                Toast.makeText(
                    this,
                    "Fehler beim Speichern der Bewertung: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RatingScreen(currentHost: String, onSubmit: (Int, Int, Int) -> Unit) {
        var hostRating by remember { mutableStateOf(0f) }
        var foodRating by remember { mutableStateOf(0f) }
        var eveningRating by remember { mutableStateOf(0f) }

        var averageHostRating by remember { mutableStateOf(0f) }
        var averageFoodRating by remember { mutableStateOf(0f) }
        var averageEveningRating by remember { mutableStateOf(0f) }

        // Bewertungen abrufen, wenn der Screen geladen wird
        LaunchedEffect(currentHost) {
            if (currentHost != "Lade...") {
                val averages = fetchAverageRatings(currentHost)
                averageHostRating = averages[0]
                averageFoodRating = averages[1]
                averageEveningRating = averages[2]
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
                            // Zeige den aktuellen Gastgeber an
                            Text(
                                text = "Bewerte den Gastgeber: $currentHost",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF318DFF),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // LazyColumn für scrollbare Slider
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)  // Horizontaler Abstand für die Slider
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            RatingSlider("Bewerte den Gastgeber", hostRating) { hostRating = it }
                            Spacer(modifier = Modifier.height(24.dp))
                            RatingSlider("Bewerte das Essen", foodRating) { foodRating = it }
                            Spacer(modifier = Modifier.height(24.dp))
                            RatingSlider("Bewerte den Abend", eveningRating) { eveningRating = it }
                            Spacer(modifier = Modifier.height(48.dp))

                            // Durchschnittsbewertungen anzeigen
                            Text(
                                "Durchschnittliche Bewertungen:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF318DFF)  // Gleiche Farbe wie die Überschrift
                            )
                            Text(
                                "Gastgeber: ${"%.2f".format(averageHostRating)}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Essen: ${"%.2f".format(averageFoodRating)}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Abend: ${"%.2f".format(averageEveningRating)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                onSubmit(
                                    hostRating.toInt(),
                                    foodRating.toInt(),
                                    eveningRating.toInt()
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),  // Button über die gesamte Breite
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF))
                        ) {
                            Text("Bewertung abschicken", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchCurrentHost(): String {
        // Zugriff auf die Benutzer-Sammlung
        val userSnapshot = db.collection("user")
            .whereEqualTo("isHost", true) // Suche nach Benutzern, die Gastgeber sind
            .get()
            .await()

        // Überprüfe, ob der Snapshot nicht leer ist und gib den Namen zurück
        return if (!userSnapshot.isEmpty) {
            val hostUser = userSnapshot.documents[0] // Angenommen, es gibt nur einen Gastgeber
            hostUser.getString("name") ?: "Unbekannt" // Nutze den Namen des Benutzers
        } else {
            "Unbekannt" // Wenn kein Gastgeber gefunden wird
        }
    }

    // Funktion zum Abrufen der durchschnittlichen Bewertungen
    private suspend fun fetchAverageRatings(hostName: String): List<Float> {
        val ratingsSnapshot = db.collection("ratings")
            .whereEqualTo("hostName", hostName)
            .get()
            .await()

        var totalHostRating = 0f
        var totalFoodRating = 0f
        var totalEveningRating = 0f
        val count = ratingsSnapshot.size()

        for (document in ratingsSnapshot) {
            val rating = document.toObject(Rating::class.java)
            totalHostRating += rating.hostRating
            totalFoodRating += rating.foodRating
            totalEveningRating += rating.eveningRating
        }

        return if (count > 0) {
            listOf(
                totalHostRating / count,
                totalFoodRating / count,
                totalEveningRating / count
            )
        } else {
            listOf(0f, 0f, 0f) // Keine Bewertungen vorhanden
        }
    }


    @Composable
    private fun RatingSlider(label: String, rating: Float, onRatingChanged: (Float) -> Unit) {
        Column {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF318DFF)  // Gleiche Farbe wie die Gesamtüberschrift
            )
            Slider(
                value = rating,
                onValueChange = { onRatingChanged(it) },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF318DFF),
                    activeTrackColor = Color(0xFF318DFF),
                    inactiveTrackColor = Color.Gray
                ),
                modifier = Modifier.padding(horizontal = 16.dp)  // Slider Padding vom Rand
            )
        }
    }
}