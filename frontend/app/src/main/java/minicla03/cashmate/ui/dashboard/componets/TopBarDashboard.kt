package minicla03.cashmate.ui.dashboard.componets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBarDashboard(
    actualBalance: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2F))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Saldo Attuale",
            fontSize = 18.sp,
            color = Color(0xFFB0BEC5)
        )
        Text(
            text = "€ ${"%,.2f".format(actualBalance)}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBB86FC)
        )
    }
}
