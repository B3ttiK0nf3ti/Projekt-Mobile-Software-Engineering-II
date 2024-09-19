import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import android.content.Intent
import android.provider.CalendarContract
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun GameScheduleScreen(
    gameDates: List<Pair<String, String>>,
    navController: NavController,
    fetchCalendarEvents: () -> Unit // Callback zur Aktualisierung der Daten
) {
    val context = LocalContext.current

    // SwipeRefreshState für Pull-to-Refresh verwenden
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // SwipeRefresh-Wrapper um den Inhalt
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
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Kalender-Überschrift
            Text(text = "Spielplan", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            // Button, um den Gerätekalender zu öffnen
            Button(
                onClick = {
                    // Intent, um den Kalender des Geräts zu öffnen
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = CalendarContract.CONTENT_URI
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Gerätekalender öffnen")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Liste der geplanten Spieltermine anzeigen
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(gameDates.size) { index ->
                    val (date, location) = gameDates[index]
                    Text(text = "Datum: $date, Ort: $location")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button, um zum Hauptbildschirm zurückzukehren
            Button(
                onClick = { navController.popBackStack() },  // Zurück zur vorherigen Seite
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Zurück zum Hauptbildschirm")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button, um ein Ereignis hinzuzufügen
            Button(
                onClick = {
                    // Intent, um ein Ereignis zum Kalender hinzuzufügen
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, "Brettspielabend")
                        putExtra(CalendarContract.Events.EVENT_LOCATION, "Bei Alex")
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 24 * 60 * 60 * 1000) // Beispiel: Morgen
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 26 * 60 * 60 * 1000) // Beispiel: Übermorgen
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Termin hinzufügen")
            }
        }
    }
}
