package com.iu.boardgamerapp.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.ui.datamodel.User
import com.google.firebase.firestore.FirebaseFirestore

class MainViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _snackbarMessage = MutableLiveData<String>("")
    val snackbarMessage: LiveData<String> get() = _snackbarMessage

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    // LiveData für die Benutzerliste
    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> get() = _userList

    // LiveData für den aktuellen Gastgeber
    private val _currentHost = MutableLiveData<String>()
    val currentHost: LiveData<String> get() = _currentHost

    // Liste der Spiele als LiveData
    private val _gameList = MutableLiveData<List<Game>>()

    // Spiel mit den meisten Votes
    private val _topVotedGame = MutableLiveData<Game?>()
    val topVotedGame: LiveData<Game?> = _topVotedGame

    // Neue LiveData für mehrere Top-Spiele
    private val _hasMultipleTopGames = MutableLiveData<Boolean>(false)
    val hasMultipleTopGames: LiveData<Boolean> get() = _hasMultipleTopGames

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        loadUsers()         // Lädt die Benutzer beim Start des ViewModels
        loadCurrentHost()   // Lädt den aktuellen Gastgeber beim Start des ViewModels
        loadGames()         // Optional, lädt auch die Spiele
    }

    fun setUserName(name: String) {
        _userName.value = name
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
}