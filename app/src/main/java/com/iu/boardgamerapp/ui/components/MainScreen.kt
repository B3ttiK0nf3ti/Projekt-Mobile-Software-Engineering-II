package com.iu.boardgamerapp.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iu.boardgamerapp.ui.ChatActivity
import com.iu.boardgamerapp.ui.GameChoosingActivity
import com.iu.boardgamerapp.ui.MainViewModel
import com.iu.boardgamerapp.ui.RatingActivity

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToGameSchedule: () -> Unit,
    onNavigateToHostRotation: () -> Unit
) {
    val userName by viewModel.userName.observeAsState("Gast")
    val currentHost by viewModel.currentHost.observeAsState("Niemand")
    val topVotedGame by viewModel.topVotedGame.observeAsState(null)
    val hasMultipleTopGames by viewModel.hasMultipleTopGames.observeAsState(false) // Neuer State für mehrere Gewinner
    val context = LocalContext.current

    val buttonStyle: @Composable (String, () -> Unit) -> Unit = { text, onClick ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
                .background(
                    color = Color(0xFF318DFF),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "BoardGamerApp",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF318DFF),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Willkommen, $userName!",
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Überprüfen, ob es mehrere Gewinner gibt oder das Top-Spiel angezeigt werden soll
            if (hasMultipleTopGames) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(
                            color = Color(0xFF318DFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            val intent = Intent(context, GameChoosingActivity::class.java)
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Mehrere Gewinner...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else if (topVotedGame != null) {
                // Zeige das Top-Spiel an, wenn es keinen "Mehrere Gewinner"-Status gibt
                val game = topVotedGame!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(
                            color = Color(0xFF318DFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            val intent = Intent(context, GameChoosingActivity::class.java)
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = game.game,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "(${game.votes} Vote${if (game.votes != 1) "s" else ""})",
                            fontSize = 18.sp,
                            color = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Spiel hinzufügen, wenn kein Spiel vorhanden ist
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(
                            color = Color(0xFF318DFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            val intent = Intent(context, GameChoosingActivity::class.java)
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Spiel hinzufügen...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            buttonStyle("Zum Spielplan") {
                onNavigateToGameSchedule()
            }
            buttonStyle("Gastgeber-Rotation") {
                onNavigateToHostRotation()
            }
            buttonStyle("Bewertung abgeben") {
                val intent = Intent(context, RatingActivity::class.java)
                intent.putExtra("currentHost", currentHost)
                context.startActivity(intent)
            }
        }

        // Zusätzliche UI-Komponenten am unteren Bildschirmrand
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Kalender-Symbol links unten
            IconButton(
                onClick = {
                    onNavigateToGameSchedule()
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
