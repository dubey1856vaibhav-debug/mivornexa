package com.example.bizflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.BusinessType
import com.example.bizflow.model.ModuleType
import com.example.bizflow.model.WorkStage
import com.example.bizflow.model.Workspace
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSelectorDialog(
    repository: BizFlowRepository,
    onDismiss: () -> Unit
) {
    val workspaces by repository.workspaces.collectAsState()
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    var showCreateModal by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Select Workspace",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Authorized BizFlow Companies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(workspaces) { ws ->
                        val isSelected = ws.id == currentWorkspace.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                width = 2.dp
                            ) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.selectWorkspace(ws.id)
                                    onDismiss()
                                }
                                .testTag("workspace_item_${ws.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                ) {
                                    Text(
                                        text = ws.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ws.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = ws.businessType.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${ws.currencyCode} (${ws.currencySymbol})  •  ${ws.enabledModules.size} modules active",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showCreateModal = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_workspace_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Workspace")
                }
            }
        }
    }

    if (showCreateModal) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateModal = false },
            onCreate = { newWs ->
                repository.addWorkspace(newWs)
                showCreateModal = false
                onDismiss()
            }
        )
    }
}

@Composable
fun CreateWorkspaceDialog(
    onDismiss: () -> Unit,
    onCreate: (Workspace) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BusinessType.RETAIL_ECOMMERCE) }
    var currencySymbol by remember { mutableStateOf("$") }
    var currencyCode by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Company Workspace", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Tagline / Subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Business Model & Template (13 Industries)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                BusinessType.values().forEach { bType ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedType == bType) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            .clickable { selectedType = bType }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == bType,
                            onClick = { selectedType = bType }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = bType.displayName, fontSize = 13.sp, fontWeight = if (selectedType == bType) FontWeight.Bold else FontWeight.Normal)
                            Text(text = "Category: ${bType.category} • Uses ${bType.workRecordPlural}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("Symbol") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currencyCode,
                        onValueChange = { currencyCode = it },
                        label = { Text("Code") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val repo = BizFlowRepository.getInstance()
                        val stages = repo.getDefaultStagesForType(selectedType)
                        val customFields = repo.getDefaultCustomFieldsForType(selectedType)

                        val newWorkspace = Workspace(
                            id = "ws-${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            tagline = tagline.trim().ifEmpty { "${selectedType.displayName} Workspace" },
                            businessType = selectedType,
                            currencySymbol = currencySymbol.trim().ifEmpty { "$" },
                            currencyCode = currencyCode.trim().uppercase().ifEmpty { "USD" },
                            enabledModules = setOf(
                                ModuleType.ORDERS,
                                ModuleType.BILLING,
                                ModuleType.EXPENSES,
                                ModuleType.TASKS,
                                ModuleType.INVENTORY
                            ),
                            workflowStages = stages,
                            customFields = customFields
                        )
                        onCreate(newWorkspace)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create Workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
