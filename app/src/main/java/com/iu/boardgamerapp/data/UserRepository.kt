package com.iu.boardgamerapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.iu.boardgamerapp.ui.datamodel.User


class UserRepository {
    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "UserRepository"
        const val USERS_COLLECTION = "user"
    }

    fun getUserByName(name: String, callback: (User?) -> Unit) {
        db.collection(USERS_COLLECTION)
            .whereEqualTo("name", name)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val userId = document.id // Verwende die ID als String
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
    // In UserRepository
    fun getAllUsers(callback: (List<User>) -> Unit) {
        db.collection(USERS_COLLECTION).get()
            .addOnSuccessListener { result ->
                val users = result.mapNotNull { document ->
                    val userId = document.id
                    val userName = document.getString("name") ?: ""
                    val isHost = document.getBoolean("isHost") ?: false
                    User(userId, userName, isHost) // Erstelle ein User-Objekt
                }
                callback(users) // Rückgabe der Liste der Benutzer
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
            val newHost = users.find { it.name == newHostName }

            if (newHost != null) {
                val currentHost = users.find { it.isHost }

                currentHost?.let { host ->
                    val currentHostRef = db.collection(USERS_COLLECTION).document(host.firebaseInstallationId)
                    currentHostRef.update("isHost", false)
                        .addOnSuccessListener {
                            Log.d(TAG, "Aktueller Gastgeber ${host.name} wurde erfolgreich deaktiviert.")
                            // Callback hier aufrufen
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Fehler beim Deaktivieren des aktuellen Gastgebers: ${e.message}")
                            // Callback hier aufrufen, um sicherzustellen, dass es aufgerufen wird
                        }
                } ?: run {
                    Log.d(TAG, "Kein aktueller Gastgeber gefunden, keine Änderung notwendig.")
                    callback() // Callback trotzdem aufrufen
                }

                val newHostRef = db.collection(USERS_COLLECTION).document(newHost.firebaseInstallationId)
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
