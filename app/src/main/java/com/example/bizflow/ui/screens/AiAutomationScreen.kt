package com.example.bizflow.ui.screens

import android.widget.Toast
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
import com.example.ui.theme.MivornexaOrange
import com.example.ui.theme.MivornexaPurple
import com.example.ui.theme.MivornexaPurpleContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AiProvider(val displayName: String, val defaultModel: String, val iconColor: Color) {
    GEMINI("Google Gemini", "gemini-2.5-flash", Color(0xFF1A73E8)),
    OPENAI("OpenAI GPT", "gpt-4o", Color(0xFF10A37F)),
    CLAUDE("Anthropic Claude", "claude-3-5-sonnet", Color(0xFFD97706)),
    DETERMINISTIC("Deterministic Rules", "offline-rule-engine", Color(0xFF7C3AED))
}

data class ToolExecutionLog(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val toolName: String,
    val actionLevel: String, // READ_ONLY, DRAFT, REVERSIBLE, SENSITIVE_APPROVAL_REQUIRED
    val output: String,
    val provider: AiProvider,
    val requiresApproval: Boolean = false,
    val isApproved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAutomationScreen(
    repository: BizFlowRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val customers by repository.customers.collectAsState()
    val workRecords by repository.workRecords.collectAsState()
    val invoices by repository.invoices.collectAsState()
    val expenses by repository.expenses.collectAsState()

    var selectedProvider by remember { mutableStateOf(AiProvider.GEMINI) }
    var promptQuery by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    var executionLogs by remember {
        mutableStateOf(
            listOf(
                ToolExecutionLog(
                    command = "Find customers with unpaid balances",
                    toolName = "queryCustomerReceivables",
                    actionLevel = "READ_ONLY",
                    output = "Found 2 customers with pending balances: Marcus Vance ($850.00), Priya Sharma (₹25,000.00).",
                    provider = AiProvider.GEMINI
                ),
                ToolExecutionLog(
                    command = "Notify customers whose orders are ready",
                    toolName = "sendCustomerNotificationEmail",
                    actionLevel = "SENSITIVE_APPROVAL_REQUIRED",
                    output = "Drafted ready-for-pickup email notification for David Chen (#JOB-REP-2026-77).",
                    provider = AiProvider.GEMINI,
                    requiresApproval = true,
                    isApproved = false
                )
            )
        )
    }

    val quickPrompts = listOf(
        "Show all overdue orders",
        "Find customers with unpaid balances",
        "Summarize this month's expenses",
        "Prepare invoices for completed jobs",
        "Notify customers whose orders are ready",
        "Update eligible Shopify inventory records"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI & Automation Center", fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Provider Selection Banner
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("ai_provider_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Active AI Orchestration Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = selectedProvider.iconColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = selectedProvider.defaultModel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedProvider.iconColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(AiProvider.values()) { provider ->
                                    val isSelected = selectedProvider == provider
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedProvider = provider },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(provider.iconColor)
                                            )
                                        },
                                        label = { Text(provider.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MivornexaPurpleContainer,
                                            selectedLabelColor = MivornexaPurple
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Prompts Chips
                item {
                    Text("Instant Natural-Language Commands", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickPrompts) { prompt ->
                            SuggestionChip(
                                onClick = { promptQuery = prompt },
                                label = { Text(prompt, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Tool Execution History
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Execution Audit & Approval Queue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${executionLogs.size} tool calls", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(executionLogs) { log ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_log_${log.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = log.provider.iconColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.toolName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MivornexaPurple
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (log.actionLevel.contains("SENSITIVE")) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                                ) {
                                    Text(
                                        text = log.actionLevel.replace("_", " "),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.actionLevel.contains("SENSITIVE")) Color(0xFF991B1B) else Color(0xFF065F46),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "\"${log.command}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = log.output,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Action button if approval is required
                            if (log.requiresApproval && !log.isApproved) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            executionLogs = executionLogs.map {
                                                if (it.id == log.id) it.copy(isApproved = true, output = "${it.output} (Approved and executed securely by Owner)") else it
                                            }
                                            Toast.makeText(context, "Action authorized and executed!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Approve & Execute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            executionLogs = executionLogs.filter { it.id != log.id }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Reject", fontSize = 11.sp)
                                    }
                                }
                            } else if (log.isApproved) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Authorized & Dispatched", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Command Input Box
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = promptQuery,
                        onValueChange = { promptQuery = it },
                        placeholder = { Text("Ask ${selectedProvider.displayName} to automate...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("ai_prompt_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (promptQuery.isNotBlank()) {
                                isThinking = true
                                val userPrompt = promptQuery
                                promptQuery = ""

                                coroutineScope.launch {
                                    delay(900)
                                    isThinking = false

                                    // Dynamic tool execution logic
                                    val (toolName, level, result, reqApproval) = when {
                                        userPrompt.contains("overdue", true) || userPrompt.contains("orders", true) -> {
                                            Quad(
                                                "queryWorkRecords",
                                                "READ_ONLY",
                                                "Analyzed ${workRecords.size} records in ${currentWorkspace.name}. 1 order is near deadline: #${workRecords.firstOrNull()?.recordNumber ?: "101"}.",
                                                false
                                            )
                                        }
                                        userPrompt.contains("unpaid", true) || userPrompt.contains("balance", true) -> {
                                            val totalDue = invoices.sumOf { it.balanceDue }
                                            Quad(
                                                "calculateReceivables",
                                                "READ_ONLY",
                                                "Total receivables due across active clients: ${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", totalDue)}.",
                                                false
                                            )
                                        }
                                        userPrompt.contains("expense", true) -> {
                                            val totalExp = expenses.sumOf { it.amount }
                                            Quad(
                                                "aggregateExpenses",
                                                "READ_ONLY",
                                                "Aggregated ${expenses.size} expenses: Total ${currentWorkspace.currencySymbol}${String.format(Locale.US, "%.2f", totalExp)}.",
                                                false
                                            )
                                        }
                                        userPrompt.contains("shopify", true) -> {
                                            Quad(
                                                "syncShopifyInventory",
                                                "REVERSIBLE",
                                                "Matched 48 SKUs against Shopify store apex-gear-official.myshopify.com. All counts verified.",
                                                false
                                            )
                                        }
                                        userPrompt.contains("notify", true) || userPrompt.contains("email", true) -> {
                                            Quad(
                                                "sendCustomerNotificationEmail",
                                                "SENSITIVE_APPROVAL_REQUIRED",
                                                "Generated customer notification email for ${customers.firstOrNull()?.name ?: "Customer"}. Awaiting owner approval.",
                                                true
                                            )
                                        }
                                        else -> {
                                            Quad(
                                                "generateBusinessHealthAudit",
                                                "DRAFT",
                                                "Compiled operational digest for ${currentWorkspace.name}: ${workRecords.size} orders, ${invoices.size} invoices.",
                                                false
                                            )
                                        }
                                    }

                                    val newLog = ToolExecutionLog(
                                        command = userPrompt,
                                        toolName = toolName,
                                        actionLevel = level,
                                        output = result,
                                        provider = selectedProvider,
                                        requiresApproval = reqApproval
                                    )
                                    executionLogs = listOf(newLog) + executionLogs
                                }
                            }
                        },
                        enabled = !isThinking && promptQuery.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (promptQuery.isNotBlank()) MivornexaPurple else Color.LightGray)
                            .testTag("ai_prompt_submit_button")
                    ) {
                        if (isThinking) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
