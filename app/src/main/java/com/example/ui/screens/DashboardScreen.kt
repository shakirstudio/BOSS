package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EmployeeEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun DashboardScreen(
    viewModel: BmsViewModel,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val leads by viewModel.leads.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val photoEdits by viewModel.photoEdits.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val shoots by viewModel.shoots.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val employees by viewModel.employees.collectAsState()

    val totalRevenue = payments.sumOf { it.amount }
    val activeBookings = bookings.count { it.status == "Confirmed" || it.status == "Upcoming" }
    val newLeads = leads.count { it.status == "New" || it.status == "Follow-up" }

    val totalPhotos = photoEdits.sumOf { it.totalPhotos }
    val editedPhotos = photoEdits.sumOf { it.editedPhotos }
    val editingProgress = if (totalPhotos > 0) (editedPhotos.toFloat() / totalPhotos.toFloat()) else 0f

    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isUserAdminOrOwner by viewModel.isUserAdminOrOwner.collectAsState()
    val authUser by viewModel.authenticatedUser.collectAsState()

    // Find the corresponding employee entity from single source of truth in Room
    val currentEmployee = employees.firstOrNull {
        it.id == (authUser?.employeeId ?: "") || it.name.equals(currentUserName, ignoreCase = true)
    }

    // Filter tasks for this user
    val userAssignedTasks = tasks.filter {
        it.assignedTo.equals(currentUserName, ignoreCase = true) ||
        (currentEmployee != null && it.assignedTo.equals(currentEmployee.name, ignoreCase = true))
    }

    val pendingUpcomingTasks = userAssignedTasks.filter { it.status != "Completed" }
    val completedTasks = userAssignedTasks.filter { it.status == "Completed" }

    val pendingCount = userAssignedTasks.count { it.status.equals("Pending", ignoreCase = true) || it.status.equals("To Do", ignoreCase = true) }
    val inProgressCount = userAssignedTasks.count { it.status.equals("In Progress", ignoreCase = true) }
    val completedCount = completedTasks.size
    val overdueCount = userAssignedTasks.count { it.isOverdue || it.status.equals("Overdue", ignoreCase = true) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var taskToComplete by remember { mutableStateOf<TaskEntity?>(null) }
    var showAssignTaskDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (!isUserAdminOrOwner) {
            // ==========================================
            // 1. EMPLOYEE PROFILE CARD
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_profile_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(
                                        text = (currentEmployee?.name ?: currentUserName).take(2).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentEmployee?.name ?: currentUserName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "EMPLOYEE",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentEmployee?.designation ?: authUser?.designation ?: "Lead Cinematographer & Editor",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = currentEmployee?.department ?: authUser?.department ?: "Production & Post-Production",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Edit Employee Details Button
                            Button(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("edit_employee_details_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Details", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Contact info strip
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentEmployee?.mobile ?: authUser?.mobile ?: "+91 98220 11223",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentEmployee?.email ?: authUser?.email ?: "employee@bms.com",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // TODAY'S ATTENDANCE & BIOMETRIC CHECK-IN
            // ==========================================
            item {
                val todayStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
                val attendanceList by viewModel.attendance.collectAsState()
                val todayRec = attendanceList.firstOrNull {
                    it.employeeId == (currentEmployee?.id ?: "") && it.date == todayStr
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_attendance_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp).fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (todayRec != null) "Attendance: ${todayRec.status}" else "Today's Attendance",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (todayRec != null) "In: ${todayRec.checkInTime} • ${todayRec.workingHours}h" else "Not Marked Yet • 9h Standard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.selectTab(BmsNavigationItem.TEAM) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (todayRec == null) "Mark Present" else "Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ==========================================
            // 2. TASK STATISTICS / STATUS SUMMARY
            // ==========================================
            item {
                BmsSectionHeader(title = "Task Statistics & Summary", subtitle = "Your Current Workload")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Pending", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            Text("$pendingCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFFF59E0B))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("In Progress", style = MaterialTheme.typography.labelSmall, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                            Text("$inProgressCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF3B82F6))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Completed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            Text("$completedCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF10B981))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Overdue", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            Text("$overdueCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            // ==========================================
            // 3. UPCOMING PENDING TASKS
            // ==========================================
            item {
                BmsSectionHeader(
                    title = "Upcoming Pending Tasks",
                    subtitle = "${pendingUpcomingTasks.size} tasks pending execution",
                    actionText = "All Tasks",
                    onActionClick = { viewModel.selectTab(BmsNavigationItem.TASKS) }
                )

                if (pendingUpcomingTasks.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("No pending tasks!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("You are all caught up with your studio assignments.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pendingUpcomingTasks.forEach { task ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_card_${task.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Priority badge
                                        val priorityColor = when (task.priority) {
                                            "Urgent" -> Color(0xFFEF4444)
                                            "High" -> Color(0xFFF59E0B)
                                            "Medium" -> Color(0xFF3B82F6)
                                            else -> Color(0xFF6B7280)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = priorityColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = task.priority.uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = priorityColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Assigned By: ${task.assignedBy}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = BmsTertiary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Due: ${task.deadline} (${task.dueTime})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BmsTertiary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        StatusBadge(text = task.status)

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (task.status != "In Progress") {
                                                OutlinedButton(
                                                    onClick = { viewModel.updateTaskStatus(task, "In Progress") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("start_task_button_${task.id}")
                                                ) {
                                                    Text("In Progress", fontSize = 11.sp)
                                                }
                                            }

                                            Button(
                                                onClick = { taskToComplete = task },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                modifier = Modifier.testTag("complete_task_button_${task.id}")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark Completed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. COMPLETED TASK STATUS
            // ==========================================
            item {
                BmsSectionHeader(
                    title = "Completed Task Status",
                    subtitle = "${completedTasks.size} tasks successfully finished",
                    actionText = "History",
                    onActionClick = { viewModel.selectTab(BmsNavigationItem.TASKS) }
                )

                if (completedTasks.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("No completed tasks yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Finished tasks will appear here with verification status and remarks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        completedTasks.forEach { task ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("completed_task_${task.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatusBadge(text = "Completed")
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Completed By: ${task.completedBy.ifBlank { task.assignedTo }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Text(
                                            text = "${task.completionDate.ifBlank { task.deadline }} (${task.completionTime.ifBlank { task.dueTime }})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    if (task.remarks.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Remarks: ${task.remarks}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // ADMIN / OWNER DASHBOARD VIEW
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "BOSS • Super Admin Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Executive Studio Operations & RBAC Control",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            StatusBadge(text = "ADMIN CONSOLE")
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "₹${totalRevenue.toInt()}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Gross Revenue",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            Button(
                                onClick = { showAssignTaskDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("admin_assign_task_button")
                            ) {
                                Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assign Task", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Core Metrics
            item {
                BmsSectionHeader(title = "Core Metrics")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = "Active Leads",
                            value = "${leads.size}",
                            subtitle = "$newLeads require follow-up",
                            icon = Icons.Default.TrendingUp,
                            accentColor = BmsPrimaryDark,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectTab(BmsNavigationItem.LEADS) }
                        )
                        StatCard(
                            title = "Booked Shoots",
                            value = "$activeBookings",
                            subtitle = "${shoots.size} scheduled",
                            icon = Icons.Default.CameraAlt,
                            accentColor = BmsSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectTab(BmsNavigationItem.BOOKINGS) }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            title = "Staff Members",
                            value = "${employees.size}",
                            subtitle = "Multi-tenancy isolated",
                            icon = Icons.Default.People,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectTab(BmsNavigationItem.ADMIN) }
                        )
                        StatCard(
                            title = "Total Tasks",
                            value = "${tasks.size}",
                            subtitle = "${tasks.count { it.status == "Completed" }} finished",
                            icon = Icons.Default.Checklist,
                            accentColor = BmsTertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectTab(BmsNavigationItem.TASKS) }
                        )
                    }
                }
            }

            // Photo Editing Pipeline Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = BmsSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Photo Editing Pipeline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$editedPhotos / $totalPhotos photos",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BmsSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { editingProgress },
                            color = BmsSecondary,
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Upcoming Pending Tasks Section (Studio Fleet)
            item {
                val adminPendingTasks = tasks.filter { it.status != "Completed" }
                BmsSectionHeader(
                    title = "Upcoming Pending Tasks",
                    subtitle = "${adminPendingTasks.size} tasks pending studio execution",
                    actionText = "+ Assign Task",
                    onActionClick = { showAssignTaskDialog = true }
                )

                if (adminPendingTasks.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("No pending tasks!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("All assigned studio projects and tasks are up to date.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        adminPendingTasks.take(4).forEach { task ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusBadge(text = task.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Assigned To: ${task.assignedTo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Due: ${task.deadline} (${task.dueTime})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Completed Task Status Section
            item {
                val adminCompletedTasks = tasks.filter { it.status == "Completed" }
                BmsSectionHeader(
                    title = "Completed Task Status",
                    subtitle = "${adminCompletedTasks.size} tasks verified and delivered",
                    actionText = "All Tasks",
                    onActionClick = { viewModel.selectTab(BmsNavigationItem.TASKS) }
                )

                if (adminCompletedTasks.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("No completed tasks yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Completed tasks by studio team will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        adminCompletedTasks.take(4).forEach { task ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusBadge(text = "Completed")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Completed By: ${task.completedBy.ifBlank { task.assignedTo }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "${task.completionDate.ifBlank { task.deadline }} (${task.completionTime.ifBlank { task.dueTime }})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (task.remarks.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Remarks: ${task.remarks}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: EDIT EMPLOYEE DETAILS
    // ==========================================
    if (showEditProfileDialog) {
        val emp = currentEmployee
        var name by remember { mutableStateOf(emp?.name ?: currentUserName) }
        var mobile by remember { mutableStateOf(emp?.mobile ?: authUser?.mobile ?: "+91 98220 11223") }
        var email by remember { mutableStateOf(emp?.email ?: authUser?.email ?: "employee@bms.com") }
        var department by remember { mutableStateOf(emp?.department ?: authUser?.department ?: "Production & Post-Production") }
        var designation by remember { mutableStateOf(emp?.designation ?: authUser?.designation ?: "Lead Cinematographer & Editor") }
        var emergencyContact by remember { mutableStateOf(emp?.emergencyContact ?: authUser?.emergencyContact ?: "+91 98220 99999") }
        var responsibilities by remember { mutableStateOf(emp?.responsibilities ?: "Cinematic filming, Multi-cam timeline editing & 4K color grading") }

        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("edit_employee_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Edit Employee Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showEditProfileDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Text(
                        text = "Allowed personal information. Updates synchronize across all studio modules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Employee Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_name")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile Number *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_mobile")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_email")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = department,
                                onValueChange = { department = it },
                                label = { Text("Department") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_department")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = designation,
                                onValueChange = { designation = it },
                                label = { Text("Designation") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_designation")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = emergencyContact,
                                onValueChange = { emergencyContact = it },
                                label = { Text("Emergency Contact") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_emergency")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = responsibilities,
                                onValueChange = { responsibilities = it },
                                label = { Text("Scope & Responsibilities") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_employee_responsibilities")
                            )
                        }
                        item {
                            // Locked Administrative Fields Indicator
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Locked Fields (Admin Controlled):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                        Text("Role: Employee • System ID: ${emp?.id ?: "emp_rahul"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showEditProfileDialog = false },
                            modifier = Modifier.testTag("cancel_edit_employee_button")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val empId = emp?.id ?: "emp_rahul"
                                viewModel.updateEmployeeDetails(
                                    employeeId = empId,
                                    newName = name,
                                    newMobile = mobile,
                                    newEmail = email,
                                    newDepartment = department,
                                    newDesignation = designation,
                                    newEmergencyContact = emergencyContact,
                                    newResponsibilities = responsibilities
                                )
                                showEditProfileDialog = false
                            },
                            enabled = name.isNotBlank() && mobile.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_employee_details_button")
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: COMPLETE TASK WITH REMARKS
    // ==========================================
    taskToComplete?.let { task ->
        var remarks by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { taskToComplete = null },
            title = { Text("Complete Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Mark \"${task.title}\" as completed?", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Completion Remarks / Notes (Optional)") },
                        placeholder = { Text("e.g. Master render exported and uploaded to cloud drive.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_completion_remarks")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTaskStatus(task, "Completed", remarks)
                        taskToComplete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("confirm_complete_task_button")
                ) {
                    Text("Confirm Completed", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToComplete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: ADMIN ASSIGN NEW TASK
    // ==========================================
    if (showAssignTaskDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var assignedTo by remember { mutableStateOf(employees.firstOrNull()?.name ?: "Rahul Verma") }
        var projectName by remember { mutableStateOf("Studio Operations") }
        var priority by remember { mutableStateOf("High") }
        var deadline by remember { mutableStateOf("10 Oct 2026") }
        var dueTime by remember { mutableStateOf("06:00 PM") }

        AlertDialog(
            onDismissRequest = { showAssignTaskDialog = false },
            title = { Text("Assign New Task to Employee") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Task Description") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = assignedTo,
                            onValueChange = { assignedTo = it },
                            label = { Text("Assign To (Employee Name) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            label = { Text("Project / Event Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = priority,
                            onValueChange = { priority = it },
                            label = { Text("Priority (Urgent, High, Medium, Low)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = deadline,
                                onValueChange = { deadline = it },
                                label = { Text("Due Date") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dueTime,
                                onValueChange = { dueTime = it },
                                label = { Text("Due Time") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.assignNewTask(
                            title = title,
                            description = description,
                            assignedTo = assignedTo,
                            projectName = projectName,
                            priority = priority,
                            deadline = deadline,
                            dueTime = dueTime
                        )
                        showAssignTaskDialog = false
                    },
                    enabled = title.isNotBlank() && assignedTo.isNotBlank()
                ) {
                    Text("Assign Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
