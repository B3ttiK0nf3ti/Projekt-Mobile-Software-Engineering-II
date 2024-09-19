// In Dialogs.kt
package com.iu.boardgamerapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun UserNameInputDialog(
    onNameEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bitte gib deinen Namen ein:")

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = null // Fehler zurücksetzen, wenn der Benutzer tippt
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    isError = errorMessage != null // Fehlerstatus des Textfelds
                )

                // Fehlermeldung anzeigen, falls vorhanden
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        if (newName.isBlank()) {
                            // Fehlermeldung setzen, wenn der Name leer ist
                            errorMessage = "Benutzernamen eingeben"
                        } else {
                            onNameEntered(newName)
                        }
                    }) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}

