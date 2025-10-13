package minicla03.cashmate.ui.dashboard.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import minicla03.cashmate.ui.dashboard.componets.DashboardScreen
import minicla03.cashmate.ui.theme.CashMatefrontendTheme

class Dashboard : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashMatefrontendTheme {
                DashboardScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    CashMatefrontendTheme {
        DashboardScreen()
    }
}