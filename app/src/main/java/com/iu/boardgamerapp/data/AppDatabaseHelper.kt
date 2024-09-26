package com.iu.boardgamerapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent

class AppDatabaseHelper {

    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "AppDatabaseHelper"
        const val USERS_COLLECTION = "user"
        const val CALENDAR_EVENTS_COLLECTION = "calendarEvents" // Neue Konstante für Kalenderereignisse
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

    // Ereignisse in Firestore speichern
    fun addCalendarEvent(event: CalendarEvent, onComplete: (Boolean) -> Unit) {
        db.collection("events")
            .add(event)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("AppDatabaseHelper", "Error adding document", e)
                onComplete(false)
            }
    }
    // Kalenderereignisse abrufen
    fun fetchCalendarEvents(onEventsFetched: (List<CalendarEvent>) -> Unit) {
        db.collection("events")
            .get()
            .addOnSuccessListener { documents ->
                val events = mutableListOf<CalendarEvent>()
                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val startTime = document.getLong("start_time") ?: 0L
                    val endTime = document.getLong("end_time") ?: 0L
                    val location = document.getString("location") ?: "" // Default auf leeren String

                    // Überprüfen, ob die Variablen gültig sind
                    if (title.isNotEmpty() && startTime > 0 && endTime > 0) {
                        events.add(CalendarEvent(document.id, title, startTime, endTime, location))
                    } else {
                        Log.w("AppDatabaseHelper", "Invalid data for document: ${document.id}. Title: '$title', Start Time: $startTime, End Time: $endTime, Location: '$location'")
                    }
                }
                onEventsFetched(events)
            }
            .addOnFailureListener { exception ->
                Log.e("AppDatabaseHelper", "Error getting documents: ", exception)
                onEventsFetched(emptyList())
            }
    }
}
