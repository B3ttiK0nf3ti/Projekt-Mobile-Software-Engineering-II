package com.iu.boardgamerapp.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

        // Create UserRepository and AppDatabaseHelper instances
        val databaseHelper = AppDatabaseHelper(this)
        val repository = UserRepository(databaseHelper)

        // Initialize ViewModel with the factory
        val factory = MainViewModelFactory(repository, databaseHelper, this)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Set the content to the HostRotationScreen composable
        setContent {
            HostRotationScreen(viewModel)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class) // Suppress experimental API warning
    @Composable
    fun HostRotationScreen(viewModel: MainViewModel) {
        // Abrufen der Benutzerliste aus dem ViewModel
        val userList by viewModel.userList.observeAsState(emptyList())

        // Abrufen des aktuellen Gastgebers aus dem ViewModel
        val currentHost by viewModel.currentHost.observeAsState("Lade Gastgeber...") // Standardtext bei Ladezustand


        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE0E0E0) // Hellgrauer Hintergrund
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color.White)
                    ) {
                        IconButton(
                            onClick = { finish() },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Display the current host
                            Text(
                                text = "Gastgeberwechsel", // Setzen Sie hier Ihren Titel ein
                                fontSize = 18.sp, // Angepasste Schriftgröße
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF318DFF), // Angepasste Farbe
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aktuellen Gastgeber anzeigen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp), // Padding auf beiden Seiten
                        contentAlignment = Alignment.Center // Zentriert den Inhalt
                    ) {
                        Text(
                            text = "Aktueller Gastgeber: $currentHost", // Zeigt den aktuellen Gastgeber an
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Benutzerliste in einer LazyColumn anzeigen
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        items(userList.size) { index ->
                            val user = userList[index]
                            UserItem(user) { selectedUser ->
                                // Gastgeber wechseln und zur Startseite zurückkehren
                                viewModel.changeHost(selectedUser.name) {
                                    // Callback, um den Hostwechsel zu bestätigen
                                    setResult(Activity.RESULT_OK) // Setze das Ergebnis
                                    finish()  // Schließt die Activity
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp)) // Fügt Platz zwischen den Benutzer-Items hinzu
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
                color = Color(0xFF318DFF) // Blue for title
            )
        }
    }

}