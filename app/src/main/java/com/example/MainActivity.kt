package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppRole
import com.example.data.model.AuthUser
import com.example.ui.components.BmsBottomNavBar
import com.example.ui.components.BmsTopBar
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.components.ModulePickerSheet
import com.example.ui.components.NotificationSheet
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel

import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "FirebaseApp init check notice: ${t.message}")
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: BmsViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                BmsApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BmsApp(viewModel: BmsViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val authUser by viewModel.authenticatedUser.collectAsState()

    var showModulePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (!isLoggedIn) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars
        ) { paddingValues ->
            LoginScreen(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
            )
        }
        return
    }

    // Android Hardware/System Back Handler
    BackHandler(enabled = currentTab != BmsNavigationItem.DASHBOARD) {
        viewModel.selectTab(BmsNavigationItem.DASHBOARD)
    }

    Scaffold(
        topBar = {
            BmsTopBar(
                viewModel = viewModel,
                onMenuClick = { showModulePicker = true }
            )
        },
        bottomBar = {
            BmsBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) },
                onMoreClick = { showModulePicker = true }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            // RBAC Access Control Guard
            if (!viewModel.canAccessModule(currentTab)) {
                AccessDeniedScreen(
                    attemptedModule = currentTab.label,
                    currentRole = authUser?.actualRole ?: com.example.data.model.AppRole.ADMIN,
                    requiredRoleDescription = viewModel.getRequiredRoleForModule(currentTab),
                    onBackToDashboard = { viewModel.selectTab(BmsNavigationItem.DASHBOARD) },
                    onSwitchAccount = { viewModel.logout() }
                )
            } else {
                when (currentTab) {
                    BmsNavigationItem.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onQuickAction = { actionKey ->
                            when (actionKey) {
                                "new_lead" -> viewModel.selectTab(BmsNavigationItem.LEADS)
                                "new_client" -> viewModel.selectTab(BmsNavigationItem.CLIENTS)
                                "new_quotation" -> viewModel.selectTab(BmsNavigationItem.QUOTATIONS)
                                "new_booking" -> viewModel.selectTab(BmsNavigationItem.BOOKINGS)
                                "new_payment" -> viewModel.selectTab(BmsNavigationItem.FINANCE)
                                "new_task" -> viewModel.selectTab(BmsNavigationItem.TASKS)
                            }
                        }
                    )
                    BmsNavigationItem.LEADS -> LeadsScreen(viewModel = viewModel)
                    BmsNavigationItem.CLIENTS -> ClientsScreen(viewModel = viewModel)
                    BmsNavigationItem.QUOTATIONS -> QuotationsScreen(viewModel = viewModel)
                    BmsNavigationItem.BOOKINGS -> BookingsScreen(viewModel = viewModel)
                    BmsNavigationItem.WORKFLOWS -> WorkflowsScreen(viewModel = viewModel)
                    BmsNavigationItem.STORAGE -> StorageScreen(viewModel = viewModel)
                    BmsNavigationItem.FINANCE -> FinanceScreen(viewModel = viewModel)
                    BmsNavigationItem.FINANCIAL_YEAR -> FinancialYearScreen(viewModel = viewModel)
                    BmsNavigationItem.TEAM -> TeamScreen(viewModel = viewModel)
                    BmsNavigationItem.SALARY_REPORTS -> SalaryReportsScreen(viewModel = viewModel)
                    BmsNavigationItem.TASKS -> TasksScreen(viewModel = viewModel)
                    BmsNavigationItem.REPORTS -> ReportsScreen(viewModel = viewModel)
                    BmsNavigationItem.SUBSCRIPTION -> SubscriptionScreen(viewModel = viewModel)
                    BmsNavigationItem.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    BmsNavigationItem.ADMIN -> AdminScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showModulePicker) {
        ModulePickerSheet(
            currentTab = currentTab,
            onTabSelected = { tab ->
                viewModel.selectTab(tab)
                showModulePicker = false
            },
            onDismiss = { showModulePicker = false }
        )
    }

    if (showNotifications) {
        NotificationSheet(
            notifications = notifications,
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onDismiss = { viewModel.toggleNotifications(false) }
        )
    }

    if (isSearchActive) {
        GlobalSearchDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleSearch(false) }
        )
    }

}
