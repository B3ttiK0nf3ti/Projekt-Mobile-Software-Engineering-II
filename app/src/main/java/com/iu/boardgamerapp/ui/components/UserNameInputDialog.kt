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
fun UserNameInputDialog(onNameEntered: (String) -> Unit) {
    var newName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { /* handle dismiss */ }) {
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
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    onNameEntered(newName)
                }) {
                    Text("Speichern")
                }
            }
        }
    }
}
