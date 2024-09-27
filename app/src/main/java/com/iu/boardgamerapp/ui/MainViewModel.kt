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

    private val _userName = mutableStateOf("")
    val userName: State<String> get() = _userName

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

    // Callback für die Navigation
    var onNavigateToHostRotation: (() -> Unit)? = null

    private var currentHostName: String? = null

    init {
        loadUsers()
        loadCurrentHost()
    }

    fun addUser(name: String, callback: (Boolean) -> Unit) {
        userRepository.addUser(name, callback)
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
            _currentHost.value = hostName ?: "Niemand" // Setze den aktuellen Host
        }
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


    fun changeHost(newHostName: String, onSuccess: () -> Unit) {
        Log.d("MainViewModel", "Wechsle Gastgeber zu: $newHostName")

        userRepository.getUserByName(newHostName) { user ->
            if (user != null) {
                Log.d("MainViewModel", "Benutzer gefunden: ${user.name}")
                userRepository.updateHostStatus(user.name) {
                    loadCurrentHost() // Dies sollte currentHost aktualisieren
                    loadUsers() // Neu laden der Benutzer

                    // Callback aufrufen, um den Hostwechsel zu bestätigen
                    onSuccess()
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