package com.example.bizflow.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.data.PdfGenerator
import com.example.bizflow.model.*
import com.example.bizflow.ui.components.EmptyStateView
import com.example.bizflow.ui.components.LanguageManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    repository: BizFlowRepository,
    onOpenEmailPreviewForInvoice: (Invoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val invoices by repository.invoices.collectAsState()
    val expenses by repository.expenses.collectAsState()
    val customers by repository.customers.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Invoices", "Expenses", "Summary")

    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf<Invoice?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }

    val totalPaid = invoices.sumOf { it.totalPaid }
    val totalBilled = invoices.sumOf { it.totalAmount }
    val totalReceivable = invoices.sumOf { it.balanceDue }
    val totalExpenses = expenses.sumOf { it.amount }
    val netCashFlow = totalPaid - totalExpenses

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = LanguageManager.t("nav_finance"), fontWeight = FontWeight.Bold)
                        Text(
                            text = "${currentWorkspace.currencyCode} (${currentWorkspace.currencySymbol}) • ${currentWorkspace.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selectedTabIndex == 0) {
                        IconButton(
                            onClick = { showCreateInvoiceDialog = true },
                            modifier = Modifier.testTag("new_invoice_appbar_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Invoice")
                        }
                    } else if (selectedTabIndex == 1) {
                        IconButton(
                            onClick = { showAddExpenseDialog = true },
                            modifier = Modifier.testTag("new_expense_appbar_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Expense")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 1) showAddExpenseDialog = true
                    else showCreateInvoiceDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("finance_primary_fab")
            ) {
                Icon(
                    imageVector = if (selectedTabIndex == 1) Icons.Default.ReceiptLong else Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> InvoicesTabContent(
                    invoices = invoices,
                    workspace = currentWorkspace,
                    onInvoiceClick = { selectedInvoiceForDetail = it },
                    onRecordPayment = { showRecordPaymentDialog = it },
                    onSharePdf = { inv ->
                        try {
                            val pdfFile = PdfGenerator.generateInvoicePdf(context, inv, currentWorkspace)
                            PdfGenerator.sharePdf(context, pdfFile)
                        } catch (e: Exception) {
                            Toast.makeText(context, "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCreateInvoice = { showCreateInvoiceDialog = true }
                )
                1 -> ExpensesTabContent(
                    expenses = expenses,
                    workspace = currentWorkspace,
                    onDeleteExpense = { repository.deleteExpense(it) },
                    onAddExpense = { showAddExpenseDialog = true }
                )
                2 -> FinancialSummaryTabContent(
                    totalBilled = totalBilled,
                    totalPaid = totalPaid,
                    totalReceivable = totalReceivable,
                    totalExpenses = totalExpenses,
                    netCashFlow = netCashFlow,
                    workspace = currentWorkspace,
                    invoicesCount = invoices.size,
                    expensesCount = expenses.size
                )
            }
        }
    }

    // Create Invoice Dialog
    if (showCreateInvoiceDialog) {
        CreateInvoiceDialog(
            workspace = currentWorkspace,
            customers = customers,
            onDismiss = { showCreateInvoiceDialog = false },
            onSave = { newInv ->
                repository.saveInvoice(newInv)
                showCreateInvoiceDialog = false
            }
        )
    }

    // Record Payment Dialog
    showRecordPaymentDialog?.let { invoice ->
        RecordPaymentDialog(
            invoice = invoice,
            currencySymbol = currentWorkspace.currencySymbol,
            onDismiss = { showRecordPaymentDialog = null },
            onSave = { payment ->
                repository.recordPayment(invoice.id, payment)
                showRecordPaymentDialog = null
                Toast.makeText(context, "Payment of ${currentWorkspace.currencySymbol}${payment.amount} recorded!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Invoice Detail Dialog
    selectedInvoiceForDetail?.let { inv ->
        InvoiceDetailDialog(
            invoice = inv,
            workspace = currentWorkspace,
            onDismiss = { selectedInvoiceForDetail = null },
            onRecordPayment = {
                selectedInvoiceForDetail = null
                showRecordPaymentDialog = inv
            },
            onSharePdf = {
                val pdfFile = PdfGenerator.generateInvoicePdf(context, inv, currentWorkspace)
                PdfGenerator.sharePdf(context, pdfFile)
            },
            onSendEmail = {
                selectedInvoiceForDetail = null
                onOpenEmailPreviewForInvoice(inv)
            }
        )
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            workspaceId = currentWorkspace.id,
            currencySymbol = currentWorkspace.currencySymbol,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { exp ->
                repository.saveExpense(exp)
                showAddExpenseDialog = false
            }
        )
    }
}

@Composable
fun InvoicesTabContent(
    invoices: List<Invoice>,
    workspace: Workspace,
    onInvoiceClick: (Invoice) -> Unit,
    onRecordPayment: (Invoice) -> Unit,
    onSharePdf: (Invoice) -> Unit,
    onCreateInvoice: () -> Unit
) {
    if (invoices.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Receipt,
            title = "No Invoices Logged",
            message = "Generate your first customer invoice, estimate, or receipt.",
            actionLabel = "Create Invoice",
            onAction = onCreateInvoice
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(invoices) { inv ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInvoiceClick(inv) }
                        .testTag("invoice_card_${inv.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = inv.invoiceNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (inv.status) {
                                    InvoiceStatus.PAID -> Color(0xFFD1FAE5)
                                    InvoiceStatus.PARTIALLY_PAID -> Color(0xFFFEF3C7)
                                    InvoiceStatus.OVERDUE -> Color(0xFFFEE2E2)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = inv.status.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (inv.status) {
                                        InvoiceStatus.PAID -> Color(0xFF065F46)
                                        InvoiceStatus.PARTIALLY_PAID -> Color(0xFF92400E)
                                        InvoiceStatus.OVERDUE -> Color(0xFF991B1B)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = inv.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Total: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", inv.totalAmount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Paid: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", inv.totalPaid)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981)
                                )
                                if (inv.balanceDue > 0.0) {
                                    Text(
                                        text = "Due: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", inv.balanceDue)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { onSharePdf(inv) }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                                }
                                if (inv.balanceDue > 0.0) {
                                    FilledTonalButton(
                                        onClick = { onRecordPayment(inv) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("+ Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun ExpensesTabContent(
    expenses: List<Expense>,
    workspace: Workspace,
    onDeleteExpense: (String) -> Unit,
    onAddExpense: () -> Unit
) {
    if (expenses.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.ReceiptLong,
            title = "No Expenses Recorded",
            message = "Track operations, material purchases, rent, utilities, and vendor payouts.",
            actionLabel = "Add Expense",
            onAction = onAddExpense
        )
    } else {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(expenses) { exp ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = exp.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "${exp.category.displayName} • Paid to: ${exp.paidTo}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${sdf.format(Date(exp.date))} • ${exp.paymentMethod.displayName}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-${workspace.currencySymbol}${String.format(Locale.US, "%.2f", exp.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626)
                            )
                            IconButton(
                                onClick = { onDeleteExpense(exp.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialSummaryTabContent(
    totalBilled: Double,
    totalPaid: Double,
    totalReceivable: Double,
    totalExpenses: Double,
    netCashFlow: Double,
    workspace: Workspace,
    invoicesCount: Int,
    expensesCount: Int
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (netCashFlow >= 0) Color(0xFF0F172A) else Color(0xFF450A0A)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Net Cash Flow (Received - Expenses)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${workspace.currencySymbol}${String.format(Locale.US, "%,.2f", netCashFlow)}",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (netCashFlow >= 0) "Healthy operating margin" else "Expenses exceed cash received",
                        color = if (netCashFlow >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Text("Financial Ledger Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinancialRow("Total Invoiced Amount", "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", totalBilled)}", Color(0xFF0284C7))
                    FinancialRow("Actual Cash Collected", "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", totalPaid)}", Color(0xFF10B981))
                    FinancialRow("Pending Receivables Due", "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", totalReceivable)}", Color(0xFFF59E0B))
                    Divider()
                    FinancialRow("Total Business Outflows / Expenses", "-${workspace.currencySymbol}${String.format(Locale.US, "%.2f", totalExpenses)}", Color(0xFFEF4444))
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Audit & Calculation Integrity", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Invoiced sales are recorded separately from real cash movements.\n• Partial advances directly update receivable balances.\n• Idempotent transaction IDs prevent double payment submissions.\n• Tax rate applies uniformly: ${workspace.taxRatePercent}%.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialRow(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceDialog(
    workspace: Workspace,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (Invoice) -> Unit
) {
    var invoiceNumber by remember { mutableStateOf("INV-${Calendar.getInstance().get(Calendar.YEAR)}-${Random().nextInt(899) + 100}") }
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var customerExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(InvoiceType.INVOICE) }

    var itemDesc by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("0") }
    var lineItems by remember {
        mutableStateOf(listOf(LineItem(description = "Business Services / Products", quantity = 1.0, unitPrice = 250.0)))
    }

    val subtotal = lineItems.sumOf { it.totalAmount }
    val taxRate = workspace.taxRatePercent
    val taxAmount = subtotal * (taxRate / 100.0)
    val total = subtotal + taxAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate ${selectedType.displayName}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InvoiceType.values().forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t.displayName.take(10), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Document #") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = customerExpanded,
                        onExpandedChange = { customerExpanded = !customerExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Select Client *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Billed To *") },
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

                item {
                    Text("Line Items", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Text(item.description, fontSize = 11.sp)
                            Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", item.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = itemDesc,
                            onValueChange = { itemDesc = it },
                            label = { Text("Item Name") },
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
                                val p = itemPrice.toDoubleOrNull() ?: 0.0
                                if (itemDesc.isNotBlank()) {
                                    lineItems = lineItems + LineItem(description = itemDesc.trim(), quantity = 1.0, unitPrice = p)
                                    itemDesc = ""
                                    itemPrice = "0"
                                }
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Divider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Subtotal:")
                        Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", subtotal)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Tax (${taxRate}%):")
                        Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", taxAmount)}")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold)
                        Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", total)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val inv = Invoice(
                        workspaceId = workspace.id,
                        invoiceNumber = invoiceNumber.trim(),
                        customerId = cust.id,
                        customerName = cust.name,
                        customerEmail = cust.email,
                        customerAddress = cust.address,
                        type = selectedType,
                        status = InvoiceStatus.DRAFT,
                        items = lineItems,
                        subtotal = subtotal,
                        taxRatePercent = taxRate,
                        taxAmount = taxAmount,
                        totalAmount = total,
                        totalPaid = 0.0,
                        balanceDue = total
                    )
                    onSave(inv)
                },
                enabled = selectedCustomer != null && lineItems.isNotEmpty()
            ) {
                Text("Save Document")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordPaymentDialog(
    invoice: Invoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Payment) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", invoice.balanceDue)) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    var referenceNumber by remember { mutableStateOf("REF-${Random().nextInt(899999) + 100000}") }
    var notes by remember { mutableStateOf("Payment applied to ${invoice.invoiceNumber}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Customer Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Invoice: #${invoice.invoiceNumber} • ${invoice.customerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Remaining Balance: $currencySymbol${String.format(Locale.US, "%.2f", invoice.balanceDue)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($currencySymbol) *") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text("Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                LazyColumn(modifier = Modifier.height(130.dp)) {
                    items(PaymentMethod.values()) { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMethod = method }
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(method.displayName, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Reference / Transaction ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Receipt Memo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        val payment = Payment(
                            invoiceId = invoice.id,
                            workspaceId = invoice.workspaceId,
                            amount = amt,
                            method = selectedMethod,
                            referenceNumber = referenceNumber.trim(),
                            paymentType = if (amt >= invoice.balanceDue) "FINAL" else "PARTIAL",
                            notes = notes.trim()
                        )
                        onSave(payment)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("submit_payment_button")
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddExpenseDialog(
    workspaceId: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidTo by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.INVENTORY_MATERIALS) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var receiptRef by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Business Expense", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title / Reason *") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($currencySymbol) *") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = paidTo,
                        onValueChange = { paidTo = it },
                        label = { Text("Paid To / Vendor *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Expense Category", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.height(130.dp)) {
                        items(ExpenseCategory.values()) { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = cat }
                            ) {
                                RadioButton(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(cat.displayName, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = receiptRef,
                        onValueChange = { receiptRef = it },
                        label = { Text("Receipt / Invoice Ref No.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        val exp = Expense(
                            workspaceId = workspaceId,
                            title = title.trim(),
                            category = selectedCategory,
                            amount = amt,
                            paidTo = paidTo.trim().ifEmpty { "Vendor" },
                            referenceReceiptNo = receiptRef.trim(),
                            notes = notes.trim()
                        )
                        onSave(exp)
                    }
                },
                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("submit_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InvoiceDetailDialog(
    invoice: Invoice,
    workspace: Workspace,
    onDismiss: () -> Unit,
    onRecordPayment: () -> Unit,
    onSharePdf: () -> Unit,
    onSendEmail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(invoice.type.displayName.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("No: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Billed To: ${invoice.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (invoice.customerEmail.isNotEmpty()) {
                        Text("Email: ${invoice.customerEmail}", fontSize = 12.sp)
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Amount:", fontSize = 12.sp)
                                Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Paid:", fontSize = 12.sp)
                                Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.totalPaid)}", color = Color(0xFF10B981), fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Remaining Due:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.balanceDue)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (invoice.balanceDue > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                if (invoice.payments.isNotEmpty()) {
                    item {
                        Text("Recorded Payments (${invoice.payments.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    items(invoice.payments) { p ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Text("${p.method.displayName} • ${p.referenceNumber.ifBlank { "Direct" }}", fontSize = 10.sp)
                                Text("+${workspace.currencySymbol}${String.format(Locale.US, "%.2f", p.amount)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(
                            onClick = onSharePdf,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onSendEmail,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (invoice.balanceDue > 0.0) {
                Button(onClick = onRecordPayment) {
                    Text("Record Payment")
                }
            } else {
                Button(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
