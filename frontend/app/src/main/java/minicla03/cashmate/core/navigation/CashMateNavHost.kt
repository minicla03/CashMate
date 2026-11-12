package minicla03.cashmate.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import minicla03.cashmate.R
import minicla03.cashmate.feature.categories.CategoriesRoute
import minicla03.cashmate.feature.dashboard.DashboardRoute
import minicla03.cashmate.feature.receipt.presentation.ReceiptReviewRoute
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanRoute
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanViewModel
import minicla03.cashmate.feature.settings.SettingsRoute
import minicla03.cashmate.feature.statistics.StatisticsRoute
import minicla03.cashmate.feature.transactions.TransactionDetailRoute
import minicla03.cashmate.feature.transactions.TransactionFormRoute
import minicla03.cashmate.feature.transactions.TransactionsRoute

private enum class TopLevelDestination(
    val route: CashMateRoute,
    @StringRes val labelResId: Int,
    val icon: ImageVector
) {
    DASHBOARD(CashMateRoute.Dashboard, R.string.nav_dashboard, Icons.Rounded.Dashboard),
    TRANSACTIONS(CashMateRoute.Transactions, R.string.nav_transactions, Icons.AutoMirrored.Rounded.ReceiptLong),
    STATISTICS(CashMateRoute.Statistics, R.string.nav_statistics, Icons.Rounded.BarChart),
    SETTINGS(CashMateRoute.Settings, R.string.nav_settings, Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashMateAppScaffold(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val activeTopLevel = TopLevelDestination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun navigateTopLevel(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun showMessage(@StringRes resId: Int) {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(resId))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (activeTopLevel != null) {
                TopAppBar(
                    title = { Text(text = stringResource(activeTopLevel.labelResId)) }
                )
            }
        },
        bottomBar = {
            if (activeTopLevel != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = activeTopLevel == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelResId)
                                )
                            },
                            label = { Text(text = stringResource(destination.labelResId)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTopLevel == TopLevelDestination.DASHBOARD ||
                activeTopLevel == TopLevelDestination.TRANSACTIONS
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(CashMateRoute.TransactionForm()) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.cd_add_transaction)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CashMateRoute.Dashboard,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable<CashMateRoute.Dashboard> {
                DashboardRoute(
                    onNavigateToAddTransaction = {
                        navController.navigate(CashMateRoute.TransactionForm())
                    },
                    onNavigateToScanReceipt = {
                        navController.navigate(CashMateRoute.ReceiptFlow) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTransactions = {
                        navigateTopLevel(TopLevelDestination.TRANSACTIONS)
                    },
                    onNavigateToTransactionDetail = { id ->
                        navController.navigate(CashMateRoute.TransactionDetail(id))
                    }
                )
            }

            composable<CashMateRoute.Transactions> {
                TransactionsRoute(
                    onNavigateToAddTransaction = {
                        navController.navigate(CashMateRoute.TransactionForm())
                    },
                    onNavigateToScanReceipt = {
                        navController.navigate(CashMateRoute.ReceiptFlow) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTransactionDetail = { id ->
                        navController.navigate(CashMateRoute.TransactionDetail(id))
                    }
                )
            }

            composable<CashMateRoute.TransactionDetail> {
                TransactionDetailRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(CashMateRoute.TransactionForm(transactionId = id))
                    },
                    onDeleted = {
                        navController.popBackStack()
                        showMessage(R.string.transaction_deleted_message)
                    }
                )
            }

            composable<CashMateRoute.TransactionForm> {
                TransactionFormRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                        showMessage(R.string.transaction_saved_message)
                    }
                )
            }

            composable<CashMateRoute.Categories> {
                CategoriesRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<CashMateRoute.Statistics> {
                StatisticsRoute()
            }

            composable<CashMateRoute.Settings> {
                SettingsRoute(
                    onNavigateToCategories = {
                        navController.navigate(CashMateRoute.Categories)
                    }
                )
            }

            navigation<CashMateRoute.ReceiptFlow>(
                startDestination = CashMateRoute.ReceiptScan
            ) {
                composable<CashMateRoute.ReceiptScan> { backStackEntry ->
                    val flowEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(CashMateRoute.ReceiptFlow)
                    }
                    val viewModel: ReceiptScanViewModel = hiltViewModel(flowEntry)
                    ReceiptScanRoute(
                        viewModel = viewModel,
                        onNavigateBack = {
                            navController.popBackStack(CashMateRoute.ReceiptFlow, inclusive = true)
                        },
                        onNavigateToReview = { receiptId ->
                            navController.navigate(CashMateRoute.ReceiptReview(receiptId)) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToTransactionDetail = { txId ->
                            navController.navigate(CashMateRoute.TransactionDetail(txId)) {
                                popUpTo(CashMateRoute.ReceiptFlow) { inclusive = true }
                            }
                        }
                    )
                }

                composable<CashMateRoute.ReceiptReview> { backStackEntry ->
                    val route = backStackEntry.toRoute<CashMateRoute.ReceiptReview>()
                    val flowEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(CashMateRoute.ReceiptFlow)
                    }
                    val viewModel: ReceiptScanViewModel = hiltViewModel(flowEntry)
                    ReceiptReviewRoute(
                        receiptId = route.receiptId,
                        viewModel = viewModel,
                        onExitFlow = {
                            navController.popBackStack(CashMateRoute.ReceiptFlow, inclusive = true)
                        },
                        onNavigateToTransactionDetail = { txId ->
                            navController.navigate(CashMateRoute.TransactionDetail(txId)) {
                                popUpTo(CashMateRoute.ReceiptFlow) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
