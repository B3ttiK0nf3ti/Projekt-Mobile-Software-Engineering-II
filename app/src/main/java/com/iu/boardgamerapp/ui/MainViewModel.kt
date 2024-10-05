package com.iu.boardgamerapp.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.ui.datamodel.User
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.State

class MainViewModel(
    private val userRepository: UserRepository,
    private val databaseHelper: AppDatabaseHelper,
    private val context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _snackbarMessage = MutableLiveData<String>("")
    val snackbarMessage: LiveData<String> get() = _snackbarMessage

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    private val _userExists = MutableLiveData<Boolean>()
    val userExists: LiveData<Boolean> = _userExists

    private val _gameSuggestions = MutableLiveData<List<String>>(emptyList())
    val gameSuggestions: LiveData<List<String>> = _gameSuggestions

    private val _votes = MutableLiveData<Map<String, Int>>(emptyMap())
    val votes: LiveData<Map<String, Int>> = _votes

    private val _selectedGame = MutableLiveData<String>("")
    val selectedGame: LiveData<String> = _selectedGame

    private val _rating = MutableLiveData<Int>(0)
    val rating: LiveData<Int> = _rating

    private val _showGameSelectionDialog = MutableLiveData<Boolean>(false)
    val showGameSelectionDialog: LiveData<Boolean> = _showGameSelectionDialog

    // LiveData für die Benutzerliste
    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> get() = _userList

    // LiveData für den aktuellen Gastgeber
    private val _currentHost = MutableLiveData<String>()
    val currentHost: LiveData<String> get() = _currentHost

    private val _showNameDialog = mutableStateOf(false)
    val showNameDialog: State<Boolean> get() = _showNameDialog

    // Liste der Spiele als LiveData
    private val _gameList = MutableLiveData<List<Game>>()
    val gameList: LiveData<List<Game>> = _gameList

    // Spiel mit den meisten Votes
    private val _topVotedGame = MutableLiveData<Game?>()
    val topVotedGame: LiveData<Game?> = _topVotedGame

    // Neue LiveData für mehrere Top-Spiele
    private val _hasMultipleTopGames = MutableLiveData<Boolean>(false)
    val hasMultipleTopGames: LiveData<Boolean> get() = _hasMultipleTopGames

    // Callback für die Navigation
    var onNavigateToHostRotation: (() -> Unit)? = null

    private var currentHostName: String? = null

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        loadUsers()         // Lädt die Benutzer beim Start des ViewModels
        loadCurrentHost()   // Lädt den aktuellen Gastgeber beim Start des ViewModels
        loadGames()         // Optional, lädt auch die Spiele
    }

    fun addUser(name: String, callback: (Boolean) -> Unit) {
        userRepository.addUser(name, callback)
    }

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun saveUser(name: String) {
        _userName.value = name
        saveUserNameToPreferences(name)
        databaseHelper.addUser(name) { success ->
            if (success) {
                Log.d("ViewModel", "Benutzer erfolgreich gespeichert: $name")
            } else {
                Log.w("ViewModel", "Fehler beim Speichern des Benutzers: $name")
            }
        }
    }

    fun saveUserNameToPreferences(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
    }

    fun clearUserName() {
        sharedPreferences.edit().remove("user_name").apply()
        _userName.value = ""
        _userExists.value = false
    }

    fun loadUserName() {
        val savedUserName = sharedPreferences.getString("user_name", "") ?: "" // Fallback zu leerem String
        _userName.value = savedUserName

        if (savedUserName.isNotEmpty()) {
            // Überprüfe, ob der Benutzer in der Datenbank existiert
            checkUserExists(savedUserName) { exists ->
                if (!exists) {
                    // Benutzer existiert nicht, zeige das Dialogfenster an
                    _showNameDialog.value = true // MutableState für den Dialog
                }
            }
        } else {
            // Zeige das Dialogfenster an, wenn kein Benutzername gespeichert ist
            _showNameDialog.value = true
        }
    }

    fun checkUserExists(name: String, onComplete: (Boolean) -> Unit) {
        val trimmedName = name.trim() // Hier trimmen wir den Namen
        Log.d("MainViewModel", "Überprüfe, ob Benutzer existiert: $trimmedName") // Log-Ausgabe für Debugging

        databaseHelper.checkUserExists(trimmedName) { exists ->
            Log.d("MainViewModel", "Benutzerexistenz: $exists")
            if (!exists) {
                clearUserName()
            }
            onComplete(exists)
        }
    }

    fun loadCurrentHost() {
        userRepository.getCurrentHost { hostName ->
            Log.d("MainViewModel", "Geladener Gastgeber: $hostName")
            _currentHost.value = hostName ?: "Niemand" // Setze den aktuellen Host
        }
    }

    fun setSnackbarMessage(message: String) {
        _snackbarMessage.value = message // Hier verwenden wir die MutableLiveData, um den Wert zu setzen
    }

    fun updateHost(newHostName: String) {
        userRepository.updateHostStatus(newHostName) {
            // Nach dem Aktualisieren den aktuellen Gastgeber erneut laden
            loadCurrentHost()
        }
    }

    fun loadUsers() {
        userRepository.getAllUsers { users ->
            _userList.value = users // Speichern der Benutzerliste in der LiveData-Variable
        }
    }

    // Methode zum Laden der Spiele aus Firestore
    private fun loadGames() {
        firestore.collection("games").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                val games = snapshot.documents.mapNotNull { doc ->
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
                // Liste der Spiele aktualisieren
                _gameList.value = games

                // Spiel mit den meisten Votes ermitteln und aktualisieren
                _topVotedGame.value = games.maxByOrNull { it.votes }

                // Überprüfen, ob es mehrere Top-Spiele gibt
                val maxVotes = games.maxOfOrNull { it.votes } ?: 0
                _hasMultipleTopGames.value = games.count { it.votes == maxVotes } > 1
            }
        }
    }

    fun changeHost(newHostName: String, onSuccess: () -> Unit) {
        Log.d("MainViewModel", "Wechsle Gastgeber zu: $newHostName")

        userRepository.getUserByName(newHostName) { user ->
            if (user != null) {
                Log.d("MainViewModel", "Benutzer gefunden: ${user.name}")
                userRepository.updateHostStatus(user.name) {
                    loadCurrentHost() // Aktualisiert den aktuellen Gastgeber nach dem Wechsel
                    loadUsers()       // Aktualisiert die Benutzerliste
                    _snackbarMessage.value = "Gastgeber gewechselt zu: $newHostName" // Snackbar-Nachricht setzen
                    onSuccess()
                }
            } else {
                Log.w("MainViewModel", "Neuer Gastgeber nicht gefunden: $newHostName")
            }
        }
    }

    // Beispiel für eine Funktion, um die Bewertungen in Firestore zu speichern
    fun submitRatings(hostRating: Int, foodRating: Int, eveningRating: Int) {
        // Erstelle ein Datenobjekt für die Bewertungen
        val ratings = hashMapOf(
            "hostRating" to hostRating,
            "foodRating" to foodRating,
            "eveningRating" to eveningRating
        )

        // Speichere die Bewertungen in Firestore (z.B. in der Collection "ratings")
        firestore.collection("ratings")
            .add(ratings)
            .addOnSuccessListener {
                // Erfolgreiches Hinzufügen der Bewertungen
                Log.d("MainViewModel", "Bewertungen erfolgreich eingereicht")
            }
            .addOnFailureListener { e ->
                // Fehler beim Hinzufügen der Bewertungen
                Log.w("MainViewModel", "Fehler beim Einreichen der Bewertungen", e)
            }

    fun voteForGame(game: String) {
        val updatedVotes = _votes.value?.toMutableMap() ?: mutableMapOf()
        updatedVotes[game] = (updatedVotes[game] ?: 0) + 1
        _votes.value = updatedVotes
        _selectedGame.value = updatedVotes.maxByOrNull { it.value }?.key ?: ""
    }

        fun addGame(newGame: String) {
        val updatedSuggestions = _gameSuggestions.value?.toMutableList() ?: mutableListOf()
        updatedSuggestions.add(newGame)
        _gameSuggestions.value = updatedSuggestions

        val updatedVotes = _votes.value?.toMutableMap() ?: mutableMapOf()
        updatedVotes[newGame] = 0
        _votes.value = updatedVotes
    }

    fun updateRating(rating: Int) {
        _rating.value = rating
    }

    fun toggleGameSelectionDialog() {
        _showGameSelectionDialog.value = _showGameSelectionDialog.value?.not() ?: true
    }

    // Neue Methode für die Navigation zur HostRotationActivity
    fun navigateToHostRotation() {
        onNavigateToHostRotation?.invoke()
    }
}
}