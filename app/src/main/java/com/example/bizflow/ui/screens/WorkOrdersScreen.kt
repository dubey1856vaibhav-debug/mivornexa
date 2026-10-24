package com.example.bizflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.*
import com.example.bizflow.ui.components.EmptyStateView
import com.example.bizflow.ui.components.WorkStagePill
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrdersScreen(
    repository: BizFlowRepository,
    onOpenEmailPreview: (WorkRecord, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val workRecords by repository.workRecords.collectAsState()
    val customers by repository.customers.collectAsState()

    var selectedStageFilterId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRecordForDetail by remember { mutableStateOf<WorkRecord?>(null) }

    val filteredRecords = workRecords.filter { wr ->
        val matchesStage = selectedStageFilterId == null || wr.currentStageId == selectedStageFilterId
        val matchesSearch = searchQuery.isBlank() ||
                wr.recordNumber.contains(searchQuery, ignoreCase = true) ||
                wr.title.contains(searchQuery, ignoreCase = true) ||
                wr.customerName.contains(searchQuery, ignoreCase = true) ||
                wr.assignedStaffName.contains(searchQuery, ignoreCase = true)
        matchesStage && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentWorkspace.businessType.workRecordPlural,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredRecords.size} records • ${currentWorkspace.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("new_work_order_appbar_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Order")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("create_order_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by #number, title, customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Dynamic Workflow Stage Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStageFilterId == null,
                        onClick = { selectedStageFilterId = null },
                        label = { Text("All Stages (${workRecords.size})") }
                    )
                }
                items(currentWorkspace.workflowStages) { stage ->
                    val count = workRecords.count { it.currentStageId == stage.id }
                    FilterChip(
                        selected = selectedStageFilterId == stage.id,
                        onClick = {
                            selectedStageFilterId = if (selectedStageFilterId == stage.id) null else stage.id
                        },
                        label = { Text("${stage.name} ($count)") }
                    )
                }
            }

            if (filteredRecords.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.WorkOutline,
                    title = "No ${currentWorkspace.businessType.workRecordPlural} Found",
                    message = if (searchQuery.isNotBlank()) "No records match '$searchQuery'."
                    else "Create your first ${currentWorkspace.businessType.workRecordLabel.lowercase()} for ${currentWorkspace.name}.",
                    actionLabel = "Create ${currentWorkspace.businessType.workRecordLabel}",
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredRecords) { wr ->
                        val stage = currentWorkspace.workflowStages.find { it.id == wr.currentStageId }
                        WorkRecordCard(
                            record = wr,
                            stage = stage,
                            workspace = currentWorkspace,
                            onClick = { selectedRecordForDetail = wr },
                            onQuickStageChange = { nextStageId ->
                                repository.updateWorkStage(wr.id, nextStageId)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        WorkRecordFormDialog(
            workspace = currentWorkspace,
            customers = customers,
            onDismiss = { showCreateDialog = false },
            onSave = { newRecord ->
                repository.saveWorkRecord(newRecord)
                showCreateDialog = false
            }
        )
    }

    selectedRecordForDetail?.let { wr ->
        val stage = currentWorkspace.workflowStages.find { it.id == wr.currentStageId }
        WorkRecordDetailDialog(
            record = wr,
            stage = stage,
            workspace = currentWorkspace,
            stages = currentWorkspace.workflowStages,
            onDismiss = { selectedRecordForDetail = null },
            onStageChange = { newStageId ->
                repository.updateWorkStage(wr.id, newStageId)
                selectedRecordForDetail = wr.copy(currentStageId = newStageId)
            },
            onSendStatusEmail = {
                selectedRecordForDetail = null
                onOpenEmailPreview(wr, stage?.name ?: "In Progress")
            }
        )
    }
}

@Composable
fun WorkRecordCard(
    record: WorkRecord,
    stage: WorkStage?,
    workspace: Workspace,
    onClick: () -> Unit,
    onQuickStageChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("work_record_card_${record.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "#${record.recordNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                WorkStagePill(
                    stageName = stage?.name ?: "Pending",
                    colorHex = stage?.colorHex ?: "#3B82F6"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Customer: ${record.customerName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Assigned: ${record.assignedStaffName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Custom fields preview (e.g. measurements, device model)
            if (record.customFieldValues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        record.customFieldValues.entries.take(2).forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Total: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", record.totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (record.balanceDue > 0.0) {
                        Text(
                            text = "Balance: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", record.balanceDue)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    } else {
                        Text(
                            text = "Paid in Full",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Advance to next stage action button
                val currentIndex = workspace.workflowStages.indexOfFirst { it.id == record.currentStageId }
                if (currentIndex >= 0 && currentIndex < workspace.workflowStages.size - 1) {
                    val nextStage = workspace.workflowStages[currentIndex + 1]
                    FilledTonalButton(
                        onClick = { onQuickStageChange(nextStage.id) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Next: ${nextStage.name}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRecordFormDialog(
    workspace: Workspace,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (WorkRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var recordNumber by remember { mutableStateOf("${workspace.businessType.workRecordLabel.take(3).uppercase()}-${Random().nextInt(8999) + 1000}") }
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var customerExpanded by remember { mutableStateOf(false) }
    var assignedStaff by remember { mutableStateOf("Alex Rivera") }
    var notes by remember { mutableStateOf("") }

    // Line items list
    var itemDesc by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("0") }
    var lineItems by remember { mutableStateOf(listOf<LineItem>()) }

    // Advance paid
    var advancePaidText by remember { mutableStateOf("0") }

    // Custom fields map based on workspace
    val customFieldsMap = remember {
        mutableStateMapOf<String, String>().apply {
            workspace.customFields.forEach { put(it.label, "") }
        }
    }

    val subtotal = lineItems.sumOf { it.totalAmount }
    val advance = advancePaidText.toDoubleOrNull() ?: 0.0
    val balanceDue = (subtotal - advance).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New ${workspace.businessType.workRecordLabel}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("${workspace.businessType.workRecordLabel} Title / Description *") },
                        modifier = Modifier.fillMaxWidth().testTag("order_input_title")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = recordNumber,
                            onValueChange = { recordNumber = it },
                            label = { Text("Record #") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = assignedStaff,
                            onValueChange = { assignedStaff = it },
                            label = { Text("Assignee") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Customer selection dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = customerExpanded,
                        onExpandedChange = { customerExpanded = !customerExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Select Customer *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Client / Customer *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = customerExpanded,
                            onDismissRequest = { customerExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = { Text("${cust.name} (${cust.phone})") },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Workspace-specific custom fields
                if (workspace.customFields.isNotEmpty()) {
                    item {
                        Text(
                            text = "${workspace.businessType.displayName} Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    workspace.customFields.forEach { fieldDef ->
                        item {
                            OutlinedTextField(
                                value = customFieldsMap[fieldDef.label] ?: "",
                                onValueChange = { customFieldsMap[fieldDef.label] = it },
                                label = { Text(fieldDef.label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Line items section
                item {
                    Text("Products / Services / Line Items", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                items(lineItems) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("${item.description} (${item.quantity}x @ ${workspace.currencySymbol}${item.unitPrice})", fontSize = 11.sp)
                            Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", item.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = itemDesc,
                            onValueChange = { itemDesc = it },
                            label = { Text("Item / Service") },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = itemPrice,
                            onValueChange = { itemPrice = it },
                            label = { Text("Price") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val price = itemPrice.toDoubleOrNull() ?: 0.0
                                val qty = itemQty.toDoubleOrNull() ?: 1.0
                                if (itemDesc.isNotBlank()) {
                                    lineItems = lineItems + LineItem(description = itemDesc.trim(), quantity = qty, unitPrice = price)
                                    itemDesc = ""
                                    itemPrice = "0"
                                }
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subtotal:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", subtotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                item {
                    OutlinedTextField(
                        value = advancePaidText,
                        onValueChange = { advancePaidText = it },
                        label = { Text("Advance Deposit Received (${workspace.currencySymbol})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Balance Due:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", balanceDue)}",
                            fontWeight = FontWeight.Bold,
                            color = if (balanceDue > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 13.sp
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes & Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val finalItems = if (lineItems.isEmpty()) {
                        listOf(LineItem(description = title.trim(), quantity = 1.0, unitPrice = subtotal.coerceAtLeast(advance)))
                    } else lineItems

                    val finalTotal = finalItems.sumOf { it.totalAmount }
                    val finalBalance = (finalTotal - advance).coerceAtLeast(0.0)

                    val newRecord = WorkRecord(
                        workspaceId = workspace.id,
                        recordNumber = recordNumber.trim(),
                        customerId = cust.id,
                        customerName = cust.name,
                        customerPhone = cust.phone,
                        title = title.trim(),
                        currentStageId = workspace.workflowStages.firstOrNull()?.id ?: "stage-1",
                        assignedStaffName = assignedStaff.trim(),
                        items = finalItems,
                        customFieldValues = customFieldsMap.toMap(),
                        notes = notes.trim(),
                        totalAmount = finalTotal,
                        advancePaid = advance,
                        balanceDue = finalBalance
                    )
                    onSave(newRecord)
                },
                enabled = title.isNotBlank() && selectedCustomer != null,
                modifier = Modifier.testTag("submit_order_button")
            ) {
                Text("Save ${workspace.businessType.workRecordLabel}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WorkRecordDetailDialog(
    record: WorkRecord,
    stage: WorkStage?,
    workspace: Workspace,
    stages: List<WorkStage>,
    onDismiss: () -> Unit,
    onStageChange: (String) -> Unit,
    onSendStatusEmail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("#${record.recordNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(record.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Current Stage / Workflow Status", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stages) { st ->
                            FilterChip(
                                selected = record.currentStageId == st.id,
                                onClick = { onStageChange(st.id) },
                                label = { Text(st.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Customer: ${record.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Assigned Staff: ${record.assignedStaffName}", fontSize = 12.sp)
                            if (record.notes.isNotEmpty()) {
                                Text("Notes: ${record.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (record.customFieldValues.isNotEmpty()) {
                    item {
                        Text("${workspace.businessType.displayName} Custom Fields", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    record.customFieldValues.forEach { (k, v) ->
                        if (v.isNotEmpty()) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(k, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(v, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                item {
                    Divider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold)
                        Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", record.totalAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Advance Paid:")
                        Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", record.advancePaid)}")
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Balance Due:", fontWeight = FontWeight.Bold)
                        Text(
                            "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", record.balanceDue)}",
                            fontWeight = FontWeight.Bold,
                            color = if (record.balanceDue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onSendStatusEmail
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Notify Customer Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
