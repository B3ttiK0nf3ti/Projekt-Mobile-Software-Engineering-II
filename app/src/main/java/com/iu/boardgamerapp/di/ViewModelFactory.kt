package com.iu.boardgamerapp.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.ui.MainViewModel

class MainViewModelFactory(
    private val userRepository: UserRepository,
    private val databaseHelper: AppDatabaseHelper,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(userRepository) as T // Verwende databaseHelper, nicht einen neuen
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
