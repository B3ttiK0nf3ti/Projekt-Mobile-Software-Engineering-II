package com.iu.boardgamerapp.data

class UserRepository(private val dbHelper: AppDatabaseHelper) {

    fun addUser(name: String) {
        dbHelper.addUser(name)
    }

    fun getUser(): String? {
        return dbHelper.getUser()
    }
}
