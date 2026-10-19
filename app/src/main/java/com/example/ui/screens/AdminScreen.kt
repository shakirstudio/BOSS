package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeEntity
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun AdminScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val photoEdits by viewModel.photoEdits.collectAsState()
    val videoEdits by viewModel.videoEdits.collectAsState()
    val albums by viewModel.albumProjects.collectAsState()
    val shoots by viewModel.shoots.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payrollRecords by viewModel.payrollRecords.collectAsState()

    var editingEmployeeRole by remember { mutableStateOf<EmployeeEntity?>(null) }
    var fullEditingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var taskAssignTargetEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Roles & Responsibilities, 1: Attendance & Payroll Approvals, 2: Multi-Tenant Workspaces

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Top Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Super Admin Console",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Individual Staff Roles, Responsibilities & RBAC Access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(text = "ADMIN CONSOLE")
            }
        }

        // Active Profile Bar with Impersonation reset
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentUserRole.contains("Owner", true) || currentUserRole.contains("Admin", true))
                        MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (currentUserRole.contains("Owner", true)) Icons.Default.Shield else Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Current Active Login: $currentUserName",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Role: $currentUserRole • ${if (currentUserRole.contains("Owner", true)) "Full System Access" else "Restricted (Assigned-Only View)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!currentUserRole.contains("Owner", true) && !currentUserRole.contains("Admin", true)) {
                        Button(
                            onClick = { viewModel.switchUserRole("Owner / Admin", "Shakir Tamboli") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Reset to Admin", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Tab Selector
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Roles & RBAC (${employees.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Payroll Approvals (${payrollRecords.count { it.status == "Draft" }})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tenants (${allWorkspaces.size})", fontSize = 12.sp) }
                )
            }
        }

        if (selectedTab == 0) {
            // Summary Stats
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        title = "Team Members",
                        value = "${employees.size}",
                        subtitle = "Individual roles assigned",
                        icon = Icons.Default.Badge,
                        accentColor = BmsPrimaryDark,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Strict RBAC",
                        value = "ACTIVE",
                        subtitle = "Staff sees assigned work only",
                        icon = Icons.Default.LockPerson,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                BmsSectionHeader(
                    title = "Individual Team Roles & Responsibilities",
                    subtitle = "Manage employees, assign duties, configure login access & assign tasks"
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showAddEmployeeDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("admin_add_employee_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Employee", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { taskAssignTargetEmployee = employees.firstOrNull() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("admin_assign_task_button")
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assign Task", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            val pendingEmployees = employees.filter { it.status.equals("Pending", ignoreCase = true) }
            if (pendingEmployees.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("pending_approvals_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pending Account Registrations (${pendingEmployees.size})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                Text(
                                    text = "Requires Admin Action",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Newly registered user accounts require your approval before they can sign in to BOSS:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            pendingEmployees.forEach { pendingEmp ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = pendingEmp.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${pendingEmp.designation} • ${pendingEmp.department}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Username: ${pendingEmp.loginUsername} | Email: ${pendingEmp.email} | Mobile: ${pendingEmp.mobile}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = { viewModel.approveUser(pendingEmp.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f).testTag("approve_user_${pendingEmp.id}")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.rejectUser(pendingEmp.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f).testTag("reject_user_${pendingEmp.id}")
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { fullEditingEmployee = pendingEmp },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Assign Role", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // List of individual employees with roles, responsibilities, assigned workload, and impersonation button
            items(employees, key = { it.id }) { emp ->
                val assignedPhotosCount = photoEdits.count { it.editorName.equals(emp.name, ignoreCase = true) }
                val assignedVideosCount = videoEdits.count { it.editorName.equals(emp.name, ignoreCase = true) }
                val assignedAlbumsCount = albums.count { it.designerName.equals(emp.name, ignoreCase = true) }
                val assignedShootsCount = shoots.count {
                    it.leadPhotographer.contains(emp.name, true) ||
                    it.videographer.contains(emp.name, true)
                }
                val assignedTasksCount = tasks.count { it.assignedTo.equals(emp.name, ignoreCase = true) }
                val totalAssignedItems = assignedPhotosCount + assignedVideosCount + assignedAlbumsCount + assignedShootsCount + assignedTasksCount

                val isCurrentlyLoggedIn = currentUserName.equals(emp.name, ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlyLoggedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header: Employee name + Role badge
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
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = emp.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = emp.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${emp.designation} • ${emp.department}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                StatusBadge(text = emp.role)
                                if (isCurrentlyLoggedIn) {
                                    Text(
                                        text = "LOGGED IN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Individual Responsibilities Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Assigned Responsibilities & Scope:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (emp.responsibilities.isNotBlank()) emp.responsibilities else "Responsible for assigned studio bookings, post-production and project deliverables.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Assigned Modules chips
                        Text(
                            text = "Permitted Modules Access:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val perms = if (emp.assignedPermissions.isBlank() || emp.assignedPermissions == "ALL") {
                                listOf("DASHBOARD", "WORKFLOWS", "TASKS", "STORAGE")
                            } else {
                                emp.assignedPermissions.split(",").map { it.trim() }
                            }
                            perms.forEach { mod ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = mod,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Currently Assigned Workload stats
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Current Assigned Workload: $totalAssignedItems items",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Photos: $assignedPhotosCount • Videos: $assignedVideosCount • Shoots: $assignedShootsCount • Tasks: $assignedTasksCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Contact & Login strip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Login: ${if (emp.loginUsername.isNotBlank()) emp.loginUsername else emp.name.lowercase().replace(" ", "")} • Status: ${emp.status}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (emp.status == "Active") Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${emp.mobile} • ${emp.email}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Complete Admin Control
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { fullEditingEmployee = emp },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).testTag("edit_employee_admin_${emp.id}")
                                ) {
                                    Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit & Login Info", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { taskAssignTargetEmployee = emp },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("assign_task_emp_${emp.id}")
                                ) {
                                    Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assign Task", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { editingEmployeeRole = emp },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Role & Duties", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.switchUserRole(emp.role, emp.name)
                                        viewModel.selectTab(BmsNavigationItem.DASHBOARD)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isCurrentlyLoggedIn) "Active View" else "Login Preview",
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Quick Status Control Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (emp.status == "Active") {
                                    OutlinedButton(
                                        onClick = { viewModel.setUserRoleAndStatus(emp.id, emp.role, "Inactive") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("deactivate_emp_${emp.id}")
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Deactivate Access", fontSize = 11.sp)
                                    }
                                } else if (emp.status == "Inactive" || emp.status == "Rejected") {
                                    Button(
                                        onClick = { viewModel.setUserRoleAndStatus(emp.id, emp.role, "Active") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("activate_emp_${emp.id}")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Activate Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (emp.status == "Pending") {
                                    Button(
                                        onClick = { viewModel.approveUser(emp.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("quick_approve_emp_${emp.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // Tab 1: Attendance & Salary Approvals
            item {
                BmsSectionHeader(title = "Monthly Salary Review & Approvals")
                Text(
                    text = "Authorized Administrator Payroll Review. Approvals update payroll status, timestamp, and audit trail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (payrollRecords.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Monthly Payroll Generated Yet", fontWeight = FontWeight.Bold)
                            Text("Go to the Team -> Salary & Payroll tab to calculate and generate monthly salary sheets.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(payrollRecords, key = { it.id }) { record ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(text = record.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(text = "Period: ${record.monthYear} • Basis: ${record.calculationBasis}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                StatusBadge(text = record.status)
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Payable Days: ${record.payableDays} (${record.totalWorkingHours}h)", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Net Salary: ₹${record.netSalary.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary, style = MaterialTheme.typography.bodyMedium)
                            }

                            if (record.deductions > 0) {
                                Text(text = "Deductions: ₹${record.deductions.toInt()} (${record.deductionReason})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }

                            if (record.status == "Draft") {
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            viewModel.approvePayroll(record.id, currentUserName.ifBlank { "Shakir Tamboli" })
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Approve Payroll", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(6.dp), color = BmsSuccess.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Approved by: ${record.approvedBy} on ${record.approvalDate}",
                                        fontSize = 11.sp,
                                        color = BmsSuccess,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                BmsSectionHeader(title = "Attendance Activity Summary")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Logged Attendance Records: ${attendance.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Present: ${attendance.count { it.status == "Present" }} • Half-Day: ${attendance.count { it.status == "Half-Day" || it.status == "Half Day" }} • Absent: ${attendance.count { it.status == "Absent" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "Weekly Off: ${attendance.count { it.status == "Weekly Off" }} • Holidays: ${attendance.count { it.status == "Holiday" }} • Leaves: ${attendance.count { it.status == "Leave" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            // Tab 2: Tenant Workspaces
            item {
                BmsSectionHeader(title = "Multi-Tenant Fleet Isolation")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    allWorkspaces.forEach { ws ->
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
                                    Text(text = ws.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    StatusBadge(text = ws.planId)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Tenant ID: ${ws.id} • ${ws.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(text = "GSTIN: ${ws.gstNumber} • Phone: ${ws.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.showMessage("Workspace ${ws.name} verified and isolated.") },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Inspect Tenant", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Assign Roles & Responsibilities Individually
    if (editingEmployeeRole != null) {
        AssignRoleResponsibilityDialog(
            employee = editingEmployeeRole!!,
            onDismiss = { editingEmployeeRole = null },
            onSave = { updatedRole, updatedDesig, updatedDept, updatedResp, updatedPerms, updatedSalary ->
                viewModel.updateEmployeeRoleAndResponsibilities(
                    employeeId = editingEmployeeRole!!.id,
                    newRole = updatedRole,
                    newDesignation = updatedDesig,
                    newDepartment = updatedDept,
                    newResponsibilities = updatedResp,
                    newAssignedPermissions = updatedPerms,
                    newSalary = updatedSalary
                )
                editingEmployeeRole = null
            }
        )
    }

    // Dialog: Comprehensive Employee Management & Login Info
    if (fullEditingEmployee != null) {
        FullEditEmployeeAdminDialog(
            employee = fullEditingEmployee!!,
            onDismiss = { fullEditingEmployee = null },
            onSave = { updatedEmp ->
                viewModel.updateEmployeeAdminFull(
                    employeeId = updatedEmp.id,
                    name = updatedEmp.name,
                    mobile = updatedEmp.mobile,
                    email = updatedEmp.email,
                    department = updatedEmp.department,
                    designation = updatedEmp.designation,
                    role = updatedEmp.role,
                    basicSalary = updatedEmp.basicSalary,
                    status = updatedEmp.status,
                    loginUsername = updatedEmp.loginUsername,
                    loginPassword = updatedEmp.loginPassword,
                    responsibilities = updatedEmp.responsibilities,
                    assignedPermissions = updatedEmp.assignedPermissions
                )
                fullEditingEmployee = null
            },
            onDelete = { emp ->
                viewModel.deleteEmployee(emp.id, emp.name)
                fullEditingEmployee = null
            }
        )
    }

    // Dialog: Onboard New Employee with Login Information
    if (showAddEmployeeDialog) {
        AddEmployeeAdminDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onAdd = { name, mobile, email, department, designation, role, salary, username, password, responsibilities, permissions ->
                viewModel.addEmployeeAdmin(
                    name = name,
                    mobile = mobile,
                    email = email,
                    department = department,
                    designation = designation,
                    role = role,
                    salary = salary,
                    loginUsername = username,
                    loginPassword = password,
                    responsibilities = responsibilities,
                    assignedPermissions = permissions
                )
                showAddEmployeeDialog = false
            }
        )
    }

    // Dialog: Admin Assign Task to Employee
    if (taskAssignTargetEmployee != null) {
        AssignTaskAdminDialog(
            targetEmployee = taskAssignTargetEmployee!!,
            employees = employees,
            onDismiss = { taskAssignTargetEmployee = null },
            onAssign = { title, description, assignedTo, projectName, priority, deadline, dueTime ->
                viewModel.assignNewTask(
                    title = title,
                    description = description,
                    assignedTo = assignedTo,
                    projectName = projectName,
                    priority = priority,
                    deadline = deadline,
                    dueTime = dueTime
                )
                taskAssignTargetEmployee = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignRoleResponsibilityDialog(
    employee: EmployeeEntity,
    onDismiss: () -> Unit,
    onSave: (role: String, designation: String, department: String, responsibilities: String, permissions: String, salary: Double) -> Unit
) {
    var role by remember { mutableStateOf(employee.role) }
    var designation by remember { mutableStateOf(employee.designation) }
    var department by remember { mutableStateOf(employee.department) }
    var responsibilities by remember { mutableStateOf(employee.responsibilities) }
    var salaryText by remember { mutableStateOf(employee.basicSalary.toInt().toString()) }

    // Pre-selected modules
    val allModules = listOf("WORKFLOWS", "TASKS", "STORAGE", "BOOKINGS", "QUOTATIONS", "FINANCE", "LEADS", "REPORTS")
    val initialSelected = remember {
        if (employee.assignedPermissions.isBlank() || employee.assignedPermissions == "ALL") {
            mutableStateListOf("WORKFLOWS", "TASKS", "STORAGE")
        } else {
            mutableStateListOf<String>().apply {
                addAll(employee.assignedPermissions.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() })
            }
        }
    }

    val rolePresets = listOf("Photo Editor", "Video Editor", "Album Designer", "Photographer", "Videographer", "Studio Manager", "Accountant")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Assign Role & Responsibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${employee.name} (${employee.employeeCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("Preset Role Title:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rolePresets.take(3).forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = {
                                    role = r
                                    if (r.contains("Photo")) designation = "Senior Colorist & Photo Editor"
                                    if (r.contains("Video")) designation = "Senior Video & Premiere Editor"
                                    if (r.contains("Album")) designation = "Couture Album Designer"
                                },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rolePresets.drop(3).take(3).forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("Exact Designation Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department (Editing, Photography, Accounts, etc.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = responsibilities,
                        onValueChange = { responsibilities = it },
                        label = { Text("Individual Duties & Responsibilities") },
                        supportingText = { Text("Describe specific tasks, cameras, tools or editing scope") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Assigned Module Scopes (Permissions):", style = MaterialTheme.typography.labelSmall)
                    Text("Staff can only view these specific modules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allModules.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { mod ->
                                    val isSelected = initialSelected.contains(mod)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) initialSelected.remove(mod) else initialSelected.add(mod)
                                        },
                                        label = { Text(mod, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = { Text("Basic Salary / Month (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val salary = salaryText.toDoubleOrNull() ?: employee.basicSalary
                    val perms = if (initialSelected.isEmpty()) "WORKFLOWS,TASKS" else initialSelected.joinToString(",")
                    onSave(role, designation, department, responsibilities, perms, salary)
                }
            ) {
                Text("Save Role & Responsibilities")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FullEditEmployeeAdminDialog(
    employee: EmployeeEntity,
    onDismiss: () -> Unit,
    onSave: (EmployeeEntity) -> Unit,
    onDelete: (EmployeeEntity) -> Unit
) {
    var name by remember { mutableStateOf(employee.name) }
    var mobile by remember { mutableStateOf(employee.mobile) }
    var email by remember { mutableStateOf(employee.email) }
    var department by remember { mutableStateOf(employee.department) }
    var designation by remember { mutableStateOf(employee.designation) }
    var role by remember { mutableStateOf(employee.role) }
    var status by remember { mutableStateOf(employee.status) }
    var salaryStr by remember { mutableStateOf(employee.basicSalary.toInt().toString()) }
    var loginUsername by remember { mutableStateOf(if (employee.loginUsername.isNotBlank()) employee.loginUsername else employee.name.lowercase().replace(" ", "")) }
    var loginPassword by remember { mutableStateOf(if (employee.loginPassword.isNotBlank()) employee.loginPassword else "employee123") }
    var responsibilities by remember { mutableStateOf(employee.responsibilities) }

    val allModules = listOf("DASHBOARD", "TASKS", "WORKFLOWS", "STORAGE", "BOOKINGS", "QUOTATIONS", "FINANCE", "LEADS", "REPORTS")
    val initialSelected = remember {
        mutableStateListOf<String>().apply {
            if (employee.assignedPermissions.isBlank() || employee.assignedPermissions == "ALL") {
                addAll(listOf("DASHBOARD", "TASKS", "WORKFLOWS", "STORAGE"))
            } else {
                addAll(employee.assignedPermissions.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Manage Employee & Login Info", fontWeight = FontWeight.Bold)
                Text("Changes sync across dashboard, tasks, records & reports", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Employee Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_edit_emp_name")
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("Designation") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role (Admin/Manager/Employee)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = status,
                            onValueChange = { status = it },
                            label = { Text("Status (Active/Inactive)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Login Information Management", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = loginUsername,
                                onValueChange = { loginUsername = it },
                                label = { Text("Login Username / Email ID *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Login Password *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Contact *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = salaryStr,
                        onValueChange = { salaryStr = it },
                        label = { Text("Basic Salary / Month (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = responsibilities,
                        onValueChange = { responsibilities = it },
                        label = { Text("Scope & Responsibilities") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Module Scopes / Permissions:", style = MaterialTheme.typography.labelSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allModules.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { mod ->
                                    val isSelected = initialSelected.contains(mod)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) initialSelected.remove(mod) else initialSelected.add(mod)
                                        },
                                        label = { Text(mod, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { onDelete(employee) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove / Deactivate Employee Record")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val salary = salaryStr.toDoubleOrNull() ?: employee.basicSalary
                    val perms = if (initialSelected.isEmpty()) "ALL" else initialSelected.joinToString(",")
                    onSave(
                        employee.copy(
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            department = department.trim(),
                            designation = designation.trim(),
                            role = role.trim(),
                            basicSalary = salary,
                            status = status.trim(),
                            loginUsername = loginUsername.trim(),
                            loginPassword = loginPassword.trim(),
                            responsibilities = responsibilities.trim(),
                            assignedPermissions = perms
                        )
                    )
                },
                enabled = name.isNotBlank() && mobile.isNotBlank() && loginUsername.isNotBlank()
            ) {
                Text("Save & Sync Everywhere")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEmployeeAdminDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, mobile: String, email: String, department: String, designation: String, role: String, salary: Double, username: String, password: String, responsibilities: String, permissions: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Photography") }
    var designation by remember { mutableStateOf("Lead Cinematographer & Editor") }
    var role by remember { mutableStateOf("Employee") }
    var salaryStr by remember { mutableStateOf("45000") }
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("emp123") }
    var responsibilities by remember { mutableStateOf("Shoots, Multi-cam Editing & Studio Operations") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add New Employee", fontWeight = FontWeight.Bold)
                Text("Creates employee record with role & login access", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (loginUsername.isBlank()) loginUsername = it.lowercase().replace(" ", "")
                            if (email.isBlank()) email = "${it.lowercase().replace(" ", "")}@bms.com"
                        },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("Designation") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role (Admin/Manager/Employee)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salaryStr,
                            onValueChange = { salaryStr = it },
                            label = { Text("Monthly Salary (₹)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Initial Login Credentials", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = loginUsername,
                                onValueChange = { loginUsername = it },
                                label = { Text("Login Username *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Initial Password *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Phone Number *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = responsibilities,
                        onValueChange = { responsibilities = it },
                        label = { Text("Assigned Responsibilities") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val salary = salaryStr.toDoubleOrNull() ?: 45000.0
                    onAdd(name, mobile, email, department, designation, role, salary, loginUsername, loginPassword, responsibilities, "ALL")
                },
                enabled = name.isNotBlank() && mobile.isNotBlank() && loginUsername.isNotBlank()
            ) {
                Text("Create Employee")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AssignTaskAdminDialog(
    targetEmployee: EmployeeEntity,
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onAssign: (title: String, description: String, assignedTo: String, projectName: String, priority: String, deadline: String, dueTime: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf(targetEmployee.name) }
    var projectName by remember { mutableStateOf("Studio Operations") }
    var priority by remember { mutableStateOf("High") }
    var deadline by remember { mutableStateOf("10 Oct 2026") }
    var dueTime by remember { mutableStateOf("06:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Task to Staff") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Name *") },
                        placeholder = { Text("e.g. 4K Color Grade & Export Teaser") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Task Instructions / Details") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = assignedTo,
                        onValueChange = { assignedTo = it },
                        label = { Text("Assigned To (Employee Name) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Associated Project / Client") },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                    onAssign(title, description, assignedTo, projectName, priority, deadline, dueTime)
                },
                enabled = title.isNotBlank() && assignedTo.isNotBlank()
            ) {
                Text("Assign Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
