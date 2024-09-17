package com.iu.boardgamerapp.ui.components

import BoxWithBorder
import ChatDialog
import GameSelectionDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iu.boardgamerapp.ui.MainViewModel
import com.iu.boardgamerapp.ui.UserNameInputDialog


@Composable
fun MainScreen(viewModel: MainViewModel) {
    val userName by viewModel.userName.observeAsState("")
    val gameSuggestions by viewModel.gameSuggestions.observeAsState(emptyList())
    val votes by viewModel.votes.observeAsState(emptyMap())
    val selectedGame by viewModel.selectedGame.observeAsState("")
    val rating by viewModel.rating.observeAsState(0)
    val showDialog by viewModel.showGameSelectionDialog.observeAsState(false)
    val showChatDialog by viewModel.showChatDialog.observeAsState(false)
    val chatMessages by viewModel.chatMessages.observeAsState(emptyList())
    val newMessage by viewModel.newMessage.observeAsState("")

    if (userName.isEmpty()) {
        UserNameInputDialog(onNameEntered = { name ->
            viewModel.saveUser(name)
        })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Willkommen, $userName!")

            Spacer(modifier = Modifier.height(16.dp))

            // Datum anzeigen
            DateDisplay(date = "12.09.2024")
            Spacer(modifier = Modifier.height(16.dp))

            // Abstimmen und Anzeige der Spielauswahl
            Button(onClick = { viewModel.toggleGameSelectionDialog() }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectedGame)
                    Text("Stimmen: ${votes[selectedGame] ?: 0}", style = MaterialTheme.typography.bodySmall)
                }
            }
            //Ort und Essen Anzeigen
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

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                IconButton(onClick = { viewModel.toggleChatDialog() }) {
                    Icon(Icons.Filled.Message, contentDescription = "Chat", tint = Color(0xFFFF5252))
                }
            }

            if (showChatDialog) {
                ChatDialog(
                    chatMessages = chatMessages,
                    newMessage = newMessage,
                    onMessageChange = { viewModel.updateNewMessage(it) },
                    onSendMessage = {
                        viewModel.sendMessage()
                    },
                    onDismiss = { viewModel.toggleChatDialog() }
                )
            }
        }
    }
}




