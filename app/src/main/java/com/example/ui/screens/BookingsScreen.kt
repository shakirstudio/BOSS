package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BookingEntity
import com.example.data.model.ShootEntity
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun BookingsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val bookings by viewModel.visibleBookings.collectAsState()
    val shoots by viewModel.visibleShoots.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isUserAdminOrOwner by viewModel.isUserAdminOrOwner.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Bookings, 1: Shoot Checklists
    var showAddBookingDialog by remember { mutableStateOf(false) }
    var selectedShootForChecklist by remember { mutableStateOf<ShootEntity?>(null) }
    var editingBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var editingShoot by remember { mutableStateOf<ShootEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBookingDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_booking_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Booking")
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
                            text = "Assigned View: $currentUserName ($currentUserRole) • Only your assigned shoots & bookings",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Confirmed Bookings (${bookings.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Shoots & Checklists (${shoots.size})") }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookings, key = { it.id }) { booking ->
                        BookingCard(
                            booking = booking,
                            onEdit = { editingBooking = booking }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shoots, key = { it.id }) { shoot ->
                        ShootCard(
                            shoot = shoot,
                            onEdit = { editingShoot = shoot },
                            onOpenChecklist = { selectedShootForChecklist = shoot },
                            onToggleBackup = {
                                viewModel.updateShootStatus(shoot, shoot.status, !shoot.dataBackupDone)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddBookingDialog) {
        AddBookingDialog(
            onDismiss = { showAddBookingDialog = false },
            onAdd = { newBooking ->
                viewModel.addBooking(newBooking)
                showAddBookingDialog = false
            }
        )
    }

    editingBooking?.let { bookingToEdit ->
        EditBookingDialog(
            booking = bookingToEdit,
            onDismiss = { editingBooking = null },
            onSave = { updated ->
                viewModel.updateBooking(updated)
                editingBooking = null
            }
        )
    }

    editingShoot?.let { shootToEdit ->
        EditShootDialog(
            shoot = shootToEdit,
            onDismiss = { editingShoot = null },
            onSave = { updated ->
                viewModel.updateShoot(updated)
                editingShoot = null
            }
        )
    }

    selectedShootForChecklist?.let { shoot ->
        ShootChecklistDialog(
            shoot = shoot,
            onDismiss = { selectedShootForChecklist = null },
            onSave = { updatedStatus ->
                viewModel.updateShootStatus(shoot, updatedStatus)
                selectedShootForChecklist = null
            }
        )
    }
}

@Composable
fun BookingCard(
    booking: BookingEntity,
    onEdit: () -> Unit
) {
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
                Column {
                    Text(
                        text = booking.bookingNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = booking.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = booking.status)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Booking",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (booking.hasConflict) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Double-booking alert: Another shoot on same date/venue!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${booking.eventType} • ${booking.eventDate} @ ${booking.eventTime}",
                style = MaterialTheme.typography.bodySmall,
                color = BmsPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Venue: ${booking.venue}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Assigned Crew: ${booking.assignedTeam}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = "Total Contract", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "₹${booking.totalAmount.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        Text(text = "Advance Received", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "₹${booking.advancePaid.toInt()}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Balance Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "₹${booking.balanceAmount.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ShootCard(
    shoot: ShootEntity,
    onEdit: () -> Unit,
    onOpenChecklist: () -> Unit,
    onToggleBackup: () -> Unit
) {
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
                Column {
                    Text(text = shoot.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${shoot.eventType} • Call Time: ${shoot.callTime}", style = MaterialTheme.typography.bodySmall, color = BmsPrimaryDark, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = shoot.status)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Shoot Details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Venue: ${shoot.venue}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Gear: ${shoot.equipmentList}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Data Backup indicator & switch
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = shoot.dataBackupDone,
                        onCheckedChange = { onToggleBackup() }
                    )
                    Text(
                        text = if (shoot.dataBackupDone) "Backup Complete ✓" else "Backup Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shoot.dataBackupDone) BmsSuccess else BmsWarning,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onOpenChecklist,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Checklist", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ShootChecklistDialog(
    shoot: ShootEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val checklistItems = remember {
        mutableStateMapOf(
            "Primary Camera Bodies (Sony A7SIII / FX3)" to true,
            "Lenses (24-70mm GM, 70-200mm GM, 50mm f1.2)" to true,
            "Formatted SD V90 / CFexpress Memory Cards" to true,
            "Charged Batteries (x6) + Dual Wall Charger" to true,
            "Gimbal Balanced (DJI Ronin RS3 Pro)" to true,
            "Flash Lights + Godox Trigger & Diffuser" to false,
            "Wireless Audio Transmitters & Lavalier Mics" to false,
            "Heavy Duty Light Stands & Tripods" to true,
            "Emergency Backup Camera & Spare Card" to true
        )
    }

    var selectedStatus by remember { mutableStateOf(shoot.status) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = "Gear & Shoot Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = shoot.clientName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(checklistItems.keys.toList()) { item ->
                        val isChecked = checklistItems[item] == true
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checklistItems[item] = it }
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Planned", "Assigned", "Shooting", "Data Backup", "Completed").forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onSave(selectedStatus) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Checklist & Status")
                }
            }
        }
    }
}

@Composable
fun AddBookingDialog(
    onDismiss: () -> Unit,
    onAdd: (BookingEntity) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Wedding") }
    var eventDate by remember { mutableStateOf("20 Nov 2026") }
    var eventTime by remember { mutableStateOf("09:00 AM") }
    var venue by remember { mutableStateOf("JW Marriott Pune") }
    var totalAmountStr by remember { mutableStateOf("250000") }
    var advancePaidStr by remember { mutableStateOf("100000") }
    var assignedTeam by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Studio Booking") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventType,
                    onValueChange = { eventType = it },
                    label = { Text("Event Type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = eventTime,
                        onValueChange = { eventTime = it },
                        label = { Text("Call Time") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Location *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalAmountStr,
                        onValueChange = { totalAmountStr = it },
                        label = { Text("Total Contract (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = advancePaidStr,
                        onValueChange = { advancePaidStr = it },
                        label = { Text("Advance (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = assignedTeam,
                    onValueChange = { assignedTeam = it },
                    label = { Text("Assigned Team Members") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank() && venue.isNotBlank()) {
                        val total = totalAmountStr.toDoubleOrNull() ?: 200000.0
                        val advance = advancePaidStr.toDoubleOrNull() ?: 50000.0
                        val bookingNumber = "BK-2026-" + (100..999).random()
                        val newBooking = BookingEntity(
                            id = "book_" + System.currentTimeMillis().toString().takeLast(6),
                            workspaceId = "",
                            bookingNumber = bookingNumber,
                            clientId = "client_" + System.currentTimeMillis().toString().takeLast(4),
                            clientName = clientName.trim(),
                            eventType = eventType.trim(),
                            eventDate = eventDate.trim(),
                            eventTime = eventTime.trim(),
                            venue = venue.trim(),
                            packageName = "Custom Studio Package",
                            totalAmount = total,
                            advancePaid = advance,
                            balanceAmount = total - advance,
                            assignedTeam = assignedTeam.trim(),
                            status = "Confirmed"
                        )
                        onAdd(newBooking)
                    }
                },
                enabled = clientName.isNotBlank() && venue.isNotBlank()
            ) {
                Text("Confirm Booking")
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
fun EditBookingDialog(
    booking: BookingEntity,
    onDismiss: () -> Unit,
    onSave: (BookingEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(booking.clientName) }
    var eventType by remember { mutableStateOf(booking.eventType) }
    var eventDate by remember { mutableStateOf(booking.eventDate) }
    var eventTime by remember { mutableStateOf(booking.eventTime) }
    var venue by remember { mutableStateOf(booking.venue) }
    var packageName by remember { mutableStateOf(booking.packageName) }
    var totalAmountStr by remember { mutableStateOf(booking.totalAmount.toInt().toString()) }
    var advancePaidStr by remember { mutableStateOf(booking.advancePaid.toInt().toString()) }
    var assignedTeam by remember { mutableStateOf(booking.assignedTeam) }
    var status by remember { mutableStateOf(booking.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Booking ${booking.bookingNumber}") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = eventType,
                        onValueChange = { eventType = it },
                        label = { Text("Event Type (Wedding, Reception, etc.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = { eventDate = it },
                            label = { Text("Shoot Date") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = eventTime,
                            onValueChange = { eventTime = it },
                            label = { Text("Call Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Venue / Location *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalAmountStr,
                            onValueChange = { totalAmountStr = it },
                            label = { Text("Total (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = advancePaidStr,
                            onValueChange = { advancePaidStr = it },
                            label = { Text("Advance (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = assignedTeam,
                        onValueChange = { assignedTeam = it },
                        label = { Text("Assigned Team / Crew") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Booking Status (Confirmed, Completed, Cancelled)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank() && venue.isNotBlank()) {
                        val total = totalAmountStr.toDoubleOrNull() ?: booking.totalAmount
                        val advance = advancePaidStr.toDoubleOrNull() ?: booking.advancePaid
                        val updated = booking.copy(
                            clientName = clientName.trim(),
                            eventType = eventType.trim(),
                            eventDate = eventDate.trim(),
                            eventTime = eventTime.trim(),
                            venue = venue.trim(),
                            packageName = packageName.trim(),
                            totalAmount = total,
                            advancePaid = advance,
                            balanceAmount = (total - advance).coerceAtLeast(0.0),
                            assignedTeam = assignedTeam.trim(),
                            status = status.trim()
                        )
                        onSave(updated)
                    }
                },
                enabled = clientName.isNotBlank() && venue.isNotBlank()
            ) {
                Text("Save Changes")
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
fun EditShootDialog(
    shoot: ShootEntity,
    onDismiss: () -> Unit,
    onSave: (ShootEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(shoot.clientName) }
    var eventType by remember { mutableStateOf(shoot.eventType) }
    var shootDate by remember { mutableStateOf(shoot.shootDate) }
    var callTime by remember { mutableStateOf(shoot.callTime) }
    var venue by remember { mutableStateOf(shoot.venue) }
    var leadPhotographer by remember { mutableStateOf(shoot.leadPhotographer) }
    var videographer by remember { mutableStateOf(shoot.videographer) }
    var equipmentList by remember { mutableStateOf(shoot.equipmentList) }
    var status by remember { mutableStateOf(shoot.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Shoot Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = eventType,
                        onValueChange = { eventType = it },
                        label = { Text("Shoot Type (Fashion, Wedding, etc.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shootDate,
                            onValueChange = { shootDate = it },
                            label = { Text("Shoot Date") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = callTime,
                            onValueChange = { callTime = it },
                            label = { Text("Call Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Shoot Location / Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = leadPhotographer,
                        onValueChange = { leadPhotographer = it },
                        label = { Text("Lead Photographer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = videographer,
                        onValueChange = { videographer = it },
                        label = { Text("Videographer / Cinematographer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = equipmentList,
                        onValueChange = { equipmentList = it },
                        label = { Text("Camera & Lens Kit / Gear") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Shoot Status (Assigned, In Progress, Completed)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        val updated = shoot.copy(
                            clientName = clientName.trim(),
                            eventType = eventType.trim(),
                            shootDate = shootDate.trim(),
                            callTime = callTime.trim(),
                            venue = venue.trim(),
                            leadPhotographer = leadPhotographer.trim(),
                            videographer = videographer.trim(),
                            equipmentList = equipmentList.trim(),
                            status = status.trim()
                        )
                        onSave(updated)
                    }
                },
                enabled = clientName.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
