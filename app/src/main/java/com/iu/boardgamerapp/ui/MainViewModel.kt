package com.iu.boardgamerapp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iu.boardgamerapp.data.UserRepository

class MainViewModel(private val repository: UserRepository) : ViewModel() {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

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

    private val _showChatDialog = MutableLiveData<Boolean>(false)
    val showChatDialog: LiveData<Boolean> = _showChatDialog

    private val _chatMessages = MutableLiveData<List<String>>(emptyList())
    val chatMessages: LiveData<List<String>> = _chatMessages

    private val _newMessage = MutableLiveData<String>("")
    val newMessage: LiveData<String> = _newMessage

    private val _currentHost = MutableLiveData<String>()
    val currentHost: LiveData<String> = _currentHost

    init {
        val savedUserName = repository.getUser() ?: ""
        _userName.value = savedUserName
        loadCurrentHost()
    }

    private fun loadCurrentHost() {
        _currentHost.value = repository.getCurrentHostName()
    }

    fun changeHost(newHostName: String) {
        val users = repository.getAllUsers()
        val newHost = users.find { it.second == newHostName } // Zugriff auf den Benutzernamen

        if (newHost != null) {
            repository.updateHostStatus(newHost.second) // Setze den neuen Gastgeber (Name)
            loadCurrentHost()
        }
    }

    fun saveUser(name: String) {
        _userName.value = name
        repository.addUser(name) // Speichern des Benutzernamens im Repository
    }

    fun voteForGame(game: String) {
        val updatedVotes = _votes.value?.toMutableMap() ?: mutableMapOf()
        updatedVotes[game] = (updatedVotes[game] ?: 0) + 1
        _votes.value = updatedVotes
        _selectedGame.value = updatedVotes.maxByOrNull { it.value }?.key ?: ""
    }

    fun rotateHost() {
        val users = repository.getAllUsers() // Zugriff auf das Repository
        val currentHostIndex = users.indexOfFirst { it.third == 1 } // Assuming third is isHost in Triple

        if (currentHostIndex != -1) {
            val nextHostIndex = (currentHostIndex + 1) % users.size
            val nextHost = users[nextHostIndex]

            // Den aktuellen Gastgeber auf "nicht Gastgeber" setzen
            repository.updateHostStatus(users[currentHostIndex].second) // Übergibt den Namen, nicht die ID
            // Den nächsten Gastgeber auf "Gastgeber" setzen
            repository.updateHostStatus(nextHost.second) // Übergibt den Namen, nicht die ID
            loadCurrentHost() // Aktuellen Gastgeber neu laden
        }
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

    fun toggleChatDialog() {
        _showChatDialog.value = _showChatDialog.value?.not() ?: true
    }

    fun updateNewMessage(message: String) {
        _newMessage.value = message
    }

    fun sendMessage() {
        val updatedMessages = _chatMessages.value?.toMutableList() ?: mutableListOf()
        _newMessage.value?.let {
            updatedMessages.add(it)
            _chatMessages.value = updatedMessages
            _newMessage.value = ""
        }
    }
}
