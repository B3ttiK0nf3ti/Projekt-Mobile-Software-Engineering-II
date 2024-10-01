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
import com.google.firebase.firestore.ktx.firestore


class UserRepository(private val databaseHelper: AppDatabaseHelper) {
    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "UserRepository"
        const val USERS_COLLECTION = "user"
    }

    // Benutzer hinzufügen
    fun addUser(name: String, callback: (Boolean) -> Unit) {
        databaseHelper.checkUserExists(name) { exists -> // Verwendung von databaseHelper
            if (exists) {
                callback(false) // Benutzer existiert bereits
            } else {
                val userData = hashMapOf(
                    "name" to name,
                    "isHost" to false // Standardmäßig kein Host
                )
                db.collection(USERS_COLLECTION).add(userData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Benutzer hinzugefügt: $name mit ID: ${documentReference.id}")
                        callback(true) // Benutzer erfolgreich hinzugefügt
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Fehler beim Hinzufügen des Benutzers", e)
                        callback(false) // Fehler beim Hinzufügen
                    }
            }
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
                    val userId = document.id.toIntOrNull() ?: 0 // Konvertiere die ID in einen Integer
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

    // Alle Benutzer abrufen
    fun getAllUsers(callback: (List<User>) -> Unit) {
        db.collection(USERS_COLLECTION).get()
            .addOnSuccessListener { result ->
                val users = result.map { document ->
                    val userId = document.id.toIntOrNull() ?: 0 // Konvertiere die ID in einen Integer
                    val userName = document.getString("name") ?: ""
                    val isHost = document.getBoolean("isHost") ?: false
                    User(userId, userName, isHost) // Benutzer-Objekt erstellen
                }
                callback(users)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Abrufen der Benutzer", e)
                callback(emptyList())
            }
    }

    // Host-Status aktualisieren
    fun updateHostStatus(newHostName: String, callback: () -> Unit) {
        Log.d(TAG, "Wechsle Gastgeber zu: $newHostName") // Debugging-Log

        getAllUsers { users ->
            // Finde den neuen Gastgeber
            val newHost = users.find { it.name == newHostName }

            if (newHost != null) {
                // Finde den aktuellen Gastgeber
                val currentHost = users.find { it.isHost }

                // Deaktiviere den aktuellen Gastgeber
                currentHost?.let { host ->
                    val currentHostRef =
                        db.collection(USERS_COLLECTION).document(host.id.toString())
                    currentHostRef.update("isHost", false)
                        .addOnSuccessListener {
                            Log.d(
                                TAG,
                                "Aktueller Gastgeber ${host.name} wurde erfolgreich deaktiviert."
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.w(
                                TAG,
                                "Fehler beim Deaktivieren des aktuellen Gastgebers: ${e.message}"
                            )
                        }
                } ?: run {
                    Log.d(TAG, "Kein aktueller Gastgeber gefunden, keine Änderung notwendig.")
                }

                // Aktiviere den neuen Gastgeber
                val newHostRef = db.collection(USERS_COLLECTION).document(newHost.id.toString())
                newHostRef.update("isHost", true)
                    .addOnSuccessListener {
                        Log.d(TAG, "Neuer Gastgeber gesetzt: ${newHost.name}")
                        callback() // Callback nach erfolgreichem Update aufrufen
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Fehler beim Aktualisieren des neuen Gastgebers: ${e.message}")
                    }
            } else {
                Log.w(TAG, "Neuer Gastgeber nicht gefunden: $newHostName")
            }
        }
    }


    // Methode, um den aktuellen Gastgeber aus der Firestore-Datenbank zu laden
    fun getCurrentHost(callback: (String?) -> Unit) {
        val hostCollection = db.collection(USERS_COLLECTION)
        hostCollection
            .whereEqualTo("isHost", true)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val hostName = document.getString("name")
                    callback(hostName)
                } else {
                    Log.d(TAG, "Kein Gastgeber gefunden")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Fehler beim Abrufen des aktuellen Gastgebers", exception)
                callback(null)
            }
    }

}
