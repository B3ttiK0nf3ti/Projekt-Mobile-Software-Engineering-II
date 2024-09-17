// DateDisplay.kt
package com.iu.boardgamerapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun DateDisplay(date: String) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color(0xFFFF5252),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = date, color = Color.White)
        }
    }
}
