package com.iu.boardgamerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatActivity : ComponentActivity() {
    private lateinit var userRepository: UserRepository
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        val dbHelper = AppDatabaseHelper(this)
        userRepository = UserRepository(dbHelper)

        val userName = userRepository.getUser() ?: "Unknown"

        setContent {
            ChatScreen(userName = userName, onBack = { finish() })
        }
    }
}

data class ChatMessage(
    val sender: String = "",
    val message: String = "",
    val timestamp: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(userName: String, onBack: () -> Unit) {
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var newMessage by remember { mutableStateOf("") }

    fun getSortedMessages(messages: List<ChatMessage>): List<ChatMessage> {
        return messages.sortedBy { it.timestamp }
    }

    // Firestore Listener
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("messages")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }
                    chatMessages = messages
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE0E0E0) // Hellgrauer Hintergrund
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.Gray
                    )
                }

                Text(
                    text = "Spieleabend-Chat",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF318DFF) // Blau
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    getSortedMessages(chatMessages).forEach { message ->
                        ChatBubble(message = message, isOwnMessage = message.sender == userName)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, shape = RoundedCornerShape(8.dp)) // Hintergrundfarbe auf Weiß setzen
                    ) {
                        TextField(
                            value = newMessage,
                            onValueChange = { newMessage = it },
                            placeholder = { Text("Nachricht schreiben") },
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent, // Transparent für den Hintergrund
                                focusedIndicatorColor = Color.Transparent, // Keine Linie bei Fokus
                                unfocusedIndicatorColor = Color.Transparent // Keine Linie bei Fokus
                            ),
                            modifier = Modifier.fillMaxWidth() // TextField füllt die Box
                        )
                    }

                    IconButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("Europe/Berlin")
                            }.format(Date())

                            val newMessageObj = ChatMessage(userName, newMessage, timestamp)
                            chatMessages = chatMessages + newMessageObj
                            newMessage = ""

                            // Nachricht in Firestore speichern
                            FirebaseFirestore.getInstance().collection("messages")
                                .add(newMessageObj)
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Senden",
                            tint = Color(0xFF318DFF)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ChatBubble(message: ChatMessage, isOwnMessage: Boolean) {
    val alignment = if (isOwnMessage) Alignment.End else Alignment.Start
    val backgroundColor = if (isOwnMessage) Color(0xFF318DFF) else Color.White
    val textColor = if (isOwnMessage) Color.White else Color.Black

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                .padding(8.dp)
                .widthIn(max = 250.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.sender,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isOwnMessage) Color.White else Color(0xFF318DFF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.message,
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.timestamp,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}
