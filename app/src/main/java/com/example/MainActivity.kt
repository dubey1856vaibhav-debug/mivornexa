package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.data.EmailDispatcher
import com.example.bizflow.data.SessionManager
import com.example.bizflow.model.Customer
import com.example.bizflow.model.EmailDraft
import com.example.bizflow.model.Invoice
import com.example.bizflow.model.WorkRecord
import com.example.bizflow.ui.components.LanguageManager
import com.example.bizflow.ui.navigation.Screen
import com.example.bizflow.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = BizFlowRepository.getInstance()
        repository.initPersistence(this)

        setContent {
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            val savedThemePref = remember { SessionManager.getDarkThemePreference(context) }
            var isDarkTheme by remember { mutableStateOf(savedThemePref ?: systemDark) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MivornexaApp(
                    repository = repository,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = {
                        isDarkTheme = it
                        SessionManager.setDarkThemePreference(context, it)
                    }
                )
            }
        }
    }
}

@Composable
fun MivornexaApp(
    repository: BizFlowRepository,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val customers by repository.customers.collectAsState()

    // Persistent authentication & session recovery
    val wasAuthenticated = remember { SessionManager.isAuthenticated(context) }
    val wasCustomerPortal = remember { SessionManager.isCustomerPortal(context) }
    val savedCustomerId = remember { SessionManager.getSavedCustomerId(context) }

    var isAuthenticated by remember { mutableStateOf(wasAuthenticated) }
    var currentScreen by remember {
        mutableStateOf(
            if (wasAuthenticated) {
                if (wasCustomerPortal) Screen.CustomerPortal else Screen.Dashboard
            } else {
                Screen.Dashboard
            }
        )
    }
    var loggedInCustomer by remember {
        mutableStateOf<Customer?>(
            if (wasAuthenticated && wasCustomerPortal && savedCustomerId != null) {
                customers.find { it.id == savedCustomerId }
            } else null
        )
    }

    // Quick Action modal states
    var showNewOrderModal by remember { mutableStateOf(false) }
    var showNewCustomerModal by remember { mutableStateOf(false) }
    var showRecordPaymentModal by remember { mutableStateOf(false) }
    var showNewExpenseModal by remember { mutableStateOf(false) }

    // Email Preview Dialog state
    var activeEmailDraft by remember { mutableStateOf<EmailDraft?>(null) }

    // Back handling
    if (!isAuthenticated) {
        // Stay on auth
    } else if (currentScreen != Screen.Dashboard) {
        BackHandler {
            currentScreen = Screen.Dashboard
        }
    }

    if (!isAuthenticated) {
        AuthScreen(
            repository = repository,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            onLoginSuccess = { isCustomerPortal, client ->
                isAuthenticated = true
                if (isCustomerPortal && client != null) {
                    loggedInCustomer = client
                    currentScreen = Screen.CustomerPortal
                } else {
                    currentScreen = Screen.Dashboard
                }
            }
        )
        return
    }

    val isTopLevelScreen = currentScreen in listOf(
        Screen.Dashboard,
        Screen.WorkOrders,
        Screen.Customers,
        Screen.Finance,
        Screen.SettingsAndMore
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("mivornexa_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = {
                            Icon(
                                if (currentScreen == Screen.Dashboard) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text(LanguageManager.t("nav_dashboard")) },
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.WorkOrders,
                        onClick = { currentScreen = Screen.WorkOrders },
                        icon = {
                            Icon(
                                if (currentScreen == Screen.WorkOrders) Icons.Filled.Work else Icons.Outlined.WorkOutline,
                                contentDescription = "Work"
                            )
                        },
                        label = { Text(currentWorkspace.businessType.workRecordPlural.take(8)) },
                        modifier = Modifier.testTag("nav_item_work")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Customers,
                        onClick = { currentScreen = Screen.Customers },
                        icon = {
                            Icon(
                                if (currentScreen == Screen.Customers) Icons.Filled.People else Icons.Outlined.PeopleOutline,
                                contentDescription = "Customers"
                            )
                        },
                        label = { Text(LanguageManager.t("nav_customers")) },
                        modifier = Modifier.testTag("nav_item_customers")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Finance,
                        onClick = { currentScreen = Screen.Finance },
                        icon = {
                            Icon(
                                if (currentScreen == Screen.Finance) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "Finance"
                            )
                        },
                        label = { Text(LanguageManager.t("nav_finance")) },
                        modifier = Modifier.testTag("nav_item_finance")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.SettingsAndMore,
                        onClick = { currentScreen = Screen.SettingsAndMore },
                        icon = {
                            Icon(
                                if (currentScreen == Screen.SettingsAndMore) Icons.Filled.Menu else Icons.Outlined.Menu,
                                contentDescription = "More"
                            )
                        },
                        label = { Text(LanguageManager.t("nav_more")) },
                        modifier = Modifier.testTag("nav_item_more")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.Auth -> {
                AuthScreen(
                    repository = repository,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onLoginSuccess = { isCustomerPortal, client ->
                        isAuthenticated = true
                        if (isCustomerPortal && client != null) {
                            loggedInCustomer = client
                            currentScreen = Screen.CustomerPortal
                        } else {
                            currentScreen = Screen.Dashboard
                        }
                    }
                )
            }

            Screen.Dashboard -> DashboardScreen(
                repository = repository,
                onNavigateToWork = { currentScreen = Screen.WorkOrders },
                onNavigateToCustomers = { currentScreen = Screen.Customers },
                onNavigateToFinance = { currentScreen = Screen.Finance },
                onNavigateToInventory = { currentScreen = Screen.Inventory },
                onNavigateToTasks = { currentScreen = Screen.Tasks },
                onNavigateToShopify = { currentScreen = Screen.Shopify },
                onNavigateToAi = { currentScreen = Screen.AiAutomation },
                onNavigateToSearch = { currentScreen = Screen.GlobalSearch },
                onOpenBackendStatus = { currentScreen = Screen.FirebaseConnection },
                onOpenNewOrderModal = { showNewOrderModal = true },
                onOpenNewCustomerModal = { showNewCustomerModal = true },
                onOpenRecordPaymentModal = { showRecordPaymentModal = true },
                onOpenNewExpenseModal = { showNewExpenseModal = true },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.WorkOrders -> WorkOrdersScreen(
                repository = repository,
                onOpenEmailPreview = { record, stageName ->
                    activeEmailDraft = EmailDispatcher.composeWorkStatusEmail(record, stageName, currentWorkspace)
                },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.Customers -> CustomersScreen(
                repository = repository,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                onStartOrderForCustomer = { cust ->
                    showNewOrderModal = true
                },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.Finance -> FinanceScreen(
                repository = repository,
                onOpenEmailPreviewForInvoice = { invoice ->
                    activeEmailDraft = EmailDispatcher.composeInvoiceEmail(invoice, currentWorkspace)
                },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.Inventory -> InventoryScreen(
                repository = repository,
                modifier = Modifier.padding(innerPadding)
            )

            Screen.Tasks -> TasksScreen(
                repository = repository,
                modifier = Modifier.padding(innerPadding)
            )

            Screen.Shopify -> ShopifyScreen(
                repository = repository,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.AiAutomation -> AiAutomationScreen(
                repository = repository,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.CustomerPortal -> {
                val activeClient = loggedInCustomer ?: customers.firstOrNull() ?: Customer(
                    workspaceId = currentWorkspace.id,
                    name = "Sarah Jenkins",
                    phone = "+1 (555) 234-8901",
                    email = "sarah.j@example.com"
                )
                CustomerPortalScreen(
                    customer = activeClient,
                    workspace = currentWorkspace,
                    repository = repository,
                    onSignOut = {
                        SessionManager.clearSession(context)
                        loggedInCustomer = null
                        isAuthenticated = false
                        currentScreen = Screen.Dashboard
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            Screen.GlobalSearch -> GlobalSearchScreen(
                repository = repository,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.FirebaseConnection -> FirebaseConnectionScreen(
                repository = repository,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                modifier = Modifier.padding(innerPadding)
            )

            Screen.SettingsAndMore -> SettingsAndMoreScreen(
                repository = repository,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                onNavigateToInventory = { currentScreen = Screen.Inventory },
                onNavigateToTasks = { currentScreen = Screen.Tasks },
                onNavigateToShopify = { currentScreen = Screen.Shopify },
                onNavigateToAi = { currentScreen = Screen.AiAutomation },
                onSwitchToCustomerPortal = {
                    loggedInCustomer = customers.firstOrNull()
                    currentScreen = Screen.CustomerPortal
                },
                onSignOut = {
                    SessionManager.clearSession(context)
                    isAuthenticated = false
                    currentScreen = Screen.Dashboard
                },
                onNavigateToBackendStatus = { currentScreen = Screen.FirebaseConnection },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Quick Action Dialogs
    if (showNewCustomerModal) {
        CustomerEditDialog(
            workspaceId = currentWorkspace.id,
            repository = repository,
            onDismiss = { showNewCustomerModal = false },
            onSave = { newCust ->
                repository.saveCustomer(newCust)
                showNewCustomerModal = false
            }
        )
    }

    if (showNewOrderModal) {
        WorkRecordFormDialog(
            workspace = currentWorkspace,
            customers = customers,
            onDismiss = { showNewOrderModal = false },
            onSave = { newRecord ->
                repository.saveWorkRecord(newRecord)
                showNewOrderModal = false
            }
        )
    }

    if (showNewExpenseModal) {
        AddExpenseDialog(
            workspaceId = currentWorkspace.id,
            currencySymbol = currentWorkspace.currencySymbol,
            onDismiss = { showNewExpenseModal = false },
            onSave = { exp ->
                repository.saveExpense(exp)
                showNewExpenseModal = false
            }
        )
    }

    if (showRecordPaymentModal) {
        val invoices by repository.invoices.collectAsState()
        val unpaidInvoice = invoices.firstOrNull { it.balanceDue > 0.0 } ?: invoices.firstOrNull()
        if (unpaidInvoice != null) {
            RecordPaymentDialog(
                invoice = unpaidInvoice,
                currencySymbol = currentWorkspace.currencySymbol,
                onDismiss = { showRecordPaymentModal = false },
                onSave = { payment ->
                    repository.recordPayment(unpaidInvoice.id, payment)
                    showRecordPaymentModal = false
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showRecordPaymentModal = false },
                title = { Text("No Open Invoices") },
                text = { Text("All logged invoices are settled! Create a new invoice or work order first.") },
                confirmButton = {
                    Button(onClick = { showRecordPaymentModal = false }) { Text("OK") }
                }
            )
        }
    }

    // Active Email Preview Dialog
    activeEmailDraft?.let { draft ->
        EmailPreviewDialog(
            initialDraft = draft,
            onDismiss = { activeEmailDraft = null }
        )
    }
}
