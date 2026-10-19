package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.ExpenseEntity
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.DocumentExportUtil
import com.example.ui.components.StatusBadge
import com.example.ui.components.SystemIntents
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel
import java.util.UUID

@Composable
fun FinancialYearScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val payments by viewModel.payments.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val workspace by viewModel.workspace.collectAsState()

    var selectedFy by remember { mutableStateOf("FY 2025-26") }
    val fyOptions = listOf("FY 2024-25", "FY 2025-26", "FY 2026-27", "All-Time")

    var showAddOverheadDialog by remember { mutableStateOf(false) }

    // Revenue calculation
    val grossRevenue = payments.sumOf { it.amount }

    // Wages calculation: Annualized salaries of team members + any salary expenses
    val annualWages = employees.sumOf { it.basicSalary * 12.0 }

    // Categorized expenses
    val overheadCategories = setOf("Office", "Software", "Maintenance", "Rent", "Utilities", "Internet")
    val overheadExpenses = expenses.filter { it.category in overheadCategories }.sumOf { it.amount }
    // Production / Direct Expenses (Travel, Equipment, Petrol, Printing, Food, Marketing, Other)
    val directExpenses = expenses.filter { it.category !in overheadCategories && it.category != "Salary" }.sumOf { it.amount }

    val totalCosts = annualWages + directExpenses + overheadExpenses
    val netProfit = grossRevenue - totalCosts
    val profitMargin = if (grossRevenue > 0) ((netProfit / grossRevenue) * 100).toInt() else 0

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOverheadDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_overhead_fab")
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Add Overhead / Cost Entry")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Financial Year (FY) Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Revenue • Wages • Expenses • Overhead • Net Profit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                val file = DocumentExportUtil.generateFinancialYearPdf(
                                    context = context,
                                    workspace = workspace,
                                    selectedFy = selectedFy,
                                    grossRevenue = grossRevenue,
                                    annualWages = annualWages,
                                    directExpenses = directExpenses,
                                    overheadExpenses = overheadExpenses,
                                    netProfit = netProfit,
                                    profitMargin = profitMargin,
                                    expenses = expenses,
                                    employees = employees
                                )
                                DocumentExportUtil.shareFile(context, file, "application/pdf", "Financial_Year_${selectedFy.replace(" ", "_")}.pdf")
                                viewModel.showMessage("Generated and opened PDF Report for $selectedFy!")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Report", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val csvContent = buildString {
                                    appendLine("Category,Description,Amount (INR)")
                                    appendLine("Gross Revenue,Total Payments Collected,${grossRevenue.toLong()}")
                                    appendLine("Wages & Salaries,Staff & Editor Payroll (Annualized),${annualWages.toLong()}")
                                    appendLine("Production Expenses,Direct Shoot & Travel Expenses,${directExpenses.toLong()}")
                                    appendLine("Overhead Costs,Studio Rent / Software / Utilities,${overheadExpenses.toLong()}")
                                    appendLine("Total Expenditures,Wages + Production + Overheads,${totalCosts.toLong()}")
                                    appendLine("Net Profit,Gross Revenue - Total Expenditures,${netProfit.toLong()}")
                                    appendLine("Profit Margin,Percentage,$profitMargin%")
                                    appendLine("\n--- Detailed Expense Entries ---")
                                    expenses.forEach { exp ->
                                        appendLine("\"${exp.category}\",\"${exp.description} (${exp.employeeName})\",${exp.amount.toLong()}")
                                    }
                                    appendLine("\n--- Team Wages Breakdown ---")
                                    employees.forEach { emp ->
                                        appendLine("\"${emp.department}\",\"${emp.name} (${emp.designation})\",${(emp.basicSalary * 12).toLong()}")
                                    }
                                }
                                SystemIntents.shareCsvData(
                                    context = context,
                                    fileName = "BOSS_Financial_Year_${selectedFy.replace(" ", "_")}",
                                    csvContent = csvContent,
                                    title = "BOSS $selectedFy Studio P&L Statement"
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 11.sp)
                        }
                    }
                }
            }

            // FY Chip Filter Selector
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fyOptions.forEach { fy ->
                        val isSelected = fy == selectedFy
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFy = fy },
                            label = { Text(fy) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Net Profit Executive Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netProfit >= 0) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NET PROFIT ($selectedFy)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusBadge(
                                text = if (netProfit >= 0) "$profitMargin% Margin" else "Deficit"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "₹${netProfit.toInt()}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = if (netProfit >= 0) "Healthy operational profitability after deducting wages & studio overhead."
                            else "Expenditures exceed collections for this cycle. Monitor receivables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 4 Pillars Breakdown Cards (Revenue, Wages, Expenses, Overhead)
            item {
                BmsSectionHeader(
                    title = "Key Financial Pillars",
                    subtitle = "Income statement components breakdown"
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pillar 1: Revenue
                    PillarMetricCard(
                        title = "Gross Revenue",
                        amount = grossRevenue,
                        subtitle = "${payments.size} payments cleared",
                        color = Color(0xFF10B981),
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    // Pillar 2: Wages
                    PillarMetricCard(
                        title = "Team Wages",
                        amount = annualWages,
                        subtitle = "${employees.size} salaried staff",
                        color = Color(0xFF6366F1),
                        icon = Icons.Default.Badge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pillar 3: Direct Expenses
                    PillarMetricCard(
                        title = "Production Costs",
                        amount = directExpenses,
                        subtitle = "Shoots, Travel, Gear",
                        color = Color(0xFFF59E0B),
                        icon = Icons.Default.CameraAlt,
                        modifier = Modifier.weight(1f)
                    )

                    // Pillar 4: Overhead
                    PillarMetricCard(
                        title = "Studio Overhead",
                        amount = overheadExpenses,
                        subtitle = "Rent, Software, Office",
                        color = Color(0xFFEC4899),
                        icon = Icons.Default.Domain,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Visual Progress Ratio Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Revenue Utilization & Cost Ratio",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val wageRatio = if (grossRevenue > 0) (annualWages / grossRevenue).coerceIn(0.0, 1.0).toFloat() else 0f
                        val expRatio = if (grossRevenue > 0) (directExpenses / grossRevenue).coerceIn(0.0, 1.0).toFloat() else 0f
                        val ovhRatio = if (grossRevenue > 0) (overheadExpenses / grossRevenue).coerceIn(0.0, 1.0).toFloat() else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RatioRow(label = "Wages & Payroll", ratio = wageRatio, color = Color(0xFF6366F1), amount = annualWages)
                            RatioRow(label = "Production Costs", ratio = expRatio, color = Color(0xFFF59E0B), amount = directExpenses)
                            RatioRow(label = "Studio Overhead", ratio = ovhRatio, color = Color(0xFFEC4899), amount = overheadExpenses)
                        }
                    }
                }
            }

            // Wages & Payroll by Department
            item {
                BmsSectionHeader(
                    title = "Wages by Department",
                    subtitle = "Annualized salary allocation"
                )
            }

            item {
                val depts = employees.groupBy { it.department }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (depts.isEmpty()) {
                            Text("No employee wage records found.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            depts.forEach { (dept, emps) ->
                                val totalDeptWages = emps.sumOf { it.basicSalary * 12.0 }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(dept, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${emps.size} members (${emps.joinToString { it.name }})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "₹${totalDeptWages.toInt()}/yr",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }

            // Overhead Expenses Breakdown
            item {
                BmsSectionHeader(
                    title = "Studio Overhead Breakdown",
                    subtitle = "Fixed and operational recurring costs",
                    actionText = "+ Add Overhead",
                    onActionClick = { showAddOverheadDialog = true }
                )
            }

            item {
                val overheadList = expenses.filter { it.category in overheadCategories }
                if (overheadList.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No studio overhead expenses recorded yet. Tap '+ Add Overhead' to log rent, software, or electricity costs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        overheadList.forEach { ovh ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ovh.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${ovh.category} • ${ovh.date} • Paid via ${ovh.paymentMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "₹${ovh.amount.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showAddOverheadDialog) {
        AddOverheadDialog(
            workspaceId = workspace?.id ?: "ws_default",
            onDismiss = { showAddOverheadDialog = false },
            onSave = { entity ->
                viewModel.recordExpense(entity)
                showAddOverheadDialog = false
            }
        )
    }
}

@Composable
private fun PillarMetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "₹${amount.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RatioRow(label: String, ratio: Float, color: Color, amount: Double) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("₹${amount.toInt()} (${(ratio * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOverheadDialog(
    workspaceId: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var category by remember { mutableStateOf("Office") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Bank Transfer") }

    val categories = listOf("Office", "Software", "Maintenance", "Rent", "Utilities", "Internet")
    val methods = listOf("Bank Transfer", "UPI", "Cash", "Card")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Studio Overhead Cost") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Overhead Category", style = MaterialTheme.typography.labelSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Cost Description (e.g. Studio Rent, Adobe CC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && description.isNotBlank()) {
                        val entity = ExpenseEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = workspaceId,
                            category = category,
                            amount = amt,
                            date = "Current FY",
                            description = description,
                            employeeName = "Studio Admin",
                            projectName = "Studio Operations",
                            paymentMethod = paymentMethod
                        )
                        onSave(entity)
                    }
                },
                enabled = description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Record Overhead")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
