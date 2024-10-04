package com.iu.boardgamerapp.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GameChoosingActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var appDatabaseHelper: AppDatabaseHelper

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        appDatabaseHelper =
            AppDatabaseHelper(this) // Or however you initialize your AppDatabaseHelper

        setContent {
            BoardGamerAppTheme {
                GameChoosingScreen(
                    firestore = firestore,
                    appDatabaseHelper = appDatabaseHelper,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

data class Game(
    val game: String = "",
    val votes: Int = 0,
    val documentId: String = "",
    val votedUsers: List<String> = listOf()
)

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameChoosingScreen(
    firestore: FirebaseFirestore,
    appDatabaseHelper: AppDatabaseHelper,
    onBackPressed: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var newGame by remember { mutableStateOf("") }
    var games by remember { mutableStateOf(listOf<Game>()) }
    var currentUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        currentUserId = getCurrentUserId(appDatabaseHelper)
        loadGames(firestore) { loadedGames ->
            games = loadedGames
        }
    }

    Surface(
        color = Color(0xFFE0E0E0),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Top bar with back arrow, title, and reset button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
                    .height(60.dp)
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = "Game Selection",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF318DFF),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                IconButton(
                    onClick = {
                        currentUserId?.let { userId ->
                            resetUserVotes(firestore, userId) {
                                loadGames(firestore) { loadedGames ->
                                    games = loadedGames
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset My Votes",
                        tint = Color(0xFF318DFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar with white background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Nach einem Spiel suchen...") },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game list
            val filteredGames = games.filter { it.game.contains(searchQuery, ignoreCase = true) }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredGames) { game ->
                    GameItem(
                        game = game,
                        firestore = firestore,
                        currentUserId = currentUserId,
                        onVote = { votedGame ->
                            games = games.map { if (it.documentId == votedGame.documentId) votedGame else it }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input field and button to add new games with white background
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
                        onValueChange = { newGame = it },
                        placeholder = { Text("Neues Spiel hinzufügen") },
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
                        val trimmedGame = newGame.trimEnd()
                        if (trimmedGame.isNotEmpty()) {
                            val newGameObj = Game(game = trimmedGame, votes = 0)
                            firestore.collection("games")
                                .add(newGameObj)
                                .addOnSuccessListener { newGame = "" }
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = Color(0xFF318DFF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameItem(
    game: Game,
    firestore: FirebaseFirestore,
    currentUserId: String?,
    onVote: (Game) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isDeleting by remember { mutableStateOf(false) } // Flag to track deletion state
    val gameDocument = firestore.collection("games").document(game.documentId)
    val hasVoted = currentUserId != null && game.votedUsers.contains(currentUserId)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .background(
                color = if (offsetX < 0) Color.Red else Color(0xFF318DFF),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                if (offsetX >= 0 && !hasVoted && currentUserId != null) {
                    voteForGame(gameDocument, currentUserId, game) { updatedGame ->
                        onVote(updatedGame)
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { /* Reset deletion state when drag starts */
                        isDeleting = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount < 0 && !isDeleting) { // Allow dragging left only when not deleting
                            offsetX += dragAmount
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        // Check if the game has no votes before allowing deletion
                        if (offsetX < -150 && !isDeleting && game.votes == 0) {
                            isDeleting = true // Set flag to prevent multiple deletions
                            gameDocument.delete()
                            offsetX = 0f
                        } else {
                            // Reset swipe position if not enough distance covered or if game has votes
                            offsetX = 0f
                        }
                    }
                )
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

            if (hasVoted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Voted",
                    tint = Color.White
                )
            }
        }
    }
}

suspend fun getCurrentUserId(appDatabaseHelper: AppDatabaseHelper): String? {
    return suspendCoroutine { continuation ->
        appDatabaseHelper.getUserWithFirebaseID { userId ->
            continuation.resume(userId)
        }
    }
}

fun loadGames(firestore: FirebaseFirestore, onGamesLoaded: (List<Game>) -> Unit) {
    firestore.collection("games").addSnapshotListener { snapshot, e ->
        if (e != null) return@addSnapshotListener
        if (snapshot != null) {
            val gameList = snapshot.documents.mapNotNull { doc ->
                val gameName = doc.getString("game")
                val votesCount = doc.getLong("votes")?.toInt() ?: 0
                val documentId = doc.id
                val votedUsers = doc.get("votedUsers") as? List<String> ?: listOf()
                gameName?.let {
                    Game(
                        documentId = documentId,
                        game = it,
                        votes = votesCount,
                        votedUsers = votedUsers
                    )
                }
            }
            onGamesLoaded(gameList.sortedByDescending { it.votes })
        }
    }
}

fun voteForGame(
    gameDocument: DocumentReference,
    userId: String,
    game: Game,
    onVoteComplete: (Game) -> Unit
) {
    gameDocument.update(
        mapOf(
            "votes" to FieldValue.increment(1),
            "votedUsers" to FieldValue.arrayUnion(userId)
        )
    ).addOnSuccessListener {
        val updatedGame = game.copy(
            votes = game.votes + 1,
            votedUsers = game.votedUsers + userId
        )
        onVoteComplete(updatedGame)
    }
}

fun resetUserVotes(firestore: FirebaseFirestore, userId: String, onComplete: () -> Unit) {
    firestore.collection("games").whereArrayContains("votedUsers", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            for (document in snapshot.documents) {
                val docRef = firestore.collection("games").document(document.id)
                batch.update(docRef, "votes", FieldValue.increment(-1))
                batch.update(docRef, "votedUsers", FieldValue.arrayRemove(userId))
            }
            batch.commit().addOnCompleteListener {
                onComplete()
            }
        }
}
