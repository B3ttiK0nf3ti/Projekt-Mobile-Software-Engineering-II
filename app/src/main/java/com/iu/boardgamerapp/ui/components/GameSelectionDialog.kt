import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun GameSelectionDialog(
    gameSuggestions: List<String>,
    votes: Map<String, Int>,
    onGameSelected: (String) -> Unit,
    onAddGame: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newGame by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Wähle ein Spiel", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                gameSuggestions.forEach { game ->
                    Button(
                        onClick = { onGameSelected(game) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(game)
                            Text("Stimmen: ${votes[game] ?: 0}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = newGame,
                    onValueChange = { newGame = it },
                    label = { Text("Neues Spiel hinzufügen") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { onAddGame(newGame) }) {
                    Text("Spiel hinzufügen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        }
    }
}
