package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.AppRole
import com.example.data.model.EmployeeEntity
import com.example.data.model.PayrollRecordEntity
import com.example.ui.components.DocumentExportUtil
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryReportsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workspace by viewModel.workspace.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val payrollRecords by viewModel.payrollRecords.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val authUser by viewModel.authenticatedUser.collectAsState()
    val currentRole = authUser?.actualRole ?: AppRole.ADMIN
    val isRestrictedEmployee = currentRole == AppRole.EMPLOYEE

    var selectedTab by remember { mutableStateOf(0) } // 0: Individual Slip, 1: Monthly Summary, 2: History & Comparison

    // Month Selector State
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val selectedMonthYear = remember(selectedCalendar) { monthFormat.format(selectedCalendar.time) }

    // Selected Employee for Slip
    var selectedEmployeeId by remember {
        mutableStateOf(
            if (isRestrictedEmployee) {
                employees.firstOrNull { it.email.equals(authUser?.email, ignoreCase = true) }?.id ?: employees.firstOrNull()?.id ?: ""
            } else {
                employees.firstOrNull()?.id ?: ""
            }
        )
    }

    LaunchedEffect(employees, authUser) {
        if (selectedEmployeeId.isBlank() && employees.isNotEmpty()) {
            selectedEmployeeId = if (isRestrictedEmployee) {
                employees.firstOrNull { it.email.equals(authUser?.email, ignoreCase = true) }?.id ?: employees.first().id
            } else {
                employees.first().id
            }
        }
    }

    val selectedEmployee = employees.firstOrNull { it.id == selectedEmployeeId }
    val currentSlipRecord = payrollRecords.firstOrNull {
        it.employeeId == selectedEmployeeId && it.monthYear.equals(selectedMonthYear, ignoreCase = true)
    }
    val monthRecords = payrollRecords.filter { it.monthYear.equals(selectedMonthYear, ignoreCase = true) }

    var showRecalculateConfirmDialog by remember { mutableStateOf(false) }
    var showRecalculateConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Salary Reports & Slips",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standard 9h/day • Configurable Basis • PDF, XLSX, DOCX",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Month Navigator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance().apply { time = selectedCalendar.time }
                                cal.add(Calendar.MONTH, -1)
                                selectedCalendar = cal
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }

                            Text(
                                text = selectedMonthYear,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(onClick = {
                                val cal = Calendar.getInstance().apply { time = selectedCalendar.time }
                                cal.add(Calendar.MONTH, 1)
                                selectedCalendar = cal
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mode Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Employee Slip", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        if (!isRestrictedEmployee) {
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Monthly Summary (${monthRecords.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Historical Audit", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            when (selectedTab) {
                // TAB 0: INDIVIDUAL SALARY SLIP
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Employee Selector Row (for Admins/Managers)
                        if (!isRestrictedEmployee && employees.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Select Employee for Salary Slip:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(employees) { emp ->
                                            val isSelected = emp.id == selectedEmployeeId
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedEmployeeId = emp.id },
                                                label = { Text(emp.name, fontSize = 12.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedEmployee == null) {
                            item {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                                    Text("No employee record selected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            // Employee Profile & Salary Overview Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Text(
                                                        text = selectedEmployee.name.take(2).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = selectedEmployee.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = "${selectedEmployee.designation} • ${selectedEmployee.department}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = BmsPrimaryDark
                                                    )
                                                }
                                            }

                                            StatusBadge(text = currentSlipRecord?.status ?: "Not Computed")
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text("Base Monthly CTC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("₹${"%,d".format(selectedEmployee.basicSalary.toLong())}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Net Salary ($selectedMonthYear)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text(
                                                    text = if (currentSlipRecord != null) "₹${"%,.2f".format(currentSlipRecord.netSalary)}" else "Pending Calculation",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentSlipRecord != null) BmsSuccess else MaterialTheme.colorScheme.outline,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Calculation Breakdown Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Attendance & Working Hours Breakdown",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )

                                        if (currentSlipRecord != null) {
                                            val basisStr = if (currentSlipRecord.calculationBasis == "SCHEDULED_WORKING_DAYS") {
                                                "Scheduled Working Days (${currentSlipRecord.scheduledWorkingDays} days)"
                                            } else {
                                                "Calendar Days (${currentSlipRecord.totalCalendarDays} days)"
                                            }
                                            Text(
                                                text = "Policy: $basisStr • Standard 9.0 hrs/day",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            // Counts Grid
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = BmsSuccess.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("P", fontWeight = FontWeight.Bold, color = BmsSuccess, fontSize = 11.sp)
                                                        Text("${currentSlipRecord.presentDays}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                                Surface(shape = RoundedCornerShape(8.dp), color = BmsTertiary.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("HD", fontWeight = FontWeight.Bold, color = BmsTertiary, fontSize = 11.sp)
                                                        Text("${currentSlipRecord.halfDays}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f)) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("WO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Text("${currentSlipRecord.weeklyOffDays}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("H", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Text("${currentSlipRecord.holidayDays}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("L", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 11.sp)
                                                        Text("${currentSlipRecord.paidLeaveDays}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                                Surface(shape = RoundedCornerShape(8.dp), color = BmsError.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("A", fontWeight = FontWeight.Bold, color = BmsError, fontSize = 11.sp)
                                                        Text("${currentSlipRecord.unpaidLeaveDays}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            val earnedSalary = maxOf(0.0, currentSlipRecord.grossPayable - currentSlipRecord.overtimeAmount)

                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Approved Payable Hours:", style = MaterialTheme.typography.bodySmall)
                                                Text("${currentSlipRecord.totalWorkingHours} hrs (${currentSlipRecord.payableDays} days)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Daily Rate:", style = MaterialTheme.typography.bodySmall)
                                                Text("₹${"%,.2f".format(currentSlipRecord.dailyRate)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Regular Earned Salary:", style = MaterialTheme.typography.bodySmall)
                                                Text("₹${"%,d".format(earnedSalary.toLong())}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (currentSlipRecord.overtimeHours > 0) {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Overtime (${currentSlipRecord.overtimeHours}h @ 1.5x):", style = MaterialTheme.typography.bodySmall)
                                                    Text("+₹${"%,d".format(currentSlipRecord.overtimeAmount.toLong())}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            if (currentSlipRecord.deductions > 0) {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Authorized Deductions (${currentSlipRecord.deductionReason.ifBlank { "Standard" }}):", style = MaterialTheme.typography.bodySmall)
                                                    Text("-₹${"%,d".format(currentSlipRecord.deductions.toLong())}", fontWeight = FontWeight.Bold, color = BmsError, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("NET PAYABLE SALARY:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text("₹${"%,.2f".format(currentSlipRecord.netSalary)}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.titleMedium)
                                            }
                                        } else {
                                            Text(
                                                text = "No saved payroll record found for $selectedMonthYear.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            if (!isRestrictedEmployee) {
                                                Button(
                                                    onClick = { showRecalculateConfigDialog = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Calculate $selectedMonthYear Salary")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Lifecycle Actions: Calculate, Approve, Mark Paid
                            if (!isRestrictedEmployee) {
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = {
                                                if (currentSlipRecord?.status == "Approved" || currentSlipRecord?.status == "Paid") {
                                                    showRecalculateConfirmDialog = true
                                                } else {
                                                    showRecalculateConfigDialog = true
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Recalculate", fontSize = 12.sp)
                                        }

                                        if (currentSlipRecord != null && currentSlipRecord.status == "Draft") {
                                            Button(
                                                onClick = {
                                                    viewModel.approvePayroll(currentSlipRecord.id, authUser?.fullName ?: "Shakir Tamboli")
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve", fontSize = 12.sp)
                                            }
                                        }

                                        if (currentSlipRecord != null && currentSlipRecord.status == "Approved") {
                                            Button(
                                                onClick = {
                                                    viewModel.markPayrollPaid(currentSlipRecord.id)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = BmsSuccess),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark Paid", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Document Export Suite (PDF, Excel, Word)
                            if (currentSlipRecord != null) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = "Download & Share Official Salary Slip",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )

                                            // 1. PDF Salary Slip
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.padding(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                                        Column {
                                                            Text("Official PDF Slip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("Printable A4 with Studio Branding", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipPdf(context, currentSlipRecord, selectedEmployee, workspace)
                                                                DocumentExportUtil.openFile(context, file, "application/pdf")
                                                                viewModel.showMessage("Opening PDF Slip Preview...")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipPdf(context, currentSlipRecord, selectedEmployee, workspace)
                                                                val saved = DocumentExportUtil.saveFileToDownloads(context, file, "application/pdf", file.name)
                                                                Toast.makeText(context, "Saved to Downloads: ${saved.name}", Toast.LENGTH_LONG).show()
                                                                viewModel.showMessage("Saved to Downloads: ${saved.name}")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipPdf(context, currentSlipRecord, selectedEmployee, workspace)
                                                                DocumentExportUtil.shareFile(context, file, "application/pdf", "Salary Slip - ${selectedEmployee.name} ($selectedMonthYear)")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            // 2. Excel (.xlsx) Salary Slip
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.padding(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                                        Column {
                                                            Text("Excel Sheet (.xlsx)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("Formulas & Calculation Ledger", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipXlsx(context, currentSlipRecord, selectedEmployee, workspace)
                                                                val saved = DocumentExportUtil.saveFileToDownloads(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.name)
                                                                Toast.makeText(context, "Saved Excel: ${saved.name}", Toast.LENGTH_LONG).show()
                                                                viewModel.showMessage("Saved to Downloads: ${saved.name}")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Download, contentDescription = "Download Excel", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipXlsx(context, currentSlipRecord, selectedEmployee, workspace)
                                                                DocumentExportUtil.shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Salary Slip XLSX - ${selectedEmployee.name}")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Share, contentDescription = "Share Excel", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            // 3. Word (.docx) Statement
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.padding(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                                                        Column {
                                                            Text("Word Document (.docx)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("Formal Salary Statement", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipDocx(context, currentSlipRecord, selectedEmployee, workspace)
                                                                val saved = DocumentExportUtil.saveFileToDownloads(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", file.name)
                                                                Toast.makeText(context, "Saved Word Doc: ${saved.name}", Toast.LENGTH_LONG).show()
                                                                viewModel.showMessage("Saved to Downloads: ${saved.name}")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Download, contentDescription = "Download Word", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val file = DocumentExportUtil.generateSalarySlipDocx(context, currentSlipRecord, selectedEmployee, workspace)
                                                                DocumentExportUtil.shareFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Salary Statement DOCX - ${selectedEmployee.name}")
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Share, contentDescription = "Share Word", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: MONTHLY PAYROLL SUMMARY
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Studio Summary Card
                        item {
                            val totalGross = monthRecords.sumOf { it.grossPayable }
                            val totalNet = monthRecords.sumOf { it.netSalary }
                            val totalDed = monthRecords.sumOf { it.deductions }
                            val totalHours = monthRecords.sumOf { it.totalWorkingHours }

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
                                        Text(
                                            text = "Monthly Studio Payroll Summary",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        StatusBadge(text = "$selectedMonthYear (${monthRecords.size} staff)")
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Total Gross", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("₹${"%,d".format(totalGross.toLong())}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Net Wage Bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("₹${"%,d".format(totalNet.toLong())}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Total Hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("${totalHours.toInt()}h", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }

                                    // Master Export Controls
                                    if (monthRecords.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Text("Export Master Payroll Statement ($selectedMonthYear):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = {
                                                    val file = DocumentExportUtil.generateMonthlyPayrollSummaryPdf(context, monthRecords, selectedMonthYear, workspace)
                                                    DocumentExportUtil.openFile(context, file, "application/pdf")
                                                    viewModel.showMessage("Opening Master PDF Summary...")
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("PDF", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val file = DocumentExportUtil.generateMonthlyPayrollSummaryXlsx(context, monthRecords, selectedMonthYear, workspace)
                                                    val saved = DocumentExportUtil.saveFileToDownloads(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.name)
                                                    Toast.makeText(context, "Saved Master Excel: ${saved.name}", Toast.LENGTH_LONG).show()
                                                    viewModel.showMessage("Saved: ${saved.name}")
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Excel", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val file = DocumentExportUtil.generateMonthlyPayrollSummaryDocx(context, monthRecords, selectedMonthYear, workspace)
                                                    DocumentExportUtil.shareFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Monthly Payroll DOCX - $selectedMonthYear")
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Word", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Staff-wise List
                        item {
                            Text("Employee Breakdown ($selectedMonthYear):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        items(employees) { emp ->
                            val record = monthRecords.firstOrNull { it.employeeId == emp.id }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEmployeeId = emp.id
                                        selectedTab = 0
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "${emp.designation} • Base: ₹${emp.basicSalary.toInt()}/mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        if (record != null) {
                                            Text(
                                                text = "Payable: ${record.payableDays}d (${record.totalWorkingHours.toInt()}h) • Basis: ${record.calculationBasis}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (record != null) {
                                            Text(
                                                text = "₹${"%,d".format(record.netSalary.toLong())}",
                                                fontWeight = FontWeight.Bold,
                                                color = BmsSuccess,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            StatusBadge(text = record.status)
                                        } else {
                                            Text("Not Computed", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: HISTORICAL AUDIT & COMPARISON
                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Preserved Historical Payroll Audit Trail",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Immutable historical snapshots. Recalculation requires authorization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        val auditRecords = if (isRestrictedEmployee) {
                            payrollRecords.filter { it.employeeId == selectedEmployeeId }
                        } else {
                            payrollRecords
                        }

                        if (auditRecords.isEmpty()) {
                            item {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                                    Text("No historical payroll records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(auditRecords, key = { it.id }) { rec ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(text = "${rec.employeeName} (${rec.monthYear})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(text = "Ref: ${rec.id} • Basis: ${rec.calculationBasis}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            StatusBadge(text = rec.status)
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Worked: ${rec.payableDays}d (${rec.totalWorkingHours.toInt()}h)", style = MaterialTheme.typography.bodySmall)
                                            Text("Net: ₹${"%,.2f".format(rec.netSalary)}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.bodySmall)
                                        }

                                        if (rec.approvedBy.isNotBlank()) {
                                            Text("Approved by: ${rec.approvedBy} on ${rec.approvalDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }

                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(
                                                onClick = {
                                                    selectedEmployeeId = rec.employeeId
                                                    val cal = Calendar.getInstance()
                                                    try {
                                                        val parsed = SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(rec.monthYear)
                                                        if (parsed != null) cal.time = parsed
                                                    } catch (_: Exception) {}
                                                    selectedCalendar = cal
                                                    selectedTab = 0
                                                }
                                            ) {
                                                Text("Open Slip Details", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before modifying an already approved payroll
    if (showRecalculateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRecalculateConfirmDialog = false },
            title = { Text("Recalculate Approved Payroll?") },
            text = {
                Text(
                    text = "The payroll record for ${selectedEmployee?.name} ($selectedMonthYear) is currently '${currentSlipRecord?.status}'. Recalculating will update the snapshot. Do you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRecalculateConfirmDialog = false
                        showRecalculateConfigDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Proceed with Recalculation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecalculateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recalculate / Calculate Configuration Dialog
    if (showRecalculateConfigDialog && selectedEmployee != null) {
        var basis by remember { mutableStateOf(currentSlipRecord?.calculationBasis ?: "SCHEDULED_WORKING_DAYS") }
        var scheduledDaysStr by remember { mutableStateOf((currentSlipRecord?.scheduledWorkingDays ?: 26).toString()) }
        var calendarDaysStr by remember { mutableStateOf((currentSlipRecord?.totalCalendarDays ?: 30).toString()) }
        var overtimeHoursStr by remember { mutableStateOf((currentSlipRecord?.overtimeHours ?: 0.0).toString()) }
        var deductionsStr by remember { mutableStateOf((currentSlipRecord?.deductions ?: 0.0).toInt().toString()) }
        var deductionReason by remember { mutableStateOf(currentSlipRecord?.deductionReason ?: "") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRecalculateConfigDialog = false },
            title = { Text("Compute Salary: ${selectedEmployee.name}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text("Month: $selectedMonthYear • Base: ₹${selectedEmployee.basicSalary.toInt()}/mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }

                    item {
                        Text("Calculation Policy Basis:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = basis == "SCHEDULED_WORKING_DAYS",
                                onClick = { basis = "SCHEDULED_WORKING_DAYS" },
                                label = { Text("Scheduled Days", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = basis == "CALENDAR_DAYS",
                                onClick = { basis = "CALENDAR_DAYS" },
                                label = { Text("Calendar Days", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (basis == "SCHEDULED_WORKING_DAYS") {
                        item {
                            OutlinedTextField(
                                value = scheduledDaysStr,
                                onValueChange = { scheduledDaysStr = it },
                                label = { Text("Scheduled Working Days (excluding offs/holidays)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = calendarDaysStr,
                                onValueChange = { calendarDaysStr = it },
                                label = { Text("Total Calendar Days in Month (28 - 31)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = overtimeHoursStr,
                            onValueChange = { overtimeHoursStr = it },
                            label = { Text("Approved Overtime Hours (@ 1.5x)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = deductionsStr,
                            onValueChange = { deductionsStr = it },
                            label = { Text("Authorized Deductions (INR)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = deductionReason,
                            onValueChange = { deductionReason = it },
                            label = { Text("Deduction Reason (if any)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Audit Remarks") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val schedDays = scheduledDaysStr.toIntOrNull() ?: 26
                        val calDays = calendarDaysStr.toIntOrNull() ?: 30
                        val ot = overtimeHoursStr.toDoubleOrNull() ?: 0.0
                        val ded = deductionsStr.toDoubleOrNull() ?: 0.0
                        viewModel.calculateAndSaveMonthlyPayroll(
                            employee = selectedEmployee,
                            monthYear = selectedMonthYear,
                            calculationBasis = basis,
                            totalCalendarDays = calDays,
                            scheduledWorkingDays = schedDays,
                            overtimeHours = ot,
                            deductions = ded,
                            deductionReason = deductionReason,
                            calculationNotes = notes
                        )
                        showRecalculateConfigDialog = false
                    }
                ) {
                    Text("Compute & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecalculateConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
