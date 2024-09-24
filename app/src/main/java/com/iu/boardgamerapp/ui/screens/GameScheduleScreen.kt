import androidx.compose.foundation.background
import androidx.compose.foundation.layout.* // für padding, Spacers, etc.
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.lazy.LazyColumn
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
//asdsf
@Composable
fun GameScheduleScreen(
    gameDates: List<Pair<String, String>>,
    navController: NavController,
    fetchCalendarEvents: () -> Unit
) {
    val context = LocalContext.current

    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE0E0E0) // Hellgrauer Hintergrund wie in der ChatActivity
    ) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                isRefreshing = true
                fetchCalendarEvents() // Daten neu laden
                isRefreshing = false
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zurück-Button wie in ChatActivity
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.Gray
                    )
                }

                // Überschrift
                Text(
                    text = "Spielplan",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF318DFF) // Blau wie in ChatActivity
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gerätekalender öffnen Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_CALENDAR) // Öffnet die Kalender-App
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)), // Blau wie in ChatActivity
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(text = "Gerätekalender öffnen", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Spieltermine in einer LazyColumn anzeigen
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    items(gameDates.size) { index ->
                        val (date, location) = gameDates[index]
                        ScheduleItem(date = date, location = location)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button, um ein Ereignis hinzuzufügen
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, "Brettspielabend")
                            putExtra(CalendarContract.Events.EVENT_LOCATION, "Bei Alex")
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 26 * 60 * 60 * 1000)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF318DFF)),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(text = "Termin hinzufügen", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(date: String, location: String) {
    Column(
        modifier = Modifier
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Datum: $date",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF318DFF) // Blau für Titel, wie in der ChatActivity
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ort: $location",
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}
