import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxWithBorder(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(BorderStroke(1.dp, Color.Black)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = content)
    }
}
