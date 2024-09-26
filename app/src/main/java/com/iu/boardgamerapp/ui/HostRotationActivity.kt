package com.iu.boardgamerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.data.UserRepository
import com.iu.boardgamerapp.di.MainViewModelFactory
import com.iu.boardgamerapp.ui.datamodel.User

class HostRotationActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Erstelle UserRepository und AppDatabaseHelper Instanzen
        val databaseHelper = AppDatabaseHelper(this)
        val repository = UserRepository(databaseHelper)

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
        val users = viewModel.getUsers() // Umgewandelte Liste von User

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE0E0E0) // Hellgrauer Hintergrund
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ... (dein vorhandener Code bleibt unverändert)

                // Benutzerliste in einer LazyColumn anzeigen
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    items(users.size) { index ->
                        val user = users[index]
                        UserItem(user) { selectedUser ->
                            // Ändere den Gastgeber und gehe zurück zum Homescreen
                            viewModel.changeHost(selectedUser.name)  // Funktion zum Ändern des Gastgebers
                            finish()  // Schließt die Activity
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button zum Wechseln des Gastgebers
                Button(
                    onClick = { viewModel.rotateHost() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(text = "Gastgeber wechseln", color = Color.White)
                }
            }
        }
    }

    @Composable
    fun UserItem(user: User, onClick: (User) -> Unit) {
        Column(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
                .fillMaxWidth()
                .clickable { onClick(user) }
        ) {
            Text(
                text = user.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF318DFF) // Blau für Titel
            )
        }
    }
}