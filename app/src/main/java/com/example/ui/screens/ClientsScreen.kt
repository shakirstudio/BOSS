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
import com.example.data.model.ClientEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.BmsPrimaryDark
import com.example.ui.theme.BmsSuccess
import com.example.ui.theme.BmsTertiary
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun ClientsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Client")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Client Portfolios",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${clients.size} active studio clients with complete lifecycle tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(clients, key = { it.id }) { client ->
                ClientCard(
                    client = client,
                    onEdit = { editingClient = client },
                    onContact = {
                        val msg = "Hello ${client.name},\nGreeting from BMS Studios! Regarding your project: ${client.eventType}."
                        copyToClipboard(context, "Client Message", msg) {
                            viewModel.showMessage("Copied message template for ${client.name}!")
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newClient ->
                viewModel.addClient(newClient)
                showAddDialog = false
            }
        )
    }

    editingClient?.let { clientToEdit ->
        EditClientDialog(
            client = clientToEdit,
            onDismiss = { editingClient = null },
            onSave = { updatedClient ->
                viewModel.updateClient(updatedClient)
                editingClient = null
            }
        )
    }
}

@Composable
fun ClientCard(
    client: ClientEntity,
    onEdit: () -> Unit,
    onContact: () -> Unit
) {
    val timelineStages = listOf("Lead", "Quotation", "Booking", "Event", "Editing", "Album", "Delivery")
    val currentStageIndex = timelineStages.indexOfFirst { it.equals(client.timelineStage, ignoreCase = true) }
        .let { if (it == -1) 3 else it }

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
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${client.eventType} • ${client.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BmsPrimaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = client.timelineStage)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Client Details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "₹${client.totalBookingValue.toInt()}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Paid Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "₹${client.paidAmount.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = BmsSuccess,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Balance Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "₹${client.pendingAmount.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = if (client.pendingAmount > 0) BmsTertiary else BmsSuccess,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Lifecycle Timeline
            Text(
                text = "Client Journey Timeline:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                timelineStages.forEachIndexed { index, stage ->
                    val isPastOrCurrent = index <= currentStageIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isPastOrCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                    if (index < timelineStages.size - 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${client.mobile} • ${client.email}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onContact) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Contact Client",
                        tint = BmsSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onAdd: (ClientEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Wedding") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Pune") }
    var totalValue by remember { mutableStateOf("250000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventType,
                    onValueChange = { eventType = it },
                    label = { Text("Primary Event Type") },
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
                    value = totalValue,
                    onValueChange = { totalValue = it },
                    label = { Text("Total Booking Contract Value (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && mobile.isNotBlank()) {
                        val tv = totalValue.toDoubleOrNull() ?: 200000.0
                        val newClient = ClientEntity(
                            id = "client_" + System.currentTimeMillis().toString().takeLast(6),
                            workspaceId = "",
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = if (email.isBlank()) "${name.lowercase().replace(" ", "")}@gmail.com" else email.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            eventType = eventType.trim(),
                            totalBookingValue = tv,
                            paidAmount = 0.0,
                            pendingAmount = tv,
                            timelineStage = "Booking"
                        )
                        onAdd(newClient)
                    }
                },
                enabled = name.isNotBlank() && mobile.isNotBlank()
            ) {
                Text("Register Client")
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
fun EditClientDialog(
    client: ClientEntity,
    onDismiss: () -> Unit,
    onSave: (ClientEntity) -> Unit
) {
    var name by remember { mutableStateOf(client.name) }
    var mobile by remember { mutableStateOf(client.mobile) }
    var email by remember { mutableStateOf(client.email) }
    var eventType by remember { mutableStateOf(client.eventType) }
    var address by remember { mutableStateOf(client.address) }
    var city by remember { mutableStateOf(client.city) }
    var totalValue by remember { mutableStateOf(client.totalBookingValue.toInt().toString()) }
    var paidAmount by remember { mutableStateOf(client.paidAmount.toInt().toString()) }
    var timelineStage by remember { mutableStateOf(client.timelineStage) }
    var notes by remember { mutableStateOf(client.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Client Profile") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Client Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = eventType,
                        onValueChange = { eventType = it },
                        label = { Text("Event Type (Wedding, Sangeet, Portfolio)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = totalValue,
                        onValueChange = { totalValue = it },
                        label = { Text("Total Contract Value (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = paidAmount,
                        onValueChange = { paidAmount = it },
                        label = { Text("Amount Paid So Far (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = timelineStage,
                        onValueChange = { timelineStage = it },
                        label = { Text("Stage (Lead, Quotation, Booking, Event, Editing, Album, Delivery)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Project Notes & Preferences") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val tv = totalValue.toDoubleOrNull() ?: client.totalBookingValue
                        val pa = paidAmount.toDoubleOrNull() ?: client.paidAmount
                        val pending = (tv - pa).coerceAtLeast(0.0)
                        val updated = client.copy(
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            eventType = eventType.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            totalBookingValue = tv,
                            paidAmount = pa,
                            pendingAmount = pending,
                            timelineStage = timelineStage.trim(),
                            notes = notes.trim()
                        )
                        onSave(updated)
                    }
                },
                enabled = name.isNotBlank()
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
