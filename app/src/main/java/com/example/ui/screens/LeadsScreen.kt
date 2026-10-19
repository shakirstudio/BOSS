package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.BmsPrimaryDark
import com.example.ui.theme.BmsSuccess
import com.example.ui.theme.BmsTertiary
import com.example.ui.viewmodel.BmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leads by viewModel.visibleLeads.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLead by remember { mutableStateOf<LeadEntity?>(null) }

    val filterOptions = listOf("All", "New", "Follow-up", "Quotation Sent", "Negotiation", "Won", "Lost")
    val filteredLeads = remember(leads, selectedFilter) {
        if (selectedFilter == "All") leads else leads.filter { it.status.equals(selectedFilter, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_lead_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Lead")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { opt ->
                    FilterChip(
                        selected = selectedFilter == opt,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt) }
                    )
                }
            }

            if (filteredLeads.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No leads in '$selectedFilter' category",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLeads, key = { it.id }) { lead ->
                        LeadCard(
                            lead = lead,
                            context = context,
                            onEdit = { editingLead = lead },
                            onConvert = { viewModel.convertLead(lead) },
                            onDelete = { viewModel.deleteLead(lead) },
                            onShowMsg = { msg -> viewModel.showMessage(msg) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLeadDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newLead ->
                viewModel.addLead(newLead)
                showAddDialog = false
            }
        )
    }

    editingLead?.let { leadToEdit ->
        EditLeadDialog(
            lead = leadToEdit,
            onDismiss = { editingLead = null },
            onSave = { updatedLead ->
                viewModel.updateLead(updatedLead)
                editingLead = null
            }
        )
    }
}

@Composable
fun LeadCard(
    lead: LeadEntity,
    context: Context,
    onEdit: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
    onShowMsg: (String) -> Unit
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
                        text = lead.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${lead.eventType} • ${lead.eventDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BmsPrimaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = lead.status)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Lead",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Venue: ${lead.venue}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Source: ${lead.source} • Rep: ${lead.assignedStaff}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Budget: ₹${lead.estimatedBudget.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BmsTertiary
                )
            }

            if (lead.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lead.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // WhatsApp Template Copy
                OutlinedButton(
                    onClick = {
                        val whatsappMessage = "Hello ${lead.name},\nThank you for considering our studio for your ${lead.eventType} on ${lead.eventDate}! We have custom packages tailored to your vision.\nBest regards,\nBMS Studios"
                        copyToClipboard(context, "WhatsApp Message", whatsappMessage) {
                            onShowMsg("Copied WhatsApp message template to clipboard!")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = BmsSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontSize = 11.sp)
                }

                // Lead to Client Conversion
                if (!lead.isConverted && lead.status != "Lost") {
                    Button(
                        onClick = onConvert,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Convert to Client", fontSize = 11.sp)
                    }
                } else if (lead.isConverted) {
                    Text(
                        text = "Converted Client ✓",
                        color = BmsSuccess,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddLeadDialog(
    onDismiss: () -> Unit,
    onAdd: (LeadEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Wedding") }
    var eventDate by remember { mutableStateOf("15 Nov 2026") }
    var venue by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("150000") }
    var source by remember { mutableStateOf("Instagram") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Lead Inquiry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client / Couple Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number (+91) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventType,
                    onValueChange = { eventType = it },
                    label = { Text("Event Type (Wedding, Sangeet, Corporate)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    label = { Text("Event Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Estimated Budget (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Inquiry Notes & Requirements") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && mobile.isNotBlank()) {
                        val newLead = LeadEntity(
                            id = "lead_" + System.currentTimeMillis().toString().takeLast(6),
                            workspaceId = "",
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = if (email.isBlank()) "${name.lowercase().replace(" ", "")}@gmail.com" else email,
                            eventType = eventType.trim(),
                            eventDate = eventDate.trim(),
                            venue = if (venue.isBlank()) "TBD" else venue.trim(),
                            source = source,
                            estimatedBudget = budget.toDoubleOrNull() ?: 100000.0,
                            assignedStaff = "Lead Staff",
                            status = "New",
                            notes = notes.trim(),
                            followUpDate = "Tomorrow"
                        )
                        onAdd(newLead)
                    }
                },
                enabled = name.isNotBlank() && mobile.isNotBlank()
            ) {
                Text("Save Lead")
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
fun EditLeadDialog(
    lead: LeadEntity,
    onDismiss: () -> Unit,
    onSave: (LeadEntity) -> Unit
) {
    var name by remember { mutableStateOf(lead.name) }
    var mobile by remember { mutableStateOf(lead.mobile) }
    var email by remember { mutableStateOf(lead.email) }
    var eventType by remember { mutableStateOf(lead.eventType) }
    var eventDate by remember { mutableStateOf(lead.eventDate) }
    var venue by remember { mutableStateOf(lead.venue) }
    var budget by remember { mutableStateOf(lead.estimatedBudget.toInt().toString()) }
    var status by remember { mutableStateOf(lead.status) }
    var notes by remember { mutableStateOf(lead.notes) }
    var source by remember { mutableStateOf(lead.source) }
    var assignedStaff by remember { mutableStateOf(lead.assignedStaff) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Lead Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Client / Lead Name *") },
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
                        label = { Text("Event Type (Wedding, Pre-Wedding, etc.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text("Event Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Shoot / Event Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Estimated Budget (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Status (New, Follow-up, Quotation Sent, Won, Lost)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = assignedStaff,
                        onValueChange = { assignedStaff = it },
                        label = { Text("Assigned Team / Rep") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("Lead Source (Instagram, Walk-in, Referral)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Client Notes & Special Demands") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = lead.copy(
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            eventType = eventType.trim(),
                            eventDate = eventDate.trim(),
                            venue = venue.trim(),
                            estimatedBudget = budget.toDoubleOrNull() ?: lead.estimatedBudget,
                            status = status.trim(),
                            notes = notes.trim(),
                            source = source.trim(),
                            assignedStaff = assignedStaff.trim()
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
