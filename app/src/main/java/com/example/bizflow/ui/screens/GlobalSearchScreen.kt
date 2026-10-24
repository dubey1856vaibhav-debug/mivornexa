package com.example.bizflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.ui.components.EmptyStateView
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    repository: BizFlowRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val customers by repository.customers.collectAsState()
    val workRecords by repository.workRecords.collectAsState()
    val invoices by repository.invoices.collectAsState()
    val products by repository.products.collectAsState()
    val tasks by repository.tasks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val matchedCustomers = remember(searchQuery, customers) {
        if (searchQuery.isBlank()) emptyList()
        else customers.filter { it.name.contains(searchQuery, true) || it.phone.contains(searchQuery, true) || it.email.contains(searchQuery, true) }
    }

    val matchedWork = remember(searchQuery, workRecords) {
        if (searchQuery.isBlank()) emptyList()
        else workRecords.filter { it.title.contains(searchQuery, true) || it.recordNumber.contains(searchQuery, true) || it.customerName.contains(searchQuery, true) }
    }

    val matchedInvoices = remember(searchQuery, invoices) {
        if (searchQuery.isBlank()) emptyList()
        else invoices.filter { it.invoiceNumber.contains(searchQuery, true) || it.customerName.contains(searchQuery, true) }
    }

    val matchedProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) emptyList()
        else products.filter { it.name.contains(searchQuery, true) || it.sku.contains(searchQuery, true) }
    }

    val totalMatches = matchedCustomers.size + matchedWork.size + matchedInvoices.size + matchedProducts.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search clients, orders, bills, products...") },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("global_search_input")
            )

            if (searchQuery.isBlank()) {
                EmptyStateView(
                    icon = Icons.Default.ManageSearch,
                    title = "Find Anything Instantly",
                    message = "Type a client name, invoice number, order code, or product SKU.",
                    modifier = Modifier.weight(1f)
                )
            } else if (totalMatches == 0) {
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = "No Matches Found",
                    message = "No records in ${currentWorkspace.name} match '$searchQuery'.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (matchedCustomers.isNotEmpty()) {
                        item { Text("Customers (${matchedCustomers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        items(matchedCustomers) { c ->
                            SearchItemCard(
                                icon = Icons.Default.Person,
                                title = c.name,
                                subtitle = "${c.phone} • ${c.email}",
                                tag = "Client"
                            )
                        }
                    }

                    if (matchedWork.isNotEmpty()) {
                        item { Text("${currentWorkspace.businessType.workRecordPlural} (${matchedWork.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        items(matchedWork) { wr ->
                            SearchItemCard(
                                icon = Icons.Default.WorkOutline,
                                title = "#${wr.recordNumber} - ${wr.title}",
                                subtitle = "Client: ${wr.customerName} • ${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", wr.totalAmount)}",
                                tag = currentWorkspace.businessType.workRecordLabel
                            )
                        }
                    }

                    if (matchedInvoices.isNotEmpty()) {
                        item { Text("Invoices & Billing (${matchedInvoices.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        items(matchedInvoices) { inv ->
                            SearchItemCard(
                                icon = Icons.Default.Receipt,
                                title = "${inv.invoiceNumber} • ${inv.customerName}",
                                subtitle = "Status: ${inv.status.displayName} • Total: ${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", inv.totalAmount)}",
                                tag = "Invoice"
                            )
                        }
                    }

                    if (matchedProducts.isNotEmpty()) {
                        item { Text("Products & Catalog (${matchedProducts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        items(matchedProducts) { p ->
                            SearchItemCard(
                                icon = Icons.Default.Inventory2,
                                title = p.name,
                                subtitle = "SKU: ${p.sku} • Price: ${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", p.unitPrice)}",
                                tag = p.category
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tag: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(text = tag, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
