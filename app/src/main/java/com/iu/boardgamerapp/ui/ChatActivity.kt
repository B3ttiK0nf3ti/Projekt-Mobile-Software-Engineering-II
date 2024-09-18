package com.iu.boardgamerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : ComponentActivity() {
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere UserRepository (du musst AppDatabaseHelper und Kontext bereitstellen)
        val dbHelper = AppDatabaseHelper(this)
        userRepository = UserRepository(dbHelper)

        // Benutzername aus dem Repository laden
        val userName = userRepository.getUser() ?: "Unknown"

        setContent {
            ChatScreen(userName = userName, onBack = { finish() })
        }
    }
}

data class ChatMessage(
    val sender: String,
    val message: String,
    val timestamp: String
)

@Composable
fun ChatScreen(userName: String, onBack: () -> Unit) {
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var newMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zurück-Pfeil oben links
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.Start) // Position oben links
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.Gray
                    )
                }

                // Titel des Dialogs
                Text("Spieleabend-Chat", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                // Nachrichten anzeigen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    chatMessages.forEach { message ->
                        ChatBubble(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Texteingabefeld mit Senden-Symbol rechts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        label = { Text("Nachricht schreiben") },
                        modifier = Modifier.weight(1f) // TextField nimmt den Platz bis zum Icon ein
                    )

                    IconButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            chatMessages = chatMessages + ChatMessage(userName, newMessage, timestamp)
                            newMessage = ""
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Senden",
                            tint = Color(0xFF318DFF) // Grün wie bei WhatsApp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = message.sender,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF318DFF) // Grün wie bei WhatsApp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.message,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.timestamp,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
