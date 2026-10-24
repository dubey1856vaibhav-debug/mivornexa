package com.example.bizflow.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.example.bizflow.model.Customer
import com.example.bizflow.model.Invoice
import com.example.bizflow.model.WorkRecord
import com.example.bizflow.model.Workspace
import com.example.bizflow.ui.components.EmptyStateView
import com.example.bizflow.ui.components.WorkStagePill
import com.example.ui.theme.MivornexaPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    customer: Customer,
    workspace: Workspace,
    repository: BizFlowRepository,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workRecords by repository.workRecords.collectAsState()
    val invoices by repository.invoices.collectAsState()

    // Strictly filter records for this customer only (ZERO data leak)
    val clientOrders = workRecords.filter { it.customerId == customer.id }
    val clientInvoices = invoices.filter { it.customerId == customer.id }
    val totalOutstanding = clientInvoices.sumOf { it.balanceDue }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("My Orders (${clientOrders.size})", "Invoices (${clientInvoices.size})", "Support & Info")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mivornexa", fontWeight = FontWeight.Bold, color = MivornexaPurple)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "CLIENT PORTAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Providing services for ${workspace.name}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("portal_sign_out_button")
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Exit Portal")
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
            // Welcome Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hello, ${customer.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track your order milestones, download PDF receipts, and contact the specialist team.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Outstanding Balance:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", totalOutstanding)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (totalOutstanding > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    if (clientOrders.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.WorkOutline,
                            title = "No Active Orders",
                            message = "You currently have no recorded orders or service jobs with ${workspace.name}."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(clientOrders) { order ->
                                val stage = workspace.workflowStages.find { it.id == order.currentStageId }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "#${order.recordNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MivornexaPurple
                                            )
                                            WorkStagePill(
                                                stageName = stage?.name ?: "In Progress",
                                                colorHex = stage?.colorHex ?: "#7C3AED"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(text = order.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Specialist: ${order.assignedStaffName}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Total Amount:", fontSize = 12.sp)
                                            Text("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", order.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Balance Due:", fontSize = 12.sp)
                                            Text(
                                                "${workspace.currencySymbol}${String.format(Locale.US, "%.2f", order.balanceDue)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (order.balanceDue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (clientInvoices.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Receipt,
                            title = "No Invoices",
                            message = "No invoices have been issued to your account yet."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(clientInvoices) { inv ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MivornexaPurple)
                                            Text(
                                                text = inv.status.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (inv.balanceDue <= 0.0) Color(0xFF10B981) else Color(0xFFF59E0B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text("Total: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", inv.totalAmount)}", fontSize = 12.sp)
                                                Text(
                                                    "Balance: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", inv.balanceDue)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (inv.balanceDue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val pdfFile = PdfGenerator.generateInvoicePdf(context, inv, workspace)
                                                        PdfGenerator.sharePdf(context, pdfFile)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download PDF", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Contact ${workspace.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Address: ${workspace.address}", fontSize = 12.sp)
                                Text("Phone: ${workspace.phone}", fontSize = 12.sp)
                                Text("Email: ${workspace.email}", fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${workspace.phone}"))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call Store", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${workspace.email}"))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Email Us", fontSize = 12.sp)
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
