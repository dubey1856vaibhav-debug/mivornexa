package com.example.bizflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.BusinessType
import com.example.bizflow.model.ModuleType
import com.example.bizflow.model.UserRole
import com.example.bizflow.model.Workspace
import com.example.bizflow.ui.components.AppLanguage
import com.example.bizflow.ui.components.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndMoreScreen(
    repository: BizFlowRepository,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToShopify: () -> Unit,
    onNavigateToAi: () -> Unit,
    onSwitchToCustomerPortal: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToBackendStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val teamMembers by repository.teamMembers.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditBusinessDialog by remember { mutableStateOf(false) }
    var showWorkspaceSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = LanguageManager.t("nav_more"), fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Workspace Identity Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = currentWorkspace.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentWorkspace.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = currentWorkspace.businessType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (currentWorkspace.tagline.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentWorkspace.tagline,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${currentWorkspace.address} • ${currentWorkspace.email} • ${currentWorkspace.phone}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = { showEditBusinessDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_edit_workspace_identity")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Business & Type", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showWorkspaceSelector = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_switch_workspace")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Company", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Business & Workspace Configuration Section
            item {
                Text("Business Profile & Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.Default.Storefront,
                            title = "Business Type & Industry Template",
                            subtitle = "Current: ${currentWorkspace.businessType.displayName} (click to change)",
                            onClick = { showEditBusinessDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.ReceiptLong,
                            title = "Currency, Pricing & Tax Rates",
                            subtitle = "${currentWorkspace.currencySymbol} (${currentWorkspace.currencyCode}) • Tax: ${currentWorkspace.taxRatePercent}%",
                            onClick = { showEditBusinessDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.Widgets,
                            title = "Active Modules & Pipeline Stages",
                            subtitle = "${currentWorkspace.enabledModules.size} modules active • ${currentWorkspace.workflowStages.size} pipeline stages",
                            onClick = { showEditBusinessDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.Apartment,
                            title = "Switch / Add Company Workspace",
                            subtitle = "Switch between multiple businesses or create a new branch",
                            onClick = { showWorkspaceSelector = true }
                        )
                    }
                }
            }

            // Quick Modules Link
            item {
                Text("Operational Modules & Integrations", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.Default.ShoppingBag,
                            title = "Shopify Store Integration",
                            subtitle = "Sync products, inventory quantities, orders & webhooks",
                            onClick = onNavigateToShopify
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "AI & Multi-Provider Automations",
                            subtitle = "Gemini, OpenAI, Claude orchestration & tool approvals",
                            onClick = onNavigateToAi
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.PersonSearch,
                            title = "Customer Portal Preview",
                            subtitle = "View client self-service tracking and PDF downloads",
                            onClick = onSwitchToCustomerPortal
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.Inventory2,
                            title = LanguageManager.t("inventory_catalog"),
                            subtitle = "Items, unit prices, supplier names, stock adjustments",
                            onClick = onNavigateToInventory
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.Checklist,
                            title = LanguageManager.t("team_tasks"),
                            subtitle = "Action items, due dates, staff assignments",
                            onClick = onNavigateToTasks
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.Cloud,
                            title = "Backend & Cloud Sync",
                            subtitle = "Shared Mivornexa database status, google-services.json setup",
                            onClick = onNavigateToBackendStatus
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowItem(
                            icon = Icons.Default.Logout,
                            title = "Sign Out to Welcome Screen",
                            subtitle = "Return to animated login and authentication panel",
                            onClick = onSignOut
                        )
                    }
                }
            }

            // App Preferences
            item {
                Text("Preferences & Interface", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true }
                                .padding(16.dp)
                                .testTag("language_selector_row")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("App Language / भाषा", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "${LanguageManager.currentLanguage.displayName} (${LanguageManager.currentLanguage.nativeName})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Dark Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Comfortable contrast for night work", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onToggleDarkTheme,
                                modifier = Modifier.testTag("dark_theme_switch")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val count = context.cacheDir.listFiles()?.size ?: 0
                                    context.cacheDir.deleteRecursively()
                                    context.cacheDir.mkdirs()
                                    Toast.makeText(context, "Cleared $count temporary cached files", Toast.LENGTH_SHORT).show()
                                }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Clear Disposable PDF Cache", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Frees temporary storage without deleting cloud records", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }

            // Team Roster Section
            item {
                Text("Workspace Team Members (${teamMembers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            items(teamMembers) { member ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = member.name.take(1),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = member.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "${member.email} • ${member.department}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = member.role.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWorkspaceSelector) {
        WorkspaceSelectorDialog(
            repository = repository,
            onDismiss = { showWorkspaceSelector = false }
        )
    }

    if (showEditBusinessDialog) {
        EditBusinessSettingsDialog(
            workspace = currentWorkspace,
            onDismiss = { showEditBusinessDialog = false },
            onSave = { updatedWorkspace, adaptWorkflows ->
                repository.updateWorkspace(updatedWorkspace, adaptWorkflowsAndFields = adaptWorkflows)
                Toast.makeText(context, "Business settings and type updated successfully", Toast.LENGTH_SHORT).show()
                showEditBusinessDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language / भाषा પસંદ કરો", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LanguageManager.currentLanguage = lang
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = LanguageManager.currentLanguage == lang,
                                onClick = {
                                    LanguageManager.currentLanguage = lang
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = lang.nativeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = lang.displayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }
}

fun getBusinessTypeIcon(type: BusinessType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        BusinessType.RETAIL_ECOMMERCE -> Icons.Default.ShoppingCart
        BusinessType.BESPOKE_BOUTIQUE -> Icons.Default.ShoppingBag
        BusinessType.PROFESSIONAL_SERVICES -> Icons.Default.Work
        BusinessType.REPAIR_TECH -> Icons.Default.Build
        BusinessType.EDUCATION_TUTORING -> Icons.Default.School
        BusinessType.RESTAURANT_HOSPITALITY -> Icons.Default.Restaurant
        BusinessType.HEALTH_WELLNESS_SPA -> Icons.Default.Spa
        BusinessType.FITNESS_GYM_STUDIO -> Icons.Default.FitnessCenter
        BusinessType.AUTOMOTIVE_GARAGE -> Icons.Default.DirectionsCar
        BusinessType.CONSTRUCTION_CONTRACTING -> Icons.Default.Construction
        BusinessType.LOGISTICS_FREIGHT -> Icons.Default.LocalShipping
        BusinessType.MANUFACTURING_FABRICATION -> Icons.Default.Handyman
        BusinessType.REAL_ESTATE_PROPERTY -> Icons.Default.HomeWork
    }
}

fun getBusinessTypeAccentColor(type: BusinessType): Color {
    return when (type) {
        BusinessType.RETAIL_ECOMMERCE -> Color(0xFF2563EB)
        BusinessType.BESPOKE_BOUTIQUE -> Color(0xFF9333EA)
        BusinessType.PROFESSIONAL_SERVICES -> Color(0xFF4F46E5)
        BusinessType.REPAIR_TECH -> Color(0xFFEA580C)
        BusinessType.EDUCATION_TUTORING -> Color(0xFF7C3AED)
        BusinessType.RESTAURANT_HOSPITALITY -> Color(0xFFD97706)
        BusinessType.HEALTH_WELLNESS_SPA -> Color(0xFF0D9488)
        BusinessType.FITNESS_GYM_STUDIO -> Color(0xFFE11D48)
        BusinessType.AUTOMOTIVE_GARAGE -> Color(0xFFDC2626)
        BusinessType.CONSTRUCTION_CONTRACTING -> Color(0xFFB45309)
        BusinessType.LOGISTICS_FREIGHT -> Color(0xFF0284C7)
        BusinessType.MANUFACTURING_FABRICATION -> Color(0xFF475569)
        BusinessType.REAL_ESTATE_PROPERTY -> Color(0xFF059669)
    }
}

fun getBusinessTypeDescription(type: BusinessType): String {
    return when (type) {
        BusinessType.RETAIL_ECOMMERCE -> "Orders, packaging, courier tracking & inventory stock"
        BusinessType.BESPOKE_BOUTIQUE -> "Custom orders, tailoring, measurements & fabric sourcing"
        BusinessType.PROFESSIONAL_SERVICES -> "Client projects, deliverables, milestones & billable hours"
        BusinessType.REPAIR_TECH -> "Repair jobs, device IMEI/serial, bench diagnostics & spare parts"
        BusinessType.EDUCATION_TUTORING -> "Cohort enrollments, classes, mid-term grading & graduation"
        BusinessType.RESTAURANT_HOSPITALITY -> "Food orders, kitchen prep, table management & dining service"
        BusinessType.HEALTH_WELLNESS_SPA -> "Appointments, treatment sessions, therapists & rebooking"
        BusinessType.FITNESS_GYM_STUDIO -> "Memberships, class passes, coaching & fitness goals"
        BusinessType.AUTOMOTIVE_GARAGE -> "Vehicle service orders, VIN, diagnostics, repairs & road testing"
        BusinessType.CONSTRUCTION_CONTRACTING -> "Job contracts, site blueprints, staging, permits & inspection"
        BusinessType.LOGISTICS_FREIGHT -> "Waybills, freight haulage, customs, tracking & proof of delivery"
        BusinessType.MANUFACTURING_FABRICATION -> "Production runs, CAD specs, CNC batches & QA tolerance check"
        BusinessType.REAL_ESTATE_PROPERTY -> "Property listings, client viewings, escrow deals & handovers"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBusinessSettingsDialog(
    workspace: Workspace,
    onDismiss: () -> Unit,
    onSave: (Workspace, Boolean) -> Unit
) {
    var selectedBusinessType by remember { mutableStateOf(workspace.businessType) }
    var name by remember { mutableStateOf(workspace.name) }
    var tagline by remember { mutableStateOf(workspace.tagline) }
    var email by remember { mutableStateOf(workspace.email) }
    var phone by remember { mutableStateOf(workspace.phone) }
    var address by remember { mutableStateOf(workspace.address) }
    var currencySymbol by remember { mutableStateOf(workspace.currencySymbol) }
    var currencyCode by remember { mutableStateOf(workspace.currencyCode) }
    var taxRatePercentStr by remember { mutableStateOf(workspace.taxRatePercent.toString()) }
    var enabledModules by remember { mutableStateOf(workspace.enabledModules.toSet()) }
    var adaptWorkflowStages by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var nameError by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "Retail & Commerce", "Services & Tech", "Hospitality & Wellness", "Industrial & Trades", "Property & Real Estate")

    val filteredBusinessTypes = remember(searchQuery, selectedCategoryFilter) {
        BusinessType.values().filter { bType ->
            val matchesCategory = selectedCategoryFilter == "All" || bType.category == selectedCategoryFilter
            val matchesSearch = searchQuery.isBlank() ||
                    bType.displayName.contains(searchQuery, ignoreCase = true) ||
                    bType.workRecordPlural.contains(searchQuery, ignoreCase = true) ||
                    getBusinessTypeDescription(bType).contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = "Business & Workspace Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize industry type, company profile & modules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Industry", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Profile", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Finance", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Modules", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // Scrollable tab content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Industry & Business Type Tab (UI-UX Pro Max Inspired)
                            // 1. Live Blueprint Preview Hero Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    width = 1.dp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(getBusinessTypeAccentColor(selectedBusinessType))
                                        ) {
                                            Icon(
                                                imageVector = getBusinessTypeIcon(selectedBusinessType),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedBusinessType.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Category: ${selectedBusinessType.category} • Record: ${selectedBusinessType.workRecordLabel}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = getBusinessTypeDescription(selectedBusinessType),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Pipeline preview
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "DEFAULT PIPELINE STAGES:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val stages = remember(selectedBusinessType) {
                                        BizFlowRepository.getInstance().getDefaultStagesForType(selectedBusinessType)
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(stages) { stage ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = CardDefaults.outlinedCardBorder()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    val color = try {
                                                        Color(android.graphics.Color.parseColor(stage.colorHex))
                                                    } catch (_: Exception) {
                                                        MaterialTheme.colorScheme.primary
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(stage.name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }

                                    // Custom fields preview
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "TAILORED INDUSTRY FIELDS:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val fields = remember(selectedBusinessType) {
                                        BizFlowRepository.getInstance().getDefaultCustomFieldsForType(selectedBusinessType)
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(fields) { cf ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = "${cf.label}${if (cf.isRequired) " *" else ""}",
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Search & Category Filters
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search 13 industries (e.g. gym, salon, logistics, garage)...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("search_business_types")
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { cat ->
                                    val isSelected = selectedCategoryFilter == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategoryFilter = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Text(
                                text = "Available Business Models (${filteredBusinessTypes.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 3. Grid of Industry Cards
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredBusinessTypes.forEach { bType ->
                                    val isSelected = bType == selectedBusinessType
                                    val accentColor = getBusinessTypeAccentColor(bType)
                                    val icon = getBusinessTypeIcon(bType)
                                    val description = getBusinessTypeDescription(bType)

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        border = if (isSelected)
                                            CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                                width = 2.dp
                                            )
                                        else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedBusinessType = bType
                                            }
                                            .testTag("business_type_option_${bType.name}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) accentColor
                                                        else accentColor.copy(alpha = 0.15f)
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else accentColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = bType.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Text(
                                                    text = description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surface
                                                    ) {
                                                        Text(
                                                            text = bType.category,
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surface
                                                    ) {
                                                        Text(
                                                            text = "Uses: ${bType.workRecordPlural}",
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedBusinessType = bType }
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedBusinessType != workspace.businessType) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Changing from ${workspace.businessType.displayName} to ${selectedBusinessType.displayName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Adapt workflow stages & custom fields",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Automatically configure standard ${selectedBusinessType.displayName} stages",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = adaptWorkflowStages,
                                                onCheckedChange = { adaptWorkflowStages = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Company Profile Details Tab
                            Text(
                                text = "Company Identity & Contact",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = it.isBlank()
                                },
                                label = { Text("Business / Company Name *") },
                                isError = nameError,
                                supportingText = if (nameError) { { Text("Business name cannot be empty") } } else null,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_edit_business_name")
                            )

                            OutlinedTextField(
                                value = tagline,
                                onValueChange = { tagline = it },
                                label = { Text("Tagline / Business Slogan") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Official Contact Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Business Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Business Address / Physical Location") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            // Currency & Financial Configuration Tab
                            Text(
                                text = "Currency & Taxation Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "These symbols and rates apply to invoices, customer balances, payments, and product catalogs.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Quick Currency Presets",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            val presets = listOf(
                                Pair("$", "USD"),
                                Pair("₹", "INR"),
                                Pair("€", "EUR"),
                                Pair("£", "GBP"),
                                Pair("C$", "CAD"),
                                Pair("A$", "AUD"),
                                Pair("AED", "AED"),
                                Pair("¥", "JPY")
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presets) { (sym, code) ->
                                    val isSelected = currencySymbol == sym && currencyCode == code
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            currencySymbol = sym
                                            currencyCode = code
                                        },
                                        label = { Text("$sym $code", fontSize = 11.sp) }
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = currencySymbol,
                                    onValueChange = { currencySymbol = it },
                                    label = { Text("Symbol (e.g. $, ₹, €)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = currencyCode,
                                    onValueChange = { currencyCode = it.uppercase() },
                                    label = { Text("Code (USD, INR)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = taxRatePercentStr,
                                onValueChange = { taxRatePercentStr = it },
                                label = { Text("Default Tax Rate (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            // Active Operational Modules Tab
                            Text(
                                text = "Active Operational Modules",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Enable or disable modules to streamline your team's interface.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val moduleList = listOf(
                                Pair(ModuleType.ORDERS, "Orders & Work Jobs (${selectedBusinessType.workRecordPlural})"),
                                Pair(ModuleType.INVENTORY, "Inventory, Products & Stock Levels"),
                                Pair(ModuleType.BILLING, "Invoicing, Quotes & Receipts"),
                                Pair(ModuleType.EXPENSES, "Expense Tracking & Financials"),
                                Pair(ModuleType.TASKS, "Team Tasks & Action Items"),
                                Pair(ModuleType.APPOINTMENTS, "Appointments & Bookings"),
                                Pair(ModuleType.PROJECTS, "Multi-Milestone Client Projects"),
                                Pair(ModuleType.CUSTOM_FIELDS, "Industry Custom Fields")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                moduleList.forEach { (mod, label) ->
                                    val isEnabled = enabledModules.contains(mod)
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = isEnabled,
                                                onCheckedChange = { checked ->
                                                    enabledModules = if (checked) {
                                                        enabledModules + mod
                                                    } else {
                                                        enabledModules - mod
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky Bottom Bar
                HorizontalDivider()
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                selectedTab = 1
                                return@Button
                            }
                            val updated = workspace.copy(
                                name = name.trim(),
                                tagline = tagline.trim(),
                                businessType = selectedBusinessType,
                                email = email.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                currencySymbol = currencySymbol.trim().ifEmpty { "$" },
                                currencyCode = currencyCode.trim().uppercase().ifEmpty { "USD" },
                                taxRatePercent = taxRatePercentStr.toDoubleOrNull() ?: 0.0,
                                enabledModules = if (enabledModules.isEmpty()) setOf(ModuleType.ORDERS) else enabledModules
                            )
                            onSave(updated, adaptWorkflowStages && selectedBusinessType != workspace.businessType)
                        },
                        modifier = Modifier.testTag("btn_save_business_settings")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
