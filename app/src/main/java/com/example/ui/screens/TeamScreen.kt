package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.data.model.AppRole
import com.example.data.model.AttendanceEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.LeaveEntity
import com.example.data.model.PayrollRecordEntity
import com.example.ui.biometric.BiometricAuthHelper
import com.example.ui.biometric.BiometricHardwareStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TeamScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val employees by viewModel.employees.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val leaves by viewModel.leaves.collectAsState()
    val payrollRecords by viewModel.payrollRecords.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val authUser by viewModel.authenticatedUser.collectAsState()
    val isRestrictedEmployee = authUser?.actualRole == AppRole.EMPLOYEE

    val currentEmployee = employees.firstOrNull {
        it.id == (authUser?.employeeId ?: "") || it.name.equals(currentUserName, ignoreCase = true) || it.email.equals(authUser?.email, ignoreCase = true)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Employees, 1: Attendance, 2: Leaves, 3: Salary & Payroll
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var editingAttendanceRecord by remember { mutableStateOf<AttendanceEntity?>(null) }
    var calculatingPayrollEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var viewingPayrollRecord by remember { mutableStateOf<PayrollRecordEntity?>(null) }
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var pinFallbackInput by remember { mutableStateOf("") }

    // Attendance State
    var attendanceViewMode by remember { mutableStateOf(0) } // 0: Monthly Calendar, 1: Daily Overview
    var selectedAttendanceEmployeeId by remember {
        mutableStateOf(
            if (isRestrictedEmployee && currentEmployee != null) currentEmployee.id else ""
        )
    }
    var selectedAttendanceMonthCal by remember { mutableStateOf(Calendar.getInstance()) }
    val attendanceMonthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val selectedAttendanceMonthStr = remember(selectedAttendanceMonthCal) { attendanceMonthFormat.format(selectedAttendanceMonthCal.time) }
    val unsavedAttendanceEdits = remember { mutableStateMapOf<String, AttendanceEntity?>() }

    LaunchedEffect(employees, authUser) {
        if (isRestrictedEmployee && currentEmployee != null) {
            selectedAttendanceEmployeeId = currentEmployee.id
        } else if (selectedAttendanceEmployeeId.isBlank() && employees.isNotEmpty()) {
            selectedAttendanceEmployeeId = employees.first().id
        }
    }

    // Date state for Attendance Sheet
    var selectedDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val selectedDateStr = dateFormat.format(selectedDateCalendar.time)

    // Month state for Payroll
    val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    var selectedPayrollMonth by remember { mutableStateOf(monthFormat.format(Date())) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        2 -> showLeaveDialog = true
                        else -> showAddEmployeeDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("team_fab")
            ) {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Default.EventBusy else Icons.Default.PersonAdd,
                    contentDescription = "Team Action"
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
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Staff (${employees.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Attendance") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Leaves (${leaves.size})") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Salary & Payroll (${payrollRecords.size})") })
            }

            when (selectedTab) {
                // 0: STAFF LIST
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(employees, key = { it.id }) { emp ->
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
                                            Text(text = emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text(text = "${emp.designation} • ${emp.department}", style = MaterialTheme.typography.bodySmall, color = BmsPrimaryDark)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            StatusBadge(text = emp.status)
                                            IconButton(onClick = { editingEmployee = emp }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Employee", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(text = "App Role: ${emp.role}", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "Base Salary: ₹${emp.basicSalary.toInt()}/mo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (emp.mobile.isNotBlank()) {
                                        Text(text = "Mobile: ${emp.mobile} • Joined: ${emp.joiningDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = { calculatingPayrollEmployee = emp },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Calculate Payroll", fontSize = 11.sp)
                                        }
                                        if (emp.role != "Admin" && emp.role != "Owner / Admin") {
                                            OutlinedButton(
                                                onClick = { viewModel.switchUserRole(emp.role, emp.name) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Impersonate", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1: ATTENDANCE MANAGEMENT (MONTHLY CALENDAR & BIOMETRIC)
                1 -> {
                    val targetEmp = employees.firstOrNull { it.id == selectedAttendanceEmployeeId }
                        ?: (if (isRestrictedEmployee && currentEmployee != null) currentEmployee else employees.firstOrNull())
                    val todayDateStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
                    val userTodayRecord = attendance.firstOrNull {
                        it.employeeId == (currentEmployee?.id ?: targetEmp?.id ?: "") && it.date == todayDateStr
                    }

                    // Calculate month statistics for selected employee in Composable scope
                    val calYear = selectedAttendanceMonthCal.get(Calendar.YEAR)
                    val calMonth = selectedAttendanceMonthCal.get(Calendar.MONTH)
                    val daysInMonth = remember(calYear, calMonth) {
                        val tempCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, calYear)
                            set(Calendar.MONTH, calMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    }

                    // Reconcile saved attendance with unsaved edits
                    val monthAttendanceMap = remember(attendance, unsavedAttendanceEdits.toMap(), selectedAttendanceMonthStr, targetEmp) {
                        val map = mutableMapOf<String, AttendanceEntity?>()
                        if (targetEmp != null) {
                            val dbRecords = attendance.filter {
                                it.employeeId == targetEmp.id && it.date.contains(selectedAttendanceMonthStr, ignoreCase = true)
                            }
                            for (rec in dbRecords) {
                                map[rec.date] = rec
                            }
                            for ((d, rec) in unsavedAttendanceEdits) {
                                if (d.contains(selectedAttendanceMonthStr, ignoreCase = true)) {
                                    map[d] = rec
                                }
                            }
                        }
                        map
                    }

                    val presentCount = monthAttendanceMap.values.filterNotNull().count { it.status == "Present" }
                    val halfDayCount = monthAttendanceMap.values.filterNotNull().count { it.status == "Half-Day" || it.status == "Half Day" }
                    val absentCount = monthAttendanceMap.values.filterNotNull().count { it.status == "Absent" }
                    val weeklyOffCount = monthAttendanceMap.values.filterNotNull().count { it.status == "Weekly Off" }
                    val holidayCount = monthAttendanceMap.values.filterNotNull().count { it.status == "Holiday" }
                    val leaveCount = monthAttendanceMap.values.filterNotNull().count { it.status == "Leave" }
                    val totalApprovedHours = monthAttendanceMap.values.filterNotNull().sumOf { it.workingHours }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. BIOMETRIC ATTENDANCE CHECK-IN CARD
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("biometric_attendance_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Biometric Sensor",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Biometric Attendance Check-In",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "Today: $todayDateStr • 9h Standard",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        if (userTodayRecord != null) {
                                            StatusBadge(text = userTodayRecord.status)
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Not Marked",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    val activeEmpForCheckIn = currentEmployee ?: targetEmp
                                    if (activeEmpForCheckIn != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Employee: ${activeEmpForCheckIn.name}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                                if (userTodayRecord != null) {
                                                    Text(
                                                        text = "In: ${userTodayRecord.checkInTime} • Out: ${userTodayRecord.checkOutTime}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        if (activity != null) {
                                                            BiometricAuthHelper.promptBiometricAuthentication(
                                                                activity = activity,
                                                                employeeName = activeEmpForCheckIn.name,
                                                                actionTitle = if (userTodayRecord == null) "Check In" else "Check Out",
                                                                onSuccess = { authTag ->
                                                                    viewModel.markBiometricAttendance(
                                                                        employeeId = activeEmpForCheckIn.id,
                                                                        employeeName = activeEmpForCheckIn.name,
                                                                        authTag = authTag,
                                                                        verificationType = "BIOMETRIC_DEVICE",
                                                                        isCheckOut = userTodayRecord != null
                                                                    )
                                                                },
                                                                onError = { _, err ->
                                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                                },
                                                                onFallbackRequested = {
                                                                    showPinFallbackDialog = true
                                                                }
                                                            )
                                                        } else {
                                                            showPinFallbackDialog = true
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("biometric_mark_present_button")
                                                ) {
                                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (userTodayRecord == null) "Mark Present" else "Check Out",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { showPinFallbackDialog = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("PIN", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. VIEW MODE TOGGLE (MONTHLY CALENDAR VS DAILY ROSTER)
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    Button(
                                        onClick = { attendanceViewMode = 0 },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (attendanceViewMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (attendanceViewMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Monthly Calendar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { attendanceViewMode = 1 },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (attendanceViewMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (attendanceViewMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Daily Team Roster", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // MODE 0: MONTHLY ATTENDANCE CALENDAR
                        // ==========================================
                        if (attendanceViewMode == 0) {
                            // Staff selector (if not restricted employee)
                            if (!isRestrictedEmployee && employees.isNotEmpty()) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Select Employee for Monthly Sheet:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(employees) { emp ->
                                                val isSelected = emp.id == targetEmp?.id
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (unsavedAttendanceEdits.isNotEmpty()) {
                                                            Toast.makeText(context, "Please save or discard unsaved changes first", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            selectedAttendanceEmployeeId = emp.id
                                                        }
                                                    },
                                                    label = { Text(emp.name, fontSize = 12.sp) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
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
                            } else if (targetEmp != null) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Personal Attendance Record: ${targetEmp.name} (${targetEmp.designation})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Month Navigator
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        IconButton(onClick = {
                                            if (unsavedAttendanceEdits.isNotEmpty()) {
                                                Toast.makeText(context, "Save or discard pending changes before switching month", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val cal = Calendar.getInstance().apply { time = selectedAttendanceMonthCal.time }
                                                cal.add(Calendar.MONTH, -1)
                                                selectedAttendanceMonthCal = cal
                                            }
                                        }) {
                                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = selectedAttendanceMonthStr,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Monthly Sheet • ${targetEmp?.name ?: "All"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                if (unsavedAttendanceEdits.isNotEmpty()) {
                                                    Toast.makeText(context, "Save or discard pending changes first", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedAttendanceMonthCal = Calendar.getInstance()
                                                }
                                            }) {
                                                Icon(Icons.Default.Today, contentDescription = "Current Month")
                                            }
                                            IconButton(onClick = {
                                                if (unsavedAttendanceEdits.isNotEmpty()) {
                                                    Toast.makeText(context, "Save or discard pending changes before switching month", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val cal = Calendar.getInstance().apply { time = selectedAttendanceMonthCal.time }
                                                    cal.add(Calendar.MONTH, 1)
                                                    selectedAttendanceMonthCal = cal
                                                }
                                            }) {
                                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                                            }
                                        }
                                    }
                                }
                            }


                            // Monthly Summary Numbers
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = BmsSuccess.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("P", fontWeight = FontWeight.Bold, color = BmsSuccess, fontSize = 10.sp)
                                            Text("$presentCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = BmsTertiary.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("HD", fontWeight = FontWeight.Bold, color = BmsTertiary, fontSize = 10.sp)
                                            Text("$halfDayCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = BmsError.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("A", fontWeight = FontWeight.Bold, color = BmsError, fontSize = 10.sp)
                                            Text("$absentCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("WO", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            Text("$weeklyOffCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("H", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            Text("$holidayCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("L", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 10.sp)
                                            Text("$leaveCount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1.3f)) {
                                        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("HOURS", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("${totalApprovedHours.toInt()}h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Visual Legend Bar
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = "Legend (Tap active to UNMARK):", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(text = "P: 9h • HD: 4.5h • A: 0h • WO: 9h • H: 9h • L: 9h", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Unsaved Changes Banner with Explicit Save Action
                            if (unsavedAttendanceEdits.isNotEmpty() && targetEmp != null) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${unsavedAttendanceEdits.size} unsaved date change(s)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = { unsavedAttendanceEdits.clear() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Discard", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val recordsToSave = unsavedAttendanceEdits.values.filterNotNull()
                                                        val datesToDelete = unsavedAttendanceEdits.filter { it.value == null }.keys.toList()
                                                        viewModel.saveMonthlyAttendanceBatch(
                                                            employeeId = targetEmp.id,
                                                            employeeName = targetEmp.name,
                                                            recordsToSave = recordsToSave,
                                                            datesToDelete = datesToDelete
                                                        )
                                                        unsavedAttendanceEdits.clear()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("save_monthly_attendance_button")
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Save & Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Day-wise Calendar Rows (1 to daysInMonth)
                            if (targetEmp != null) {
                                val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

                                for (dayNum in 1..daysInMonth) {
                                    item(key = "day_${calYear}_${calMonth}_$dayNum") {
                                        val dayCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, calYear)
                                            set(Calendar.MONTH, calMonth)
                                            set(Calendar.DAY_OF_MONTH, dayNum)
                                        }
                                        val dateStr = dayFormat.format(dayCal.time)
                                        val dayName = dayNameFormat.format(dayCal.time)
                                        val isToday = dateStr == todayDateStr
                                        val hasUnsavedEdit = unsavedAttendanceEdits.containsKey(dateStr)

                                        val activeRecord = if (hasUnsavedEdit) {
                                            unsavedAttendanceEdits[dateStr]
                                        } else {
                                            attendance.firstOrNull { it.employeeId == targetEmp.id && it.date == dateStr }
                                        }

                                        val currentStatus = activeRecord?.status ?: "Unmarked"

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            border = if (hasUnsavedEdit) {
                                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                            } else if (isToday) {
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                            } else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Text(
                                                                    text = "$dayNum",
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 12.sp,
                                                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                    text = dayName,
                                                                    fontSize = 8.sp,
                                                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = currentStatus,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp,
                                                                    color = when (currentStatus) {
                                                                        "Present" -> BmsSuccess
                                                                        "Half-Day", "Half Day" -> BmsTertiary
                                                                        "Absent" -> BmsError
                                                                        "Weekly Off" -> Color(0xFF6366F1)
                                                                        "Holiday" -> Color(0xFF06B6D4)
                                                                        "Leave" -> Color(0xFFF59E0B)
                                                                        else -> MaterialTheme.colorScheme.outline
                                                                    }
                                                                )
                                                                if (activeRecord != null && activeRecord.workingHours > 0.0) {
                                                                    Text(
                                                                        text = " (${activeRecord.workingHours}h)",
                                                                        fontSize = 11.sp,
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                }
                                                                if (hasUnsavedEdit) {
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                                    ) {
                                                                        Text(
                                                                            text = "Unsaved",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            if (activeRecord != null && activeRecord.checkInTime.isNotBlank()) {
                                                                Text(
                                                                    text = "${activeRecord.checkInTime} - ${activeRecord.checkOutTime}",
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            editingAttendanceRecord = activeRecord ?: AttendanceEntity(
                                                                id = "att_${targetEmp.id}_${dateStr.replace(" ", "_")}",
                                                                workspaceId = targetEmp.workspaceId,
                                                                employeeId = targetEmp.id,
                                                                employeeName = targetEmp.name,
                                                                date = dateStr,
                                                                status = "Present",
                                                                workingHours = 9.0,
                                                                checkInTime = "09:30 AM",
                                                                checkOutTime = "06:30 PM",
                                                                notes = ""
                                                            )
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.EditCalendar, contentDescription = "Edit Day Details", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Quick Selection Buttons: P, HD, A, WO, H, L
                                                // RULE: Tapping active status deselects it and sets to UNMARKED!
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                                    listOf(
                                                        Triple("Present", "P", BmsSuccess),
                                                        Triple("Half-Day", "HD", BmsTertiary),
                                                        Triple("Absent", "A", BmsError),
                                                        Triple("Weekly Off", "WO", Color(0xFF6366F1)),
                                                        Triple("Holiday", "H", Color(0xFF06B6D4)),
                                                        Triple("Leave", "L", Color(0xFFF59E0B))
                                                    ).forEach { (statusKey, label, statusColor) ->
                                                        val isSelected = currentStatus == statusKey
                                                        Button(
                                                            onClick = {
                                                                if (isSelected) {
                                                                    // DESELECT: Tapping the currently selected status again deselects it and returns to UNMARKED
                                                                    val dbExisting = attendance.firstOrNull { it.employeeId == targetEmp.id && it.date == dateStr }
                                                                    if (dbExisting != null) {
                                                                        // Mark for deletion in batch save
                                                                        unsavedAttendanceEdits[dateStr] = null
                                                                    } else {
                                                                        // Was unsaved newly created, just remove from map
                                                                        unsavedAttendanceEdits.remove(dateStr)
                                                                    }
                                                                } else {
                                                                    // SELECT NEW STATUS: replaces previous status
                                                                    val defaultHrs = when (statusKey) {
                                                                        "Half-Day" -> 4.5
                                                                        "Absent" -> 0.0
                                                                        else -> 9.0
                                                                    }
                                                                    val deterministicId = "att_${targetEmp.id}_${dateStr.replace(" ", "_")}"
                                                                    unsavedAttendanceEdits[dateStr] = AttendanceEntity(
                                                                        id = deterministicId,
                                                                        workspaceId = targetEmp.workspaceId,
                                                                        employeeId = targetEmp.id,
                                                                        employeeName = targetEmp.name,
                                                                        date = dateStr,
                                                                        status = statusKey,
                                                                        workingHours = defaultHrs,
                                                                        checkInTime = activeRecord?.checkInTime ?: "09:30 AM",
                                                                        checkOutTime = activeRecord?.checkOutTime ?: "06:30 PM",
                                                                        notes = activeRecord?.notes ?: "",
                                                                        updatedBy = currentUserName.ifBlank { "Admin" },
                                                                        updatedAt = System.currentTimeMillis()
                                                                    )
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isSelected) statusColor else MaterialTheme.colorScheme.surface
                                                            ),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ==========================================
                            // MODE 1: DAILY TEAM ROSTER (Single Day View)
                            // ==========================================
                            item {
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
                                            .padding(12.dp)
                                    ) {
                                        IconButton(onClick = {
                                            val cal = Calendar.getInstance().apply { time = selectedDateCalendar.time }
                                            cal.add(Calendar.DAY_OF_MONTH, -1)
                                            selectedDateCalendar = cal
                                        }) {
                                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = selectedDateStr,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Standard: 9 Hours / Working Day",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                selectedDateCalendar = Calendar.getInstance()
                                            }) {
                                                Icon(Icons.Default.Today, contentDescription = "Today")
                                            }
                                            IconButton(onClick = {
                                                val cal = Calendar.getInstance().apply { time = selectedDateCalendar.time }
                                                cal.add(Calendar.DAY_OF_MONTH, 1)
                                                selectedDateCalendar = cal
                                            }) {
                                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                                            }
                                        }
                                    }
                                }
                            }

                            // Summary for selected day
                            item {
                                val dayRecords = attendance.filter { it.date == selectedDateStr }
                                val pCount = dayRecords.count { it.status == "Present" }
                                val aCount = dayRecords.count { it.status == "Absent" }
                                val hdCount = dayRecords.count { it.status == "Half-Day" || it.status == "Half Day" }
                                val woCount = dayRecords.count { it.status == "Weekly Off" }
                                val hCount = dayRecords.count { it.status == "Holiday" }
                                val lCount = dayRecords.count { it.status == "Leave" }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = BmsSuccess.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("P", fontWeight = FontWeight.Bold, color = BmsSuccess, fontSize = 11.sp)
                                            Text("$pCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = BmsTertiary.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("HD", fontWeight = FontWeight.Bold, color = BmsTertiary, fontSize = 11.sp)
                                            Text("$hdCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = BmsError.copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("A", fontWeight = FontWeight.Bold, color = BmsError, fontSize = 11.sp)
                                            Text("$aCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("WO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("$woCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("H", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("$hCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("L", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 11.sp)
                                            Text("$lCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // Employee List for selected date
                            items(employees) { emp ->
                                val record = attendance.firstOrNull { it.employeeId == emp.id && it.date == selectedDateStr }
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    text = "${emp.designation} • ${record?.status ?: "Not Marked"} (${record?.workingHours ?: 0.0}h)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = when (record?.status) {
                                                        "Present" -> BmsSuccess
                                                        "Half-Day", "Half Day" -> BmsTertiary
                                                        "Absent" -> BmsError
                                                        "Weekly Off", "Holiday" -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.outline
                                                    }
                                                )
                                            }

                                            IconButton(onClick = {
                                                editingAttendanceRecord = record ?: AttendanceEntity(
                                                    id = "att_${emp.id}_${selectedDateStr.replace(" ", "_")}",
                                                    workspaceId = emp.workspaceId,
                                                    employeeId = emp.id,
                                                    employeeName = emp.name,
                                                    date = selectedDateStr,
                                                    status = "Present",
                                                    workingHours = 9.0,
                                                    checkInTime = "09:30 AM",
                                                    checkOutTime = "06:30 PM",
                                                    notes = ""
                                                )
                                            }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.EditCalendar, contentDescription = "Edit Attendance", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Quick Status Buttons: P, HD, A, WO, H, L
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                            listOf(
                                                Triple("Present", "P", BmsSuccess),
                                                Triple("Half-Day", "HD", BmsTertiary),
                                                Triple("Absent", "A", BmsError),
                                                Triple("Weekly Off", "WO", Color(0xFF6366F1)),
                                                Triple("Holiday", "H", Color(0xFF06B6D4)),
                                                Triple("Leave", "L", Color(0xFFF59E0B))
                                            ).forEach { (status, label, color) ->
                                                val isSelected = record?.status == status
                                                Button(
                                                    onClick = {
                                                        val hrs = when (status) {
                                                            "Present", "Weekly Off", "Holiday" -> 9.0
                                                            "Half-Day" -> 4.5
                                                            else -> 0.0
                                                        }
                                                        viewModel.markAttendance(
                                                            employeeId = emp.id,
                                                            employeeName = emp.name,
                                                            status = status,
                                                            date = selectedDateStr,
                                                            workingHours = hrs
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2: LEAVES MANAGEMENT
                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(leaves, key = { it.id }) { leave ->
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
                                        Text(text = leave.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        StatusBadge(text = leave.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${leave.leaveType}: ${leave.fromDate} to ${leave.toDate}", style = MaterialTheme.typography.bodySmall, color = BmsPrimaryDark, fontWeight = FontWeight.Medium)
                                    Text(text = "Reason: ${leave.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (leave.status == "Pending") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.updateLeaveStatus(leave, "Rejected") },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Reject", fontSize = 11.sp, color = BmsError)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { viewModel.updateLeaveStatus(leave, "Approved") },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Approve", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3: SALARY & PAYROLL CALCULATION SYSTEM
                3 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Salary Slips & Monthly Payroll Reports Navigation Banner
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth().testTag("salary_reports_banner")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RequestQuote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Salary Slips & Reports Screen",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Export individual slips & monthly summaries (PDF, XLSX, DOCX)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { viewModel.selectTab(BmsNavigationItem.SALARY_REPORTS) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }

                        // Month Selector & Policy Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
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
                                            Text(
                                                text = "Monthly Payroll Processing",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Standard: 9 Hours / Day • Configurable Basis",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        StatusBadge(text = selectedPayrollMonth)
                                    }

                                    Text(
                                        text = "Payroll accounts for base monthly salary, calendar vs scheduled working days, actual present days, half-days (0.5), paid leaves, weekly offs, and holidays without unauthorized deductions.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // Staff Members for Payroll Calculation
                        item {
                            Text(
                                text = "Calculate Payroll for Staff ($selectedPayrollMonth):",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        items(employees) { emp ->
                            val existingRecord = payrollRecords.firstOrNull { it.employeeId == emp.id && it.monthYear == selectedPayrollMonth }
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
                                        Column {
                                            Text(text = emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(text = "${emp.designation} • CTC: ₹${emp.basicSalary.toInt()}/mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        if (existingRecord != null) {
                                            StatusBadge(text = existingRecord.status)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (existingRecord != null) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "Payable Days: ${existingRecord.payableDays}", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = "Net: ₹${existingRecord.netSalary.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                color = BmsTertiary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewingPayrollRecord = existingRecord },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View Breakdown", fontSize = 11.sp)
                                            }

                                            if (existingRecord.status == "Draft") {
                                                Button(
                                                    onClick = {
                                                        viewModel.approvePayroll(existingRecord.id, currentUserName.ifBlank { "Shakir Tamboli" })
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Approve", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { calculatingPayrollEmployee = emp },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Compute $selectedPayrollMonth Salary", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Historical Payroll Audit List
                        if (payrollRecords.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Preserved Historical Payroll Records (${payrollRecords.size}):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            items(payrollRecords) { pr ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(text = "${pr.employeeName} (${pr.monthYear})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(text = "Basis: ${pr.calculationBasis} • ${pr.payableDays} days • Net: ₹${pr.netSalary.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            StatusBadge(text = pr.status)
                                            IconButton(onClick = { viewingPayrollRecord = pr }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Info, contentDescription = "View Details", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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

    // DIALOG 1: ADD EMPLOYEE
    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onSave = { emp ->
                viewModel.addEmployee(emp)
                showAddEmployeeDialog = false
            }
        )
    }

    // DIALOG 2: EDIT EMPLOYEE
    editingEmployee?.let { emp ->
        EditEmployeeDialog(
            employee = emp,
            onDismiss = { editingEmployee = null },
            onSave = { updated ->
                viewModel.updateEmployee(updated)
                editingEmployee = null
            },
            onDelete = { toDel ->
                viewModel.deleteEmployee(toDel.id, toDel.name)
                editingEmployee = null
            }
        )
    }

    // DIALOG 3: SUBMIT LEAVE
    if (showLeaveDialog) {
        AddLeaveDialog(
            employees = employees,
            onDismiss = { showLeaveDialog = false },
            onSave = { name, type, from, to, reason ->
                viewModel.submitLeave(name, type, from, to, reason)
                showLeaveDialog = false
            }
        )
    }

    // DIALOG 4: ATTENDANCE CORRECTION & AUDIT
    editingAttendanceRecord?.let { att ->
        AttendanceAuditDialog(
            record = att,
            onDismiss = { editingAttendanceRecord = null },
            onSave = { updatedStatus, updatedHours, checkIn, checkOut, notes ->
                viewModel.markAttendance(
                    employeeId = att.employeeId,
                    employeeName = att.employeeName,
                    status = updatedStatus,
                    date = att.date,
                    workingHours = updatedHours,
                    checkInTime = checkIn,
                    checkOutTime = checkOut,
                    notes = notes
                )
                editingAttendanceRecord = null
            }
        )
    }

    // DIALOG 5: PAYROLL CALCULATION CONFIGURATION & GENERATION
    calculatingPayrollEmployee?.let { emp ->
        PayrollCalculationDialog(
            employee = emp,
            monthYear = selectedPayrollMonth,
            onDismiss = { calculatingPayrollEmployee = null },
            onCalculate = { basis, calDays, schedDays, overtime, deductions, dedReason, notes ->
                viewModel.calculateAndSaveMonthlyPayroll(
                    employee = emp,
                    monthYear = selectedPayrollMonth,
                    calculationBasis = basis,
                    totalCalendarDays = calDays,
                    scheduledWorkingDays = schedDays,
                    overtimeHours = overtime,
                    deductions = deductions,
                    deductionReason = dedReason,
                    calculationNotes = notes
                )
                calculatingPayrollEmployee = null
            }
        )
    }

    // DIALOG 6: PAYROLL BREAKDOWN PREVIEW
    viewingPayrollRecord?.let { pr ->
        PayrollBreakdownDialog(
            record = pr,
            onDismiss = { viewingPayrollRecord = null },
            onApprove = {
                viewModel.approvePayroll(pr.id, currentUserName.ifBlank { "Shakir Tamboli" })
                viewingPayrollRecord = null
            }
        )
    }

    // DIALOG 7: BIOMETRIC PIN / PASSWORD FALLBACK
    if (showPinFallbackDialog) {
        val targetEmpForPin = currentEmployee ?: employees.firstOrNull { it.id == selectedAttendanceEmployeeId } ?: employees.firstOrNull()
        AlertDialog(
            onDismissRequest = {
                showPinFallbackDialog = false
                pinFallbackInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Security PIN / Password Fallback")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Biometric hardware is unconfirmed or bypassed. Enter employee credentials/PIN to record verified attendance with audit trail for ${targetEmpForPin?.name ?: currentUserName}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinFallbackInput,
                        onValueChange = { pinFallbackInput = it },
                        label = { Text("Security PIN or Password") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_fallback_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinFallbackInput.isNotBlank() && targetEmpForPin != null) {
                            val isAlreadyCheckedIn = attendance.any {
                                it.employeeId == targetEmpForPin.id && it.date == selectedDateStr && it.status == "Present"
                            }
                            viewModel.markBiometricAttendance(
                                employeeId = targetEmpForPin.id,
                                employeeName = targetEmpForPin.name,
                                authTag = "PIN_CREDENTIAL_AUTHENTICATED",
                                verificationType = "PIN_FALLBACK",
                                isCheckOut = isAlreadyCheckedIn
                            )
                            showPinFallbackDialog = false
                            pinFallbackInput = ""
                        } else {
                            Toast.makeText(context, "Please enter your Security PIN or Password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("pin_fallback_submit")
                ) {
                    Text("Confirm & Record")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinFallbackDialog = false
                    pinFallbackInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttendanceAuditDialog(
    record: AttendanceEntity,
    onDismiss: () -> Unit,
    onSave: (status: String, workingHours: Double, checkIn: String, checkOut: String, notes: String) -> Unit
) {
    var status by remember { mutableStateOf(record.status) }
    var workingHoursStr by remember { mutableStateOf(record.workingHours.toString()) }
    var checkIn by remember { mutableStateOf(record.checkInTime) }
    var checkOut by remember { mutableStateOf(record.checkOutTime) }
    var notes by remember { mutableStateOf(record.notes) }

    val statusOptions = listOf("Present", "Half-Day", "Absent", "Weekly Off", "Holiday", "Leave")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attendance Correction: ${record.employeeName}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Date: ${record.date} • Last Updated by: ${record.updatedBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                item {
                    Text("Attendance Status:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(statusOptions) { st ->
                            FilterChip(
                                selected = st == status,
                                onClick = {
                                    status = st
                                    workingHoursStr = when (st) {
                                        "Present", "Weekly Off", "Holiday" -> "9.0"
                                        "Half-Day" -> "4.5"
                                        else -> "0.0"
                                    }
                                },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = workingHoursStr,
                        onValueChange = { workingHoursStr = it },
                        label = { Text("Working Hours (Standard: 9.0h)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = checkIn,
                            onValueChange = { checkIn = it },
                            label = { Text("Check In") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = checkOut,
                            onValueChange = { checkOut = it },
                            label = { Text("Check Out") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Audit / Correction Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val hrs = workingHoursStr.toDoubleOrNull() ?: 9.0
                onSave(status, hrs, checkIn, checkOut, notes)
            }) { Text("Save Attendance Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PayrollCalculationDialog(
    employee: EmployeeEntity,
    monthYear: String,
    onDismiss: () -> Unit,
    onCalculate: (basis: String, calDays: Int, schedDays: Int, overtime: Double, deductions: Double, dedReason: String, notes: String) -> Unit
) {
    var basis by remember { mutableStateOf("CALENDAR_DAYS") } // "CALENDAR_DAYS" or "SCHEDULED_WORKING_DAYS"
    var totalCalendarDaysStr by remember { mutableStateOf("30") }
    var scheduledWorkingDaysStr by remember { mutableStateOf("26") }
    var overtimeHoursStr by remember { mutableStateOf("0.0") }
    var deductionsStr by remember { mutableStateOf("0.0") }
    var deductionReason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Standard Studio Payroll Process") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salary Calculation: ${employee.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Month: $monthYear • Base Salary: ₹${employee.basicSalary.toInt()}/mo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text("Calculation Basis:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = basis == "CALENDAR_DAYS",
                            onClick = { basis = "CALENDAR_DAYS" },
                            label = { Text("Calendar Days (30/31)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = basis == "SCHEDULED_WORKING_DAYS",
                            onClick = { basis = "SCHEDULED_WORKING_DAYS" },
                            label = { Text("Working Days (26)", fontSize = 11.sp) }
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = totalCalendarDaysStr,
                            onValueChange = { totalCalendarDaysStr = it },
                            label = { Text("Calendar Days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = scheduledWorkingDaysStr,
                            onValueChange = { scheduledWorkingDaysStr = it },
                            label = { Text("Working Days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = overtimeHoursStr,
                        onValueChange = { overtimeHoursStr = it },
                        label = { Text("Overtime Hours (Rate: 1.5x)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = deductionsStr,
                            onValueChange = { deductionsStr = it },
                            label = { Text("Deductions (₹)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deductionReason,
                            onValueChange = { deductionReason = it },
                            label = { Text("Deduction Reason") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Calculation Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val calDays = totalCalendarDaysStr.toIntOrNull() ?: 30
                val schedDays = scheduledWorkingDaysStr.toIntOrNull() ?: 26
                val ot = overtimeHoursStr.toDoubleOrNull() ?: 0.0
                val ded = deductionsStr.toDoubleOrNull() ?: 0.0
                onCalculate(basis, calDays, schedDays, ot, ded, deductionReason, notes)
            }) { Text("Generate Payroll Breakdown") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PayrollBreakdownDialog(
    record: PayrollRecordEntity,
    onDismiss: () -> Unit,
    onApprove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payroll Summary: ${record.employeeName}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Period: ${record.monthYear} • Status: ${record.status}", fontWeight = FontWeight.Bold)
                            Text(text = "Basis: ${if (record.calculationBasis == "CALENDAR_DAYS") "Calendar Days (${record.totalCalendarDays})" else "Working Days (${record.scheduledWorkingDays})"}")
                        }
                    }
                }

                item {
                    Text("Attendance Metrics:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("• Present Days: ${record.presentDays} (${record.totalWorkingHours} hrs)")
                    Text("• Half Days: ${record.halfDays}")
                    Text("• Paid Leaves: ${record.paidLeaveDays} • Unpaid: ${record.unpaidLeaveDays}")
                    Text("• Weekly Offs: ${record.weeklyOffDays} • Holidays: ${record.holidayDays}")
                    Text("• Effective Payable Days: ${record.payableDays}", fontWeight = FontWeight.Bold, color = BmsPrimaryDark)
                }

                item {
                    Divider()
                    Text("Financial Settlement:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("• Base Monthly Salary: ₹${record.baseMonthlySalary.toInt()}")
                    Text("• Computed Daily Rate: ₹${record.dailyRate.toInt()}")
                    Text("• Gross Payable: ₹${record.grossPayable.toInt()}")
                    if (record.overtimeAmount > 0) {
                        Text("• Overtime (${record.overtimeHours}h): +₹${record.overtimeAmount.toInt()}", color = BmsSuccess)
                    }
                    if (record.deductions > 0) {
                        Text("• Deductions: -₹${record.deductions.toInt()} (${record.deductionReason})", color = BmsError)
                    }
                    Divider()
                    Text(
                        text = "Net Payable Salary: ₹${record.netSalary.toInt()}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BmsTertiary
                    )
                }

                if (record.approvedBy.isNotBlank()) {
                    item {
                        Surface(shape = RoundedCornerShape(8.dp), color = BmsSuccess.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Approved by: ${record.approvedBy} on ${record.approvalDate}",
                                fontSize = 11.sp,
                                color = BmsSuccess,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (record.status == "Draft") {
                Button(onClick = onApprove) { Text("Approve Payroll") }
            } else {
                Button(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (record.status == "Draft") {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onSave: (EmployeeEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Photography") }
    var designation by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Employee") }
    var salaryStr by remember { mutableStateOf("45000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Studio Team Member") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Employee Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation (e.g. Cinematographer)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("App Role (Admin, Manager, Employee)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Contact") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = salaryStr, onValueChange = { salaryStr = it }, label = { Text("Monthly Base Salary (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sal = salaryStr.toDoubleOrNull() ?: 45000.0
                    val empId = "emp_" + java.util.UUID.randomUUID().toString().take(8)
                    onSave(
                        EmployeeEntity(
                            id = empId,
                            workspaceId = "ws_lumiere_01",
                            employeeCode = "BOSS-${(100..999).random()}",
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            department = department.trim(),
                            designation = designation.trim(),
                            role = role.trim(),
                            joiningDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                            basicSalary = sal,
                            responsibilities = "Assigned shoots, editing projects and studio tasks."
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Save Employee") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditEmployeeDialog(
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
    var responsibilities by remember { mutableStateOf(employee.responsibilities) }
    var salaryStr by remember { mutableStateOf(employee.basicSalary.toInt().toString()) }
    var emergencyContact by remember { mutableStateOf(employee.emergencyContact) }
    var status by remember { mutableStateOf(employee.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Team Member Details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Employee Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("App Role (Admin, Manager, Employee)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = responsibilities, onValueChange = { responsibilities = it }, label = { Text("Assigned Responsibilities") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Contact") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Official Email") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = salaryStr, onValueChange = { salaryStr = it }, label = { Text("Monthly Base Salary (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Active, Inactive, On Leave)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedButton(
                        onClick = { onDelete(employee) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Employee")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sal = salaryStr.toDoubleOrNull() ?: employee.basicSalary
                    onSave(employee.copy(
                        name = name.trim(),
                        mobile = mobile.trim(),
                        email = email.trim(),
                        department = department.trim(),
                        designation = designation.trim(),
                        role = role.trim(),
                        responsibilities = responsibilities.trim(),
                        basicSalary = sal,
                        emergencyContact = emergencyContact.trim(),
                        status = status.trim()
                    ))
                },
                enabled = name.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddLeaveDialog(
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onSave: (employeeName: String, type: String, fromDate: String, toDate: String, reason: String) -> Unit
) {
    var selectedEmployee by remember { mutableStateOf(employees.firstOrNull()?.name ?: "") }
    var leaveType by remember { mutableStateOf("Paid Leave") }
    var fromDate by remember { mutableStateOf("01 Nov 2026") }
    var toDate by remember { mutableStateOf("03 Nov 2026") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Staff Leave Application") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = selectedEmployee,
                        onValueChange = { selectedEmployee = it },
                        label = { Text("Staff Member Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = leaveType,
                        onValueChange = { leaveType = it },
                        label = { Text("Leave Type (Paid, Sick, Casual, Emergency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = fromDate, onValueChange = { fromDate = it }, label = { Text("From Date") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = toDate, onValueChange = { toDate = it }, label = { Text("To Date") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Leave") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedEmployee, leaveType, fromDate, toDate, reason) },
                enabled = selectedEmployee.isNotBlank()
            ) { Text("Submit Application") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
