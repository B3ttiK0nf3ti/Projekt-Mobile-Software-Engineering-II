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

        // Initialize Firestore and UserRepository
        firestore = FirebaseFirestore.getInstance()
        userRepository = UserRepository(AppDatabaseHelper(this))

        setContent {
            var userName by remember { mutableStateOf("Unknown") }

            // Fetch user name
            LaunchedEffect(Unit) {
                userRepository.getUser { name ->
                    userName = name ?: "Unknown"
                }
            }

            ChatScreen(userName = userName, onBack = { finish() }, firestore = firestore)
        }
    }
}

data class ChatMessage(
    val sender: String = "",
    val message: String = "",
    val timestamp: String = ""
)

@Composable
fun ChatScreen(userName: String, onBack: () -> Unit, firestore: FirebaseFirestore) {
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var newMessage by remember { mutableStateOf("") }

    // Firestore Listener
    LaunchedEffect(Unit) {
        firestore.collection("messages")
            .orderBy("timestamp") // Order by timestamp to get sorted messages
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val messages = it.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }
                    chatMessages = messages
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE0E0E0)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.White)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.Gray
                    )
                }

                Text(
                    text = "Spieleabend-Chat",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF318DFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .wrapContentSize(Alignment.Center)
                )
            }

            // Messages list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                chatMessages.forEach { message ->
                    ChatBubble(message = message, isOwnMessage = message.sender == userName)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Message input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Nachricht schreiben") },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                )

                IconButton(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("Europe/Berlin")
                        }.format(Date())

                        val newMessageObj = ChatMessage(sender = userName, message = newMessage, timestamp = timestamp)
                        newMessage = ""

                        // Save message to Firestore
                        firestore.collection("messages")
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

@Composable
fun ChatBubble(message: ChatMessage, isOwnMessage: Boolean) {
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
