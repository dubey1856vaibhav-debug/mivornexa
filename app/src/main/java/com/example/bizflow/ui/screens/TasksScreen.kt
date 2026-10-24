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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.model.TaskPriority
import com.example.bizflow.model.TaskStatus
import com.example.bizflow.model.TeamMember
import com.example.bizflow.model.TeamTask
import com.example.bizflow.ui.components.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    repository: BizFlowRepository,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by repository.currentWorkspace.collectAsState()
    val tasks by repository.tasks.collectAsState()
    val teamMembers by repository.teamMembers.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedPriorityFilter by remember { mutableStateOf<TaskPriority?>(null) }

    val filteredTasks = tasks.filter { t ->
        selectedPriorityFilter == null || t.priority == selectedPriorityFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Team & Tasks", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${tasks.count { it.status != TaskStatus.COMPLETED }} pending • ${currentWorkspace.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.testTag("add_task_appbar_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                FilterChip(
                    selected = selectedPriorityFilter == null,
                    onClick = { selectedPriorityFilter = null },
                    label = { Text("All (${tasks.size})") }
                )
                TaskPriority.values().forEach { prio ->
                    FilterChip(
                        selected = selectedPriorityFilter == prio,
                        onClick = {
                            selectedPriorityFilter = if (selectedPriorityFilter == prio) null else prio
                        },
                        label = { Text(prio.displayName) }
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Checklist,
                    title = "No Tasks Here",
                    message = "Assign tasks to your team to keep business milestones on track.",
                    actionLabel = "Create Task",
                    onAction = { showAddTaskDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTasks) { task ->
                        val isDone = task.status == TaskStatus.COMPLETED
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_item_${task.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                IconButton(
                                    onClick = { repository.toggleTaskStatus(task.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Toggle status",
                                        tint = if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (task.description.isNotEmpty()) {
                                        Text(
                                            text = task.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = task.assignedToMemberName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Due: ${sdf.format(Date(task.dueDate))}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (task.priority) {
                                        TaskPriority.URGENT -> Color(0xFFFEE2E2)
                                        TaskPriority.HIGH -> Color(0xFFFEF3C7)
                                        TaskPriority.MEDIUM -> Color(0xFFE0F2FE)
                                        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = task.priority.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (task.priority) {
                                            TaskPriority.URGENT -> Color(0xFF991B1B)
                                            TaskPriority.HIGH -> Color(0xFF92400E)
                                            TaskPriority.MEDIUM -> Color(0xFF0369A1)
                                            TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            workspaceId = currentWorkspace.id,
            teamMembers = teamMembers,
            onDismiss = { showAddTaskDialog = false },
            onSave = { newTask ->
                repository.saveTask(newTask)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun AddTaskDialog(
    workspaceId: String,
    teamMembers: List<TeamMember>,
    onDismiss: () -> Unit,
    onSave: (TeamTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var assignedMember by remember { mutableStateOf(teamMembers.firstOrNull()?.name ?: "Alex Rivera") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Team Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Specifics") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.values().forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Assign to Staff Member", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(teamMembers) { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { assignedMember = member.name }
                        ) {
                            RadioButton(
                                selected = assignedMember == member.name,
                                onClick = { assignedMember = member.name }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${member.name} (${member.role.displayName})", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val task = TeamTask(
                            workspaceId = workspaceId,
                            title = title.trim(),
                            description = description.trim(),
                            priority = selectedPriority,
                            assignedToMemberName = assignedMember
                        )
                        onSave(task)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("submit_task_button")
            ) {
                Text("Assign Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
