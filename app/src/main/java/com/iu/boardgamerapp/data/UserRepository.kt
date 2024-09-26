package com.iu.boardgamerapp.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.iu.boardgamerapp.ui.datamodel.User
import com.google.firebase.database.ValueEventListener

class UserRepository(private val databaseHelper: AppDatabaseHelper) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "UserRepository"
        const val USERS_COLLECTION = "user"
    }

    // Benutzer hinzufügen
    fun addUser(name: String) {
        val userData = hashMapOf(
            "name" to name,
            "isHost" to false // Standardmäßig kein Host
        )

        db.collection(USERS_COLLECTION).add(userData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Benutzer hinzugefügt: $name mit ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Hinzufügen des Benutzers", e)
            }
    }

    // Benutzer abrufen
    fun getUser(onComplete: (String?) -> Unit) {
        db.collection(USERS_COLLECTION).limit(1).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    onComplete(document.getString("name"))
                }
                onComplete(null) // Rückgabe von null, wenn kein Benutzer vorhanden ist
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Abrufen des Benutzers", e)
                onComplete(null)
            }
    }

    fun getUserByName(name: String, callback: (User?) -> Unit) {
        db.collection(USERS_COLLECTION)
            .whereEqualTo("name", name)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val userId = document.id // ID des Dokuments in der Datenbank
                    val userName = document.getString("name") ?: ""
                    val isHost = document.getBoolean("isHost") ?: false
                    callback(User(userId, userName, isHost)) // Benutzer mit ID zurückgeben
                } else {
                    callback(null) // Benutzer nicht gefunden
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Abrufen des Benutzers mit dem Namen: $name", e)
                callback(null) // Rückgabe von null im Fehlerfall
            }
    }

    // Funktion, die eine LiveData-Liste von Benutzern zurückgibt
    fun getUserList(): LiveData<List<Pair<String, Boolean>>> {
        val userList = MutableLiveData<List<Pair<String, Boolean>>>()

        // Hier kannst du deine Logik einfügen, um die Benutzer zu laden
        // Zum Beispiel aus einer Datenbank oder einer API.
        // Dies ist ein Platzhalter für die tatsächliche Implementierung:
        userList.value = listOf(
            Pair("Benutzer 1", false),
            Pair("Benutzer 2", true),
            Pair("Benutzer 3", false)
        )

        return userList
    }



    // Alle Benutzer abrufen
    fun getAllUsers(onComplete: (List<Pair<String, Boolean>>) -> Unit) {
        val users = mutableListOf<Pair<String, Boolean>>() // Liste zum Speichern der Benutzer
        db.collection(USERS_COLLECTION).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    val isHost = document.getBoolean("isHost") ?: false
                    users.add(name to isHost)
                }
                Log.d(TAG, "Benutzer aus der Datenbank: ${users.joinToString()}")
                onComplete(users)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Abrufen aller Benutzer", e)
                onComplete(emptyList())
            }
    }

    // Host-Status aktualisieren
    fun updateHostStatus(newHostName: String) {
        getAllUsers { users ->
            val newHost = users.find { it.first == newHostName } // Benutzer anhand des Namens finden

            if (newHost != null) {
                val currentHost = users.find { it.second } // Aktuellen Gastgeber finden

                if (currentHost != null) {
                    // Den aktuellen Gastgeber auf "nicht Gastgeber" setzen
                    val currentHostRef = db.collection(USERS_COLLECTION).document(currentHost.first)
                    currentHostRef.update("isHost", false)
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Fehler beim Aktualisieren des aktuellen Gastgebers", e)
                        }
                }

                // Den neuen Gastgeber auf "Gastgeber" setzen
                val newHostRef = db.collection(USERS_COLLECTION).document(newHost.first)
                newHostRef.update("isHost", true)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Fehler beim Aktualisieren des neuen Gastgebers", e)
                    }
            }
        }
    }

    // Den aktuellen Host abrufen
    fun getCurrentHostName(callback: (String?) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("hosts") // Beispiel-Pfad
        dbRef.child("currentHost").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hostName = snapshot.getValue(String::class.java)
                callback(hostName)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("UserRepository", "loadHost:onCancelled", error.toException())
                callback(null) // Fehlerfall
            }
    })
}
}
