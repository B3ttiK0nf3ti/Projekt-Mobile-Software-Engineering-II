package com.iu.boardgamerapp.ui

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import java.util.Calendar
import com.google.firebase.Timestamp // Import für Timestamp
import com.iu.boardgamerapp.ui.datamodel.User
import java.util.Date // Import für Date
import com.iu.boardgamerapp.ui.HostRotationActivity

class ExpiredEventWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    private val firestore = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        return try {
            removeExpiredEvents()
            Result.success()
        } catch (e: Exception) {
            Log.e("ExpiredEventWorker", "Fehler im Worker: ${e.message}", e)
            Result.failure()
        }
    }

    private fun removeExpiredEvents() {
        val now = System.currentTimeMillis()
        Log.d("ExpiredEventWorker", "Aktuelle Zeit: $now")

        firestore.collection("calendarEvents")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(
                    "ExpiredEventWorker",
                    "Anzahl der gefundenen Dokumente: ${snapshot.documents.size}"
                )
                for (document in snapshot.documents) {
                    val event = document.toObject(CalendarEvent::class.java)
                    if (event != null && event.endTime.toDate().time < now) {
                        Log.d("ExpiredEventWorker", "Löschen des Ereignisses: ${document.id}")
                        firestore.collection("calendarEvents").document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d(
                                    "ExpiredEventWorker",
                                    "Ereignis erfolgreich entfernt: ${document.id}"
                                )
                                rotateHostForDeletedEvent(event) // Wechselt den Gastgeber
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "ExpiredEventWorker",
                                    "Fehler beim Entfernen des Ereignisses: $e"
                                )
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExpiredEventWorker", "Fehler beim Abrufen von Ereignissen: $e")
            }
    }


    private fun rotateHostForDeletedEvent(event: CalendarEvent) {
        val userList = mutableListOf<User>() // Liste zur Speicherung der Benutzer

        // Hier sicherstellen, dass die Sammlung "user" heißt
        firestore.collection("user").get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    val firebaseInstallationId = document.getString("firebaseInstallationId") ?: ""
                    val name = document.getString("name") ?: ""
                    val isHost = document.getBoolean("isHost") ?: false

                    // Erstelle ein User-Objekt und füge es zur Liste hinzu
                    val user = User(firebaseInstallationId, name, isHost)
                    userList.add(user)
                }

                Log.d("ExpiredEventWorker", "Benutzer abgerufen: ${userList.size} gefunden.")

                if (userList.isNotEmpty()) {
                    // Suche nach dem aktuellen Gastgeber
                    val currentHost = userList.find { it.isHost }

                    if (currentHost == null) {
                        Log.w("ExpiredEventWorker", "Kein aktueller Gastgeber gefunden.")
                        return@addOnSuccessListener // Beende die Methode hier, wenn kein Gastgeber gefunden wird
                    }

                    // Filtere die Benutzer, um einen neuen Gastgeber zu wählen
                    val availableHosts = userList.filter { it.firebaseInstallationId != currentHost.firebaseInstallationId }

                    if (availableHosts.isEmpty()) {
                        Log.w("ExpiredEventWorker", "Keine verfügbaren Gastgeber zum Wechseln gefunden.")
                        return@addOnSuccessListener // Beende die Methode, wenn keine verfügbaren Gastgeber gefunden wurden
                    }

                    // Wähle zufällig einen neuen Gastgeber aus
                    val newHost = availableHosts.random()

                    // Firestore Batch verwenden
                    val batch = firestore.batch()

                    // Setze den aktuellen Host auf false
                    val currentHostRef = firestore.collection("user").document(currentHost.firebaseInstallationId)
                    batch.update(currentHostRef, "isHost", false)

                    // Setze den neuen Host auf true
                    val newHostRef = firestore.collection("user").document(newHost.firebaseInstallationId)
                    batch.update(newHostRef, "isHost", true)

                    // Batch-Commit durchführen
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("ExpiredEventWorker", "Gastgeber gewechselt zu: ${newHost.name}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ExpiredEventWorker", "Fehler beim Ändern des Gastgebers: $e")
                        }
                } else {
                    Log.w("ExpiredEventWorker", "Benutzerliste ist leer, kein Gastgeberwechsel möglich.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExpiredEventWorker", "Fehler beim Abrufen der Benutzer: $e")
            }
    }
}
