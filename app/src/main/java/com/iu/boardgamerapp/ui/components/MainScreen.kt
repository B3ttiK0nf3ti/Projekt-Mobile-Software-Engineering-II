package com.iu.boardgamerapp.ui.components

import BoxWithBorder
import com.iu.boardgamerapp.ui.GameScheduleActivity
import GameSelectionDialog
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iu.boardgamerapp.ui.ChatActivity
import com.iu.boardgamerapp.ui.GameChoosingActivity
import com.iu.boardgamerapp.ui.MainViewModel
import com.iu.boardgamerapp.ui.UserNameInputDialog

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateToGameSchedule: () -> Unit,
    onNavigateToHostRotation: () -> Unit
) {
    val userName = viewModel.userName.value
    val currentHost by viewModel.currentHost.observeAsState("Niemand")
    val rating by viewModel.rating.observeAsState(0)
    val userList by viewModel.userList.observeAsState(emptyList())

    val context = LocalContext.current

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
        Text("Aktueller Gastgeber: $currentHost") // Zeige den aktuellen Gastgeber an

        Button(onClick = { onNavigateToHostRotation() }) {
            Text("Gastgeber ändern")
        }

        Spacer(modifier = Modifier.height(16.dp))
        BoxWithBorder(content = "Ort: Bei Alex")
        Spacer(modifier = Modifier.height(16.dp))
        BoxWithBorder(content = "Essen: Pizza")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Bewertung: $rating")
        Slider(
            value = rating.toFloat(),
            onValueChange = { viewModel.updateRating(it.toInt()) },
            valueRange = 0f..5f,
            steps = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

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
}
