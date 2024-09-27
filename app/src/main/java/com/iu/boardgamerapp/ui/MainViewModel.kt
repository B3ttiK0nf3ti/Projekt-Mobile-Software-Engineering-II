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

class MainViewModel(
    private val userRepository: UserRepository,
    private val databaseHelper: AppDatabaseHelper,
    private val context: Context
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

    // LiveData für die Benutzerliste
    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> get() = _userList

    // LiveData für den aktuellen Gastgeber
    private val _currentHost = MutableLiveData<String?>() // Nullable LiveData
    val currentHost: LiveData<String?> get() = _currentHost

    // Callback für die Navigation
    var onNavigateToHostRotation: (() -> Unit)? = null

    private var currentHostName: String? = null

    init {
        loadCurrentHost()
        loadUsers()
    }

    // Methode zum Abrufen des aktuellen Gastgebers
    fun getCurrentHost(): String {
        return _currentHost.value ?: "Kein Gastgeber gesetzt"
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

    fun loadCurrentHost() {
        userRepository.getCurrentHost { hostName ->
            if (hostName != null) {
                _currentHost.value = hostName // Setzt den aktuellen Gastgeber in die LiveData-Variable
            } else {
                Log.w("MainViewModel", "Kein Gastgeber gesetzt")
                _currentHost.value = null // Setze auf null, wenn kein Gastgeber vorhanden ist
            }
        }
    }

    fun loadUsers() {
        userRepository.getAllUsers { users ->
            // Mapping der Paar-Liste (Name, isHost) in eine Liste von User-Objekten
            val userList = users.map { (name, isHost) ->
                User(id = name, name = name, isHost = isHost)
            }
            // Speichern der gemappten Benutzerliste in der LiveData-Variable
            _userList.value = userList
        }
    }

    fun changeHost(newHostName: String) {
        userRepository.getUserByName(newHostName) { user ->
            if (user != null) {
                // Setzt den neuen Gastgeber in Firestore
                userRepository.updateHostStatus(user.name) {
                    loadCurrentHost() // Aktualisiere den aktuellen Gastgeber
                    loadUsers() // Benutzerliste neu laden
                }
            } else {
                Log.w("MainViewModel", "Neuer Gastgeber nicht gefunden: $newHostName")
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
        val users = _userList.value ?: return
        val currentHostIndex = users.indexOfFirst { it.isHost }

        if (currentHostIndex != -1) {
            // Finde den nächsten Gastgeber (zyklisch)
            val nextHostIndex = (currentHostIndex + 1) % users.size
            val nextHost = users[nextHostIndex]

            // Aktualisiere den Gastgeber in der Datenbank
            userRepository.updateHostStatus(nextHost.name) {
                loadCurrentHost() // Aktualisiere den aktuellen Gastgeber
                loadUsers() // Benutzerliste neu laden
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

    // Neue Methode für die Navigation zur HostRotationActivity
    fun navigateToHostRotation() {
        onNavigateToHostRotation?.invoke()
    }
}