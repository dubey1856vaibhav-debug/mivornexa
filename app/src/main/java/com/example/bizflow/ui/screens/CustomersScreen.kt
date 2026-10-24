package com.example.bizflow.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.Customer
import com.example.bizflow.ui.components.EmptyStateView
import com.example.bizflow.ui.components.LanguageManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    repository: BizFlowRepository,
    onNavigateBack: () -> Unit,
    onStartOrderForCustomer: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val customers by repository.customers.collectAsState()
    val workRecords by repository.workRecords.collectAsState()
    val invoices by repository.invoices.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var selectedCustomerForDetail by remember { mutableStateOf<Customer?>(null) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val allTags = remember(customers) {
        customers.flatMap { it.tags }.distinct()
    }

    val filteredCustomers = customers.filter { cust ->
        val matchesQuery = searchQuery.isBlank() ||
                cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true) ||
                cust.email.contains(searchQuery, ignoreCase = true) ||
                cust.companyName.contains(searchQuery, ignoreCase = true)
        val matchesTag = selectedTagFilter == null || cust.tags.contains(selectedTagFilter)
        matchesQuery && matchesTag
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${LanguageManager.t("nav_customers")} (${customers.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAddCustomerDialog = true },
                        modifier = Modifier.testTag("add_customer_appbar_button")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Customer")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_customer_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
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
                placeholder = { Text("Search by name, phone, or email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("customer_search_input")
            )

            // Tags filter row
            if (allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTagFilter == null,
                            onClick = { selectedTagFilter = null },
                            label = { Text("All") }
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTagFilter == tag,
                            onClick = { selectedTagFilter = if (selectedTagFilter == tag) null else tag },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PeopleOutline,
                    title = "No Customers Found",
                    message = if (searchQuery.isNotBlank()) "No records match '$searchQuery'."
                    else "Add your first client to start logging orders and invoices.",
                    actionLabel = "Add Customer",
                    onAction = { showAddCustomerDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredCustomers) { customer ->
                        val customerOrders = workRecords.filter { it.customerId == customer.id }
                        val customerInvoices = invoices.filter { it.customerId == customer.id }
                        val outstandingDue = customerInvoices.sumOf { it.balanceDue }

                        CustomerListItem(
                            customer = customer,
                            currencySymbol = currentWorkspace.currencySymbol,
                            ordersCount = customerOrders.size,
                            outstandingBalance = outstandingDue,
                            onClick = { selectedCustomerForDetail = customer }
                        )
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        CustomerEditDialog(
            workspaceId = currentWorkspace.id,
            repository = repository,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { newCust ->
                repository.saveCustomer(newCust)
                showAddCustomerDialog = false
            }
        )
    }

    // Customer Detail Modal
    selectedCustomerForDetail?.let { cust ->
        val custOrders = workRecords.filter { it.customerId == cust.id }
        val custInvoices = invoices.filter { it.customerId == cust.id }
        val outstandingDue = custInvoices.sumOf { it.balanceDue }

        CustomerDetailDialog(
            customer = cust,
            workspace = currentWorkspace,
            orders = custOrders,
            invoices = custInvoices,
            outstandingDue = outstandingDue,
            onDismiss = { selectedCustomerForDetail = null },
            onCreateOrder = {
                selectedCustomerForDetail = null
                onStartOrderForCustomer(cust)
            },
            onDelete = {
                repository.deleteCustomer(cust.id)
                selectedCustomerForDetail = null
            }
        )
    }
}

@Composable
fun CustomerListItem(
    customer: Customer,
    currencySymbol: String,
    ordersCount: Int,
    outstandingBalance: Double,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_item_${customer.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = customer.phone.ifBlank { customer.email },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (customer.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        customer.tags.take(2).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$ordersCount orders",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (outstandingBalance > 0.0) {
                    Text(
                        text = "Due: $currencySymbol${String.format(Locale.US, "%.2f", outstandingBalance)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                } else {
                    Text(
                        text = "Settled",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerEditDialog(
    workspaceId: String,
    repository: BizFlowRepository,
    customerToEdit: Customer? = null,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(customerToEdit?.phone ?: "") }
    var email by remember { mutableStateOf(customerToEdit?.email ?: "") }
    var address by remember { mutableStateOf(customerToEdit?.address ?: "") }
    var tagsInput by remember { mutableStateOf(customerToEdit?.tags?.joinToString(", ") ?: "") }
    var notes by remember { mutableStateOf(customerToEdit?.notes ?: "") }

    // Duplicate check reactive state
    val potentialDuplicates = remember(name, phone, email) {
        if (name.length > 2 || phone.length > 4 || email.length > 4) {
            repository.findPossibleDuplicateCustomers(name, phone, email, customerToEdit?.id)
        } else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customerToEdit == null) "New Customer" else "Edit Customer", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Duplicate warning banner if detected
                if (potentialDuplicates.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("duplicate_warning_banner")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = LanguageManager.t("duplicate_warning"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = "Matches existing customer: ${potentialDuplicates.first().name} (${potentialDuplicates.first().phone})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("customer_input_name")
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("customer_input_phone")
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("customer_input_email")
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Physical Address / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags (comma separated: VIP, Wholesale...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Customer Notes & Preferences") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val cust = customerToEdit?.copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            tags = tagsList,
                            notes = notes.trim()
                        ) ?: Customer(
                            workspaceId = workspaceId,
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            tags = tagsList,
                            notes = notes.trim()
                        )
                        onSave(cust)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_customer_submit_button")
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CustomerDetailDialog(
    customer: Customer,
    workspace: com.example.bizflow.model.Workspace,
    orders: List<com.example.bizflow.model.WorkRecord>,
    invoices: List<com.example.bizflow.model.Invoice>,
    outstandingDue: Double,
    onDismiss: () -> Unit,
    onCreateOrder: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(customer.name, fontWeight = FontWeight.Bold, maxLines = 1)
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (customer.phone.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp)
                            }
                        }
                        if (customer.email.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${customer.email}"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email", fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Contact & Info", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Phone: ${customer.phone.ifBlank { "N/A" }}", fontSize = 12.sp)
                            Text("Email: ${customer.email.ifBlank { "N/A" }}", fontSize = 12.sp)
                            if (customer.address.isNotEmpty()) {
                                Text("Address: ${customer.address}", fontSize = 12.sp)
                            }
                            if (customer.notes.isNotEmpty()) {
                                Text("Notes: ${customer.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Outstanding Balance:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", outstandingDue)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (outstandingDue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }

                item {
                    Text("${workspace.businessType.workRecordPlural} (${orders.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                items(orders) { ord ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("#${ord.recordNumber} - ${ord.title}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", ord.totalAmount)}", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreateOrder) {
                Text("New ${workspace.businessType.workRecordLabel}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
