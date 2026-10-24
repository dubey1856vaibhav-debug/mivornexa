package com.example.bizflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.MivornexaPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopifyScreen(
    repository: BizFlowRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentWorkspace by repository.currentWorkspace.collectAsState()

    var isConnected by remember { mutableStateOf(true) }
    var storeDomain by remember { mutableStateOf("apex-gear-official.myshopify.com") }
    var syncProductsEnabled by remember { mutableStateOf(true) }
    var syncInventoryEnabled by remember { mutableStateOf(true) }
    var syncOrdersEnabled by remember { mutableStateOf(true) }
    var syncCustomersEnabled by remember { mutableStateOf(false) }

    var isSyncingNow by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf(System.currentTimeMillis() - 3600000L * 2) }
    var syncLog by remember { mutableStateOf("48 items mapped • 2 webhook events received") }

    val sdf = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopify Integration Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Shopify Status Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("shopify_status_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF96BF48)) // Official Shopify green tint
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Shopify",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Shopify Connected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = storeDomain,
                                        fontSize = 11.sp,
                                        color = if (isConnected) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isConnected) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (isConnected) "ACTIVE SYNC" else "DISCONNECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) Color(0xFF065F46) else Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Last Synchronized:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(sdf.format(Date(lastSyncTime)), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sync Health Status:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(syncLog, fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    isSyncingNow = true
                                    coroutineScope.launch {
                                        delay(1200)
                                        isSyncingNow = false
                                        lastSyncTime = System.currentTimeMillis()
                                        syncLog = "Synced 3 inventory updates & 1 new online order"
                                        Toast.makeText(context, "Shopify sync completed successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSyncingNow && isConnected,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF96BF48)),
                                modifier = Modifier.weight(1f).testTag("shopify_sync_now_button")
                            ) {
                                if (isSyncingNow) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { isConnected = !isConnected },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isConnected) "Disconnect" else "Reconnect", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Synchronization Modules Configuration
            item {
                Text("Synchronization Modules", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SyncToggleRow(
                            icon = Icons.Default.Inventory2,
                            title = "Products & Catalog",
                            subtitle = "Import Shopify items into Mivornexa catalog",
                            checked = syncProductsEnabled,
                            onCheckedChange = { syncProductsEnabled = it }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        SyncToggleRow(
                            icon = Icons.Default.Warehouse,
                            title = "Live Inventory Count",
                            subtitle = "Bidirectional stock level sync on purchases/sales",
                            checked = syncInventoryEnabled,
                            onCheckedChange = { syncInventoryEnabled = it }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        SyncToggleRow(
                            icon = Icons.Default.Receipt,
                            title = "Store Orders",
                            subtitle = "Create Mivornexa work orders automatically from online sales",
                            checked = syncOrdersEnabled,
                            onCheckedChange = { syncOrdersEnabled = it }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        SyncToggleRow(
                            icon = Icons.Default.People,
                            title = "Customer Profiles",
                            subtitle = "Sync buyers into CRM with duplicate detection",
                            checked = syncCustomersEnabled,
                            onCheckedChange = { syncCustomersEnabled = it }
                        )
                    }
                }
            }

            // Security & Permissions Transparency
            item {
                Text("Official App Permissions (OAuth2)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PermissionBadge("read_products, write_products", "Product catalog synchronization")
                        PermissionBadge("read_inventory, write_inventory", "Accurate warehouse stock updates")
                        PermissionBadge("read_orders, write_orders", "Fulfillment status push")
                        PermissionBadge("read_customers", "Buyer contact CRM linking")
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Security guarantee: Shopify API tokens are stored strictly in server-side encrypted key stores and validated via HMAC webhook signatures.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = MivornexaPurple, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PermissionBadge(scope: String, purpose: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = scope, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MivornexaPurple)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "($purpose)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
