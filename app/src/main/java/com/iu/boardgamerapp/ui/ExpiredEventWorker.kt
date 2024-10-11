package com.iu.boardgamerapp.ui

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.iu.boardgamerapp.R
import com.iu.boardgamerapp.ui.datamodel.CalendarEvent
import com.iu.boardgamerapp.ui.datamodel.User

class ExpiredEventWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    private val firestore = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        return try {
            removeExpiredEvents()
            Result.success()
        } catch (e: Exception) {
            Log.e("ExpiredEventWorker", applicationContext.getString(R.string.worker_error, e.message), e) // Verwende String-Ressource
            Result.failure()
        }
    }

    private fun removeExpiredEvents() {
        val now = System.currentTimeMillis()
        Log.d("ExpiredEventWorker", applicationContext.getString(R.string.current_time_log, now.toString()))

        firestore.collection("calendarEvents")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(
                    "ExpiredEventWorker",
                    applicationContext.getString(R.string.documents_found_log, snapshot.documents.size.toString())
                )
                for (document in snapshot.documents) {
                    val event = document.toObject(CalendarEvent::class.java)
                    if (event != null && event.endTime.toDate().time < now) {
                        Log.d("ExpiredEventWorker", applicationContext.getString(R.string.delete_event_log, document.id))
                        firestore.collection("calendarEvents").document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d(
                                    "ExpiredEventWorker",
                                    applicationContext.getString(R.string.event_removed_log, document.id)
                                )
                                rotateHostForDeletedEvent(event) // Wechselt den Gastgeber
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "ExpiredEventWorker",
                                    applicationContext.getString(R.string.event_removal_error, e.toString())
                                )
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExpiredEventWorker", applicationContext.getString(R.string.events_fetch_error, e.toString()))
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

                Log.d("ExpiredEventWorker", applicationContext.getString(R.string.users_fetched_log, userList.size.toString()))

                if (userList.isNotEmpty()) {
                    // Suche nach dem aktuellen Gastgeber
                    val currentHost = userList.find { it.isHost }

                    if (currentHost == null) {
                        Log.w("ExpiredEventWorker", applicationContext.getString(R.string.no_host_found_warning))
                        return@addOnSuccessListener // Beende die Methode hier, wenn kein Gastgeber gefunden wird
                    }

                    // Filtere die Benutzer, um einen neuen Gastgeber zu wählen
                    val availableHosts = userList.filter { it.firebaseInstallationId != currentHost.firebaseInstallationId }

                    if (availableHosts.isEmpty()) {
                        Log.w("ExpiredEventWorker", applicationContext.getString(R.string.no_available_hosts_warning))
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
                            Log.d("ExpiredEventWorker", applicationContext.getString(R.string.host_changed_log, newHost.name))
                        }
                        .addOnFailureListener { e ->
                            Log.e("ExpiredEventWorker", applicationContext.getString(R.string.host_change_error, e.toString()))
                        }
                } else {
                    Log.w("ExpiredEventWorker", applicationContext.getString(R.string.user_list_empty_warning))
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExpiredEventWorker", applicationContext.getString(R.string.users_fetch_error, e.toString()))
            }
    }
}
