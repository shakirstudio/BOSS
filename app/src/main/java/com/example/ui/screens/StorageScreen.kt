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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectFileEntity
import com.example.data.model.StorageDriveEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.SystemIntents
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun StorageScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageDrives by viewModel.storageDrives.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Drives Inventory, 1: Project Files & Links
    var showAddDriveDialog by remember { mutableStateOf(false) }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var editingDrive by remember { mutableStateOf<StorageDriveEntity?>(null) }
    var editingFile by remember { mutableStateOf<ProjectFileEntity?>(null) }

    val totalCapacityGb = storageDrives.sumOf { it.totalCapacityGb }
    val totalFreeGb = storageDrives.sumOf { it.freeSpaceGb }
    val totalUsedGb = (totalCapacityGb - totalFreeGb).coerceAtLeast(0)
    val usedRatio = if (totalCapacityGb > 0) totalUsedGb.toFloat() / totalCapacityGb.toFloat() else 0f

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddDriveDialog = true else showAddFileDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("storage_fab")
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.AddHomeWork else Icons.Default.CloudUpload,
                    contentDescription = "Add Storage Asset"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage Health Summary Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Studio Hard Drive & Vault Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero Guesswork: Track RAW Footage, Dual Backups & Cloud Links",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${(totalUsedGb / 1000.0).let { String.format("%.1f", it) }} TB Used / ${(totalCapacityGb / 1000.0).let { String.format("%.1f", it) }} TB Total",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(usedRatio * 100).toInt()}% Allocated",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (usedRatio > 0.85f) BmsError else BmsPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { usedRatio },
                        color = if (usedRatio > 0.85f) BmsError else BmsTertiary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${storageDrives.size} Drives Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${(totalFreeGb / 1000.0).let { String.format("%.1f", it) }} TB Free Space Available",
                            style = MaterialTheme.typography.labelSmall,
                            color = BmsSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Hard Drives & NAS (${storageDrives.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Project Files & Links (${projectFiles.size})") }
                )
            }

            if (selectedTab == 0) {
                // Drives Tab
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(storageDrives, key = { it.id }) { drive ->
                        DriveCard(
                            drive = drive,
                            onEdit = { editingDrive = drive },
                            onDelete = { viewModel.deleteStorageDrive(drive) }
                        )
                    }
                }
            } else {
                // Project Files Tab
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projectFiles, key = { it.id }) { pfile ->
                        ProjectFileCard(
                            pfile = pfile,
                            onOpenUrl = { url -> SystemIntents.openExternalUrl(context, url) },
                            onEdit = { editingFile = pfile },
                            onDelete = { viewModel.deleteProjectFile(pfile) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDriveDialog) {
        AddDriveDialog(
            onDismiss = { showAddDriveDialog = false },
            onAdd = { newDrive ->
                viewModel.addStorageDrive(newDrive)
                showAddDriveDialog = false
            }
        )
    }

    if (showAddFileDialog) {
        AddProjectFileDialog(
            storageDrives = storageDrives,
            onDismiss = { showAddFileDialog = false },
            onAdd = { newFile ->
                viewModel.addProjectFile(newFile)
                showAddFileDialog = false
            }
        )
    }

    editingDrive?.let { driveToEdit ->
        EditDriveDialog(
            drive = driveToEdit,
            onDismiss = { editingDrive = null },
            onSave = { updated ->
                viewModel.updateStorageDrive(updated)
                editingDrive = null
            }
        )
    }

    editingFile?.let { fileToEdit ->
        EditProjectFileDialog(
            pfile = fileToEdit,
            storageDrives = storageDrives,
            onDismiss = { editingFile = null },
            onSave = { updated ->
                viewModel.updateProjectFile(updated)
                editingFile = null
            }
        )
    }
}

@Composable
fun DriveCard(
    drive: StorageDriveEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val usedGb = (drive.totalCapacityGb - drive.freeSpaceGb).coerceAtLeast(0)
    val usedRatio = if (drive.totalCapacityGb > 0) usedGb.toFloat() / drive.totalCapacityGb.toFloat() else 0f

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
                        imageVector = if (drive.driveType.contains("NAS", true)) Icons.Default.Dns else Icons.Default.Save,
                        contentDescription = null,
                        tint = BmsPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = drive.driveLabel,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = drive.driveModel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = drive.status)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Drive", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${drive.driveType} • ${drive.totalCapacityGb / 1000} TB",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${drive.freeSpaceGb} GB Free (${((1f - usedRatio) * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (drive.freeSpaceGb < 200) BmsError else BmsSuccess,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { usedRatio },
                color = if (usedRatio > 0.9f) BmsError else BmsSecondary,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Physical Location: ${drive.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (drive.assignedClients.isNotBlank()) {
                Text(
                    text = "Stored Projects: ${drive.assignedClients}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BmsTertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (drive.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = drive.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ProjectFileCard(
    pfile: ProjectFileEntity,
    onOpenUrl: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pfile.clientName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${pfile.eventType} • ${pfile.rawFootageGb.toInt()} GB RAW",
                        style = MaterialTheme.typography.bodySmall,
                        color = BmsPrimaryDark
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = pfile.status)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit File Links", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Column {
                        Text(text = "RAW HDD: ${pfile.rawDriveLabel}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Backup Vault: ${pfile.backupDriveLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (pfile.isDualBackupVerified) {
                        Surface(
                            color = BmsSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BmsSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dual Backup ✓", style = MaterialTheme.typography.labelSmall, color = BmsSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Drive & Cloud Links:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (pfile.googleDriveRawLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onOpenUrl(pfile.googleDriveRawLink) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp), tint = BmsPrimaryDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Google Drive RAW", fontSize = 11.sp)
                    }
                }

                if (pfile.clientGalleryLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onOpenUrl(pfile.clientGalleryLink) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp), tint = BmsSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Client Gallery", fontSize = 11.sp)
                    }
                }

                if (pfile.finalDeliverablesLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onOpenUrl(pfile.finalDeliverablesLink) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = BmsTertiary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Masters Export", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddDriveDialog(
    onDismiss: () -> Unit,
    onAdd: (StorageDriveEntity) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var capacityStr by remember { mutableStateOf("4000") }
    var freeStr by remember { mutableStateOf("2000") }
    var type by remember { mutableStateOf("External Rugged HDD") }
    var location by remember { mutableStateOf("Shelf A - Locker 1") }
    var assignedClients by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Hard Drive / NAS Storage") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Drive Tag / Label * (e.g. HDD-ORANGE-01)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Hardware Model * (e.g. LaCie Rugged 4TB)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = capacityStr, onValueChange = { capacityStr = it }, label = { Text("Total GB") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = freeStr, onValueChange = { freeStr = it }, label = { Text("Free GB") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Drive Type (External HDD, Portable SSD, Studio NAS)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Physical Location (Shelf, Bay, Locker)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = assignedClients, onValueChange = { assignedClients = it }, label = { Text("Assigned Clients / Projects") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Drive Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && model.isNotBlank()) {
                        val cap = capacityStr.toIntOrNull() ?: 4000
                        val free = freeStr.toIntOrNull() ?: 2000
                        val newDrive = StorageDriveEntity(
                            id = "drive_" + System.currentTimeMillis().toString().takeLast(6),
                            workspaceId = "",
                            driveLabel = label.trim().uppercase(),
                            driveModel = model.trim(),
                            totalCapacityGb = cap,
                            freeSpaceGb = free,
                            driveType = type.trim(),
                            location = location.trim(),
                            assignedClients = assignedClients.trim(),
                            notes = notes.trim()
                        )
                        onAdd(newDrive)
                    }
                },
                enabled = label.isNotBlank() && model.isNotBlank()
            ) { Text("Register Drive") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditDriveDialog(
    drive: StorageDriveEntity,
    onDismiss: () -> Unit,
    onSave: (StorageDriveEntity) -> Unit
) {
    var label by remember { mutableStateOf(drive.driveLabel) }
    var model by remember { mutableStateOf(drive.driveModel) }
    var capacityStr by remember { mutableStateOf(drive.totalCapacityGb.toString()) }
    var freeStr by remember { mutableStateOf(drive.freeSpaceGb.toString()) }
    var type by remember { mutableStateOf(drive.driveType) }
    var location by remember { mutableStateOf(drive.location) }
    var assignedClients by remember { mutableStateOf(drive.assignedClients) }
    var status by remember { mutableStateOf(drive.status) }
    var notes by remember { mutableStateOf(drive.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Storage Drive ${drive.driveLabel}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Drive Label *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Hardware Model *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = capacityStr, onValueChange = { capacityStr = it }, label = { Text("Total GB") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = freeStr, onValueChange = { freeStr = it }, label = { Text("Free GB") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Physical Location") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Active, Full / Archived, In Transit)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = assignedClients, onValueChange = { assignedClients = it }, label = { Text("Assigned Projects / Clients") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacityStr.toIntOrNull() ?: drive.totalCapacityGb
                    val free = freeStr.toIntOrNull() ?: drive.freeSpaceGb
                    onSave(drive.copy(driveLabel = label.trim().uppercase(), driveModel = model.trim(), totalCapacityGb = cap, freeSpaceGb = free, driveType = type.trim(), location = location.trim(), status = status.trim(), assignedClients = assignedClients.trim(), notes = notes.trim()))
                },
                enabled = label.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddProjectFileDialog(
    storageDrives: List<StorageDriveEntity>,
    onDismiss: () -> Unit,
    onAdd: (ProjectFileEntity) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Wedding") }
    var rawDrive by remember { mutableStateOf(storageDrives.firstOrNull()?.driveLabel ?: "HDD-01") }
    var backupDrive by remember { mutableStateOf("NAS-STUDIO-01") }
    var gdriveRaw by remember { mutableStateOf("") }
    var clientGallery by remember { mutableStateOf("") }
    var finalDeliverables by remember { mutableStateOf("") }
    var footageGbStr by remember { mutableStateOf("250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Project Files & Drive Folders") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = eventType, onValueChange = { eventType = it }, label = { Text("Event Type") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = rawDrive, onValueChange = { rawDrive = it }, label = { Text("RAW HDD") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = backupDrive, onValueChange = { backupDrive = it }, label = { Text("Backup NAS") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = footageGbStr, onValueChange = { footageGbStr = it }, label = { Text("RAW Footage Size (GB)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = gdriveRaw, onValueChange = { gdriveRaw = it }, label = { Text("Google Drive RAW Folder Link") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = clientGallery, onValueChange = { clientGallery = it }, label = { Text("Online Client Gallery Link") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = finalDeliverables, onValueChange = { finalDeliverables = it }, label = { Text("Final Master Exports Drive Link") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        val gb = footageGbStr.toDoubleOrNull() ?: 250.0
                        val newFile = ProjectFileEntity(
                            id = "pfile_" + System.currentTimeMillis().toString().takeLast(6),
                            workspaceId = "",
                            clientName = clientName.trim(),
                            eventType = eventType.trim(),
                            projectFolder = "2026/${clientName.replace(" ", "_")}",
                            rawDriveLabel = rawDrive.trim().uppercase(),
                            backupDriveLabel = backupDrive.trim().uppercase(),
                            googleDriveRawLink = gdriveRaw.trim(),
                            clientGalleryLink = clientGallery.trim(),
                            finalDeliverablesLink = finalDeliverables.trim(),
                            isDualBackupVerified = true,
                            rawFootageGb = gb,
                            status = "RAW Ingested"
                        )
                        onAdd(newFile)
                    }
                },
                enabled = clientName.isNotBlank()
            ) { Text("Save Project Links") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditProjectFileDialog(
    pfile: ProjectFileEntity,
    storageDrives: List<StorageDriveEntity>,
    onDismiss: () -> Unit,
    onSave: (ProjectFileEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(pfile.clientName) }
    var eventType by remember { mutableStateOf(pfile.eventType) }
    var rawDrive by remember { mutableStateOf(pfile.rawDriveLabel) }
    var backupDrive by remember { mutableStateOf(pfile.backupDriveLabel) }
    var gdriveRaw by remember { mutableStateOf(pfile.googleDriveRawLink) }
    var clientGallery by remember { mutableStateOf(pfile.clientGalleryLink) }
    var finalDeliverables by remember { mutableStateOf(pfile.finalDeliverablesLink) }
    var status by remember { mutableStateOf(pfile.status) }
    var isDualBackup by remember { mutableStateOf(pfile.isDualBackupVerified) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Project Files & Links") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = eventType, onValueChange = { eventType = it }, label = { Text("Event Type") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = rawDrive, onValueChange = { rawDrive = it }, label = { Text("RAW HDD") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = backupDrive, onValueChange = { backupDrive = it }, label = { Text("Backup NAS") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (RAW Ingested, In Editing, Ready for Delivery, Delivered)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = gdriveRaw, onValueChange = { gdriveRaw = it }, label = { Text("Google Drive RAW Folder Link") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = clientGallery, onValueChange = { clientGallery = it }, label = { Text("Online Client Gallery Link") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = finalDeliverables, onValueChange = { finalDeliverables = it }, label = { Text("Final Master Exports Drive Link") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDualBackup, onCheckedChange = { isDualBackup = it })
                        Text("Dual Redundant Backup Verified ✓", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BmsSuccess)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(pfile.copy(clientName = clientName.trim(), eventType = eventType.trim(), rawDriveLabel = rawDrive.trim().uppercase(), backupDriveLabel = backupDrive.trim().uppercase(), googleDriveRawLink = gdriveRaw.trim(), clientGalleryLink = clientGallery.trim(), finalDeliverablesLink = finalDeliverables.trim(), status = status.trim(), isDualBackupVerified = isDualBackup))
                },
                enabled = clientName.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
