package com.iu.boardgamerapp.ui

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import java.util.Calendar
import com.google.firebase.Timestamp // Import für Timestamp
import java.util.Date // Import für Date

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
        // Holen Sie sich die aktuelle Zeit in Millisekunden
        val now = System.currentTimeMillis()

        // Überprüfen Sie die Ereignisse und entfernen Sie abgelaufene
        firestore.collection("calendarEvents")
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    val event = document.toObject(CalendarEvent::class.java)
                    // Überprüfen, ob das Ereignis abgelaufen ist
                    if (event != null && event.endTime.toDate().time < now) {
                        firestore.collection("calendarEvents").document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d(
                                    "ExpiredEventWorker",
                                    "Ereignis erfolgreich entfernt: ${document.id}"
                                )
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

}