package minicla03.cashmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import minicla03.cashmate.ui.dashboard.componets.HomeScreen
import minicla03.cashmate.ui.dashboard.componets.ProfileScreen
import minicla03.cashmate.ui.dashboard.componets.SearchScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier)
{
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") { HomeScreen(navController) }
        composable("search") { SearchScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }

}
