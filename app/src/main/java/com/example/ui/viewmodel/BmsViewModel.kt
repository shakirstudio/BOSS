package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.BmsDatabase
import com.example.data.database.SeedData
import com.example.data.model.*
import com.example.data.payroll.PayrollCalculationEngine
import com.example.data.repository.BmsRepository
import com.example.data.sync.FirestoreSyncManager
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class BmsNavigationItem(val label: String) {
    DASHBOARD("Dashboard"),
    LEADS("Leads CRM"),
    CLIENTS("Clients"),
    QUOTATIONS("Quotations"),
    BOOKINGS("Bookings & Shoots"),
    WORKFLOWS("Editing & Albums"),
    STORAGE("Storage & Drives"),
    FINANCE("Payments & Expenses"),
    FINANCIAL_YEAR("FY Analytics"),
    TEAM("Team & HR"),
    TASKS("Tasks"),
    REPORTS("Reports & Analytics"),
    SALARY_REPORTS("Salary Reports"),
    SUBSCRIPTION("SaaS Plans"),
    SETTINGS("Settings"),
    ADMIN("Super Admin")
}

data class GlobalSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val module: String,
    val targetTab: BmsNavigationItem
)

class BmsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BmsDatabase.getDatabase(application, viewModelScope)
    private val repository = BmsRepository(db.bmsDao())
    val syncManager = FirestoreSyncManager(application, db.bmsDao())
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ensure default genuine studio data exists if fresh launch
                repository.ensureInitialData()
            } catch (t: Throwable) {
                android.util.Log.w("BmsViewModel", "Notice: initial data check: ${t.message}")
            }

            try {
                // 2. Automatically purge all demo leads, fake clients, dummy quotations, demo employees
                repository.purgeAllDemoData()
            } catch (t: Throwable) {
                android.util.Log.w("BmsViewModel", "Notice: demo data purge: ${t.message}")
            }

            // 3. Initiate background cloud synchronization and attach live listeners safely
            try {
                syncManager.syncAll(SeedData.DEFAULT_WORKSPACE_ID)
                syncManager.startRealtimeListeners(SeedData.DEFAULT_WORKSPACE_ID, viewModelScope)
            } catch (t: Throwable) {
                android.util.Log.w("BmsViewModel", "Background sync notice: ${t.message}")
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = syncManager.syncAll(_currentWorkspaceId.value)
            if (res.isSuccess) {
                _snackbarMessage.value = "Synced ${res.getOrNull()} records with Firebase Cloud"
            } else {
                _snackbarMessage.value = "Offline mode active: ${res.exceptionOrNull()?.message?.take(50) ?: "Local changes saved"}"
            }
        }
    }


    // Theme Mode State (Dark Mode default, toggleable to Light Mode)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean? = null) {
        _isDarkMode.value = enabled ?: !_isDarkMode.value
    }

    // Genuine Authorized Account (Demo accounts removed)
    val defaultAccounts = listOf(
        AuthAccount(
            id = "usr_admin_01",
            email = "admin@bms.com",
            username = "admin",
            passwordHash = "admin123",
            fullName = "Shakir Tamboli",
            actualRole = AppRole.ADMIN,
            designation = "Super Admin & Managing Director",
            department = "Executive Management",
            permissions = setOf(
                AppPermission.FULL_ADMIN_ACCESS,
                AppPermission.USER_MANAGEMENT,
                AppPermission.EMPLOYEE_MANAGEMENT,
                AppPermission.MANAGER_MANAGEMENT,
                AppPermission.ROLES_PERMISSIONS,
                AppPermission.SYSTEM_CONFIGURATION,
                AppPermission.AUDIT_LOGS,
                AppPermission.FINANCE_FULL,
                AppPermission.SETTINGS_ACCESS
            ),
            mobile = "+91 98234 56789",
            emergencyContact = "+91 98234 00000",
            employeeId = "emp_shakir"
        )
    )

    private val _mustChangePasswordOnLogin = MutableStateFlow(false)
    val mustChangePasswordOnLogin: StateFlow<Boolean> = _mustChangePasswordOnLogin.asStateFlow()

    private val _authenticatedUser = MutableStateFlow<AuthAccount?>(null)
    val authenticatedUser: StateFlow<AuthAccount?> = _authenticatedUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginState = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginState: StateFlow<LoginResult> = _loginState.asStateFlow()

    private val _currentUserRole = MutableStateFlow("")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    fun clearLoginError() {
        if (_loginState.value is LoginResult.Error) {
            _loginState.value = LoginResult.Idle
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _authenticatedUser.value = null
        _loginState.value = LoginResult.Idle
        _currentUserRole.value = ""
        _currentUserName.value = ""
        _currentTab.value = BmsNavigationItem.DASHBOARD
        _snackbarMessage.value = "Signed out safely."
    }

    fun login(
        emailOrUsername: String,
        password: String,
        selectedRole: AppRole?,
        rememberMe: Boolean
    ) {
        viewModelScope.launch {
            _loginState.value = LoginResult.Loading

            val rawIdentifier = emailOrUsername.trim()
            val cleanIdentifier = rawIdentifier.lowercase()
            val numericIdentifier = rawIdentifier.filter { it.isDigit() }
            val cleanPassword = password.trim()

            if (rawIdentifier.isBlank()) {
                _loginState.value = LoginResult.Error("Please enter your User ID, Username, Email, or Mobile Number.")
                return@launch
            }
            if (cleanPassword.isBlank()) {
                _loginState.value = LoginResult.Error("Please enter your password.")
                return@launch
            }

            kotlinx.coroutines.delay(300) // Realistic auth validation delay

            // Check Room Database employees as Single Source of Truth
            // Supports: Username, Email, User ID / Employee ID, Employee Code, and Mobile Number
            val dbEmp = employees.value.firstOrNull {
                (it.loginUsername.isNotBlank() && it.loginUsername.equals(cleanIdentifier, ignoreCase = true)) ||
                (it.email.isNotBlank() && it.email.equals(cleanIdentifier, ignoreCase = true)) ||
                (it.id.equals(cleanIdentifier, ignoreCase = true)) ||
                (it.employeeCode.equals(cleanIdentifier, ignoreCase = true)) ||
                (numericIdentifier.length >= 8 && it.mobile.filter { c -> c.isDigit() }.contains(numericIdentifier))
            }

            val fallbackAccount = defaultAccounts.find {
                it.email.lowercase() == cleanIdentifier ||
                it.username.lowercase() == cleanIdentifier ||
                it.id.lowercase() == cleanIdentifier ||
                it.employeeId.lowercase() == cleanIdentifier ||
                (numericIdentifier.length >= 8 && it.mobile.filter { c -> c.isDigit() }.contains(numericIdentifier))
            }

            if (dbEmp == null && fallbackAccount == null) {
                _loginState.value = LoginResult.Error("Invalid credentials. Account not found with the provided identifier.")
                return@launch
            }

            // Verify account status if found in database
            if (dbEmp != null) {
                if (dbEmp.status.equals("Pending", ignoreCase = true)) {
                    _loginState.value = LoginResult.Error("Account Approval Pending: Your account has been registered and is awaiting Administrator approval. Please contact Shakir Tamboli to activate your access.")
                    return@launch
                }
                if (dbEmp.status.equals("Inactive", ignoreCase = true) || dbEmp.status.equals("Rejected", ignoreCase = true)) {
                    _loginState.value = LoginResult.Error("Access Restricted: This user account is currently marked as '${dbEmp.status}'. Please contact the Administrator.")
                    return@launch
                }
            }

            // Verify password securely against stored hash / password
            val expectedPassword = dbEmp?.loginPassword?.ifBlank { fallbackAccount?.passwordHash } ?: fallbackAccount?.passwordHash
            if (expectedPassword == null || expectedPassword != cleanPassword) {
                _loginState.value = LoginResult.Error("Invalid password. Please check your password and try again.")
                return@launch
            }

            // Determine actual role from DB employee if present, else fallback
            val actualRole = when {
                dbEmp != null -> {
                    when {
                        dbEmp.role.contains("Admin", true) || dbEmp.role.contains("Owner", true) -> AppRole.ADMIN
                        dbEmp.role.contains("Manager", true) -> AppRole.MANAGER
                        else -> AppRole.EMPLOYEE
                    }
                }
                fallbackAccount != null -> fallbackAccount.actualRole
                else -> AppRole.EMPLOYEE
            }

            // If user explicitly selected a role in dropdown, validate match to prevent unauthorized role escalation
            if (selectedRole != null && actualRole != selectedRole) {
                val empName = dbEmp?.name ?: fallbackAccount?.fullName ?: "User"
                _loginState.value = LoginResult.Error(
                    "Invalid role selected for this account. Account '$empName' is assigned as '${actualRole.label}', but '${selectedRole.label}' was selected."
                )
                return@launch
            }

            val loggedInAccount = if (dbEmp != null) {
                AuthAccount(
                    id = dbEmp.id,
                    email = dbEmp.email,
                    username = dbEmp.loginUsername.ifBlank { dbEmp.email.substringBefore("@") },
                    passwordHash = dbEmp.loginPassword,
                    fullName = dbEmp.name,
                    actualRole = actualRole,
                    designation = dbEmp.designation,
                    department = dbEmp.department,
                    permissions = when (actualRole) {
                        AppRole.ADMIN -> setOf(AppPermission.FULL_ADMIN_ACCESS)
                        AppRole.MANAGER -> setOf(AppPermission.EMPLOYEE_MANAGEMENT, AppPermission.TASK_MANAGEMENT, AppPermission.ATTENDANCE_MANAGEMENT, AppPermission.TEAM_REPORTS)
                        AppRole.EMPLOYEE -> setOf(AppPermission.PERSONAL_DASHBOARD, AppPermission.MY_TASKS, AppPermission.MY_ATTENDANCE, AppPermission.MY_DOCUMENTS, AppPermission.MY_PROFILE)
                    },
                    avatarUrl = dbEmp.avatarUrl,
                    mobile = dbEmp.mobile,
                    emergencyContact = dbEmp.emergencyContact,
                    employeeId = dbEmp.id
                )
            } else {
                fallbackAccount!!
            }

            // Authorized & Authenticated
            _authenticatedUser.value = loggedInAccount
            _currentUserRole.value = loggedInAccount.actualRole.label
            _currentUserName.value = loggedInAccount.fullName
            _isLoggedIn.value = true
            _loginState.value = LoginResult.Success
            _currentTab.value = BmsNavigationItem.DASHBOARD
            _snackbarMessage.value = "Welcome back, ${loggedInAccount.fullName} (${loggedInAccount.actualRole.label})"
        }
    }

    fun updateAdminPassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val clean = newPassword.trim()
            if (clean.length < 8) {
                onResult(false, "Password must be at least 8 characters long.")
                return@launch
            }
            val emp = employees.value.find { it.id == "emp_shakir" || it.loginUsername.equals("admin", true) }
            if (emp != null) {
                repository.updateEmployee(emp.copy(loginPassword = clean))
            } else {
                val newAdmin = EmployeeEntity(
                    id = "emp_shakir",
                    workspaceId = _currentWorkspaceId.value,
                    employeeCode = "BOSS-000",
                    name = "Shakir Tamboli",
                    mobile = "+91 98234 56789",
                    email = "shakirtamboli.pune11@gmail.com",
                    department = "Executive Management",
                    designation = "Super Admin & Managing Director",
                    role = "Admin",
                    joiningDate = "01 Jan 2022",
                    basicSalary = 120000.0,
                    assignedPermissions = "FULL_ADMIN_ACCESS",
                    loginUsername = "admin",
                    loginPassword = clean
                )
                repository.saveEmployee(newAdmin)
            }
            _mustChangePasswordOnLogin.value = false
            logActivity("Security Setup", "Super Admin password set and secured")
            _snackbarMessage.value = "Super Admin password set successfully!"
            onResult(true, "Password updated successfully!")
        }
    }

    fun registerNewUser(
        fullName: String,
        username: String,
        email: String,
        mobile: String,
        department: String,
        designation: String,
        password: String,
        confirmPassword: String,
        avatarUrl: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = fullName.trim()
            val cleanUsername = username.trim()
            val cleanEmail = email.trim().lowercase()
            val cleanMobile = mobile.trim()
            val cleanDept = department.trim()
            val cleanDesig = designation.trim()
            val cleanPassword = password.trim()
            val cleanConfirm = confirmPassword.trim()

            if (cleanName.isBlank()) {
                onResult(false, "Full Name is required.")
                return@launch
            }
            if (cleanUsername.isBlank()) {
                onResult(false, "Username / User ID is required.")
                return@launch
            }
            if (cleanEmail.isBlank()) {
                onResult(false, "Email address is required.")
                return@launch
            }
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                onResult(false, "Please enter a valid email address.")
                return@launch
            }
            if (cleanMobile.isBlank()) {
                onResult(false, "Mobile number is required.")
                return@launch
            }
            val numericMobile = cleanMobile.filter { it.isDigit() }
            if (numericMobile.length < 10) {
                onResult(false, "Please enter a valid mobile number (minimum 10 digits).")
                return@launch
            }
            if (cleanPassword.isBlank()) {
                onResult(false, "Password is required.")
                return@launch
            }
            if (cleanPassword.length < 6) {
                onResult(false, "Password must be at least 6 characters long.")
                return@launch
            }
            if (cleanPassword != cleanConfirm) {
                onResult(false, "Password and Confirm Password do not match.")
                return@launch
            }

            // Check uniqueness of username
            val existingUsername = employees.value.any { it.loginUsername.equals(cleanUsername, ignoreCase = true) } ||
                defaultAccounts.any { it.username.equals(cleanUsername, ignoreCase = true) }
            if (existingUsername) {
                onResult(false, "Username '$cleanUsername' is already taken. Please choose another username.")
                return@launch
            }

            // Check uniqueness of email
            val existingEmail = employees.value.any { it.email.equals(cleanEmail, ignoreCase = true) } ||
                defaultAccounts.any { it.email.equals(cleanEmail, ignoreCase = true) }
            if (existingEmail) {
                onResult(false, "Email address '$cleanEmail' is already registered.")
                return@launch
            }

            // Check uniqueness of mobile
            val existingMobile = employees.value.any { it.mobile.filter { c -> c.isDigit() } == numericMobile } ||
                defaultAccounts.any { it.mobile.filter { c -> c.isDigit() } == numericMobile }
            if (existingMobile) {
                onResult(false, "Mobile number '$cleanMobile' is already registered.")
                return@launch
            }

            val newId = "emp_" + UUID.randomUUID().toString().take(8)
            val code = "BOSS-00" + (10..99).random()
            val newEmployee = EmployeeEntity(
                id = newId,
                workspaceId = _currentWorkspaceId.value,
                employeeCode = code,
                name = cleanName,
                mobile = cleanMobile,
                email = cleanEmail,
                department = if (cleanDept.isNotBlank()) cleanDept else "Studio Operations",
                designation = if (cleanDesig.isNotBlank()) cleanDesig else "Staff Member",
                role = "Employee", // Safe default role: NEVER Admin
                joiningDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                basicSalary = 30000.0,
                status = "Pending", // Safe default: requires Admin approval
                emergencyContact = "",
                responsibilities = "Assigned duties upon onboarding approval",
                assignedPermissions = "PERSONAL_DASHBOARD,MY_TASKS,MY_ATTENDANCE,MY_DOCUMENTS,MY_PROFILE",
                avatarUrl = avatarUrl,
                loginUsername = cleanUsername,
                loginPassword = cleanPassword
            )

            repository.saveEmployee(newEmployee)
            repository.logActivity(
                _currentWorkspaceId.value,
                cleanName,
                "New user account registration submitted ($cleanEmail) - Pending Admin Approval",
                "Self-Registration"
            )
            onResult(true, "Registration submitted successfully! Your account is pending Administrator approval before you can log in.")
        }
    }

    fun logActivity(action: String, details: String = "", module: String = "Operations") {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUserName.value.ifBlank { "Admin" }
            repository.logActivity(
                workspaceId = _currentWorkspaceId.value,
                user = user,
                action = if (details.isNotBlank()) "$action: $details" else action,
                module = module
            )
        }
    }

    fun approveUser(employeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val emp = employees.value.firstOrNull { it.id == employeeId }
            if (emp != null) {
                repository.updateEmployee(emp.copy(status = "Active"))
                repository.logActivity(
                    _currentWorkspaceId.value,
                    _currentUserName.value.ifBlank { "Admin" },
                    "Approved user registration for ${emp.name} (${emp.email})",
                    "Admin / Approvals"
                )
                _snackbarMessage.value = "Approved: ${emp.name} is now Active and can sign in."
            }
        }
    }

    fun rejectUser(employeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val emp = employees.value.firstOrNull { it.id == employeeId }
            if (emp != null) {
                repository.updateEmployee(emp.copy(status = "Rejected"))
                repository.logActivity(
                    _currentWorkspaceId.value,
                    _currentUserName.value.ifBlank { "Admin" },
                    "Rejected user registration for ${emp.name}",
                    "Admin / Approvals"
                )
                _snackbarMessage.value = "Registration for ${emp.name} rejected."
            }
        }
    }

    fun setUserRoleAndStatus(employeeId: String, newRole: String, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val emp = employees.value.firstOrNull { it.id == employeeId }
            if (emp != null) {
                repository.updateEmployee(emp.copy(role = newRole, status = newStatus))
                repository.logActivity(
                    _currentWorkspaceId.value,
                    _currentUserName.value.ifBlank { "Admin" },
                    "Updated role to $newRole and status to $newStatus for ${emp.name}",
                    "Admin / Roles"
                )
                _snackbarMessage.value = "Updated ${emp.name}: Role = $newRole, Status = $newStatus"
            }
        }
    }

    fun switchUserRole(role: String, name: String) {
        val targetRole = AppRole.fromLabel(role) ?: AppRole.ADMIN
        val matchedAccount = defaultAccounts.find { it.actualRole == targetRole }
            ?: AuthAccount(
                id = UUID.randomUUID().toString(),
                email = "${name.lowercase().replace(" ", ".")}@bms.com",
                username = name.lowercase().replace(" ", ""),
                passwordHash = "pass123",
                fullName = name,
                actualRole = targetRole,
                designation = role,
                department = "Operations",
                permissions = emptySet()
            )

        _authenticatedUser.value = matchedAccount
        _currentUserRole.value = matchedAccount.actualRole.label
        _currentUserName.value = matchedAccount.fullName
        _snackbarMessage.value = "Switched to: ${matchedAccount.fullName} (${matchedAccount.actualRole.label})"
    }

    fun canAccessModule(item: BmsNavigationItem): Boolean {
        val user = _authenticatedUser.value
        val actualRole = user?.actualRole ?: AppRole.ADMIN

        return when (actualRole) {
            AppRole.ADMIN -> true
            AppRole.MANAGER -> {
                // Manager permissions:
                // Dashboard, Employee Management (Team), Attendance, Task Management, Team Reports, Team Analytics, Notifications, Profile
                // Bookings, Clients, Workflows
                // Forbidden: ADMIN, SETTINGS (System Config, Audit Logs), SUBSCRIPTION, FINANCIAL_YEAR
                item != BmsNavigationItem.ADMIN &&
                item != BmsNavigationItem.SETTINGS &&
                item != BmsNavigationItem.SUBSCRIPTION &&
                item != BmsNavigationItem.FINANCIAL_YEAR
            }
            AppRole.EMPLOYEE -> {
                // Employee permissions:
                // Personal Dashboard, My Tasks, My Attendance, My Documents, My Profile, Notifications, Personal Reports
                // Forbidden: ADMIN, SETTINGS, TEAM, FINANCE, FINANCIAL_YEAR, LEADS, CLIENTS, QUOTATIONS, SUBSCRIPTION
                item in listOf(
                    BmsNavigationItem.DASHBOARD,
                    BmsNavigationItem.TASKS,
                    BmsNavigationItem.STORAGE,
                    BmsNavigationItem.REPORTS
                )
            }
        }
    }

    fun getRequiredRoleForModule(item: BmsNavigationItem): String {
        return when (item) {
            BmsNavigationItem.ADMIN,
            BmsNavigationItem.SETTINGS,
            BmsNavigationItem.SUBSCRIPTION,
            BmsNavigationItem.FINANCIAL_YEAR -> "Admin"
            BmsNavigationItem.FINANCE,
            BmsNavigationItem.LEADS,
            BmsNavigationItem.CLIENTS,
            BmsNavigationItem.QUOTATIONS,
            BmsNavigationItem.TEAM -> "Admin or Manager"
            else -> "Admin, Manager, or Employee"
        }
    }

    // Active Workspace State
    private val _currentWorkspaceId = MutableStateFlow(SeedData.DEFAULT_WORKSPACE_ID)
    val currentWorkspaceId: StateFlow<String> = _currentWorkspaceId.asStateFlow()

    // Navigation State
    private val _currentTab = MutableStateFlow(BmsNavigationItem.DASHBOARD)
    val currentTab: StateFlow<BmsNavigationItem> = _currentTab.asStateFlow()

    // Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Notification Dialog
    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    // SnackBar Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Observables bound to active workspace
    val workspace: StateFlow<WorkspaceEntity?> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getWorkspace(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allWorkspaces: StateFlow<List<WorkspaceEntity>> = repository.getAllWorkspaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserEntity?> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getCurrentUser(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val leads: StateFlow<List<LeadEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getLeads(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<ClientEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getClients(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quotations: StateFlow<List<QuotationEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getQuotations(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BookingEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getBookings(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoots: StateFlow<List<ShootEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getShoots(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photoEdits: StateFlow<List<PhotoEditEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getPhotoEdits(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoEdits: StateFlow<List<VideoEditEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getVideoEdits(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumProjects: StateFlow<List<AlbumProjectEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getAlbumProjects(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveries: StateFlow<List<DeliveryEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getDeliveries(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getPayments(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getExpenses(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<EmployeeEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getEmployees(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<AttendanceEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getAttendance(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payrollRecords: StateFlow<List<PayrollRecordEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getPayrollRecords(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaves: StateFlow<List<LeaveEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getLeaves(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getTasks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getNotifications(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getActivityLogs(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscription: StateFlow<SubscriptionEntity?> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getSubscription(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val storageDrives: StateFlow<List<StorageDriveEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getStorageDrives(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectFiles: StateFlow<List<ProjectFileEntity>> = _currentWorkspaceId
        .flatMapLatest { id -> repository.getProjectFiles(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Role-based Assigned-Only View flows:
    // If Admin/Owner/Manager is logged in, they see all items.
    // If an individual editor/photographer/staff is logged in, they ONLY see items assigned to them!
    val isUserAdminOrOwner: StateFlow<Boolean> = _currentUserRole
        .map { r ->
            val role = r.lowercase()
            role.contains("owner") || role.contains("admin") || role.contains("manager")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val visiblePhotoEdits: StateFlow<List<PhotoEditEntity>> = combine(
        photoEdits,
        _currentUserName,
        _currentUserRole
    ) { list, name, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) list else list.filter { it.editorName.equals(name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleVideoEdits: StateFlow<List<VideoEditEntity>> = combine(
        videoEdits,
        _currentUserName,
        _currentUserRole
    ) { list, name, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) list else list.filter { it.editorName.equals(name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleAlbumProjects: StateFlow<List<AlbumProjectEntity>> = combine(
        albumProjects,
        _currentUserName,
        _currentUserRole
    ) { list, name, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) list else list.filter { it.designerName.equals(name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleShoots: StateFlow<List<ShootEntity>> = combine(
        shoots,
        _currentUserName,
        _currentUserRole
    ) { list, name, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) list else list.filter {
            it.leadPhotographer.contains(name, ignoreCase = true) ||
            it.videographer.contains(name, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleTasks: StateFlow<List<TaskEntity>> = combine(
        tasks,
        _currentUserName,
        _currentUserRole
    ) { list, name, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) list else list.filter { it.assignedTo.equals(name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleLeads: StateFlow<List<LeadEntity>> = combine(
        leads,
        _currentUserName,
        _currentUserRole
    ) { list, name, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) list else list.filter { it.assignedStaff.equals(name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleBookings: StateFlow<List<BookingEntity>> = combine(
        bookings,
        visibleShoots,
        _currentUserRole
    ) { allBookings, activeShoots, role ->
        val isPrivileged = role.contains("owner", true) || role.contains("admin", true) || role.contains("manager", true)
        if (isPrivileged) {
            allBookings
        } else {
            val bookingIds = activeShoots.map { it.bookingId }.toSet()
            allBookings.filter { it.id in bookingIds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: BmsNavigationItem) {
        _currentTab.value = tab
    }

    fun switchWorkspace(workspaceId: String) {
        _currentWorkspaceId.value = workspaceId
        _snackbarMessage.value = "Switched studio workspace"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncManager.syncAll(workspaceId)
                syncManager.startRealtimeListeners(workspaceId, viewModelScope)
            } catch (_: Exception) {}
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun toggleNotifications(show: Boolean) {
        _showNotifications.value = show
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // Lead Operations
    fun addLead(lead: LeadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveLead(lead.copy(workspaceId = _currentWorkspaceId.value))
            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Added new Lead: ${lead.name}", "Leads")
            _snackbarMessage.value = "Lead added successfully"
        }
    }

    fun updateLead(lead: LeadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLead(lead)
            _snackbarMessage.value = "Lead updated"
        }
    }

    fun deleteLead(lead: LeadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLead(lead)
            _snackbarMessage.value = "Lead removed"
        }
    }

    fun convertLead(lead: LeadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val userName = currentUser.value?.name ?: "Admin"
            repository.convertLeadToClient(lead, userName)
            _snackbarMessage.value = "Converted ${lead.name} to Client & Draft Quotation generated!"
        }
    }

    // Client Operations
    fun addClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveClient(client.copy(workspaceId = _currentWorkspaceId.value))
            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Added new Client: ${client.name}", "Clients")
            _snackbarMessage.value = "Client registered successfully"
        }
    }

    fun updateClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClient(client)
            _snackbarMessage.value = "Client details updated"
        }
    }

    // Quotation Operations
    fun addQuotation(quotation: QuotationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQuotation(quotation.copy(workspaceId = _currentWorkspaceId.value))
            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Created Quotation: ${quotation.quotationNumber}", "Quotations")
            _snackbarMessage.value = "Quotation ${quotation.quotationNumber} saved"
        }
    }

    fun updateQuotation(quotation: QuotationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateQuotation(quotation)
            _snackbarMessage.value = "Quotation ${quotation.quotationNumber} updated"
        }
    }

    fun convertQuotationToBooking(quotation: QuotationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val userName = currentUser.value?.name ?: "Admin"
            repository.convertQuotationToBooking(quotation, userName)
            _snackbarMessage.value = "Quotation accepted! Booking confirmed & shoot scheduled."
        }
    }

    // Booking Operations
    fun addBooking(booking: BookingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check conflict for same venue / date
            val existing = bookings.value.firstOrNull { it.eventDate == booking.eventDate && it.venue.equals(booking.venue, ignoreCase = true) }
            val hasConflict = existing != null
            val finalBooking = booking.copy(workspaceId = _currentWorkspaceId.value, hasConflict = hasConflict)
            repository.saveBooking(finalBooking)

            // Also create shoot entry
            val shoot = ShootEntity(
                id = "shoot_" + UUID.randomUUID().toString().take(8),
                workspaceId = _currentWorkspaceId.value,
                bookingId = finalBooking.id,
                clientName = finalBooking.clientName,
                eventType = finalBooking.eventType,
                shootDate = finalBooking.eventDate,
                callTime = finalBooking.eventTime,
                venue = finalBooking.venue,
                leadPhotographer = finalBooking.assignedTeam.take(25),
                videographer = "Videographer Assigned",
                equipmentList = "Standard Gear Kit",
                status = "Assigned"
            )
            repository.saveShoot(shoot)

            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Created Booking ${finalBooking.bookingNumber} for ${finalBooking.clientName}", "Bookings")
            _snackbarMessage.value = if (hasConflict) "Booking saved! (Warning: Date conflict detected)" else "Booking ${finalBooking.bookingNumber} confirmed"
        }
    }

    fun updateBookingStatus(booking: BookingEntity, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBooking(booking.copy(status = newStatus))
            _snackbarMessage.value = "Booking status updated to $newStatus"
        }
    }

    fun updateBooking(booking: BookingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBooking(booking)
            _snackbarMessage.value = "Booking details updated"
        }
    }

    // Shoot Operations
    fun updateShootStatus(shoot: ShootEntity, newStatus: String, backupDone: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = shoot.copy(
                status = newStatus,
                dataBackupDone = backupDone ?: shoot.dataBackupDone
            )
            repository.updateShoot(updated)
            _snackbarMessage.value = "Shoot workflow updated"
        }
    }

    fun updateShoot(shoot: ShootEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateShoot(shoot)
            _snackbarMessage.value = "Shoot details updated"
        }
    }

    // Workflow Operations
    fun updatePhotoEditProgress(edit: PhotoEditEntity, editedPhotos: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = edit.copy(
                editedPhotos = editedPhotos,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            repository.updatePhotoEdit(updated)
            _snackbarMessage.value = "Photo edit progress saved: $editedPhotos/${edit.totalPhotos}"
        }
    }

    fun updateVideoEditStatus(edit: VideoEditEntity, newStatus: String, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = edit.copy(
                status = newStatus,
                notes = if (notes.isNotEmpty()) notes else edit.notes,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateVideoEdit(updated)
            _snackbarMessage.value = "Video edit status: $newStatus"
        }
    }

    fun updateAlbumStatus(album: AlbumProjectEntity, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = album.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateAlbumProject(updated)
            _snackbarMessage.value = "Album project status: $newStatus"
        }
    }

    fun updateDelivery(delivery: DeliveryEntity, newStatus: String, confirmed: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = delivery.copy(status = newStatus, confirmedByClient = confirmed)
            repository.updateDelivery(updated)
            _snackbarMessage.value = "Delivery marked as $newStatus"
        }
    }

    // Payment & Expense
    fun recordPayment(payment: PaymentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePayment(payment.copy(workspaceId = _currentWorkspaceId.value))
            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Recorded payment ₹${payment.amount} (${payment.paymentMethod}) for ${payment.clientName}", "Payments")
            _snackbarMessage.value = "Payment of ₹${payment.amount} recorded"
        }
    }

    fun recordExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveExpense(expense.copy(workspaceId = _currentWorkspaceId.value))
            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Recorded expense ₹${expense.amount} for ${expense.category}", "Expenses")
            _snackbarMessage.value = "Expense of ₹${expense.amount} recorded"
        }
    }

    fun updatePhotoEdit(edit: PhotoEditEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePhotoEdit(edit.copy(updatedAt = System.currentTimeMillis()))
            _snackbarMessage.value = "Photo project updated"
        }
    }

    fun updateVideoEdit(edit: VideoEditEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVideoEdit(edit.copy(updatedAt = System.currentTimeMillis()))
            _snackbarMessage.value = "Video project updated"
        }
    }

    fun updateAlbumProject(album: AlbumProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAlbumProject(album.copy(updatedAt = System.currentTimeMillis()))
            _snackbarMessage.value = "Album project updated"
        }
    }

    fun updatePayment(payment: PaymentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePayment(payment)
            _snackbarMessage.value = "Payment details updated"
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(expense)
            _snackbarMessage.value = "Expense details updated"
        }
    }

    fun updateEmployee(employee: EmployeeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldEmployee = employees.value.firstOrNull { it.id == employee.id }
            val oldName = oldEmployee?.name ?: ""
            repository.updateEmployee(employee)

            if (oldName.isNotBlank() && oldName != employee.name) {
                repository.cascadeEmployeeNameUpdate(oldName, employee.name, _currentWorkspaceId.value, employee.id)
                if (_currentUserName.value == oldName) {
                    _currentUserName.value = employee.name
                }
                val auth = _authenticatedUser.value
                if (auth != null && (auth.employeeId == employee.id || auth.fullName.equals(oldName, ignoreCase = true))) {
                    _authenticatedUser.value = auth.copy(fullName = employee.name, email = employee.email, designation = employee.designation, department = employee.department, mobile = employee.mobile)
                }
            }
            _snackbarMessage.value = "Employee details updated"
        }
    }

    fun updateEmployeeDetails(
        employeeId: String,
        newName: String,
        newMobile: String,
        newEmail: String,
        newDepartment: String,
        newDesignation: String,
        newEmergencyContact: String = "",
        newResponsibilities: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = employees.value.firstOrNull { it.id == employeeId }
                ?: employees.value.firstOrNull { it.name.equals(newName, ignoreCase = true) }
            val oldName = existing?.name ?: _currentUserName.value

            val updatedEmployee = if (existing != null) {
                existing.copy(
                    name = newName.trim(),
                    mobile = newMobile.trim(),
                    email = newEmail.trim(),
                    department = newDepartment.trim(),
                    designation = newDesignation.trim(),
                    emergencyContact = if (newEmergencyContact.isNotBlank()) newEmergencyContact.trim() else existing.emergencyContact,
                    responsibilities = if (newResponsibilities.isNotBlank()) newResponsibilities.trim() else existing.responsibilities
                )
            } else {
                EmployeeEntity(
                    id = employeeId,
                    workspaceId = _currentWorkspaceId.value,
                    employeeCode = "BOSS-001",
                    name = newName.trim(),
                    mobile = newMobile.trim(),
                    email = newEmail.trim(),
                    department = newDepartment.trim(),
                    designation = newDesignation.trim(),
                    role = "Employee",
                    joiningDate = "15 Jan 2024",
                    basicSalary = 48000.0,
                    emergencyContact = newEmergencyContact.trim(),
                    responsibilities = newResponsibilities.trim()
                )
            }

            // 1. Update in Room Database
            repository.updateEmployee(updatedEmployee)

            // 2. Cascade Name Update across Tasks, Attendance, Activity Logs, Leaves, Expenses
            if (oldName.isNotBlank() && oldName != newName.trim()) {
                repository.cascadeEmployeeNameUpdate(oldName, newName.trim(), _currentWorkspaceId.value, employeeId)
            }

            // 3. Update authenticated session if this employee is the current user
            val currentAuth = _authenticatedUser.value
            if (currentAuth != null && (currentAuth.employeeId == employeeId || currentAuth.fullName.equals(oldName, ignoreCase = true))) {
                val updatedAuth = currentAuth.copy(
                    fullName = newName.trim(),
                    email = newEmail.trim(),
                    mobile = newMobile.trim(),
                    department = newDepartment.trim(),
                    designation = newDesignation.trim(),
                    emergencyContact = newEmergencyContact.trim()
                )
                _authenticatedUser.value = updatedAuth
                _currentUserName.value = newName.trim()
            }

            // 4. Log the action
            repository.logActivity(
                _currentWorkspaceId.value,
                newName.trim(),
                "Updated personal employee profile & synchronized across modules",
                "HR / Profile"
            )

            _snackbarMessage.value = "Employee details updated and synchronized across all modules!"
        }
    }

    fun addEmployeeAdmin(
        name: String,
        mobile: String,
        email: String,
        department: String,
        designation: String,
        role: String,
        salary: Double,
        loginUsername: String = "",
        loginPassword: String = "",
        responsibilities: String = "",
        assignedPermissions: String = "ALL"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val code = "BOSS-00" + (10..99).random()
            val newId = "emp_" + UUID.randomUUID().toString().take(8)
            val emp = EmployeeEntity(
                id = newId,
                workspaceId = _currentWorkspaceId.value,
                employeeCode = code,
                name = name.trim(),
                mobile = mobile.trim(),
                email = email.trim(),
                department = department.trim(),
                designation = designation.trim(),
                role = role.trim(),
                joiningDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                basicSalary = salary,
                responsibilities = if (responsibilities.isNotBlank()) responsibilities.trim() else "Shoots, Editing & Studio Operations",
                assignedPermissions = if (assignedPermissions.isNotBlank()) assignedPermissions.trim() else "ALL",
                loginUsername = if (loginUsername.isNotBlank()) loginUsername.trim() else name.lowercase().replace(" ", ""),
                loginPassword = if (loginPassword.isNotBlank()) loginPassword.trim() else "emp123"
            )
            repository.saveEmployee(emp)
            repository.logActivity(
                _currentWorkspaceId.value,
                _currentUserName.value,
                "Added new employee ${emp.name} (${emp.designation}) with login credentials",
                "Admin / HR"
            )
            _snackbarMessage.value = "New employee ${emp.name} added successfully"
        }
    }

    fun updateEmployeeAdminFull(
        employeeId: String,
        name: String,
        mobile: String,
        email: String,
        department: String,
        designation: String,
        role: String,
        basicSalary: Double,
        status: String,
        loginUsername: String,
        loginPassword: String,
        responsibilities: String,
        assignedPermissions: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = employees.value.firstOrNull { it.id == employeeId }
            val oldName = existing?.name ?: ""
            val updated = (existing ?: EmployeeEntity(
                id = employeeId,
                workspaceId = _currentWorkspaceId.value,
                employeeCode = "BOSS-001",
                name = name,
                mobile = mobile,
                email = email,
                department = department,
                designation = designation,
                role = role,
                joiningDate = "Today",
                basicSalary = basicSalary
            )).copy(
                name = name.trim(),
                mobile = mobile.trim(),
                email = email.trim(),
                department = department.trim(),
                designation = designation.trim(),
                role = role.trim(),
                basicSalary = basicSalary,
                status = status.trim(),
                loginUsername = loginUsername.trim(),
                loginPassword = loginPassword.trim(),
                responsibilities = responsibilities.trim(),
                assignedPermissions = assignedPermissions.trim()
            )

            repository.updateEmployee(updated)

            if (oldName.isNotBlank() && oldName != name.trim()) {
                repository.cascadeEmployeeNameUpdate(oldName, name.trim(), _currentWorkspaceId.value, employeeId)
                if (_currentUserName.value == oldName) {
                    _currentUserName.value = name.trim()
                }
            }

            val currentAuth = _authenticatedUser.value
            if (currentAuth != null && (currentAuth.employeeId == employeeId || currentAuth.fullName.equals(oldName, ignoreCase = true))) {
                _authenticatedUser.value = currentAuth.copy(
                    fullName = name.trim(),
                    email = email.trim(),
                    mobile = mobile.trim(),
                    department = department.trim(),
                    designation = designation.trim(),
                    actualRole = when {
                        role.contains("Admin", true) || role.contains("Owner", true) -> AppRole.ADMIN
                        role.contains("Manager", true) -> AppRole.MANAGER
                        else -> AppRole.EMPLOYEE
                    }
                )
                _currentUserRole.value = _authenticatedUser.value?.actualRole?.label ?: role
            }

            repository.logActivity(
                _currentWorkspaceId.value,
                _currentUserName.value,
                "Updated details and login for employee ${name.trim()}",
                "Admin / HR"
            )
            _snackbarMessage.value = "Employee ${name.trim()} updated & synchronized across all modules!"
        }
    }

    fun deleteEmployee(employeeId: String, employeeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEmployee(employeeId)
            repository.logActivity(
                _currentWorkspaceId.value,
                _currentUserName.value,
                "Removed employee $employeeName from workspace",
                "Admin / HR"
            )
            _snackbarMessage.value = "Employee $employeeName removed"
        }
    }

    fun toggleEmployeeStatus(employee: EmployeeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = if (employee.status == "Active") "Inactive" else "Active"
            repository.updateEmployee(employee.copy(status = newStatus))
            _snackbarMessage.value = "${employee.name} marked as $newStatus"
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: String, remarks: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowCompleted = newStatus == "Completed"
            val now = java.util.Date()
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

            val updated = task.copy(
                status = newStatus,
                completionDate = if (isNowCompleted) dateFormat.format(now) else task.completionDate,
                completionTime = if (isNowCompleted) timeFormat.format(now) else task.completionTime,
                completedBy = if (isNowCompleted) _currentUserName.value else task.completedBy,
                remarks = if (remarks.isNotBlank()) remarks else task.remarks
            )
            repository.updateTask(updated)
            repository.logActivity(
                _currentWorkspaceId.value,
                _currentUserName.value,
                "Updated task '${task.title}' status to $newStatus",
                "Tasks"
            )
            _snackbarMessage.value = "Task status updated to $newStatus"
        }
    }

    fun assignNewTask(
        title: String,
        description: String,
        assignedTo: String,
        projectName: String,
        priority: String,
        deadline: String,
        dueTime: String = "06:00 PM"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = TaskEntity(
                id = "task_" + UUID.randomUUID().toString().take(8),
                workspaceId = _currentWorkspaceId.value,
                title = title.trim(),
                description = description.trim(),
                assignedTo = assignedTo.trim(),
                projectName = projectName.trim(),
                priority = priority,
                deadline = deadline.trim(),
                dueTime = dueTime.trim(),
                status = "Pending",
                assignedBy = _currentUserName.value
            )
            repository.saveTask(task)
            repository.logActivity(
                _currentWorkspaceId.value,
                _currentUserName.value,
                "Assigned new task '$title' to $assignedTo",
                "Tasks"
            )
            _snackbarMessage.value = "Task '$title' assigned to $assignedTo"
        }
    }

    fun updateEmployeeRoleAndResponsibilities(
        employeeId: String,
        newRole: String,
        newDesignation: String,
        newDepartment: String,
        newResponsibilities: String,
        newAssignedPermissions: String,
        newSalary: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = employees.value.firstOrNull { it.id == employeeId }
            if (existing != null) {
                val updated = existing.copy(
                    role = newRole,
                    designation = newDesignation,
                    department = newDepartment,
                    responsibilities = newResponsibilities,
                    assignedPermissions = newAssignedPermissions,
                    basicSalary = newSalary
                )
                repository.updateEmployee(updated)
                _snackbarMessage.value = "Assigned role & responsibilities for ${updated.name}"
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
            _snackbarMessage.value = "Task details updated"
        }
    }

    // HR Operations
    fun addEmployee(employee: EmployeeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveEmployee(employee.copy(workspaceId = _currentWorkspaceId.value))
            _snackbarMessage.value = "Employee ${employee.name} onboarded"
        }
    }

    // Attendance Management (Full Control & Audit Trail)
    fun markAttendance(
        employeeId: String,
        employeeName: String,
        status: String,
        date: String = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
        workingHours: Double = if (status == "Half-Day" || status == "Half Day") 4.5 else if (status == "Absent" || status == "Leave") 0.0 else 9.0,
        checkInTime: String = "09:30 AM",
        checkOutTime: String = "06:30 PM",
        notes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val deterministicId = "att_${employeeId}_${date.replace(" ", "_").replace("-", "_")}"
            val record = AttendanceEntity(
                id = deterministicId,
                workspaceId = _currentWorkspaceId.value,
                employeeId = employeeId,
                employeeName = employeeName,
                date = date,
                status = status,
                workingHours = workingHours,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                notes = notes,
                updatedBy = _currentUserName.value.ifBlank { "Admin" },
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAttendance(record)
            logActivity(
                action = "Attendance Recorded",
                details = "$employeeName marked as '$status' for $date ($workingHours hrs)"
            )
            _snackbarMessage.value = "Attendance updated: $employeeName - $status ($date)"
        }
    }

    fun deleteAttendanceRecord(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAttendance(id)
            _snackbarMessage.value = "Attendance record removed"
        }
    }

    // Biometric Attendance Authentication with Audit Logging
    fun markBiometricAttendance(
        employeeId: String,
        employeeName: String,
        authTag: String,
        verificationType: String = "BIOMETRIC_DEVICE",
        isCheckOut: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            val deterministicId = "att_${employeeId}_${dateStr.replace(" ", "_").replace("-", "_")}"

            val existing = attendance.value.firstOrNull { it.employeeId == employeeId && it.date == dateStr }
            val checkIn = if (isCheckOut) (existing?.checkInTime ?: "09:30 AM") else timeStr
            val checkOut = if (isCheckOut) timeStr else (existing?.checkOutTime ?: "06:30 PM")

            val record = AttendanceEntity(
                id = deterministicId,
                workspaceId = _currentWorkspaceId.value,
                employeeId = employeeId,
                employeeName = employeeName,
                date = dateStr,
                status = "Present",
                workingHours = 9.0,
                checkInTime = checkIn,
                checkOutTime = checkOut,
                notes = "Verified via $verificationType ($authTag)",
                updatedBy = "$employeeName ($authTag)",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAttendance(record)
            logActivity(
                action = if (isCheckOut) "Biometric Check-Out" else "Biometric Check-In",
                details = "$employeeName verified via $authTag ($verificationType) on $dateStr at $timeStr"
            )
            triggerCloudSync()
            _snackbarMessage.value = if (isCheckOut) "Checked Out successfully for $employeeName" else "Verified! Attendance marked Present for $employeeName"
        }
    }

    // Attendance Management (Full Control & Audit Trail)
    fun saveMonthlyAttendanceBatch(
        employeeId: String,
        employeeName: String,
        recordsToSave: List<AttendanceEntity>,
        datesToDelete: List<String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val wsId = _currentWorkspaceId.value
            for (rec in recordsToSave) {
                repository.saveAttendance(rec.copy(workspaceId = wsId))
            }
            for (d in datesToDelete) {
                repository.deleteAttendanceByEmployeeDate(wsId, employeeId, d)
            }
            logActivity(
                action = "Attendance Saved",
                details = "Updated ${recordsToSave.size} dates for $employeeName"
            )
            triggerCloudSync()
            _snackbarMessage.value = "Attendance updated (${recordsToSave.size} days saved)"
        }
    }

    // Salary & Payroll Calculation System (9h standard, configurable basis, breakdown)
    fun calculateAndSaveMonthlyPayroll(
        employee: EmployeeEntity,
        monthYear: String,
        calculationBasis: String = "SCHEDULED_WORKING_DAYS", // SCHEDULED_WORKING_DAYS or CALENDAR_DAYS
        totalCalendarDays: Int = 30,
        scheduledWorkingDays: Int = 26,
        overtimeHours: Double = 0.0,
        deductions: Double = 0.0,
        deductionReason: String = "",
        calculationNotes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val empAttendance = attendance.value.filter { it.employeeId == employee.id }
            val empLeaves = leaves.value.filter { it.employeeName.equals(employee.name, ignoreCase = true) }

            val input = PayrollCalculationEngine.PayrollInput(
                monthlyBaseSalary = employee.basicSalary,
                monthYear = monthYear,
                calculationBasis = calculationBasis,
                scheduledWorkingDays = scheduledWorkingDays,
                totalCalendarDays = totalCalendarDays,
                standardDailyHours = 9.0,
                attendanceRecords = empAttendance,
                approvedLeaves = empLeaves,
                overtimeHours = overtimeHours,
                overtimeMultiplier = 1.5,
                bonuses = 0.0,
                deductions = deductions,
                deductionReason = deductionReason,
                manualNotes = calculationNotes
            )
            val result = PayrollCalculationEngine.calculate(input)

            val payrollId = "pay_${employee.id}_${monthYear.replace(" ", "_").replace("-", "_")}"
            val existing = payrollRecords.value.firstOrNull { it.id == payrollId }
            val payrollRecord = PayrollRecordEntity(
                id = payrollId,
                workspaceId = _currentWorkspaceId.value,
                employeeId = employee.id,
                employeeName = employee.name,
                monthYear = monthYear,
                baseMonthlySalary = result.baseMonthlySalary,
                totalCalendarDays = result.totalCalendarDays,
                scheduledWorkingDays = result.scheduledWorkingDays,
                calculationBasis = result.calculationBasis,
                standardDailyHours = result.standardDailyHours,
                presentDays = result.presentDays,
                halfDays = result.halfDays,
                paidLeaveDays = result.paidLeaveDays,
                unpaidLeaveDays = result.unpaidLeaveDays,
                weeklyOffDays = result.weeklyOffDays,
                holidayDays = result.holidayDays,
                payableDays = result.payableDays,
                totalWorkingHours = result.approvedPayableHours,
                dailyRate = result.dailyRate,
                grossPayable = result.grossPayable,
                deductions = result.deductions,
                deductionReason = result.deductionReason,
                overtimeHours = result.overtimeHours,
                overtimeAmount = result.overtimeAmount,
                netSalary = result.netSalary,
                status = existing?.status ?: "Draft",
                approvedBy = existing?.approvedBy ?: "",
                approvalDate = existing?.approvalDate ?: "",
                paymentDate = existing?.paymentDate ?: "",
                calculationNotes = result.transparentBreakdown,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )

            repository.savePayrollRecord(payrollRecord)
            triggerCloudSync()
            logActivity(
                action = "Payroll Calculated",
                details = "Computed salary for ${employee.name} ($monthYear): Net ₹${result.netSalary.toInt()} via ${result.calculationBasis}"
            )
            _snackbarMessage.value = "Calculated salary for ${employee.name}: Net ₹${result.netSalary.toInt()}"
        }
    }

    fun approvePayroll(payrollId: String, approverName: String = "Shakir Tamboli") {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            repository.approvePayrollRecord(payrollId, "Approved", approverName, dateStr)
            logActivity(
                action = "Payroll Approved",
                details = "Approved payroll record $payrollId by $approverName on $dateStr"
            )
            triggerCloudSync()
            _snackbarMessage.value = "Payroll approved successfully!"
        }
    }

    fun markPayrollPaid(payrollId: String, paymentDate: String = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.approvePayrollRecord(payrollId, "Paid", _currentUserName.value.ifBlank { "Management" }, paymentDate)
            logActivity("Payroll Paid", "Payroll record $payrollId marked as Paid on $paymentDate")
            triggerCloudSync()
            _snackbarMessage.value = "Payroll marked as Paid ($paymentDate)"
        }
    }

    fun deletePayrollRecord(payrollId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePayrollRecord(payrollId)
            logActivity("Payroll Deleted", "Payroll record $payrollId deleted")
            triggerCloudSync()
            _snackbarMessage.value = "Payroll record removed"
        }
    }

    fun deletePayroll(payrollId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePayrollRecord(payrollId)
            _snackbarMessage.value = "Payroll record removed"
        }
    }

    fun submitLeave(employeeName: String, type: String, fromDate: String, toDate: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val leave = LeaveEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = _currentWorkspaceId.value,
                employeeName = employeeName,
                leaveType = type,
                fromDate = fromDate,
                toDate = toDate,
                reason = reason,
                status = "Pending"
            )
            repository.saveLeave(leave)
            _snackbarMessage.value = "Leave request submitted for approval"
        }
    }

    fun updateLeaveStatus(leave: LeaveEntity, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLeave(leave.copy(status = newStatus))
            _snackbarMessage.value = "Leave request $newStatus"
        }
    }

    // Tasks
    fun addTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTask(task.copy(workspaceId = _currentWorkspaceId.value))
            _snackbarMessage.value = "Task created"
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = if (task.status == "Completed") "In Progress" else "Completed"
            repository.updateTask(task.copy(status = newStatus))
            _snackbarMessage.value = "Task marked $newStatus"
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllNotificationsAsRead(_currentWorkspaceId.value)
            _snackbarMessage.value = "All notifications marked as read"
        }
    }

    // Subscription & Billing
    fun upgradeSubscription(planId: String, billingCycle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val amount = when (planId) {
                "FREE" -> 0.0
                "PRO" -> if (billingCycle == "YEARLY") 2999.0 else 299.0
                "STUDIO" -> if (billingCycle == "YEARLY") 6999.0 else 699.0
                "BUSINESS" -> if (billingCycle == "YEARLY") 14999.0 else 1499.0
                else -> 699.0
            }
            val sub = SubscriptionEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = _currentWorkspaceId.value,
                planId = planId,
                planName = "$planId Tier",
                billingCycle = billingCycle,
                amount = amount,
                status = "Active",
                startDate = "30 Sep 2026",
                renewalDate = if (billingCycle == "YEARLY") "30 Sep 2027" else "30 Oct 2026",
                razorpayOrderId = "order_RZP_" + UUID.randomUUID().toString().take(8)
            )
            repository.saveSubscription(sub)
            // Update workspace plan
            workspace.value?.let { ws ->
                repository.saveWorkspace(ws.copy(planId = planId, isTrialActive = false))
            }
            repository.logActivity(_currentWorkspaceId.value, currentUser.value?.name ?: "Admin", "Upgraded plan to $planId ($billingCycle) via Razorpay", "SaaS Billing")
            _snackbarMessage.value = "Successfully upgraded to $planId ($billingCycle)!"
        }
    }

    // Create New Workspace (Onboarding / Multi-tenant)
    fun createWorkspace(
        name: String,
        businessType: String,
        city: String,
        phone: String,
        email: String,
        plan: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = "ws_" + UUID.randomUUID().toString().take(6)
            val newWs = WorkspaceEntity(
                id = newId,
                name = name,
                businessType = businessType,
                city = city,
                phone = phone,
                email = email,
                address = "Studio 1, $city",
                gstNumber = "27AABC${(1000..9999).random()}K1Z0",
                planId = plan,
                isTrialActive = true,
                trialDaysRemaining = 7
            )
            repository.saveWorkspace(newWs)

            val newUser = UserEntity(
                id = "user_" + UUID.randomUUID().toString().take(6),
                workspaceId = newId,
                name = "Studio Owner",
                email = email,
                mobile = phone,
                role = "Owner / Admin",
                loginMethod = "OTP"
            )
            repository.saveUser(newUser)

            _currentWorkspaceId.value = newId
            _snackbarMessage.value = "Workspace '$name' created and activated!"
        }
    }

    fun updateWorkspace(updated: WorkspaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveWorkspace(updated)
            repository.logActivity(updated.id, currentUser.value?.name ?: "Admin", "Updated Studio name: ${updated.name}", "Settings")
            _snackbarMessage.value = "Studio name updated successfully!"
        }
    }

    fun canUseFeature(featureKey: String): Boolean {
        val currentPlan = workspace.value?.planId ?: "STUDIO"
        return repository.canUseFeature(currentPlan, featureKey)
    }

    // Search query results
    fun getSearchResults(query: String): List<GlobalSearchResult> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val results = mutableListOf<GlobalSearchResult>()

        clients.value.filter { it.name.lowercase().contains(q) || it.mobile.contains(q) }.forEach {
            results.add(GlobalSearchResult(it.id, it.name, "Client • ${it.eventType} • ${it.city}", "Clients", BmsNavigationItem.CLIENTS))
        }
        leads.value.filter { it.name.lowercase().contains(q) || it.eventType.lowercase().contains(q) }.forEach {
            results.add(GlobalSearchResult(it.id, it.name, "Lead • ${it.status} • Budget ₹${it.estimatedBudget.toInt()}", "Leads", BmsNavigationItem.LEADS))
        }
        bookings.value.filter { it.clientName.lowercase().contains(q) || it.bookingNumber.lowercase().contains(q) || it.venue.lowercase().contains(q) }.forEach {
            results.add(GlobalSearchResult(it.id, it.bookingNumber, "Booking • ${it.clientName} • ${it.eventDate}", "Bookings", BmsNavigationItem.BOOKINGS))
        }
        quotations.value.filter { it.quotationNumber.lowercase().contains(q) || it.clientName.lowercase().contains(q) }.forEach {
            results.add(GlobalSearchResult(it.id, it.quotationNumber, "Quotation • ${it.clientName} • ₹${it.totalAmount.toInt()}", "Quotations", BmsNavigationItem.QUOTATIONS))
        }
        employees.value.filter { it.name.lowercase().contains(q) || it.designation.lowercase().contains(q) }.forEach {
            results.add(GlobalSearchResult(it.id, it.name, "Employee • ${it.designation} (${it.department})", "Team", BmsNavigationItem.TEAM))
        }
        tasks.value.filter { it.title.lowercase().contains(q) || it.assignedTo.lowercase().contains(q) }.forEach {
            results.add(GlobalSearchResult(it.id, it.title, "Task • ${it.assignedTo} • ${it.priority}", "Tasks", BmsNavigationItem.TASKS))
        }

        return results
    }

    // Storage & Project Files Operations
    fun addStorageDrive(drive: StorageDriveEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStorageDrive(drive.copy(workspaceId = _currentWorkspaceId.value))
            _snackbarMessage.value = "Storage drive '${drive.driveLabel}' registered"
        }
    }

    fun updateStorageDrive(drive: StorageDriveEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStorageDrive(drive)
            _snackbarMessage.value = "Storage drive '${drive.driveLabel}' updated"
        }
    }

    fun deleteStorageDrive(drive: StorageDriveEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStorageDrive(drive)
            _snackbarMessage.value = "Drive removed from inventory"
        }
    }

    fun addProjectFile(file: ProjectFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProjectFile(file.copy(workspaceId = _currentWorkspaceId.value))
            _snackbarMessage.value = "Project files link saved for ${file.clientName}"
        }
    }

    fun updateProjectFile(file: ProjectFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProjectFile(file)
            _snackbarMessage.value = "Project files updated for ${file.clientName}"
        }
    }

    fun deleteProjectFile(file: ProjectFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProjectFile(file)
            _snackbarMessage.value = "Project file record removed"
        }
    }

    // CSV Data Export & Import
    fun exportClientsCsv(): String {
        val sb = StringBuilder("ID,Name,Mobile,Email,Event Type,City,Address,Total Value,Paid Amount,Pending Amount,Stage\n")
        clients.value.forEach {
            sb.append("\"${it.id}\",\"${it.name}\",\"${it.mobile}\",\"${it.email}\",\"${it.eventType}\",\"${it.city}\",\"${it.address}\",${it.totalBookingValue},${it.paidAmount},${it.pendingAmount},\"${it.timelineStage}\"\n")
        }
        return sb.toString()
    }

    fun exportLeadsCsv(): String {
        val sb = StringBuilder("ID,Name,Mobile,Email,Event Type,Event Date,Venue,Source,Budget,Status,Assigned Staff\n")
        leads.value.forEach {
            sb.append("\"${it.id}\",\"${it.name}\",\"${it.mobile}\",\"${it.email}\",\"${it.eventType}\",\"${it.eventDate}\",\"${it.venue}\",\"${it.source}\",${it.estimatedBudget},\"${it.status}\",\"${it.assignedStaff}\"\n")
        }
        return sb.toString()
    }

    fun exportBookingsCsv(): String {
        val sb = StringBuilder("Booking No,Client Name,Event Type,Event Date,Event Time,Venue,Package,Total Amount,Advance Paid,Balance,Assigned Crew,Status\n")
        bookings.value.forEach {
            sb.append("\"${it.bookingNumber}\",\"${it.clientName}\",\"${it.eventType}\",\"${it.eventDate}\",\"${it.eventTime}\",\"${it.venue}\",\"${it.packageName}\",${it.totalAmount},${it.advancePaid},${it.balanceAmount},\"${it.assignedTeam}\",\"${it.status}\"\n")
        }
        return sb.toString()
    }

    fun exportFinanceLedgerCsv(): String {
        val sb = StringBuilder("Type,Record ID,Name/Category,Date,Amount,Mode,Reference,Status\n")
        payments.value.forEach {
            sb.append("Payment Inflow,\"${it.id}\",\"${it.clientName}\",\"${it.paymentDate}\",${it.amount},\"${it.paymentMethod}\",\"${it.transactionRef}\",\"${it.status}\"\n")
        }
        expenses.value.forEach {
            sb.append("Expense Outflow,\"${it.id}\",\"${it.category} - ${it.description}\",\"${it.date}\",-${it.amount},\"${it.paymentMethod}\",\"${it.employeeName}\",\"Paid\"\n")
        }
        return sb.toString()
    }

    fun importClientsFromCsv(csvText: String): Int {
        var count = 0
        viewModelScope.launch(Dispatchers.IO) {
            val lines = csvText.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) return@launch
            val newClients = mutableListOf<ClientEntity>()
            for (i in 1 until lines.size) {
                val tokens = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
                if (tokens.isNotEmpty() && tokens[0].isNotBlank()) {
                    val name = if (tokens.size > 1) tokens[1] else tokens[0]
                    val mobile = if (tokens.size > 2) tokens[2] else "+91 98000 00000"
                    val email = if (tokens.size > 3) tokens[3] else ""
                    val event = if (tokens.size > 4) tokens[4] else "Wedding"
                    val city = if (tokens.size > 5) tokens[5] else "Pune"
                    val tv = if (tokens.size > 7) tokens[7].toDoubleOrNull() ?: 150000.0 else 150000.0
                    val pa = if (tokens.size > 8) tokens[8].toDoubleOrNull() ?: 0.0 else 0.0
                    newClients.add(
                        ClientEntity(
                            id = "client_" + UUID.randomUUID().toString().take(6),
                            workspaceId = _currentWorkspaceId.value,
                            name = name,
                            mobile = mobile,
                            email = email,
                            address = "Imported",
                            city = city,
                            eventType = event,
                            totalBookingValue = tv,
                            paidAmount = pa,
                            pendingAmount = (tv - pa).coerceAtLeast(0.0),
                            timelineStage = "Booking"
                        )
                    )
                    count++
                }
            }
            if (newClients.isNotEmpty()) {
                newClients.forEach { repository.saveClient(it) }
                _snackbarMessage.value = "Imported $count clients from CSV successfully!"
            }
        }
        return count
    }
}
