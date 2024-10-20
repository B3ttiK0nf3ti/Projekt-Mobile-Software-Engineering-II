package com.iu.boardgamerapp.ui

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.installations.FirebaseInstallations
import com.iu.boardgamerapp.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

class ChatActivity : ComponentActivity() {
    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        dbHelper = AppDatabaseHelper()

        dbHelper.getUserWithFirebaseID { userName ->
            val name = userName ?: "Unknown"
            Log.d("ChatActivity", "UserName retrieved: $name")

            setContent {
                ChatScreen(userName = name, onBack = { finish() })
            }
        }
    }
}

data class ChatMessage(
    val user: String = "",
    val message: String = "",
    val timestamp: String = "",
    val firebaseInstallationId: String = ""
)

@Composable
fun ChatScreen(userName: String, onBack: () -> Unit) {
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var newMessage by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

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
                    }.sortedBy { chatMessage ->
                        // Wandelt den String-Timestamp in ein Date-Objekt um
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                        dateFormat.parse(chatMessage.timestamp)
                    }
                    chatMessages = messages
                }
            }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
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
                        onClick = onBack,
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
                        Text(
                            text = "Spieleabend-Chat",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF318DFF),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.width(48.dp))
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    items(chatMessages) { message ->
                        ChatBubble(
                            message = message,
                            isOwnMessage = message.user == userName
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        TextField(
                            value = newMessage,
                            onValueChange = { newMessage = it },
                            placeholder = { Text("Nachricht schreiben") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = {
                            val timestamp = SimpleDateFormat(
                                "dd-MM-yyyy HH:mm:ss",
                                Locale.getDefault()
                            ).apply {
                                timeZone = TimeZone.getTimeZone("Europe/Berlin")
                            }.format(Date())

                            // Firebase Installation ID abrufen
                            FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val firebaseId = task.result ?: return@addOnCompleteListener

                                    val newMessageObj = ChatMessage(
                                        user = userName,
                                        message = newMessage,
                                        timestamp = timestamp,
                                        firebaseInstallationId = firebaseId // Speichern der Installation ID
                                    )

                                    chatMessages = chatMessages + newMessageObj
                                    newMessage = ""

                                    // Nachricht in Firestore speichern
                                    FirebaseFirestore.getInstance().collection("messages")
                                        .add(newMessageObj)
                                } else {
                                    Log.e("ChatActivity", "Fehler beim Abrufen der Firebase Installation ID", task.exception)
                                }
                            }
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
                text = message.user,  // "user" statt "sender"
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
