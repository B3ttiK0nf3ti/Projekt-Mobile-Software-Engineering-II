package com.iu.boardgamerapp.ui.components

import BoxWithBorder
import com.iu.boardgamerapp.ui.ChatActivity
import GameSelectionDialog
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iu.boardgamerapp.ui.MainViewModel
import com.iu.boardgamerapp.ui.UserNameInputDialog

@Composable
fun MainScreen(viewModel: MainViewModel, navController: NavController, onRotateHost: () -> Unit) {
    val userName by viewModel.userName.observeAsState("")
    val currentHost by viewModel.currentHost.observeAsState("") // Aktueller Gastgeber
    val gameSuggestions by viewModel.gameSuggestions.observeAsState(emptyList())
    val votes by viewModel.votes.observeAsState(emptyMap())
    val selectedGame by viewModel.selectedGame.observeAsState("")
    val rating by viewModel.rating.observeAsState(0)
    val showDialog by viewModel.showGameSelectionDialog.observeAsState(false)

    // Dialog-Zustände
    var showNameDialog by remember { mutableStateOf(userName.isEmpty()) }
    var showHostDialog by remember { mutableStateOf(false) } // Zustand für den Gastgeber-Dialog

    val context = LocalContext.current

    // Dialog für Benutzernamen
    if (showNameDialog) {
        UserNameInputDialog(
            onNameEntered = { name ->
                viewModel.saveUser(name)
                showNameDialog = false
            },
            onDismiss = { showNameDialog = false }
        )
    }

    // Dialog für den Gastgeber
    if (showHostDialog) {
        UserNameInputDialog(
            onNameEntered = { name ->
                viewModel.changeHost(name) // Neuen Gastgeber setzen
                showHostDialog = false
            },
            onDismiss = { showHostDialog = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Willkommen, $userName!")
            Spacer(modifier = Modifier.height(16.dp))
            DateDisplay(date = "12.09.2024")
            Spacer(modifier = Modifier.height(16.dp))

            // Abstimmen und Anzeige der Spielauswahl
            Button(onClick = { viewModel.toggleGameSelectionDialog() }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectedGame)
                    Text("Stimmen: ${votes[selectedGame] ?: 0}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Aktueller Gastgeber: $currentHost") // Zeige den aktuellen Gastgeber an

            Button(onClick = { showHostDialog = true }) {
                Text("Gastgeber ändern")
            }

            Spacer(modifier = Modifier.height(16.dp))
            BoxWithBorder(content = "Ort: Bei Alex")
            Spacer(modifier = Modifier.height(16.dp))
            BoxWithBorder(content = "Essen: Pizza")

            Spacer(modifier = Modifier.height(16.dp))

            if (showDialog) {
                GameSelectionDialog(
                    gameSuggestions = gameSuggestions,
                    votes = votes,
                    onGameSelected = { game ->
                        viewModel.voteForGame(game)
                        viewModel.toggleGameSelectionDialog() // Dialog schließen
                    },
                    onAddGame = { newGame ->
                        viewModel.addGame(newGame)
                    },
                    onDismiss = { viewModel.toggleGameSelectionDialog() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Bewertung: $rating")
            Slider(
                value = rating.toFloat(),
                onValueChange = { viewModel.updateRating(it.toInt()) },
                valueRange = 0f..5f,
                steps = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button zum Rotieren des Hosts
            Button(onClick = { onRotateHost() }) {
                Text("Gastgeber wechseln")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Kalender-Symbol links unten
                IconButton(
                    onClick = {
                        navController.navigate("game_schedule")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = "Kalender",
                        tint = Color(0xFF318DFF)
                    )
                }

                // Chat-Symbol rechts unten
                IconButton(
                    onClick = {
                        val intent = Intent(context, ChatActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = "Chat",
                        tint = Color(0xFF318DFF)
                    )
                }
            }
        }
    }
}
