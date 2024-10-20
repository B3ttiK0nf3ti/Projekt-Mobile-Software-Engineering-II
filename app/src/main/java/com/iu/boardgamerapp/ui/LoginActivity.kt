package com.iu.boardgamerapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iu.boardgamerapp.data.AppDatabaseHelper
import com.iu.boardgamerapp.ui.theme.BoardGamerAppTheme

class LoginActivity : ComponentActivity() {

    private lateinit var databaseHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = AppDatabaseHelper()

        setContent {
            BoardGamerAppTheme {
                LoginScreen(
                    onSave = { username ->
                        saveUsername(username)
                    }
                )
            }
        }
    }

    private fun saveUsername(username: String) {
        databaseHelper.addUser(username) { success ->
            if (success) {
                Toast.makeText(this, "Nutzername erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                // Starte MainActivity nach erfolgreichem Speichern
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Fehler beim Speichern des Nutzernamens", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onSave: (String) -> Unit) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    Surface(
        color = Color(0xFFE0E0E0), // Grauer Hintergrund
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Login",
                fontSize = 24.sp,
                color = Color(0xFF318DFF)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Nutzername eingeben") },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmedUsername = username.text.trim()
                    if (trimmedUsername.isNotEmpty()) {
                        onSave(trimmedUsername)
                    } else {
                        snackbarMessage = "Bitte geben Sie einen Nutzernamen ein"
                        showSnackbar = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    }
}
