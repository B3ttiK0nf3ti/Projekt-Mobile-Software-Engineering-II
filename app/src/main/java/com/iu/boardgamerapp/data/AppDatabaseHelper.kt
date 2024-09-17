package com.iu.boardgamerapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "boardgamerapp.db"
        private const val TABLE_NAME = "user"
        private const val COLUMN_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_USER_TABLE = ("CREATE TABLE $TABLE_NAME ($COLUMN_NAME TEXT)")
        db.execSQL(CREATE_USER_TABLE)
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
}
