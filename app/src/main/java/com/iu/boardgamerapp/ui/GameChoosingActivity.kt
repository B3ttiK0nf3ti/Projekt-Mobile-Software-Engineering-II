package com.iu.boardgamerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

class GameChoosingActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setContent {
            BoardGamerAppTheme {
                GameChoosingScreen(
                    firestore = firestore,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

data class Game(
    val game: String = "",
    val votes: Int = 0,
    val documentId: String = "", // Dokument-ID hinzufügen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameChoosingScreen(firestore: FirebaseFirestore, onBackPressed: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var newGame by remember { mutableStateOf("") }
    var games by remember { mutableStateOf(listOf<Game>()) }

    LaunchedEffect(Unit) {
        val gamesCollection = firestore.collection("games")
        gamesCollection.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                val gameList = snapshot.documents.mapNotNull { doc ->
                    val gameName = doc.getString("game") // Den Spielnamen abrufen
                    val votesCount = doc.getLong("votes")?.toInt() ?: 0 // Die Stimmenanzahl abrufen
                    val documentId = doc.id // Dokument-ID abrufen
                    gameName?.let { Game(documentId = documentId, game = it, votes = votesCount) } // Game-Objekt erstellen
                }
                // Spiele nach Votes sortieren (absteigend)
                games = gameList.sortedByDescending { it.votes }
            }
        }
    }

    val filteredGames = games.filter { it.game.contains(searchQuery, ignoreCase = true) }

    Surface(
        color = Color(0xFFE0E0E0), // Grauer Hintergrund
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp) // Abstand zum unteren Rand hinzufügen
        ) {
            // Obere Leiste mit Zurück-Pfeil und Titel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // Weißer Hintergrund
                    .padding(8.dp)
                    .height(60.dp)
            ) {
                IconButton(onClick = onBackPressed) {
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
                        text = "Spielauswahl",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF318DFF),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(48.dp)) // Abstand zum Rand
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suchzeile mit weißem Hintergrund
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) // Abstand zu den Seiten
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Spiel suchen...") },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spieleliste anzeigen
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp) // Abstand links und rechts einfügen
            ) {
                items(filteredGames) { game ->
                    GameItem(game = game, firestore = firestore)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Eingabefeld und Button zum Hinzufügen neuer Spiele mit weißem Hintergrund
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                ) {
                    TextField(
                        value = newGame,
                        onValueChange = { newGame = it }, // Keine Trim-Anwendung hier
                        placeholder = { Text("Neues Spiel eingeben") },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                IconButton(
                    onClick = {
                        val trimmedGame = newGame.trimEnd() // Trimmen nur am Ende
                        if (trimmedGame.isNotEmpty()) { // Überprüfen, ob der Spielname nicht leer ist
                            val newGameObj = Game(game = trimmedGame, votes = 0)
                            firestore.collection("games")
                                .add(newGameObj)
                                .addOnSuccessListener { newGame = "" } // Eingabefeld zurücksetzen
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Hinzufügen",
                        tint = Color(0xFF318DFF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameItem(game: Game, firestore: FirebaseFirestore) {
    var offsetX by remember { mutableStateOf(0f) }
    val gameDocument = firestore.collection("games").document(game.documentId)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .background(
                color = if (offsetX < 0) Color.Red else Color(0xFF318DFF),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                if (offsetX >= 0) {
                    // Stimmenanzahl erhöhen, wenn nicht gelöscht
                    gameDocument.update("votes", game.votes + 1)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < 0) { // Nur nach links wischen
                        offsetX += dragAmount
                    }
                    change.consume()
                }
            }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = game.game,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${game.votes} Votes",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }

    // Löschen, wenn der Benutzer den gesamten Bereich nach links geschoben hat
    LaunchedEffect(offsetX) {
        if (offsetX < -200) { // Wenn mehr als 200 dp nach links geschoben wird
            gameDocument.delete() // Dokument aus Firestore löschen
            offsetX = 0f // Offset zurücksetzen
        }
    }
}
