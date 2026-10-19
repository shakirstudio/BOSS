package com.example.data.model

enum class AppRole(val label: String, val badgeColorHex: String) {
    ADMIN("Admin", "#EF4444"),
    MANAGER("Manager", "#3B82F6"),
    EMPLOYEE("Employee", "#10B981");

    companion object {
        fun fromLabel(label: String): AppRole? {
            return entries.find { it.label.equals(label.trim(), ignoreCase = true) }
        }
    }
}

enum class AppPermission {
    FULL_ADMIN_ACCESS,
    USER_MANAGEMENT,
    EMPLOYEE_MANAGEMENT,
    MANAGER_MANAGEMENT,
    ROLES_PERMISSIONS,
    SYSTEM_CONFIGURATION,
    AUDIT_LOGS,
    FINANCE_FULL,
    SETTINGS_ACCESS,
    TEAM_REPORTS,
    TEAM_ANALYTICS,
    TASK_MANAGEMENT,
    ATTENDANCE_MANAGEMENT,
    PERSONAL_DASHBOARD,
    MY_TASKS,
    MY_ATTENDANCE,
    MY_DOCUMENTS,
    MY_PROFILE,
    NOTIFICATIONS,
    PERSONAL_REPORTS
}

data class AuthUser(
    val id: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val actualRole: AppRole,
    val designation: String,
    val department: String,
    val permissions: Set<AppPermission>,
    val avatarUrl: String = "",
    val mobile: String = "",
    val emergencyContact: String = "",
    val employeeId: String = ""
)

typealias AuthAccount = AuthUser

sealed class LoginResult {
    object Idle : LoginResult()
    object Loading : LoginResult()
    object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
}
