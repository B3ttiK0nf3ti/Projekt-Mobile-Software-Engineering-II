package com.iu.boardgamerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.components.MainScreen
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = AppDatabaseHelper(this)
        val userRepository = UserRepository(dbHelper)

        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(userRepository)
        }

        setContent {
            BoardGamerAppTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
