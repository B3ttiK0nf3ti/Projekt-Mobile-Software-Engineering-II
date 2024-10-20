package com.iu.boardgamerapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase

class AppDatabaseHelper {

    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "AppDatabaseHelper"
        const val USERS_COLLECTION = "user"
    }

    // Benutzer hinzufügen
    fun addUser(name: String, onComplete: (Boolean) -> Unit) {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseId = task.result // Firebase Installation ID abrufen

                // Benutzer-Daten mit Firebase Installation ID erstellen
                val userData = hashMapOf(
                    "name" to name,
                    "isHost" to false,
                    "firebaseInstallationId" to firebaseId // Firebase Installation ID hinzufügen
                )

                // Benutzer in der Datenbank speichern
                db.collection(USERS_COLLECTION).document(firebaseId).set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User successfully added with Firebase Installation ID: $firebaseId")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding user", e)
                        onComplete(false)
                    }
            } else {
                Log.e(TAG, "Error fetching Firebase Installation ID", task.exception)
                onComplete(false)
            }
        }
    }

    fun getUserWithFirebaseID(onComplete: (String?) -> Unit) {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseId = task.result

                db.collection(USERS_COLLECTION)
                    .document(firebaseId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            onComplete(document.getString("name"))
                        } else {
                            onComplete(null)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error getting user", e)
                        onComplete(null)
                    }
            } else {
                Log.e(TAG, "Error fetching Firebase Installation ID", task.exception)
                onComplete(null)
            }
        }
    }

}
