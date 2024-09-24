package com.iu.boardgamerapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AppDatabaseHelper {

    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "AppDatabaseHelper"
        const val USERS_COLLECTION = "user"
    }

    // Benutzer hinzufügen
    fun addUser(name: String, onComplete: (Boolean) -> Unit) {
        db.collection(USERS_COLLECTION).get()
            .addOnSuccessListener { result ->
                val nextId = result.size() + 1
                val userData = hashMapOf(
                    "id" to nextId,
                    "name" to name,
                    "isHost" to false
                )

                db.collection(USERS_COLLECTION).document("$nextId").set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User successfully added with ID: $nextId")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding user", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user count", e)
                onComplete(false)
            }
    }

    // Benutzername abrufen
    fun getUser(onComplete: (String?) -> Unit) {
        db.collection(USERS_COLLECTION).limit(1).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents.first()
                    onComplete(document.getString("name"))
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user", e)
                onComplete(null)
            }
    }

    // Überprüfen, ob Benutzer existiert
    fun checkUserExists(name: String, onComplete: (Boolean) -> Unit) {
        db.collection(USERS_COLLECTION).whereEqualTo("name", name).get()
            .addOnSuccessListener { result ->
                onComplete(!result.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error checking user existence", e)
                onComplete(false)
            }
    }
}
