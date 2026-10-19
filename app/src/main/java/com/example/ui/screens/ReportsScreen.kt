package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.DocumentExportUtil
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun ReportsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workspace by viewModel.workspace.collectAsState()
    val leads by viewModel.leads.collectAsState()
    val quotations by viewModel.quotations.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val shoots by viewModel.shoots.collectAsState()
    val photoEdits by viewModel.photoEdits.collectAsState()
    val videoEdits by viewModel.videoEdits.collectAsState()
    val employees by viewModel.employees.collectAsState()

    var selectedPeriod by remember { mutableStateOf("Monthly") } // Date-wise, Monthly, Quarterly, FY-wise
    val periodOptions = listOf("Date-wise", "Monthly", "Quarterly", "FY 2025-26")

    var selectedEmployeeFilter by remember { mutableStateOf("All Staff") }

    val totalRevenue = payments.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalRevenue - totalExpenses

    val wonLeads = leads.count { it.status == "Won" || it.isConverted }
    val conversionRate = if (leads.isNotEmpty()) (wonLeads.toFloat() / leads.size.toFloat()) else 0f

    val completedShoots = shoots.count { it.status == "Completed" }
    val pendingShoots = shoots.size - completedShoots

    val completedPhotoEdits = photoEdits.count { it.status == "Delivered" || it.status == "Approved" }
    val pendingPhotoEdits = photoEdits.size - completedPhotoEdits

    val completedVideoEdits = videoEdits.count { it.status == "Delivered" || it.status == "Approved" }
    val pendingVideoEdits = videoEdits.size - completedVideoEdits

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
                        text = "Executive Reports & Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time Sales Workload & Operational P&L",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        val file = DocumentExportUtil.generateOperationsReportPdf(
                            context = context,
                            workspace = workspace,
                            leads = leads,
                            quotations = quotations,
                            bookings = bookings,
                            periodText = selectedPeriod
                        )
                        DocumentExportUtil.shareFile(context, file, "application/pdf", "Operations_Report_${selectedPeriod}.pdf")
                        viewModel.showMessage("Generated and opened Official PDF Report ($selectedPeriod)!")
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Report", fontSize = 11.sp)
                }
            }
        }

        // Period Filters Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Reporting Period:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periodOptions) { period ->
                        val isSelected = period == selectedPeriod
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Dedicated Salary & Payroll Reports Entry
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RequestQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Salary Slips & Payroll Reports",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generate individual salary slips & workspace summaries (PDF, XLSX, DOCX)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.selectTab(BmsNavigationItem.SALARY_REPORTS) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Sales Workload & Funnel Card
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
                        Text(
                            text = "Sales Funnel & Conversion Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusBadge(text = "$selectedPeriod Audit")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Lead to Client Conversion", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${(conversionRate * 100).toInt()}% ($wonLeads / ${leads.size} inquiries)", fontWeight = FontWeight.Bold, color = BmsSuccess)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { conversionRate },
                        color = BmsSuccess,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Total Inquiries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${leads.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Won Clients", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("$wonLeads", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Draft Quotes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${quotations.count { it.status == "Draft" }}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Accepted Quotes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${quotations.count { it.status == "Accepted" }}", fontWeight = FontWeight.Bold, color = BmsTertiary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Operational Workload Pipeline
        item {
            BmsSectionHeader(title = "Operational Workload: Shoots & Post-Production")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Shoot Assignments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${shoots.size} Total • $completedShoots Done • $pendingShoots Active", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        StatusBadge(text = if (pendingShoots > 0) "$pendingShoots Pending" else "All Shot")
                    }

                    Divider(color = MaterialTheme.colorScheme.surface)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Photo Retouching Projects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${photoEdits.size} Total • $completedPhotoEdits Delivered • $pendingPhotoEdits Queued", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        StatusBadge(text = if (pendingPhotoEdits > 0) "$pendingPhotoEdits Queued" else "Cleared")
                    }

                    Divider(color = MaterialTheme.colorScheme.surface)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Video & Cinematic Edits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${videoEdits.size} Total • $completedVideoEdits Delivered • $pendingVideoEdits in Pipeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        StatusBadge(text = if (pendingVideoEdits > 0) "$pendingVideoEdits Active" else "Cleared")
                    }
                }
            }
        }

        // Financial Reconciliation Summary
        item {
            BmsSectionHeader(title = "Financial Reconciliation (Database Ledger)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Total Receipts",
                    value = "₹${totalRevenue.toInt()}",
                    subtitle = "${payments.size} payments verified",
                    icon = Icons.Default.CurrencyRupee,
                    accentColor = BmsSuccess,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Net Operating Surplus",
                    value = "₹${netProfit.toInt()}",
                    subtitle = "Reconciled ledger balance",
                    icon = Icons.Default.Savings,
                    accentColor = BmsTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Expense Allocation Breakdown
        item {
            BmsSectionHeader(title = "Expense Allocation Breakdown")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val grouped = expenses.groupBy { it.category }
                    if (grouped.isEmpty()) {
                        Text("No expense entries recorded in ledger.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        grouped.forEach { (cat, exps) ->
                            val catTotal = exps.sumOf { it.amount }
                            val pct = if (totalExpenses > 0) (catTotal.toFloat() / totalExpenses.toFloat()) else 0f
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(text = "₹${catTotal.toInt()} (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    color = BmsPrimaryDark,
                                    trackColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
