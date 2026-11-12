package minicla03.cashmate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.navigation.CashMateAppScaffold

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashMateApp()
        }
    }
}

@Composable
fun CashMateApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    CashMateTheme(themeMode = preferences.themeMode) {
        CashMateAppScaffold()
    }
}
