package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.QuotationEntity
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.DocumentExportUtil
import com.example.ui.components.StatusBadge
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.BmsPrimaryDark
import com.example.ui.theme.BmsSuccess
import com.example.ui.theme.BmsTertiary
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun QuotationsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val quotations by viewModel.quotations.collectAsState()
    val workspace by viewModel.workspace.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingQuotation by remember { mutableStateOf<QuotationEntity?>(null) }
    var viewingQuotation by remember { mutableStateOf<QuotationEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_quotation_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Quotation")
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
                            text = "Commercial Quotations",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GST-ready pricing proposals & package contracts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(quotations, key = { it.id }) { quot ->
                QuotationCard(
                    quotation = quot,
                    onEdit = { editingQuotation = quot },
                    onView = { viewingQuotation = quot },
                    onAcceptAndBook = { viewModel.convertQuotationToBooking(quot) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddQuotationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newQuot ->
                viewModel.addQuotation(newQuot)
                showAddDialog = false
            }
        )
    }

    editingQuotation?.let { quotToEdit ->
        EditQuotationDialog(
            quotation = quotToEdit,
            onDismiss = { editingQuotation = null },
            onSave = { updatedQuot ->
                viewModel.updateQuotation(updatedQuot)
                editingQuotation = null
            }
        )
    }

    viewingQuotation?.let { quot ->
        QuotationFullControlDialog(
            quotation = quot,
            workspace = workspace,
            onDismiss = { viewingQuotation = null },
            onEdit = {
                viewingQuotation = null
                editingQuotation = quot
            },
            onRegenerate = {
                val taxable = maxOf(0.0, quot.subtotal - quot.discount)
                val tax = taxable * 0.18
                val total = taxable + tax
                val advance = total * 0.30
                val balance = total - advance
                val regenerated = quot.copy(
                    taxAmount = tax,
                    totalAmount = total,
                    advanceRequired = advance,
                    balanceAmount = balance
                )
                viewModel.updateQuotation(regenerated)
                viewingQuotation = regenerated
                viewModel.showMessage("Quotation ${quot.quotationNumber} recalculated & regenerated!")
            }
        )
    }
}

