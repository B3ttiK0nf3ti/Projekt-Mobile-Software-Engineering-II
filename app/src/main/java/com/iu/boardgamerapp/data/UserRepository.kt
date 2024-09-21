package com.iu.boardgamerapp.data

import android.annotation.SuppressLint
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.COLUMN_ID
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.COLUMN_IS_HOST
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.COLUMN_NAME
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.TABLE_NAME

class UserRepository(private val dbHelper: AppDatabaseHelper) {

    fun addUser(name: String) {
        dbHelper.addUser(name)
    }

    fun getUser(): String? {
        return dbHelper.getUser()
    }

    @SuppressLint("Range")
    fun getAllUsers(): List<Triple<Int, String, Int>> {
        val users = mutableListOf<Triple<Int, String, Int>>() // Liste zum Speichern der Benutzer
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_IS_HOST), // Spalten, die abgerufen werden sollen
            null,
            null,
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(COLUMN_ID)) // ID abfragen
                val name = it.getString(it.getColumnIndex(COLUMN_NAME)) // Name abfragen
                val isHost = it.getInt(it.getColumnIndex(COLUMN_IS_HOST)) // Host-Status abfragen
                users.add(Triple(id, name, isHost)) // Benutzer zur Liste hinzufügen
            }
        }
        db.close()
        return users // Liste mit Benutzern zurückgeben
    }

    fun updateHostStatus(newHostName: String) {
        val users = getAllUsers() // Alle Benutzer abrufen
        val newHost = users.find { it.second == newHostName } // Benutzer anhand des Namens finden

        if (newHost != null) {
            val currentHost = users.find { it.third == 1 } // Aktuellen Gastgeber finden

            if (currentHost != null) {
                // Den aktuellen Gastgeber auf "nicht Gastgeber" setzen
                dbHelper.updateHostStatus(currentHost.first, 0) // Hier wird die ID des aktuellen Gastgebers verwendet
            }

            // Den neuen Gastgeber auf "Gastgeber" setzen
            dbHelper.updateHostStatus(newHost.first, 1) // Hier wird die ID des neuen Gastgebers verwendet
        }
    }

    @SuppressLint("Range")
    fun getCurrentHostId(): Int? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_IS_HOST = 1 LIMIT 1", null)

        var hostId: Int? = null
        if (cursor.moveToFirst()) {
            hostId = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
        }

        cursor.close()
        db.close()
        return hostId
    }

    @SuppressLint("Range")
    fun getCurrentHostName(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_IS_HOST = 1 LIMIT 1", null)

        var hostName: String? = null
        if (cursor.moveToFirst()) {
            hostName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
        }

        cursor.close()
        db.close()
        return hostName ?: "Kein Gastgeber"
    }
}
