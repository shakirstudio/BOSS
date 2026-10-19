package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BmsPrimaryDark
import com.example.ui.theme.BmsTertiary
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun SettingsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val workspace by viewModel.workspace.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()

    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }
    var showEditStudioDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Studio & System Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Workspace configuration, profile, and multi-tenancy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Studio Info Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = workspace?.name ?: "BMS Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Tagline: ${workspace?.tagline ?: "Manage. Track. Grow."}", style = MaterialTheme.typography.bodySmall, color = BmsPrimaryDark)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge(text = workspace?.planId ?: "STUDIO")
                            IconButton(
                                onClick = { showEditStudioDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Studio Name & Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Address: ${workspace?.address ?: "Penthouse 4B, Pune"}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "GSTIN: ${workspace?.gstNumber ?: "27AAACL1234F1Z8"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = "Contact: ${workspace?.phone ?: "+91 98234 56789"} • ${workspace?.email ?: "studio@bms.in"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showEditStudioDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Studio Name & Info")
                    }
                }
            }
        }

        // Multi-Tenant Workspaces Switcher
        item {
            BmsSectionHeader(
                title = "Switch or Create Workspace",
                subtitle = "Tenant Data Isolation",
                actionText = "+ New Studio",
                onActionClick = { showCreateWorkspaceDialog = true }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allWorkspaces.forEach { ws ->
                    val isSelected = ws.id == workspace?.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.switchWorkspace(ws.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(text = ws.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${ws.city} • ${ws.businessType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                StatusBadge(text = "ACTIVE WORKSPACE")
                            } else {
                                Text("Switch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Current User Profile
        item {
            BmsSectionHeader(title = "Logged In Profile")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = currentUser?.name ?: "Studio Administrator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(text = "Role: ${currentUser?.role ?: "Owner / Admin"}", style = MaterialTheme.typography.bodySmall, color = BmsTertiary)
                    Text(text = "Email: ${currentUser?.email ?: "admin@bms.com"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = "Auth Provider: ${currentUser?.loginMethod ?: "GOOGLE OAuth"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Appearance / Theme Mode
        item {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            BmsSectionHeader(title = "App Appearance & Interface", subtitle = "Switch between Dark and Light mode")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDarkMode) "Dark Mode (Active)" else "Light Mode (Active)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (isDarkMode) "Sleek contrast for editing & dark studios" else "High contrast clean daylight mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            }
        }

        // Cloud Backend & Data Synchronization
        item {
            val syncStatus by viewModel.syncStatus.collectAsState()
            BmsSectionHeader(
                title = "Firebase Cloud Backend & Synchronization",
                subtitle = "Shared real-time sync with Admin Web Portal & multi-device ledger"
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (syncStatus.errorMessage == null) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Firebase Cloud Firestore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Project: gen-lang-client-0601048347", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        StatusBadge(text = if (syncStatus.isSyncing) "SYNCING..." else "ONLINE / SECURED")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Records Synced: ${syncStatus.syncedRecordsCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (syncStatus.lastSyncTime != null) "Last sync: Just now" else "Offline-first Room cache active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            enabled = !syncStatus.isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_sync_button")
                        ) {
                            Icon(
                                imageVector = if (syncStatus.isSyncing) Icons.Default.Sync else Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (syncStatus.isSyncing) "Syncing..." else "Sync Now", fontSize = 12.sp)
                        }
                    }

                    if (syncStatus.errorMessage != null) {
                        Text(
                            text = "Status notice: ${syncStatus.errorMessage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Account Security & Credentials Management
        item {
            BmsSectionHeader(
                title = "Account Security & Credentials",
                subtitle = "Manage administrator login credentials and security settings"
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Administrator Credentials", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Account: ${currentUser?.name ?: "Shakir Tamboli"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        StatusBadge(text = "ACTIVE")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Update login password anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Password", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCreateWorkspaceDialog) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateWorkspaceDialog = false },
            onCreate = { name, type, city, phone, email, plan ->
                viewModel.createWorkspace(name, type, city, phone, email, plan)
                showCreateWorkspaceDialog = false
            }
        )
    }

    if (showEditStudioDialog && workspace != null) {
        EditStudioDialog(
            workspace = workspace!!,
            onDismiss = { showEditStudioDialog = false },
            onSave = { updated ->
                viewModel.updateWorkspace(updated)
                showEditStudioDialog = false
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onUpdatePassword = { newPass, callback ->
                viewModel.updateAdminPassword(newPass) { success, msg ->
                    callback(success, msg)
                    if (success) {
                        showChangePasswordDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun EditStudioDialog(
    workspace: com.example.data.model.WorkspaceEntity,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.WorkspaceEntity) -> Unit
) {
    var name by remember { mutableStateOf(workspace.name) }
    var tagline by remember { mutableStateOf(workspace.tagline) }
    var businessType by remember { mutableStateOf(workspace.businessType) }
    var city by remember { mutableStateOf(workspace.city) }
    var phone by remember { mutableStateOf(workspace.phone) }
    var email by remember { mutableStateOf(workspace.email) }
    var address by remember { mutableStateOf(workspace.address) }
    var gstNumber by remember { mutableStateOf(workspace.gstNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Studio Profile & Name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Studio Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Tagline / Motto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = businessType,
                    onValueChange = { businessType = it },
                    label = { Text("Business Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Studio Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gstNumber,
                    onValueChange = { gstNumber = it },
                    label = { Text("GST Number (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        workspace.copy(
                            name = name.trim(),
                            tagline = tagline.trim(),
                            businessType = businessType.trim(),
                            city = city.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            gstNumber = gstNumber.trim()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CreateWorkspaceDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Photography & Cinema") }
    var city by remember { mutableStateOf("Mumbai") }
    var phone by remember { mutableStateOf("+91 99887 76655") }
    var email by remember { mutableStateOf("studio@gmail.com") }
    var plan by remember { mutableStateOf("STUDIO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Onboard New Business Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business / Studio Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Business Type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Operating City *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Primary Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Studio Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plan, onValueChange = { plan = it }, label = { Text("Plan (FREE, PRO, STUDIO, BUSINESS)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, type, city, phone, email, plan) },
                enabled = name.isNotBlank() && city.isNotBlank()
            ) { Text("Create & Switch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onUpdatePassword: (String, (Boolean, String) -> Unit) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Change Password")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a new secure password (minimum 8 characters) for your administrator account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("New Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm New Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.length < 8) {
                        errorMessage = "Password must be at least 8 characters long."
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    isSubmitting = true
                    onUpdatePassword(newPassword) { success, msg ->
                        isSubmitting = false
                        if (!success) {
                            errorMessage = msg
                        }
                    }
                },
                enabled = !isSubmitting && newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text(if (isSubmitting) "Updating..." else "Update Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}