@Composable
fun QuotationCard(
    quotation: QuotationEntity,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onAcceptAndBook: () -> Unit
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
                        text = quotation.quotationNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = quotation.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(text = quotation.status)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Quotation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${quotation.eventDate} • ${quotation.eventLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = quotation.services,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                        Text(text = "Subtotal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "₹${quotation.subtotal.toInt()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        Text(text = "GST (18%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "+₹${quotation.taxAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Total Proposal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "₹${quotation.totalAmount.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onView,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View / Share PDF", fontSize = 11.sp)
                }

                if (quotation.status != "Accepted") {
                    Button(
                        onClick = onAcceptAndBook,
                        colors = ButtonDefaults.buttonColors(containerColor = BmsSuccess),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept & Book", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuotationFullControlDialog(
    quotation: QuotationEntity,
    workspace: com.example.data.model.WorkspaceEntity?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormatTab by remember { mutableStateOf(0) } // 0: PDF, 1: Excel (.xlsx), 2: Word (.docx)
    val studioName = workspace?.name ?: "Shakir Photography & Cinema"
    val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = quotation.quotationNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$studioName • GSTIN: $studioGst",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Format Selector Tabs
                TabRow(selectedTabIndex = selectedFormatTab) {
                    Tab(
                        selected = selectedFormatTab == 0,
                        onClick = { selectedFormatTab = 0 },
                        text = { Text("PDF Document", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedFormatTab == 1,
                        onClick = { selectedFormatTab = 1 },
                        text = { Text("Excel (.xlsx)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedFormatTab == 2,
                        onClick = { selectedFormatTab = 2 },
                        text = { Text("Word (.docx)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Document Preview
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(text = "PROPOSAL TO:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text(text = quotation.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = quotation.contact, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "EVENT DETAILS:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text(text = quotation.eventDate, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(text = quotation.eventLocation, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Scope of Work & Deliverables:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = quotation.services, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                if (quotation.packageDetails.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Notes: ${quotation.packageDetails}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Commercial Summary (INR):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "Subtotal:", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "₹${quotation.subtotal.toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (quotation.discount > 0) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(text = "Special Discount:", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "-₹${quotation.discount.toInt()}", color = BmsSuccess, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "GST @ 18% (CGST 9% + SGST 9%):", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "+₹${quotation.taxAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "Total Proposal Value:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "₹${quotation.totalAmount.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "Advance Required (30%):", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "₹${quotation.advanceRequired.toInt()}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "Balance on Project Delivery:", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "₹${quotation.balanceAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Payment Terms & Conditions:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(text = quotation.terms, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }

                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (selectedFormatTab) {
                                    0 -> "Selected Format: Adobe PDF (.pdf) with official studio header and signature block."
                                    1 -> "Selected Format: Microsoft Excel Workbook (.xlsx) with structured columns and formulas."
                                    else -> "Selected Format: Microsoft Word Document (.docx) formatted for contracts and proposals."
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Controls Row: Download, Share, Print, Regenerate, Edit
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                when (selectedFormatTab) {
                                    0 -> {
                                        val file = DocumentExportUtil.generateQuotationPdf(context, quotation, workspace)
                                        DocumentExportUtil.openFile(context, file, "application/pdf")
                                    }
                                    1 -> {
                                        val file = DocumentExportUtil.generateQuotationXlsx(context, quotation, workspace)
                                        DocumentExportUtil.openFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    }
                                    else -> {
                                        val file = DocumentExportUtil.generateQuotationDocx(context, quotation, workspace)
                                        DocumentExportUtil.openFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                when (selectedFormatTab) {
                                    0 -> {
                                        val file = DocumentExportUtil.generateQuotationPdf(context, quotation, workspace)
                                        DocumentExportUtil.shareFile(context, file, "application/pdf", "Quotation_${quotation.quotationNumber}.pdf")
                                    }
                                    1 -> {
                                        val file = DocumentExportUtil.generateQuotationXlsx(context, quotation, workspace)
                                        DocumentExportUtil.shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Quotation_${quotation.quotationNumber}.xlsx")
                                    }
                                    else -> {
                                        val file = DocumentExportUtil.generateQuotationDocx(context, quotation, workspace)
                                        DocumentExportUtil.shareFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Quotation_${quotation.quotationNumber}.docx")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val file = DocumentExportUtil.generateQuotationPdf(context, quotation, workspace)
                                DocumentExportUtil.printPdf(context, file, "Quotation_${quotation.quotationNumber}")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onRegenerate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Regenerate", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Quote", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddQuotationDialog(
    onDismiss: () -> Unit,
    onAdd: (QuotationEntity) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("15 Dec 2026") }
    var location by remember { mutableStateOf("") }
    var services by remember { mutableStateOf("Candid Photography, 4K Cinematic Video, Drone Coverage, 12x36 Album") }
    var subtotalStr by remember { mutableStateOf("250000") }
    var discountStr by remember { mutableStateOf("15000") }

    val subtotal = subtotalStr.toDoubleOrNull() ?: 200000.0
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val taxable = maxOf(0.0, subtotal - discount)
    val taxAmount = taxable * 0.18
    val grandTotal = taxable + taxAmount
    val advance = grandTotal * 0.3
    val balance = grandTotal * 0.7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Quotation Proposal") },
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
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Phone *") },
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
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Event Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = services,
                    onValueChange = { services = it },
                    label = { Text("Included Services & Deliverables") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subtotalStr,
                        onValueChange = { subtotalStr = it },
                        label = { Text("Subtotal (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it },
                        label = { Text("Discount (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Grand Total (with 18% GST): ₹${grandTotal.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary)
                        Text(text = "Advance (30%): ₹${advance.toInt()} • Balance: ₹${balance.toInt()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        val quotNumber = "QT-2026-" + (1050..1999).random()
                        val newQuot = QuotationEntity(
                            id = "quot_" + System.currentTimeMillis().toString().takeLast(6),
                            workspaceId = "",
                            quotationNumber = quotNumber,
                            clientId = "client_" + System.currentTimeMillis().toString().takeLast(4),
                            clientName = clientName.trim(),
                            contact = contact.trim(),
                            eventDate = eventDate.trim(),
                            eventLocation = if (location.isBlank()) "Pune" else location.trim(),
                            services = services.trim(),
                            packageDetails = "Premium Studio Package",
                            subtotal = subtotal,
                            discount = discount,
                            taxRate = 18.0,
                            taxAmount = taxAmount,
                            totalAmount = grandTotal,
                            advanceRequired = advance,
                            balanceAmount = balance,
                            status = "Sent",
                            terms = "1. 30% advance on confirmation. 2. 50% on event date. 3. 20% on delivery."
                        )
                        onAdd(newQuot)
                    }
                },
                enabled = clientName.isNotBlank()
            ) {
                Text("Save & Send")
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
fun EditQuotationDialog(
    quotation: QuotationEntity,
    onDismiss: () -> Unit,
    onSave: (QuotationEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(quotation.clientName) }
    var contact by remember { mutableStateOf(quotation.contact) }
    var eventDate by remember { mutableStateOf(quotation.eventDate) }
    var location by remember { mutableStateOf(quotation.eventLocation) }
    var services by remember { mutableStateOf(quotation.services) }
    var packageDetails by remember { mutableStateOf(quotation.packageDetails) }
    var subtotalStr by remember { mutableStateOf(quotation.subtotal.toInt().toString()) }
    var discountStr by remember { mutableStateOf(quotation.discount.toInt().toString()) }
    var status by remember { mutableStateOf(quotation.status) }
    var terms by remember { mutableStateOf(quotation.terms) }

    val subtotal = subtotalStr.toDoubleOrNull() ?: quotation.subtotal
    val discount = discountStr.toDoubleOrNull() ?: quotation.discount
    val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount = taxableAmount * 0.18
    val grandTotal = taxableAmount + taxAmount
    val advance = grandTotal * 0.30
    val balance = grandTotal - advance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Quotation ${quotation.quotationNumber}") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Phone / Contact") },
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
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Event City / Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = packageDetails,
                        onValueChange = { packageDetails = it },
                        label = { Text("Package Name / Tier") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = services,
                        onValueChange = { services = it },
                        label = { Text("Services & Deliverables") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = subtotalStr,
                        onValueChange = { subtotalStr = it },
                        label = { Text("Contract Base Subtotal (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it },
                        label = { Text("Special Discount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Status (Draft, Sent, Accepted, Declined)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Total With 18% GST: ₹${grandTotal.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary)
                            Text(text = "Advance (30%): ₹${advance.toInt()} • Balance: ₹${balance.toInt()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = terms,
                        onValueChange = { terms = it },
                        label = { Text("Payment Terms & Conditions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        val updated = quotation.copy(
                            clientName = clientName.trim(),
                            contact = contact.trim(),
                            eventDate = eventDate.trim(),
                            eventLocation = location.trim(),
                            services = services.trim(),
                            packageDetails = packageDetails.trim(),
                            subtotal = subtotal,
                            discount = discount,
                            taxRate = 18.0,
                            taxAmount = taxAmount,
                            totalAmount = grandTotal,
                            advanceRequired = advance,
                            balanceAmount = balance,
                            status = status.trim(),
                            terms = terms.trim()
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
