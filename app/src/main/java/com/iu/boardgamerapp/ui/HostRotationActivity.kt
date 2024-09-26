package com.iu.boardgamerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.MainViewModel

class HostRotationActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Erstelle UserRepository und AppDatabaseHelper Instanzen
        val databaseHelper = AppDatabaseHelper(this) // Korrekte Instanziierung mit Context
        val repository = UserRepository(databaseHelper) // Falls UserRepository einen Constructor hat, der den databaseHelper benötigt

        // Initialisiere das ViewModel mit der Factory
        val factory = MainViewModelFactory(repository, databaseHelper, this)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Setze den Inhalt auf das HostRotationScreen Composable
        setContent {
            HostRotationScreen(viewModel)
        }
    }

    @Composable
    fun HostRotationScreen(viewModel: MainViewModel) {
        // Beobachte die Benutzerliste
        val userList by viewModel.userList.observeAsState(emptyList())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Wähle einen neuen Gastgeber", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Zeige die Liste der Benutzer an
            userList.forEach { user ->
                Text(user.first) // Angenommen, user.first ist der Benutzername
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button zum Wechseln des Gastgebers
            Button(onClick = { viewModel.rotateHost() }) {
                Text("Gastgeber wechseln")
            }
        }
    }
}
