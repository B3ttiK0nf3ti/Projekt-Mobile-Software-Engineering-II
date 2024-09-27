package com.iu.boardgamerapp.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent

class AppDatabaseHelper(context: Context) {

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
        val trimmedName = name.trim() // Hier trimmen wir den Namen
        Log.d(TAG, "Überprüfe, ob Benutzer existiert: $trimmedName") // Log-Ausgabe für Debugging

        db.collection(USERS_COLLECTION)
            .whereEqualTo("name", trimmedName) // Abfrage für den getrimmten Namen
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Abfrage erfolgreich: ${result.size()} Dokument(e) gefunden.")
                onComplete(!result.isEmpty) // Callback mit dem Ergebnis der Abfrage
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Überprüfen der Benutzerexistenz", e)
                onComplete(false) // Callback mit false im Fehlerfall
            }
    }

    // Ereignisse in Firestore speichern
    fun addCalendarEvent(event: CalendarEvent, callback: (Boolean) -> Unit) {
        val documentRef = db.collection(CALENDAR_EVENTS_COLLECTION).document() // Korrekte Sammlung verwenden
        val eventWithId = event.copy(id = documentRef.id)

        documentRef.set(eventWithId)
            .addOnSuccessListener {
                Log.d(TAG, "Kalenderereignis erfolgreich hinzugefügt")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fehler beim Hinzufügen des Kalenderereignisses", e)
                callback(false)
            }
    }

    // Kalenderereignisse abrufen
    fun fetchCalendarEvents(callback: (List<CalendarEvent>) -> Unit) {
        db.collection(CALENDAR_EVENTS_COLLECTION) // Korrekte Sammlung verwenden
            .get()
            .addOnSuccessListener { result ->
                val events = result.mapNotNull { document ->
                    document.toObject(CalendarEvent::class.java)?.copy(id = document.id) // ID zuweisen
                }
                callback(events)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Fehler beim Abrufen der Kalenderereignisse", exception)
                callback(emptyList())
            }
    }
}
