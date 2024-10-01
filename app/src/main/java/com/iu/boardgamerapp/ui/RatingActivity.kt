package com.iu.boardgamerapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

data class Rating(
    val hostName: String = "",  // Name des Gastgebers
    val hostRating: Int = 0,    // Bewertung für den Gastgeber (0-5)
    val foodRating: Int = 0,     // Bewertung für das Essen (0-5)
    val eveningRating: Int = 0   // Bewertung für den Abend (0-5)
)

class RatingActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance() // Firestore Instanz

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentHost =
            intent.getStringExtra("currentHost") ?: "Unbekannt" // Hole den aktuellen Gastgeber

        setContent {
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE0E0E0) // Gleiche hellgraue Hintergrundfarbe
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TopAppBar im Stil der HostRotationActivity
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Bewertungen",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Aktion, um zurückzugehen */ }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(
                            0xFF318DFF
                        )
                    ) // Blaue Farbe
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Zeige den aktuellen Gastgeber an
                Text(
                    text = "Bewerte den Gastgeber: $currentHost",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bewertung für den Gastgeber
                Text("Bewerte den Gastgeber")
                Slider(
                    value = hostRating,
                    onValueChange = { hostRating = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF318DFF), // Farbe des Daumens
                        activeTrackColor = Color(0xFF318DFF), // Farbe des aktiven Tracks
                        inactiveTrackColor = Color.Gray // Farbe des inaktiven Tracks
                    )
                )
                Text("Gastgeber Bewertung: ${hostRating.toInt()}")

                Spacer(modifier = Modifier.height(16.dp))

                // Bewertung für das Essen
                Text("Bewerte das Essen")
                Slider(
                    value = foodRating,
                    onValueChange = { foodRating = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF318DFF), // Farbe des Daumens
                        activeTrackColor = Color(0xFF318DFF), // Farbe des aktiven Tracks
                        inactiveTrackColor = Color.Gray // Farbe des inaktiven Tracks
                    )
                )
                Text("Essen Bewertung: ${foodRating.toInt()}")

                Spacer(modifier = Modifier.height(16.dp))

                // Bewertung für den Abend
                Text("Bewerte den Abend")
                Slider(
                    value = eveningRating,
                    onValueChange = { eveningRating = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF318DFF), // Farbe des Daumens
                        activeTrackColor = Color(0xFF318DFF), // Farbe des aktiven Tracks
                        inactiveTrackColor = Color.Gray // Farbe des inaktiven Tracks
                    )
                )
                Text("Abend Bewertung: ${eveningRating.toInt()}")

                Spacer(modifier = Modifier.height(32.dp))

                // Button zum Absenden der Bewertungen
                Button(
                    onClick = {
                        // Rufe die Callback-Funktion auf, um die Bewertungen zu übergeben
                        onSubmit(hostRating.toInt(), foodRating.toInt(), eveningRating.toInt())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)) // Blaue Schaltfläche
                ) {
                    Text("Bewertung abschicken", color = Color.White)
                }
            }
        }
    }
}