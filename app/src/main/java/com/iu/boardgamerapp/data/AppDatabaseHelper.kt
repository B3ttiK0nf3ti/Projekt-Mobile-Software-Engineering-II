package com.iu.boardgamerapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "boardgamerapp.db"
        private const val TABLE_NAME = "user"
        private const val COLUMN_NAME = "name"
        private const val EVENT_TABLE_NAME = "events"
        private const val EVENT_COLUMN_ID = "_id"
        private const val EVENT_COLUMN_DATE = "date"
        private const val EVENT_COLUMN_LOCATION = "location"
    }

    private val context: Context = context

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_USER_TABLE = ("CREATE TABLE $TABLE_NAME ($COLUMN_NAME TEXT)")
        db.execSQL(CREATE_USER_TABLE)

        val CREATE_EVENT_TABLE = ("CREATE TABLE $EVENT_TABLE_NAME (" +
                "$EVENT_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$EVENT_COLUMN_DATE TEXT, " +
                "$EVENT_COLUMN_LOCATION TEXT)"
                )
        db.execSQL(CREATE_EVENT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addUser(name: String) {
        val db = this.writableDatabase
        db.execSQL("INSERT INTO $TABLE_NAME ($COLUMN_NAME) VALUES ('$name')")
        db.close()
    }

    fun getUser(): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME LIMIT 1", null)

        var name: String? = null
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(COLUMN_NAME)
            if (columnIndex >= 0) {
                name = cursor.getString(columnIndex)
            }
        }

        cursor.close()
        db.close()
        return name
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

    fun clearEventTable() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $EVENT_TABLE_NAME")
        db.close()
    }

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