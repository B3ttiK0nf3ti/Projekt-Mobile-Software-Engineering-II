package com.iu.boardgamerapp.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.util.Log
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.COLUMN_ID
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.COLUMN_IS_HOST
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.COLUMN_NAME
import com.iu.boardgamerapp.data.AppDatabaseHelper.Companion.TABLE_NAME

class UserRepository(private val dbHelper: AppDatabaseHelper) {

    fun addUser(name: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_IS_HOST, 0)
        }
        val newRowId = db.insert(TABLE_NAME, null, values)
        Log.d("UserRepository", "Benutzer hinzugefügt: $name mit ID: $newRowId")
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
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_IS_HOST),
            null, null, null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME))
                val isHost = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_HOST))
                users.add(Triple(id, name, isHost))
            }
        }
        Log.d("UserRepository", "Benutzer aus der Datenbank: ${users.joinToString()}")
        return users
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
