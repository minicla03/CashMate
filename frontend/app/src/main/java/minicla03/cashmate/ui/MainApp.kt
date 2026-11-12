package minicla03.cashmate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.navigation.CashMateAppScaffold
import minicla03.cashmate.core.navigation.CashMateAuthNavHost
import minicla03.cashmate.domain.model.AuthenticationState

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
    val authState by viewModel.authenticationState.collectAsStateWithLifecycle()

    CashMateTheme(themeMode = preferences.themeMode) {
        when (authState) {
            AuthenticationState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {
                LoadingStateView(message = stringResource(R.string.auth_loading_session))
            }
            AuthenticationState.Unauthenticated -> CashMateAuthNavHost()
            is AuthenticationState.Authenticated -> CashMateAppScaffold()
        }
    }
}
