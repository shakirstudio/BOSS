package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.dao.BmsDao
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null,
    val syncedRecordsCount: Int = 0,
    val isCloudConnected: Boolean = true
)

class FirestoreSyncManager(
    private val context: Context,
    private val dao: BmsDao
) {
    private val tag = "FirestoreSyncManager"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val app = FirebaseApp.getInstance()
            val dbIdResId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            val dbId = if (dbIdResId != 0) context.getString(dbIdResId) else ""
            if (dbId.isNotBlank() && dbId != "(default)") {
                try {
                    FirebaseFirestore.getInstance(app, dbId)
                } catch (e: Exception) {
                    Log.w(tag, "Custom database $dbId unavailable, falling back to default instance", e)
                    FirebaseFirestore.getInstance(app)
                }
            } else {
                FirebaseFirestore.getInstance(app)
            }
        } catch (t: Throwable) {
            Log.w(tag, "Firestore is unavailable; operating in offline-safe mode", t)
            null
        }
    }

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()

    /**
     * Complete bidirectional synchronization between local Room SQLite and Cloud Firestore
     */
    suspend fun syncAll(workspaceId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (workspaceId.isBlank()) return@withContext Result.failure(IllegalArgumentException("Workspace ID is blank"))

        val fs = firestore ?: run {
            Log.w(tag, "Firestore unavailable, operating in offline-safe mode")
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isCloudConnected = false,
                errorMessage = "Cloud sync offline (local changes safe)"
            )
            return@withContext Result.failure(IllegalStateException("Cloud Firestore is offline"))
        }

        // Gate cloud writes behind authenticated Firebase session to prevent PERMISSION_DENIED
        val auth = try {
            com.google.firebase.auth.FirebaseAuth.getInstance()
        } catch (_: Exception) { null }

        if (auth?.currentUser == null) {
            Log.d(tag, "Local-first mode active: Sign in with cloud account to enable cloud synchronization")
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isCloudConnected = false,
                errorMessage = "Local-first mode: Sign in to sync with cloud"
            )
            return@withContext Result.success(0)
        }

        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, errorMessage = null)
        var totalRecords = 0

        try {
            // 1. Push Workspace metadata
            dao.getWorkspaceOnce(workspaceId)?.let { ws ->
                val wsMap = hashMapOf(
                    "id" to ws.id,
                    "name" to ws.name,
                    "businessType" to ws.businessType,
                    "city" to ws.city,
                    "phone" to ws.phone,
                    "email" to ws.email,
                    "address" to ws.address,
                    "gstNumber" to ws.gstNumber,
                    "tagline" to ws.tagline,
                    "planId" to ws.planId,
                    "isTrialActive" to ws.isTrialActive,
                    "trialDaysRemaining" to ws.trialDaysRemaining,
                    "updatedAt" to System.currentTimeMillis()
                )
                fs.collection("workspaces").document(ws.id).set(wsMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 2. Push & Reconcile Employees
            val localEmployees = dao.getEmployeesOnce(workspaceId)
            for (emp in localEmployees) {
                val empMap = hashMapOf(
                    "id" to emp.id,
                    "workspaceId" to emp.workspaceId,
                    "employeeCode" to emp.employeeCode,
                    "name" to emp.name,
                    "mobile" to emp.mobile,
                    "email" to emp.email,
                    "department" to emp.department,
                    "designation" to emp.designation,
                    "role" to emp.role,
                    "joiningDate" to emp.joiningDate,
                    "basicSalary" to emp.basicSalary,
                    "status" to emp.status,
                    "emergencyContact" to emp.emergencyContact,
                    "responsibilities" to emp.responsibilities,
                    "assignedPermissions" to emp.assignedPermissions,
                    "avatarUrl" to emp.avatarUrl,
                    "loginUsername" to emp.loginUsername,
                    "updatedAt" to System.currentTimeMillis()
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("employees").document(emp.id)
                    .set(empMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 3. Push & Reconcile Attendance
            val localAttendance = dao.getAttendanceOnce(workspaceId)
            for (att in localAttendance) {
                val attMap = hashMapOf(
                    "id" to att.id,
                    "workspaceId" to att.workspaceId,
                    "employeeId" to att.employeeId,
                    "employeeName" to att.employeeName,
                    "date" to att.date,
                    "status" to att.status,
                    "workingHours" to att.workingHours,
                    "checkInTime" to att.checkInTime,
                    "checkOutTime" to att.checkOutTime,
                    "notes" to att.notes,
                    "updatedBy" to att.updatedBy,
                    "updatedAt" to att.updatedAt
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("attendance").document(att.id)
                    .set(attMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 4. Push & Reconcile Payroll Records
            val localPayroll = dao.getPayrollRecordsOnce(workspaceId)
            for (pr in localPayroll) {
                val prMap = hashMapOf(
                    "id" to pr.id,
                    "workspaceId" to pr.workspaceId,
                    "employeeId" to pr.employeeId,
                    "employeeName" to pr.employeeName,
                    "monthYear" to pr.monthYear,
                    "baseMonthlySalary" to pr.baseMonthlySalary,
                    "totalCalendarDays" to pr.totalCalendarDays,
                    "scheduledWorkingDays" to pr.scheduledWorkingDays,
                    "calculationBasis" to pr.calculationBasis,
                    "standardDailyHours" to pr.standardDailyHours,
                    "presentDays" to pr.presentDays,
                    "halfDays" to pr.halfDays,
                    "paidLeaveDays" to pr.paidLeaveDays,
                    "unpaidLeaveDays" to pr.unpaidLeaveDays,
                    "weeklyOffDays" to pr.weeklyOffDays,
                    "holidayDays" to pr.holidayDays,
                    "payableDays" to pr.payableDays,
                    "totalWorkingHours" to pr.totalWorkingHours,
                    "dailyRate" to pr.dailyRate,
                    "grossPayable" to pr.grossPayable,
                    "deductions" to pr.deductions,
                    "deductionReason" to pr.deductionReason,
                    "overtimeHours" to pr.overtimeHours,
                    "overtimeAmount" to pr.overtimeAmount,
                    "netSalary" to pr.netSalary,
                    "status" to pr.status,
                    "approvedBy" to pr.approvedBy,
                    "approvalDate" to pr.approvalDate,
                    "paymentDate" to pr.paymentDate,
                    "calculationNotes" to pr.calculationNotes,
                    "createdAt" to pr.createdAt
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("payroll").document(pr.id)
                    .set(prMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 5. Push & Reconcile Clients
            val localClients = dao.getClientsOnce(workspaceId)
            for (client in localClients) {
                val clMap = hashMapOf(
                    "id" to client.id,
                    "workspaceId" to client.workspaceId,
                    "name" to client.name,
                    "mobile" to client.mobile,
                    "email" to client.email,
                    "address" to client.address,
                    "city" to client.city,
                    "eventType" to client.eventType,
                    "totalBookingValue" to client.totalBookingValue,
                    "paidAmount" to client.paidAmount,
                    "pendingAmount" to client.pendingAmount,
                    "notes" to client.notes,
                    "timelineStage" to client.timelineStage,
                    "createdAt" to client.createdAt
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("clients").document(client.id)
                    .set(clMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 6. Push & Reconcile Quotations
            val localQuotations = dao.getQuotationsOnce(workspaceId)
            for (qt in localQuotations) {
                val qtMap = hashMapOf(
                    "id" to qt.id,
                    "workspaceId" to qt.workspaceId,
                    "quotationNumber" to qt.quotationNumber,
                    "clientId" to qt.clientId,
                    "clientName" to qt.clientName,
                    "contact" to qt.contact,
                    "eventDate" to qt.eventDate,
                    "eventLocation" to qt.eventLocation,
                    "services" to qt.services,
                    "packageDetails" to qt.packageDetails,
                    "subtotal" to qt.subtotal,
                    "discount" to qt.discount,
                    "taxRate" to qt.taxRate,
                    "taxAmount" to qt.taxAmount,
                    "totalAmount" to qt.totalAmount,
                    "advanceRequired" to qt.advanceRequired,
                    "balanceAmount" to qt.balanceAmount,
                    "status" to qt.status,
                    "terms" to qt.terms,
                    "createdAt" to qt.createdAt
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("quotations").document(qt.id)
                    .set(qtMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 7. Push & Reconcile Bookings
            val localBookings = dao.getBookingsOnce(workspaceId)
            for (bk in localBookings) {
                val bkMap = hashMapOf(
                    "id" to bk.id,
                    "workspaceId" to bk.workspaceId,
                    "bookingNumber" to bk.bookingNumber,
                    "clientId" to bk.clientId,
                    "clientName" to bk.clientName,
                    "eventType" to bk.eventType,
                    "eventDate" to bk.eventDate,
                    "eventTime" to bk.eventTime,
                    "venue" to bk.venue,
                    "packageName" to bk.packageName,
                    "totalAmount" to bk.totalAmount,
                    "advancePaid" to bk.advancePaid,
                    "balanceAmount" to bk.balanceAmount,
                    "assignedTeam" to bk.assignedTeam,
                    "status" to bk.status,
                    "notes" to bk.notes,
                    "hasConflict" to bk.hasConflict,
                    "createdAt" to bk.createdAt
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("bookings").document(bk.id)
                    .set(bkMap, SetOptions.merge()).await()
                totalRecords++
            }

            // 8. Push & Reconcile Payments & Expenses
            val localPayments = dao.getPaymentsOnce(workspaceId)
            for (p in localPayments) {
                val pMap = hashMapOf(
                    "id" to p.id,
                    "workspaceId" to p.workspaceId,
                    "bookingId" to p.bookingId,
                    "clientName" to p.clientName,
                    "amount" to p.amount,
                    "paymentDate" to p.paymentDate,
                    "paymentMethod" to p.paymentMethod,
                    "transactionRef" to p.transactionRef,
                    "paymentType" to p.paymentType,
                    "status" to p.status,
                    "notes" to p.notes
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("payments").document(p.id)
                    .set(pMap, SetOptions.merge()).await()
                totalRecords++
            }

            val localExpenses = dao.getExpensesOnce(workspaceId)
            for (e in localExpenses) {
                val eMap = hashMapOf(
                    "id" to e.id,
                    "workspaceId" to e.workspaceId,
                    "category" to e.category,
                    "amount" to e.amount,
                    "date" to e.date,
                    "description" to e.description,
                    "employeeName" to e.employeeName,
                    "projectName" to e.projectName,
                    "paymentMethod" to e.paymentMethod,
                    "receiptRef" to e.receiptRef
                )
                fs.collection("workspaces").document(workspaceId)
                    .collection("expenses").document(e.id)
                    .set(eMap, SetOptions.merge()).await()
                totalRecords++
            }

            // Pull any remote records from Firestore to Room (e.g. created on Admin Web Panel)
            pullRemoteEmployees(workspaceId)
            pullRemoteClients(workspaceId)
            pullRemoteAttendance(workspaceId)

            _syncStatus.value = SyncStatus(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                errorMessage = null,
                syncedRecordsCount = totalRecords,
                isCloudConnected = true
            )
            Result.success(totalRecords)
        } catch (e: Exception) {
            if (e is com.google.firebase.firestore.FirebaseFirestoreException &&
                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(tag, "Cloud sync notice: Cloud authorization required (${e.message})")
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    isCloudConnected = false,
                    errorMessage = "Cloud authorization required: Running in offline-safe mode"
                )
            } else {
                Log.w(tag, "Cloud sync notice: ${e.message}")
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Cloud sync paused. Operating in offline-safe mode."
                )
            }
            Result.failure(e)
        }
    }

    private suspend fun pullRemoteEmployees(workspaceId: String) {
        val fs = firestore ?: return
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (_: Exception) { null }
        if (auth?.currentUser == null) return
        try {
            val snapshot = fs.collection("workspaces").document(workspaceId)
                .collection("employees").get().await()
            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val emp = EmployeeEntity(
                    id = id,
                    workspaceId = workspaceId,
                    employeeCode = doc.getString("employeeCode") ?: "BOSS-000",
                    name = doc.getString("name") ?: "",
                    mobile = doc.getString("mobile") ?: "",
                    email = doc.getString("email") ?: "",
                    department = doc.getString("department") ?: "Management",
                    designation = doc.getString("designation") ?: "Staff",
                    role = doc.getString("role") ?: "Employee",
                    joiningDate = doc.getString("joiningDate") ?: "",
                    basicSalary = doc.getDouble("basicSalary") ?: 0.0,
                    status = doc.getString("status") ?: "Active",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    responsibilities = doc.getString("responsibilities") ?: "",
                    assignedPermissions = doc.getString("assignedPermissions") ?: "ALL",
                    avatarUrl = doc.getString("avatarUrl") ?: "",
                    loginUsername = doc.getString("loginUsername") ?: "",
                    loginPassword = ""
                )
                if (emp.name.isNotBlank()) {
                    dao.insertEmployee(emp)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to pull remote employees", e)
        }
    }

    private suspend fun pullRemoteClients(workspaceId: String) {
        val fs = firestore ?: return
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (_: Exception) { null }
        if (auth?.currentUser == null) return
        try {
            val snapshot = fs.collection("workspaces").document(workspaceId)
                .collection("clients").get().await()
            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val cl = ClientEntity(
                    id = id,
                    workspaceId = workspaceId,
                    name = doc.getString("name") ?: "",
                    mobile = doc.getString("mobile") ?: "",
                    email = doc.getString("email") ?: "",
                    address = doc.getString("address") ?: "",
                    city = doc.getString("city") ?: "Pune",
                    eventType = doc.getString("eventType") ?: "Wedding",
                    totalBookingValue = doc.getDouble("totalBookingValue") ?: 0.0,
                    paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                    pendingAmount = doc.getDouble("pendingAmount") ?: 0.0,
                    notes = doc.getString("notes") ?: "",
                    timelineStage = doc.getString("timelineStage") ?: "Booking",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                if (cl.name.isNotBlank()) {
                    dao.insertClient(cl)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to pull remote clients", e)
        }
    }

    private suspend fun pullRemoteAttendance(workspaceId: String) {
        val fs = firestore ?: return
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (_: Exception) { null }
        if (auth?.currentUser == null) return
        try {
            val snapshot = fs.collection("workspaces").document(workspaceId)
                .collection("attendance").get().await()
            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val empId = doc.getString("employeeId") ?: ""
                val date = doc.getString("date") ?: ""
                if (empId.isNotBlank() && date.isNotBlank()) {
                    val att = AttendanceEntity(
                        id = id,
                        workspaceId = workspaceId,
                        employeeId = empId,
                        employeeName = doc.getString("employeeName") ?: "",
                        date = date,
                        status = doc.getString("status") ?: "Present",
                        workingHours = doc.getDouble("workingHours") ?: 9.0,
                        checkInTime = doc.getString("checkInTime") ?: "09:30 AM",
                        checkOutTime = doc.getString("checkOutTime") ?: "06:30 PM",
                        notes = doc.getString("notes") ?: "",
                        updatedBy = doc.getString("updatedBy") ?: "Admin",
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                    dao.insertAttendance(att)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to pull remote attendance", e)
        }
    }

    /**
     * Start live real-time listeners for updates initiated on the Admin Panel
     */
    fun startRealtimeListeners(workspaceId: String, scope: CoroutineScope) {
        stopRealtimeListeners()
        val fs = firestore ?: run {
            Log.w(tag, "Firestore is unavailable; skipping realtime snapshot listeners")
            return
        }
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (_: Exception) { null }
        if (auth?.currentUser == null) {
            Log.d(tag, "Realtime listeners standby: waiting for authenticated session")
            return
        }
        try {
            val empReg = fs.collection("workspaces").document(workspaceId)
                .collection("employees")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Employees listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                pullRemoteEmployees(workspaceId)
                            } catch (t: Throwable) {
                                Log.w(tag, "Error pulling remote employees in listener", t)
                            }
                        }
                    }
                }
            activeListeners.add(empReg)

            val attReg = fs.collection("workspaces").document(workspaceId)
                .collection("attendance")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Attendance listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                pullRemoteAttendance(workspaceId)
                            } catch (t: Throwable) {
                                Log.w(tag, "Error pulling remote attendance in listener", t)
                            }
                        }
                    }
                }
            activeListeners.add(attReg)
        } catch (e: Exception) {
            Log.w(tag, "Could not register realtime snapshot listeners", e)
        }
    }

    fun stopRealtimeListeners() {
        for (listener in activeListeners) {
            try {
                listener.remove()
            } catch (_: Exception) {}
        }
        activeListeners.clear()
    }
}
