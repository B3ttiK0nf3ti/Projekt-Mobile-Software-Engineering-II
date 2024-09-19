package com.iu.boardgamerapp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iu.boardgamerapp.data.UserRepository

class MainViewModel(private val repository: UserRepository) : ViewModel() {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _gameSuggestions = MutableLiveData<List<String>>()
    val gameSuggestions: LiveData<List<String>> = _gameSuggestions

    private val _votes = MutableLiveData<Map<String, Int>>()
    val votes: LiveData<Map<String, Int>> = _votes

    private val _selectedGame = MutableLiveData<String>()
    val selectedGame: LiveData<String> = _selectedGame

    private val _rating = MutableLiveData<Int>()
    val rating: LiveData<Int> = _rating

    private val _showGameSelectionDialog = MutableLiveData<Boolean>()
    val showGameSelectionDialog: LiveData<Boolean> = _showGameSelectionDialog

    private val _showChatDialog = MutableLiveData<Boolean>()
    val showChatDialog: LiveData<Boolean> = _showChatDialog

    private val _chatMessages = MutableLiveData<List<String>>()
    val chatMessages: LiveData<List<String>> = _chatMessages

    private val _newMessage = MutableLiveData<String>()
    val newMessage: LiveData<String> = _newMessage

    init {
        val savedUserName = repository.getUser() ?: ""
        _userName.value = savedUserName
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


