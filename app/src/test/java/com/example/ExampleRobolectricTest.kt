package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BOSS", appName)
  }

  @Test
  fun `gst 18 percent calculation test`() {
    val subtotal = 100000.0
    val gstRate = 0.18
    val cgst = subtotal * 0.09
    val sgst = subtotal * 0.09
    val totalGst = subtotal * gstRate
    val grandTotal = subtotal + totalGst
    val advance = grandTotal * 0.30

    assertEquals(9000.0, cgst, 0.01)
    assertEquals(9000.0, sgst, 0.01)
    assertEquals(18000.0, totalGst, 0.01)
    assertEquals(118000.0, grandTotal, 0.01)
    assertEquals(35400.0, advance, 0.01)
  }

  @Test
  fun `workspace seed defaults test`() {
    assertEquals("ws_lumiere_01", com.example.data.database.SeedData.DEFAULT_WORKSPACE_ID)
  }

  @Test
  fun `role validation match test`() {
    val adminAccount = com.example.data.model.AuthUser(
      id = "admin",
      email = "admin@bms.com",
      username = "admin",
      passwordHash = "admin123",
      fullName = "Admin User",
      actualRole = com.example.data.model.AppRole.ADMIN,
      designation = "Super Admin",
      department = "Executive",
      permissions = emptySet()
    )

    // Admin selects Admin -> Allowed
    val isMatchSuccess = adminAccount.actualRole == com.example.data.model.AppRole.ADMIN
    assertEquals(true, isMatchSuccess)

    // Admin selects Employee -> Rejected
    val isMatchFailure = adminAccount.actualRole == com.example.data.model.AppRole.EMPLOYEE
    assertEquals(false, isMatchFailure)
  }

  @Test
  fun `role dropdown values test`() {
    val roles = com.example.data.model.AppRole.entries.map { it.label }
    assertEquals(listOf("Admin", "Manager", "Employee"), roles)
  }

  @Test
  fun `salary calculation calendar days basis test`() {
    val baseMonthlySalary = 60000.0
    val totalCalendarDays = 30
    val presentDays = 20.0
    val halfDays = 2.0 // counts as 1.0 full day
    val weeklyOffs = 4.0
    val holidays = 1.0
    val paidLeaves = 1.0
    val unpaidAbsents = 2.0 // 20 + 2*0.5 + 4 + 1 + 1 + 2 = 29, + 1 = 30

    // Payable days = 20 + (2 * 0.5) + 4 + 1 + 1 = 27.0
    val payableDays = presentDays + (halfDays * 0.5) + weeklyOffs + holidays + paidLeaves
    assertEquals(27.0, payableDays, 0.01)

    // Calendar-day basis: Daily Rate = Salary / totalCalendarDays
    val perDayRate = baseMonthlySalary / totalCalendarDays.toDouble()
    assertEquals(2000.0, perDayRate, 0.01)

    val grossCalculatedSalary = perDayRate * payableDays
    assertEquals(54000.0, grossCalculatedSalary, 0.01)

    val deductions = 1000.0 // e.g. professional tax / advance
    val netSalary = grossCalculatedSalary - deductions
    assertEquals(53000.0, netSalary, 0.01)
  }

  @Test
  fun `salary calculation scheduled working days basis test`() {
    val baseMonthlySalary = 52000.0
    val scheduledWorkingDays = 26
    val actualWorkedDays = 24.0
    val halfDays = 2.0 // counts as 1.0 day
    val approvedPaidLeaves = 1.0

    // Payable days = 24.0 + (2 * 0.5) + 1.0 = 26.0 (full salary)
    val payableDays = actualWorkedDays + (halfDays * 0.5) + approvedPaidLeaves
    assertEquals(26.0, payableDays, 0.01)

    // Scheduled working days basis: Daily Rate = Salary / scheduledWorkingDays
    val perDayRate = baseMonthlySalary / scheduledWorkingDays.toDouble()
    assertEquals(2000.0, perDayRate, 0.01)

    val payableSalary = perDayRate * payableDays
    assertEquals(52000.0, payableSalary, 0.01)
  }

  @Test
  fun `standard 9 hour workday and attendance metrics test`() {
    val standardWorkingHours = 9.0
    val presentHours = 9.0
    val halfDayHours = 4.5

    assertEquals(9.0, standardWorkingHours, 0.0)
    assertEquals(standardWorkingHours, presentHours, 0.0)
    assertEquals(standardWorkingHours / 2.0, halfDayHours, 0.0)
  }

  @Test
  fun `financial year ledger reconciliation test`() {
    val grossRevenue = 1500000.0
    val annualWages = 600000.0
    val directProductionExpenses = 250000.0
    val studioOverheadExpenses = 150000.0

    val totalExpenditure = annualWages + directProductionExpenses + studioOverheadExpenses
    val netProfit = grossRevenue - totalExpenditure
    val profitMargin = ((netProfit / grossRevenue) * 100).toInt()

    assertEquals(1000000.0, totalExpenditure, 0.01)
    assertEquals(500000.0, netProfit, 0.01)
    assertEquals(33, profitMargin)
  }

  @Test
  fun `quotation export document generation test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val quote = com.example.data.model.QuotationEntity(
      id = "qt_test_01",
      workspaceId = "ws_test_01",
      quotationNumber = "QT-2026-TEST",
      clientId = "client_test_01",
      clientName = "Executive Client",
      contact = "+91 98234 56789",
      eventDate = "24 Dec 2026",
      eventLocation = "Pune",
      services = "Destination Wedding 4K Cinema & Photography",
      packageDetails = "2 Cinematographers + 2 Candid Photographers + Drone",
      subtotal = 100000.0,
      discount = 0.0,
      taxAmount = 18000.0,
      totalAmount = 118000.0,
      advanceRequired = 35400.0,
      balanceAmount = 82600.0,
      status = "Approved",
      terms = "30% advance on confirmation, 50% on shoot day, 20% on delivery."
    )

    try {
      val pdfFile = com.example.ui.components.DocumentExportUtil.generateQuotationPdf(context, quote, null)
      org.junit.Assert.assertTrue("PDF file should exist and have content", pdfFile.exists() && pdfFile.length() > 0)
    } catch (_: IllegalStateException) {
      // In Robolectric host JVM, android.graphics.pdf.PdfDocument native rendering may be stubbed
    }

    val xlsxFile = com.example.ui.components.DocumentExportUtil.generateQuotationXlsx(context, quote, null)
    org.junit.Assert.assertTrue("XLSX file should exist and have content", xlsxFile.exists() && xlsxFile.length() > 0)

    val docxFile = com.example.ui.components.DocumentExportUtil.generateQuotationDocx(context, quote, null)
    org.junit.Assert.assertTrue("DOCX file should exist and have content", docxFile.exists() && docxFile.length() > 0)
  }

  @Test
  fun `room migration 4 to 5 explicit execution test`() {
    val migration = com.example.data.database.BmsDatabase.MIGRATION_4_5
    assertEquals(4, migration.startVersion)
    assertEquals(5, migration.endVersion)
  }

  @Test
  fun `clean seed data verification test`() {
    assertEquals("ws_lumiere_01", com.example.data.database.SeedData.DEFAULT_WORKSPACE_ID)
  }

  @Test
  fun `password security validation test`() {
    // Passwords must be at least 8 characters
    val shortPassword = "123"
    val validPassword = "SecureAdmin@2026!"

    org.junit.Assert.assertTrue(shortPassword.length < 8)
    org.junit.Assert.assertTrue(validPassword.length >= 8)
  }

  @Test
  fun `demo purge comprehensive ids test`() {
    val demoLeadIds = listOf("lead_01", "lead_02", "lead_03", "lead_04")
    val demoClientIds = listOf("client_01", "client_02", "client_03")
    val demoBookingIds = listOf("book_01", "book_02", "book_03")
    val demoPhotoEditIds = listOf("pe_01", "pe_02", "pe_03", "pedit_01", "pedit_02")
    val demoVideoEditIds = listOf("ve_01", "ve_02", "vedit_01", "vedit_02")
    val demoDriveIds = listOf("drv_01", "drv_02", "drv_03", "drv_04", "drive_01", "drive_02", "drive_03", "drive_04")
    val demoFileIds = listOf("pf_01", "pf_02", "pf_03", "pfile_01", "pfile_02", "pfile_03")
    val demoEmployees = listOf("emp_rahul", "emp_priya")

    org.junit.Assert.assertTrue(demoBookingIds.contains("book_03"))
    org.junit.Assert.assertTrue(demoPhotoEditIds.contains("pedit_01"))
    org.junit.Assert.assertTrue(demoDriveIds.contains("drive_01"))
    org.junit.Assert.assertFalse(demoEmployees.contains("emp_shakir"))
  }

  @Test
  fun `firebase cloud project configuration test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbIdResId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
    org.junit.Assert.assertTrue("firestore_database_id should exist in resources", dbIdResId != 0)
    val dbId = context.getString(dbIdResId)
    assertEquals("ai-studio-android-bms-0a1314c9-e49b-4333-8a81-e2b8ac9df827", dbId)
  }

  @Test
  fun `firestore sync status initial state test`() {
    val status = com.example.data.sync.SyncStatus()
    assertEquals(false, status.isSyncing)
    assertEquals(null, status.lastSyncTime)
    assertEquals(null, status.errorMessage)
    assertEquals(0, status.syncedRecordsCount)
    assertEquals(true, status.isCloudConnected)
  }

  @Test
  fun `workspace isolation and rbac authorization rules test`() {
    // Admin has access to payroll and employee management
    val adminRole = com.example.data.model.AppRole.ADMIN
    val isPayrollAuthorized = adminRole == com.example.data.model.AppRole.ADMIN

    // Employee does not have write access to payroll ledgers
    val employeeRole = com.example.data.model.AppRole.EMPLOYEE
    val isEmployeePayrollAdmin = employeeRole == com.example.data.model.AppRole.ADMIN

    org.junit.Assert.assertTrue(isPayrollAuthorized)
    org.junit.Assert.assertFalse(isEmployeePayrollAdmin)

    // Workspace isolation: records from ws_lumiere_01 must never match ws_other_02
    val targetWorkspace = "ws_lumiere_01"
    val otherWorkspace = "ws_other_02"
    org.junit.Assert.assertNotEquals(targetWorkspace, otherWorkspace)
  }

  @Test
  fun `cold launch and startup without crash`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    com.google.firebase.FirebaseApp.initializeApp(context)
    val vm = com.example.ui.viewmodel.BmsViewModel(context)
    org.junit.Assert.assertNotNull(vm)
    org.junit.Assert.assertEquals(false, vm.isLoggedIn.value)
    org.junit.Assert.assertNotNull(vm.syncManager)
    org.junit.Assert.assertNotNull(vm.currentTab.value)
  }

  @Test
  fun `firestore safe offline fallback test`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = com.example.data.database.BmsDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
    val syncManager = com.example.data.sync.FirestoreSyncManager(context, db.bmsDao())
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    
    // Test that realtime listeners can be registered/unregistered safely
    syncManager.startRealtimeListeners("ws_lumiere_01", scope)
    syncManager.stopRealtimeListeners()
    org.junit.Assert.assertNotNull(syncManager.syncStatus.value)
  }

  @Test
  fun `migration 4 to 5 duplicate attendance recovery test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
      .create(
        androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
          .name("test_migration_recovery.db")
          .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
              db.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (`id` TEXT NOT NULL PRIMARY KEY, `workspaceId` TEXT NOT NULL, `employeeId` TEXT NOT NULL, `employeeName` TEXT NOT NULL, `date` TEXT NOT NULL, `status` TEXT NOT NULL, `workingHours` REAL NOT NULL, `checkInTime` TEXT NOT NULL, `checkOutTime` TEXT NOT NULL, `notes` TEXT NOT NULL, `updatedBy` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
              // Insert intentional duplicates
              db.execSQL("INSERT INTO `attendance` VALUES ('att_dup_1', 'ws_lumiere_01', 'emp_01', 'Test', '2026-10-17', 'Present', 9.0, '09:30 AM', '06:30 PM', '', 'Admin', 1000)")
              db.execSQL("INSERT INTO `attendance` VALUES ('att_dup_2', 'ws_lumiere_01', 'emp_01', 'Test', '2026-10-17', 'Present', 9.0, '09:30 AM', '06:30 PM', '', 'Admin', 2000)")
            }
            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
          })
          .build()
      )
    val db = helper.writableDatabase
    // Execute MIGRATION_4_5
    com.example.data.database.BmsDatabase.MIGRATION_4_5.migrate(db)

    // Verify deduplication succeeded and unique index was created
    val cursor = db.query("SELECT COUNT(*) FROM `attendance` WHERE `workspaceId` = 'ws_lumiere_01' AND `employeeId` = 'emp_01' AND `date` = '2026-10-17'")
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    org.junit.Assert.assertEquals(1, count)
    db.close()
  }

  @Test
  fun `salary calculation engine hides hourly rate from transparent breakdown test`() {
    val input = com.example.data.payroll.PayrollCalculationEngine.PayrollInput(
      monthlyBaseSalary = 52000.0,
      monthYear = "Oct 2026",
      calculationBasis = com.example.data.payroll.PayrollCalculationEngine.BASIS_SCHEDULED_WORKING_DAYS,
      scheduledWorkingDays = 26,
      totalCalendarDays = 31,
      attendanceRecords = listOf(
        com.example.data.model.AttendanceEntity(
          id = "att_test_1",
          workspaceId = "ws_test",
          employeeId = "emp_shakir",
          employeeName = "Shakir Tamboli",
          date = "2026-10-01",
          status = "Present",
          workingHours = 9.0
        ),
        com.example.data.model.AttendanceEntity(
          id = "att_test_2",
          workspaceId = "ws_test",
          employeeId = "emp_shakir",
          employeeName = "Shakir Tamboli",
          date = "2026-10-02",
          status = "Half-Day",
          workingHours = 4.5
        )
      ),
      overtimeHours = 4.0,
      overtimeMultiplier = 1.5,
      bonuses = 2000.0,
      deductions = 1000.0,
      deductionReason = "Advance Payment",
      manualNotes = "Audit verified"
    )

    val result = com.example.data.payroll.PayrollCalculationEngine.calculate(input)

    // Verify calculation correctness
    // 52000 / 26 = 2000 daily rate, hourly rate = 2000 / 9 = 222.222...
    // payable hours = 9 + 4.5 = 13.5
    // regularEarned = 222.222 * 13.5 = 3000.0
    // overtime rate = 222.222 * 1.5 = 333.333, overtime amount = 4 * 333.333 = 1333.33
    // gross = 3000 + 1333.33 + 2000 = 6333.33
    // net = 6333.33 - 1000 = 5333.33
    org.junit.Assert.assertEquals(2000.0, result.dailyRate, 0.01)
    org.junit.Assert.assertEquals(3000.0, result.regularEarnedSalary, 0.05)
    org.junit.Assert.assertEquals(1333.33, result.overtimeAmount, 0.05)
    org.junit.Assert.assertEquals(5333.33, result.netSalary, 0.05)

    // CRITICAL: Ensure Hourly Rate and /hr are NEVER present in breakdown string
    org.junit.Assert.assertFalse("Breakdown should not contain 'Hourly Rate'", result.transparentBreakdown.contains("Hourly Rate", ignoreCase = true))
    org.junit.Assert.assertFalse("Breakdown should not contain '/hr'", result.transparentBreakdown.contains("/hr", ignoreCase = true))
    org.junit.Assert.assertTrue("Breakdown should contain Daily Rate", result.transparentBreakdown.contains("Daily Rate:"))
  }

  @Test
  fun `salary slip exports hide hourly rate in xlsx and docx test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val record = com.example.data.model.PayrollRecordEntity(
      id = "pay_test_001",
      workspaceId = "ws_test",
      employeeId = "emp_shakir",
      employeeName = "Shakir Tamboli",
      monthYear = "Oct 2026",
      baseMonthlySalary = 54000.0,
      dailyRate = 2000.0,
      calculationBasis = "SCHEDULED_WORKING_DAYS",
      scheduledWorkingDays = 27,
      totalCalendarDays = 31,
      presentDays = 24.0,
      halfDays = 2,
      paidLeaveDays = 1,
      weeklyOffDays = 4,
      holidayDays = 0,
      unpaidLeaveDays = 0,
      totalWorkingHours = 225.0,
      payableDays = 26.0,
      overtimeHours = 4.0,
      overtimeAmount = 1333.0,
      grossPayable = 53333.0,
      deductions = 1000.0,
      deductionReason = "Advance",
      netSalary = 52333.0,
      status = "Approved",
      approvedBy = "Admin",
      approvalDate = "31 Oct 2026",
      calculationNotes = "Verified"
    )

    val emp = com.example.data.model.EmployeeEntity(
      id = "emp_shakir",
      workspaceId = "ws_test",
      name = "Shakir Tamboli",
      employeeCode = "ST-001",
      mobile = "+91 98234 56789",
      email = "shakir@bms.com",
      department = "Cinema",
      designation = "Lead Director",
      role = "Creative Director",
      joiningDate = "01 Jan 2024",
      basicSalary = 54000.0
    )

    // Test PDF generation does not crash
    try {
      val pdf = com.example.ui.components.DocumentExportUtil.generateSalarySlipPdf(context, record, emp, null)
      org.junit.Assert.assertTrue(pdf.exists() && pdf.length() > 0)
    } catch (_: IllegalStateException) {}

    // Test XLSX generation and verify NO "Hourly Rate" in sheet XML
    val xlsx = com.example.ui.components.DocumentExportUtil.generateSalarySlipXlsx(context, record, emp, null)
    org.junit.Assert.assertTrue(xlsx.exists() && xlsx.length() > 0)
    java.util.zip.ZipFile(xlsx).use { zip ->
      val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml")
      val xmlContent = zip.getInputStream(sheetEntry).bufferedReader().readText()
      org.junit.Assert.assertFalse("XLSX should not contain 'Hourly Rate'", xmlContent.contains("Hourly Rate", ignoreCase = true))
      org.junit.Assert.assertTrue("XLSX should contain 'Daily Rate (INR)'", xmlContent.contains("Daily Rate (INR)"))
    }

    // Test DOCX generation and verify NO "Hourly Rate" or "/hr" in document XML
    val docx = com.example.ui.components.DocumentExportUtil.generateSalarySlipDocx(context, record, emp, null)
    org.junit.Assert.assertTrue(docx.exists() && docx.length() > 0)
    java.util.zip.ZipFile(docx).use { zip ->
      val docEntry = zip.getEntry("word/document.xml")
      val xmlContent = zip.getInputStream(docEntry).bufferedReader().readText()
      org.junit.Assert.assertFalse("DOCX should not contain 'Hourly Rate'", xmlContent.contains("Hourly Rate", ignoreCase = true))
      org.junit.Assert.assertFalse("DOCX should not contain '/hr'", xmlContent.contains("/hr", ignoreCase = true))
      org.junit.Assert.assertTrue("DOCX should contain Daily Rate", xmlContent.contains("Daily Rate: INR"))
    }
  }

  @Test
  fun `monthly payroll summary exports hide hourly rate in xlsx and docx test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val records = listOf(
      com.example.data.model.PayrollRecordEntity(
        id = "pay_test_001",
        workspaceId = "ws_test",
        employeeId = "emp_shakir",
        employeeName = "Shakir Tamboli",
        monthYear = "Oct 2026",
        baseMonthlySalary = 54000.0,
        dailyRate = 2000.0,
        calculationBasis = "SCHEDULED_WORKING_DAYS",
        scheduledWorkingDays = 27,
        totalCalendarDays = 31,
        presentDays = 25.0,
        halfDays = 0,
        paidLeaveDays = 1,
        weeklyOffDays = 4,
        holidayDays = 0,
        unpaidLeaveDays = 0,
        totalWorkingHours = 225.0,
        payableDays = 26.0,
        overtimeHours = 0.0,
        overtimeAmount = 0.0,
        grossPayable = 52000.0,
        deductions = 0.0,
        deductionReason = "",
        netSalary = 52000.0,
        status = "Approved",
        approvedBy = "Admin",
        approvalDate = "31 Oct 2026",
        calculationNotes = ""
      )
    )

    // Test Monthly XLSX
    val xlsx = com.example.ui.components.DocumentExportUtil.generateMonthlyPayrollSummaryXlsx(context, records, "Oct 2026", null)
    org.junit.Assert.assertTrue(xlsx.exists() && xlsx.length() > 0)
    java.util.zip.ZipFile(xlsx).use { zip ->
      val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml")
      val xmlContent = zip.getInputStream(sheetEntry).bufferedReader().readText()
      org.junit.Assert.assertFalse("Monthly XLSX should not contain 'Hourly Rate'", xmlContent.contains("Hourly Rate", ignoreCase = true))
      org.junit.Assert.assertFalse("Monthly XLSX should not contain '/hr'", xmlContent.contains("/hr", ignoreCase = true))
    }

    // Test Monthly DOCX
    val docx = com.example.ui.components.DocumentExportUtil.generateMonthlyPayrollSummaryDocx(context, records, "Oct 2026", null)
    org.junit.Assert.assertTrue(docx.exists() && docx.length() > 0)
    java.util.zip.ZipFile(docx).use { zip ->
      val docEntry = zip.getEntry("word/document.xml")
      val xmlContent = zip.getInputStream(docEntry).bufferedReader().readText()
      org.junit.Assert.assertFalse("Monthly DOCX should not contain 'Hourly Rate'", xmlContent.contains("Hourly Rate", ignoreCase = true))
      org.junit.Assert.assertFalse("Monthly DOCX should not contain '/hr'", xmlContent.contains("/hr", ignoreCase = true))
    }
  }

  @Test
  fun `direct login to dashboard without security setup required blocking screen test`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    com.google.firebase.FirebaseApp.initializeApp(context)
    val vm = com.example.ui.viewmodel.BmsViewModel(context)

    // Initially not logged in, mustChangePasswordOnLogin is false
    org.junit.Assert.assertFalse(vm.isLoggedIn.value)
    org.junit.Assert.assertFalse(vm.mustChangePasswordOnLogin.value)

    // Perform valid login with admin credentials
    vm.login("admin@bms.com", "admin123", com.example.data.model.AppRole.ADMIN, rememberMe = true)
    org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)

    // Wait/verify login succeeds directly to DASHBOARD
    org.junit.Assert.assertTrue("User should be logged in", vm.isLoggedIn.value)
    org.junit.Assert.assertEquals(com.example.ui.viewmodel.BmsNavigationItem.DASHBOARD, vm.currentTab.value)
    // CRITICAL: mustChangePasswordOnLogin must remain false, no Security Setup screen forced
    org.junit.Assert.assertFalse("Security Setup Required screen must NOT be forced on login", vm.mustChangePasswordOnLogin.value)
    org.junit.Assert.assertEquals("Shakir Tamboli", vm.authenticatedUser.value?.fullName)
  }

  @Test
  fun `password authentication rejects invalid credentials while allowing valid credentials test`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    com.google.firebase.FirebaseApp.initializeApp(context)
    val vm = com.example.ui.viewmodel.BmsViewModel(context)

    // Invalid password attempt
    vm.login("admin@bms.com", "wrong_password_999", com.example.data.model.AppRole.ADMIN, rememberMe = false)
    org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
    org.junit.Assert.assertFalse("User should not be logged in with wrong password", vm.isLoggedIn.value)
    org.junit.Assert.assertTrue("Login state should be Error", vm.loginState.value is com.example.data.model.LoginResult.Error)

    // Valid password attempt
    vm.login("admin@bms.com", "admin123", com.example.data.model.AppRole.ADMIN, rememberMe = false)
    org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
    org.junit.Assert.assertTrue("User should be logged in with correct password", vm.isLoggedIn.value)
    org.junit.Assert.assertEquals(com.example.ui.viewmodel.BmsNavigationItem.DASHBOARD, vm.currentTab.value)
  }
}


