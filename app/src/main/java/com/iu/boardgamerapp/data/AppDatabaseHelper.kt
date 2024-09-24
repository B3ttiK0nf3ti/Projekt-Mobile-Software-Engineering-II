package com.iu.boardgamerapp.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AppDatabaseHelper(context: Context) {

    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "AppDatabaseHelper"
        const val USERS_COLLECTION = "user"     // Bestehende Sammlung für Benutzer
        const val MESSAGES_COLLECTION = "messages" // Bestehende Sammlung für Nachrichten
    }

    // Benutzer hinzufügen
    fun addUser(name: String, isHost: Boolean = false, onComplete: (Boolean) -> Unit) {
        val userId = db.collection(USERS_COLLECTION).document().id // Automatisch generierte ID
        val userData = hashMapOf(
            "name" to name,
            "isHost" to isHost
        )

        db.collection(USERS_COLLECTION).document(userId).set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User successfully added with ID: $userId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding user", e)
                onComplete(false)
            }
    }

    // Alle Benutzer abrufen
    fun getUser(onComplete: (String?) -> Unit) {
        db.collection(USERS_COLLECTION).limit(1).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    onComplete(document.getString("name"))
                }
                onComplete(null) // Rückgabe von null, wenn kein Benutzer vorhanden ist
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user", e)
                onComplete(null)
            }
    }

    // Den aktuellen Host-Namen abrufen
    fun getCurrentHostName(onComplete: (String?) -> Unit) {
        db.collection(USERS_COLLECTION)
            .whereEqualTo("isHost", true)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    onComplete(document.getString("name"))
                }
                onComplete(null)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting current host", e)
                onComplete(null)
            }
    }

    // Host-Status aktualisieren
    fun updateHostStatus(userId: String, isHost: Boolean) {
        val userRef = db.collection(USERS_COLLECTION).document(userId)
        userRef.update("isHost", isHost)
            .addOnSuccessListener {
                Log.d(TAG, "Host status updated for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating host status", e)
            }
    }

    // Host rotieren
    fun rotateHost() {
        // Zuerst alle Host-Status auf false setzen
        db.collection(USERS_COLLECTION).whereEqualTo("isHost", true).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    document.reference.update("isHost", false)
                }

                // Dann den nächsten Benutzer als Host festlegen
                db.collection(USERS_COLLECTION).whereEqualTo("isHost", false).limit(1).get()
                    .addOnSuccessListener { newHostSnapshot ->
                        for (document in newHostSnapshot.documents) {
                            document.reference.update("isHost", true)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error rotating host", e)
            }
    }

    // Nachricht hinzufügen
    fun addMessage(content: String, userId: String, onComplete: (Boolean) -> Unit) {
        val messageData = hashMapOf(
            "content" to content,
            "userId" to userId,
            "timestamp" to System.currentTimeMillis() // Zeitstempel hinzufügen
        )

        db.collection(MESSAGES_COLLECTION).add(messageData)
            .addOnSuccessListener {
                Log.d(TAG, "Message successfully added")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding message", e)
                onComplete(false)
            }
    }

    // Alle Nachrichten abrufen
    fun getAllMessages(onComplete: (List<Pair<String, String>>) -> Unit) {
        db.collection(MESSAGES_COLLECTION).orderBy("timestamp").get()
            .addOnSuccessListener { result ->
                val messages = mutableListOf<Pair<String, String>>()
                for (document in result) {
                    val content = document.getString("content") ?: ""
                    val userId = document.getString("userId") ?: ""
                    messages.add(content to userId)
                }
                onComplete(messages)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting messages", e)
                onComplete(emptyList())
            }
    }

    // Benutzer löschen (Optional)
    fun clearUserTable() {
        db.collection(USERS_COLLECTION).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    document.reference.delete()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error clearing user table", e)
            }
    }
}
