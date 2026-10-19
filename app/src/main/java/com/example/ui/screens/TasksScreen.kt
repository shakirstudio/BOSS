package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BmsError
import com.example.ui.theme.BmsPrimaryDark
import com.example.ui.theme.BmsSuccess
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun TasksScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.visibleTasks.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isUserAdminOrOwner by viewModel.isUserAdminOrOwner.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "All" -> tasks
            "Urgent" -> tasks.filter { it.priority == "Urgent" }
            "In Progress" -> tasks.filter { it.status == "In Progress" || it.status == "To Do" }
            "Completed" -> tasks.filter { it.status == "Completed" }
            else -> tasks
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.AddTask, contentDescription = "Add Task")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUserAdminOrOwner) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockPerson,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assigned View: $currentUserName ($currentUserRole) • Only tasks assigned to you",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = listOf("All", "Urgent", "In Progress", "Completed").indexOf(selectedFilter),
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                listOf("All", "Urgent", "In Progress", "Completed").forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val isDone = task.status == "Completed"
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { viewModel.toggleTaskComplete(task) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        StatusBadge(text = task.priority)
                                        IconButton(onClick = { editingTask = task }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Task", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                if (task.description.isNotBlank()) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Assignee: ${task.assignedTo} • ${task.projectName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Due: ${task.deadline}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (task.priority == "Urgent") BmsError else BmsPrimaryDark,
                                        fontWeight = FontWeight.SemiBold
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
            onDismiss = { showAddTaskDialog = false },
            onAdd = { newTask ->
                viewModel.addTask(newTask)
                showAddTaskDialog = false
            }
        )
    }

    editingTask?.let { taskToEdit ->
        EditTaskDialog(
            task = taskToEdit,
            onDismiss = { editingTask = null },
            onSave = { updated ->
                viewModel.updateTask(updated)
                editingTask = null
            }
        )
    }
}

@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var assignedTo by remember { mutableStateOf(task.assignedTo) }
    var projectName by remember { mutableStateOf(task.projectName) }
    var priority by remember { mutableStateOf(task.priority) }
    var deadline by remember { mutableStateOf(task.deadline) }
    var status by remember { mutableStateOf(task.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task Details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = assignedTo, onValueChange = { assignedTo = it }, label = { Text("Assigned Team Member") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Project / Client") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority (Low, Medium, High, Urgent)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (To Do, In Progress, Completed)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(task.copy(title = title.trim(), description = description.trim(), assignedTo = assignedTo.trim(), projectName = projectName.trim(), priority = priority.trim(), deadline = deadline.trim(), status = status.trim()))
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("Studio Operations") }
    var priority by remember { mutableStateOf("High") }
    var deadline by remember { mutableStateOf("Tomorrow") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Studio Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = assignedTo, onValueChange = { assignedTo = it }, label = { Text("Assigned Team Member") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Associated Project / Client") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority (Low, Medium, High, Urgent)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val task = TaskEntity(
                        id = "task_" + System.currentTimeMillis().toString().takeLast(6),
                        workspaceId = "",
                        title = title.trim(),
                        description = description.trim(),
                        assignedTo = assignedTo.trim(),
                        projectName = projectName.trim(),
                        priority = priority.trim(),
                        deadline = deadline.trim(),
                        status = "To Do"
                    )
                    onAdd(task)
                },
                enabled = title.isNotBlank()
            ) { Text("Create Task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
