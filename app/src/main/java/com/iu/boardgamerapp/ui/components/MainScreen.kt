package com.iu.boardgamerapp.ui.components

import BoxWithBorder
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iu.boardgamerapp.ui.ChatActivity
import com.iu.boardgamerapp.ui.GameChoosingActivity
import com.iu.boardgamerapp.ui.MainViewModel
import com.iu.boardgamerapp.ui.RatingActivity

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateToGameSchedule: () -> Unit,
    onNavigateToHostRotation: () -> Unit
) {
    val userName = viewModel.userName.value
    val currentHost by viewModel.currentHost.observeAsState("Niemand")  // Den aktuellen Gastgeber observieren
    val userList by viewModel.userList.observeAsState(emptyList())

    val context = LocalContext.current
    val scrollState = rememberScrollState() // ScrollState für die Spalte

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Willkommen, ${userName ?: "Gast"}!")
            Spacer(modifier = Modifier.height(16.dp))
            DateDisplay(date = "12.09.2024")
            Spacer(modifier = Modifier.height(16.dp))

            // Button zum Aufrufen der GameChoosingActivity
            Button(onClick = {
                val intent = Intent(context, GameChoosingActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Abstimmen")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Aktueller Gastgeber: $currentHost")



            Button(onClick = { onNavigateToHostRotation() }) {
                Text("Gastgeber ändern")
            }

            // Button zum Öffnen der RatingActivity, bei dem der aktuelle Gastgeber übergeben wird
            Button(onClick = {
                // Hier wird der Intent für die RatingActivity erstellt und der aktuelle Gastgeber übergeben
                val intent = Intent(context, RatingActivity::class.java)
                intent.putExtra("currentHost", currentHost)  // Übergabe des aktuellen Gastgebers
                context.startActivity(intent) // Startet die RatingActivity
            }) {
                Text("Bewertung abgeben") // Button, um zur Bewertung zu gehen
            }

            Spacer(modifier = Modifier.height(16.dp))
            BoxWithBorder(content = "Ort: Bei Alex")
            Spacer(modifier = Modifier.height(16.dp))
            BoxWithBorder(content = "Essen: Pizza")
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Kalender-Symbol links unten
            IconButton(
                onClick = {
                    onNavigateToGameSchedule() // Nutzung des Callback
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = "Kalender",
                    tint = Color(0xFF318DFF)
                )
            }

            // Chat-Symbol rechts unten
            IconButton(
                onClick = {
                    val intent = Intent(context, ChatActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Chat",
                    tint = Color(0xFF318DFF)
                )
            }
        }
    }
