package com.example.bizflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.ItemType
import com.example.bizflow.model.ModuleType
import com.example.bizflow.model.ProductItem
import com.example.bizflow.ui.components.EmptyStateView
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    repository: BizFlowRepository,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val products by repository.products.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedItemTypeFilter by remember { mutableStateOf<ItemType?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAdjustStockDialog by remember { mutableStateOf<ProductItem?>(null) }

    val isInventoryEnabled = currentWorkspace.enabledModules.contains(ModuleType.INVENTORY)

    val filteredProducts = products.filter { p ->
        val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedItemTypeFilter == null || p.type == selectedItemTypeFilter
        matchesSearch && matchesType
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Catalog & Inventory", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isInventoryEnabled) "${products.size} catalog items • Stock Tracking Active"
                            else "Service Catalog (Inventory Module Disabled)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddProductDialog = true },
                        modifier = Modifier.testTag("add_product_appbar_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_item_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Item")
            }
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
                placeholder = { Text("Search product name, SKU, category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                FilterChip(
                    selected = selectedItemTypeFilter == null,
                    onClick = { selectedItemTypeFilter = null },
                    label = { Text("All (${products.size})") }
                )
                FilterChip(
                    selected = selectedItemTypeFilter == ItemType.PRODUCT,
                    onClick = { selectedItemTypeFilter = if (selectedItemTypeFilter == ItemType.PRODUCT) null else ItemType.PRODUCT },
                    label = { Text("Products") }
                )
                FilterChip(
                    selected = selectedItemTypeFilter == ItemType.SERVICE,
                    onClick = { selectedItemTypeFilter = if (selectedItemTypeFilter == ItemType.SERVICE) null else ItemType.SERVICE },
                    label = { Text("Services") }
                )
            }

            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "No Catalog Items",
                    message = "Add products or billable services to your workspace catalog.",
                    actionLabel = "Add Item",
                    onAction = { showAddProductDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("product_item_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (item.type == ItemType.PRODUCT) Color(0xFFE0F2FE) else Color(0xFFF3E8FF)
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (item.type == ItemType.PRODUCT) Icons.Default.Inventory2 else Icons.Default.MiscellaneousServices,
                                        contentDescription = null,
                                        tint = if (item.type == ItemType.PRODUCT) Color(0xFF0284C7) else Color(0xFF9333EA),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${item.category} • SKU: ${item.sku.ifBlank { "N/A" }}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Price: ${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", item.unitPrice)}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (item.type == ItemType.PRODUCT && isInventoryEnabled) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (item.isLowStock) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = "${item.currentStock.toInt()} ${item.unitOfMeasure}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (item.isLowStock) Color(0xFF991B1B) else Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TextButton(
                                            onClick = { showAdjustStockDialog = item },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Adjust", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        AddProductDialog(
            workspaceId = currentWorkspace.id,
            currencySymbol = currentWorkspace.currencySymbol,
            onDismiss = { showAddProductDialog = false },
            onSave = { newItem ->
                repository.saveProduct(newItem)
                showAddProductDialog = false
            }
        )
    }

    // Adjust Stock Dialog
    showAdjustStockDialog?.let { prod ->
        AdjustStockDialog(
            product = prod,
            onDismiss = { showAdjustStockDialog = null },
            onAdjust = { qtyChange, reason ->
                repository.adjustStock(prod.id, qtyChange, reason)
                showAdjustStockDialog = null
            }
        )
    }
}

@Composable
fun AddProductDialog(
    workspaceId: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (ProductItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var priceText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("10") }
    var selectedType by remember { mutableStateOf(ItemType.PRODUCT) }
    var supplier by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Catalog Item", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == ItemType.PRODUCT,
                            onClick = { selectedType = ItemType.PRODUCT },
                            label = { Text("Physical Product") }
                        )
                        FilterChip(
                            selected = selectedType == ItemType.SERVICE,
                            onClick = { selectedType = ItemType.SERVICE },
                            label = { Text("Service / Labor") }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU / Code") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Selling Price ($currencySymbol) *") },
                            modifier = Modifier.weight(1f).testTag("product_price_input")
                        )
                        OutlinedTextField(
                            value = costText,
                            onValueChange = { costText = it },
                            label = { Text("Cost Price") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedType == ItemType.PRODUCT) {
                    item {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("Initial Stock Count") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = supplier,
                            onValueChange = { supplier = it },
                            label = { Text("Supplier Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val c = costText.toDoubleOrNull() ?: 0.0
                    val s = stockText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        val item = ProductItem(
                            workspaceId = workspaceId,
                            name = name.trim(),
                            sku = sku.trim(),
                            type = selectedType,
                            category = category.trim().ifEmpty { "General" },
                            unitPrice = p,
                            costPrice = c,
                            currentStock = if (selectedType == ItemType.PRODUCT) s else 999.0,
                            supplierName = supplier.trim()
                        )
                        onSave(item)
                    }
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: -1.0) >= 0.0,
                modifier = Modifier.testTag("submit_product_button")
            ) {
                Text("Add to Catalog")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdjustStockDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onAdjust: (Double, String) -> Unit
) {
    var changeText by remember { mutableStateOf("5") }
    var isAddition by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("Restock shipment received") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock: ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Stock: ${product.currentStock.toInt()} ${product.unitOfMeasure}", fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAddition,
                        onClick = { isAddition = true },
                        label = { Text("+ Add / Restock") }
                    )
                    FilterChip(
                        selected = !isAddition,
                        onClick = { isAddition = false },
                        label = { Text("- Deduct / Damage") }
                    )
                }

                OutlinedTextField(
                    value = changeText,
                    onValueChange = { changeText = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Audit Reason / Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawQty = changeText.toDoubleOrNull() ?: 0.0
                    val finalChange = if (isAddition) rawQty else -rawQty
                    onAdjust(finalChange, reason.trim())
                }
            ) {
                Text("Save Adjustment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
