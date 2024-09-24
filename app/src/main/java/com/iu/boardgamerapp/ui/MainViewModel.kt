package com.iu.boardgamerapp.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository

class MainViewModel(
    private val repository: UserRepository,
    private val databaseHelper: AppDatabaseHelper,
    context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

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

    private val _currentHost = MutableLiveData<String>()
    val currentHost: LiveData<String> = _currentHost

    private val _userList = MutableLiveData<List<Pair<String, Boolean>>>()
    val userList: LiveData<List<Pair<String, Boolean>>> = _userList



    init {
        loadUserName()
        loadCurrentHost()
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

    private fun saveUserNameToPreferences(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
    }

    fun clearUserName() {
        sharedPreferences.edit().remove("user_name").apply()
        _userName.value = ""
        _userExists.value = false
    }

    private fun loadUserName() {
        val savedUserName = sharedPreferences.getString("user_name", "")
        _userName.value = savedUserName
        if (!savedUserName.isNullOrEmpty()) {
            checkUserExists(savedUserName)
        } else {
            _userExists.value = false
        }
    }

    fun checkUserExists(name: String) {
        databaseHelper.checkUserExists(name) { exists ->
            _userExists.value = exists
            if (!exists) {
                clearUserName()
            }
        }
    }

    private fun loadCurrentHost() {
        repository.getCurrentHostName { hostName ->
            _currentHost.value = hostName
            Log.d("ViewModel", "Aktueller Host geladen: $hostName")
        }
    }

    fun loadUsers() {
        repository.getAllUsers { users ->
            _userList.value = users
            Log.d("ViewModel", "Benutzerliste geladen: ${users.joinToString()}")
        }
    }

    fun changeHost(newHostName: String) {
        _userList.value?.let { users ->
            val newHost = users.find { it.first == newHostName }
            if (newHost != null) {
                repository.updateHostStatus(newHost.first) // Setze den neuen Gastgeber
                loadCurrentHost() // Lade den aktuellen Gastgeber neu
            }
        }
    }

    fun voteForGame(game: String) {
        val updatedVotes = _votes.value?.toMutableMap() ?: mutableMapOf()
        updatedVotes[game] = (updatedVotes[game] ?: 0) + 1
        _votes.value = updatedVotes
        _selectedGame.value = updatedVotes.maxByOrNull { it.value }?.key ?: ""
    }

    fun rotateHost() {
        _userList.value?.let { users ->
            val currentHostIndex = users.indexOfFirst { it.second } // Assuming second is isHost in Pair

            if (currentHostIndex != -1) {
                val nextHostIndex = (currentHostIndex + 1) % users.size
                val nextHost = users[nextHostIndex]

                // Den aktuellen Gastgeber auf "nicht Gastgeber" setzen
                repository.updateHostStatus(users[currentHostIndex].first) // Übergibt den Namen
                // Den nächsten Gastgeber auf "Gastgeber" setzen
                repository.updateHostStatus(nextHost.first) // Übergibt den Namen
                loadCurrentHost() // Aktuellen Gastgeber neu laden
            }
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
}
