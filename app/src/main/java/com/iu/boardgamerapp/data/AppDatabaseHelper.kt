package com.iu.boardgamerapp.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "boardgamerapp.db"
        const val TABLE_NAME = "user"
        const val COLUMN_ID = "id" // Neue ID-Spalte
        internal const val COLUMN_NAME = "name" // Spalte für den Namen, jetzt public oder internal
        const val COLUMN_IS_HOST = "isHost" // Neue Spalte für Host-Status
        private const val EVENT_TABLE_NAME = "events"
        private const val EVENT_COLUMN_ID = "_id"
        private const val EVENT_COLUMN_DATE = "date"
        private const val EVENT_COLUMN_LOCATION = "location"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_USER_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, 
                $COLUMN_NAME TEXT, 
                $COLUMN_IS_HOST INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(CREATE_USER_TABLE)

        val CREATE_EVENT_TABLE = """
            CREATE TABLE $EVENT_TABLE_NAME (
                $EVENT_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, 
                $EVENT_COLUMN_DATE TEXT, 
                $EVENT_COLUMN_LOCATION TEXT
            )
        """.trimIndent()
        db.execSQL(CREATE_EVENT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TEMPORARY TABLE user_backup(name TEXT, isHost INTEGER)")
            db.execSQL("INSERT INTO user_backup(name, isHost) SELECT name, isHost FROM $TABLE_NAME")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")

            val CREATE_USER_TABLE = """
                CREATE TABLE $TABLE_NAME (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, 
                    $COLUMN_NAME TEXT, 
                    $COLUMN_IS_HOST INTEGER DEFAULT 0
                )
            """.trimIndent()
            db.execSQL(CREATE_USER_TABLE)

            db.execSQL("INSERT INTO $TABLE_NAME (name, isHost) SELECT name, isHost FROM user_backup")
            db.execSQL("DROP TABLE user_backup")
        }
    }

    fun addUser(name: String, isHost: Int = 0) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_IS_HOST, isHost)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getUser(): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME LIMIT 1", null)

        var name: String? = null
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
        }

        cursor.close()
        db.close()
        return name
    }

    @SuppressLint("Range")
    fun getCurrentHostName(): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_IS_HOST = 1 LIMIT 1", null)

        var hostName: String? = null
        if (cursor.moveToFirst()) {
            hostName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
        }

        cursor.close()
        db.close()
        return hostName
    }

    fun updateHostStatus(userId: Int, isHost: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_HOST, isHost)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(userId.toString()))
        db.close()
    }

    fun rotateHost() {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TABLE_NAME SET $COLUMN_IS_HOST = 0 WHERE $COLUMN_IS_HOST = 1")
        db.execSQL("UPDATE $TABLE_NAME SET $COLUMN_IS_HOST = 1 WHERE $COLUMN_ID = (SELECT MIN($COLUMN_ID) FROM $TABLE_NAME WHERE $COLUMN_IS_HOST = 0)")
        db.close()
    }

    fun clearUserTable() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
        db.close()
    }

    fun insertEvent(date: String, location: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(EVENT_COLUMN_DATE, date)
            put(EVENT_COLUMN_LOCATION, location)
        }
        db.insert(EVENT_TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllEvents(): List<Pair<String, String>> {
        val events = mutableListOf<Pair<String, String>>()
        val db = this.readableDatabase
        val cursor = db.query(
            EVENT_TABLE_NAME,
            arrayOf(EVENT_COLUMN_DATE, EVENT_COLUMN_LOCATION),
            null,
            null,
            null,
            null,
            "$EVENT_COLUMN_DATE ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val date = it.getString(it.getColumnIndex(EVENT_COLUMN_DATE))
                val location = it.getString(it.getColumnIndex(EVENT_COLUMN_LOCATION))
                events.add(date to location)
            }
        }
        db.close()
        return events
    }
}
