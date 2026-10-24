package com.example.bizflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.ModuleType
import com.example.bizflow.model.TaskStatus
import com.example.bizflow.ui.components.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: BizFlowRepository,
    onNavigateToWork: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToShopify: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onOpenBackendStatus: () -> Unit,
    onOpenNewOrderModal: () -> Unit,
    onOpenNewCustomerModal: () -> Unit,
    onOpenRecordPaymentModal: () -> Unit,
    onOpenNewExpenseModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val customers by repository.customers.collectAsState()
    val workRecords by repository.workRecords.collectAsState()
    val invoices by repository.invoices.collectAsState()
    val expenses by repository.expenses.collectAsState()
    val tasks by repository.tasks.collectAsState()
    val products by repository.products.collectAsState()
    val syncState by repository.syncState.collectAsState()
    val pendingSyncCount by repository.pendingSyncCount.collectAsState()

    var showWorkspaceDialog by remember { mutableStateOf(false) }
    var showFreeTierInfoDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    val totalPaidRevenue = invoices.sumOf { it.totalPaid }
    val totalReceivablesDue = invoices.sumOf { it.balanceDue }
    val totalExpensesAmount = expenses.sumOf { it.amount }
    val activeOrdersCount = workRecords.count { wr ->
        val stage = currentWorkspace.workflowStages.find { it.id == wr.currentStageId }
        stage?.isCompleted != true
    }
    val lowStockItems = products.filter { it.isLowStock }
    val pendingTasks = tasks.filter { it.status != TaskStatus.COMPLETED }

    val fullyPaidCount = invoices.count { it.status.name == "PAID" }
    val partialCount = invoices.count { it.balanceDue > 0.0 && it.totalPaid > 0.0 }
    val revenueSubtitle = when {
        fullyPaidCount > 0 && partialCount > 0 -> "$fullyPaidCount paid • $partialCount partial"
        fullyPaidCount > 0 -> "$fullyPaidCount fully paid"
        partialCount > 0 -> "$partialCount partial collected"
        totalPaidRevenue > 0.0 -> "${invoices.count { it.totalPaid > 0 }} payment recorded"
        else -> "0 collected"
    }

    val orderActionLabel = if (currentWorkspace.businessType.workRecordLabel.equals("Order", ignoreCase = true)) {
        "New Order"
    } else {
        "New ${currentWorkspace.businessType.workRecordLabel}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mivornexa",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    SyncStatusBadge(
                        syncState = syncState,
                        pendingCount = pendingSyncCount,
                        onSyncClick = { repository.triggerSync() }
                    )
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("dashboard_search_button")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Workspace Header
            item {
                WorkspaceHeaderCard(
                    workspace = currentWorkspace,
                    onSwitchClick = { showWorkspaceDialog = true },
                    onConnectionClick = onOpenBackendStatus
                )
            }

            // Quick Hubs: Shopify & Multi-Provider AI
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF14241C) else Color(0xFFF0FDF4)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isDark) Color(0xFF22543D) else Color(0xFF86EFAC)
                            ),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToShopify() }
                            .testTag("dashboard_shopify_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF96BF48))
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Shopify Hub",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                                )
                                Text(
                                    text = "Active sync",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF201633) else Color(0xFFFAF5FF)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isDark) Color(0xFF4C1D95) else Color(0xFFD8B4FE)
                            ),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToAi() }
                            .testTag("dashboard_ai_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C3AED))
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Automations",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF581C87)
                                )
                                Text(
                                    text = "Multi-provider",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA855F7) else Color(0xFF7E22CE)
                                )
                            }
                        }
                    }
                }
            }

            // 4-Year Free Subscription Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFreeTierInfoDialog = true }
                        .testTag("free_subscription_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.t("free_subscription"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Zero subscription fees until October 2030 • No paywall",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Actions Bar
            item {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickActionItem(
                                label = LanguageManager.t("quick_add_customer"),
                                icon = Icons.Default.PersonAdd,
                                color = Color(0xFF3B82F6),
                                onClick = onOpenNewCustomerModal,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionItem(
                                label = orderActionLabel,
                                icon = Icons.Default.Assignment,
                                color = Color(0xFF8B5CF6),
                                onClick = onOpenNewOrderModal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickActionItem(
                                label = LanguageManager.t("quick_record_payment"),
                                icon = Icons.Default.Payments,
                                color = Color(0xFF10B981),
                                onClick = onOpenRecordPaymentModal,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionItem(
                                label = LanguageManager.t("quick_add_expense"),
                                icon = Icons.Default.ReceiptLong,
                                color = Color(0xFFEF4444),
                                onClick = onOpenNewExpenseModal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Stat Cards Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = LanguageManager.t("stat_revenue"),
                            value = "${currentWorkspace.currencySymbol}${String.format(Locale.US, "%,.2f", totalPaidRevenue)}",
                            subtitle = revenueSubtitle,
                            icon = Icons.Default.TrendingUp,
                            iconTint = Color(0xFF10B981),
                            onClick = onNavigateToFinance,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = LanguageManager.t("stat_receivables"),
                            value = "${currentWorkspace.currencySymbol}${String.format(Locale.US, "%,.2f", totalReceivablesDue)}",
                            subtitle = "${invoices.count { it.balanceDue > 0.0 }} pending collection",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = Color(0xFFF59E0B),
                            onClick = onNavigateToFinance,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = "${currentWorkspace.businessType.workRecordPlural} Active",
                            value = "$activeOrdersCount",
                            subtitle = "${workRecords.size} total logged",
                            icon = Icons.Default.WorkOutline,
                            iconTint = Color(0xFF0284C7),
                            onClick = onNavigateToWork,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = LanguageManager.t("stat_expenses"),
                            value = "${currentWorkspace.currencySymbol}${String.format(Locale.US, "%,.2f", totalExpensesAmount)}",
                            subtitle = "${expenses.size} entries",
                            icon = Icons.Default.CreditCardOff,
                            iconTint = Color(0xFFEC4899),
                            onClick = onNavigateToFinance,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Low Stock Inventory Alert (if inventory enabled)
            if (currentWorkspace.enabledModules.contains(ModuleType.INVENTORY) && lowStockItems.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5)),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToInventory() }
                            .testTag("inventory_low_stock_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Inventory Stock Alert (${lowStockItems.size} items)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "${lowStockItems.first().name} has only ${lowStockItems.first().currentStock} ${lowStockItems.first().unitOfMeasure} left.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                            Text(
                                text = "Restock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // Recent Active Work Records Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recent ${currentWorkspace.businessType.workRecordPlural}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToWork) {
                        Text("View All")
                    }
                }
            }

            if (workRecords.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Assignment,
                        title = "No ${currentWorkspace.businessType.workRecordPlural} Yet",
                        message = "Start by creating a new ${currentWorkspace.businessType.workRecordLabel.lowercase()} for ${currentWorkspace.name}.",
                        actionLabel = "Create ${currentWorkspace.businessType.workRecordLabel}",
                        onAction = onOpenNewOrderModal
                    )
                }
            } else {
                items(workRecords.take(3)) { wr ->
                    val stage = currentWorkspace.workflowStages.find { it.id == wr.currentStageId }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWork() }
                            .testTag("dashboard_work_item_${wr.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "#${wr.recordNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                WorkStagePill(
                                    stageName = stage?.name ?: "Pending",
                                    colorHex = stage?.colorHex ?: "#3B82F6"
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = wr.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = wr.customerName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", wr.totalAmount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Team Tasks Snapshot
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Team Action Queue (${pendingTasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToTasks) {
                        Text("Manage Tasks")
                    }
                }
            }

            items(pendingTasks.take(2)) { task ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        IconButton(
                            onClick = { repository.toggleTaskStatus(task.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Complete task",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Assigned to: ${task.assignedToMemberName} • Priority: ${task.priority.displayName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWorkspaceDialog) {
        WorkspaceSelectorDialog(
            repository = repository,
            onDismiss = { showWorkspaceDialog = false }
        )
    }

    if (showFreeTierInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFreeTierInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "BizFlow 4-Year Free SaaS Tier",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "BizFlow provides completely unrestricted core software access for the first 4 years (Active until October 23, 2030).",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Zero monthly subscription lockouts\n• No hidden paywalls or gated CRUD records\n• Multi-workspace & multi-industry included\n• Native Android PDF generation included",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Notice: Third-party backend infrastructure (e.g. Firebase Cloud Firestore database quota or Google Cloud SMS/Email dispatching limits) is subject to provider tiers.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFreeTierInfoDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
fun QuickActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = if (isDark) 0.20f else 0.12f),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("quick_action_${label.take(10).lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) color.copy(alpha = 0.95f) else color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
