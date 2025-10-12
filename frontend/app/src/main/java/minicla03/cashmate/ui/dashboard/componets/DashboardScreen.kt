package minicla03.cashmate.ui.dashboard.componets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.rememberNavController

data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun DashboardScreen() {
    val navController = rememberNavController()
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }

    val destinations = listOf(
        BottomNavDestination("home", "Home", Icons.Default.Home),
        BottomNavDestination("profile", "Profilo", Icons.Default.Person),
        BottomNavDestination("settings", "Impostazioni", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = Color(0xFFF3F4F6), // sfondo generale chiaro e neutro
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E1E2F), // blu notte elegante
                contentColor = Color.White
            ) {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = { selectedDestination = index },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.label,
                                tint = if (selectedDestination == index)
                                    Color(0xFFBB86FC) else Color(0xFFE0E0E0)
                            )
                        },
                        label = {
                            Text(
                                destination.label,
                                color = if (selectedDestination == index)
                                    Color(0xFFBB86FC) else Color(0xFFE0E0E0)
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* aggiungi transazione */ },
                containerColor = Color(0xFFBB86FC),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopBarDashboard(actualBalance = 1234.56f)
            GraphSection(modifier = Modifier.fillMaxWidth())
            TransactionSection(modifier = Modifier.fillMaxWidth())
        }
    }
}