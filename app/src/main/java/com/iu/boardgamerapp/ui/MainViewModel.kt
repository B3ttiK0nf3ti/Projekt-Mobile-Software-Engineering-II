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

    private val _currentHost = MutableLiveData<String?>()
    val currentHost: LiveData<String?> get() = _currentHost

    private val _userList = MutableLiveData<List<Pair<String, Boolean>>>()
    val userList: LiveData<List<Pair<String, Boolean>>> = userRepository.getUserList()

    // Callback für die Navigation
    var onNavigateToHostRotation: (() -> Unit)? = null

    private var currentHostName: String? = null

    init {
        loadUserName()
        loadCurrentHost()
        loadUsers()
    }

    // Methode zum Abrufen des aktuellen Gastgebers
    fun getCurrentHost(): String {
        return currentHostName ?: "Kein Gastgeber gesetzt" // Rückgabe eines Standardwerts, falls kein Gastgeber gesetzt ist
    }

    fun getUsers(): List<User> {
        return userList.value?.mapIndexed { index, (name, isHost) ->
            User(id = index.toString(), name = name, isHost = isHost)
        } ?: emptyList()
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
        userRepository.getCurrentHostName { hostName ->
            Log.d("MainViewModel", "Aktueller Gastgeber abgerufen: $hostName")
            _currentHost.value = hostName // Aktualisiere den aktuellen Gastgeber
        }
    }

    fun loadUsers() {
        // Implementiere die Logik zum Laden der Benutzer
        userRepository.getAllUsers { users ->
            _userList.value = users // Speichern der Benutzerliste in der LiveData-Variable
        }
    }

    fun changeHost(newHostName: String) {
        // Suche nach dem neuen Gastgeber in der Benutzerdatenbank
        userRepository.getUserByName(newHostName) { user ->
            user?.let {
                // Setze den neuen Gastgeber in der Datenbank
                userRepository.updateHostStatus(it.name) // Aktualisiere den Status des neuen Gastgebers
                loadCurrentHost() // Lade den aktuellen Gastgeber neu
                loadUsers() // Benutzerliste neu laden
            } ?: run {
                Log.w("ViewModel", "Neuer Gastgeber nicht gefunden: $newHostName")
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
                userRepository.updateHostStatus(users[currentHostIndex].first) // Übergibt den Namen
                // Den nächsten Gastgeber auf "Gastgeber" setzen
                userRepository.updateHostStatus(nextHost.first) // Übergibt den Namen
                loadCurrentHost() // Aktuellen Gastgeber neu laden
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