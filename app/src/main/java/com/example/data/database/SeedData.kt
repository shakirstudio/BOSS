package com.example.data.database

import com.example.data.dao.BmsDao
import com.example.data.model.*

object SeedData {
    const val DEFAULT_WORKSPACE_ID = "ws_lumiere_01"

    suspend fun populateInitialData(dao: BmsDao) {
        val workspace = WorkspaceEntity(
            id = DEFAULT_WORKSPACE_ID,
            name = "Shakir Photography & Cinema",
            businessType = "Photography & Film Production",
            city = "Pune",
            phone = "+91 98234 56789",
            email = "shakirtamboli.pune11@gmail.com",
            address = "Penthouse 4B, Sky Vista Tower, Viman Nagar",
            gstNumber = "27AAACL1234F1Z8",
            tagline = "Manage. Track. Grow.",
            planId = "STUDIO",
            isTrialActive = true,
            trialDaysRemaining = 7
        )
        dao.insertWorkspace(workspace)

        val currentUser = UserEntity(
            id = "user_owner_01",
            workspaceId = DEFAULT_WORKSPACE_ID,
            name = "Shakir Tamboli",
            email = "shakirtamboli.pune11@gmail.com",
            mobile = "+91 98234 56789",
            role = "Owner / Admin",
            loginMethod = "GOOGLE"
        )
        dao.insertUser(currentUser)

        // Only genuine Super Admin account is seeded; zero dummy employees
        val initialEmployees = listOf(
            EmployeeEntity(
                id = "emp_shakir",
                workspaceId = DEFAULT_WORKSPACE_ID,
                employeeCode = "BOSS-000",
                name = "Shakir Tamboli",
                mobile = "+91 98234 56789",
                email = "shakirtamboli.pune11@gmail.com",
                department = "Executive Management",
                designation = "Super Admin & Managing Director",
                role = "Admin",
                joiningDate = "01 Jan 2022",
                basicSalary = 120000.0,
                emergencyContact = "+91 98234 00000",
                responsibilities = "Overall studio management, client contracts, financial ledgers & system administration",
                assignedPermissions = "FULL_ADMIN_ACCESS",
                loginUsername = "admin",
                loginPassword = "" // Requires user to set secure password on first launch
            )
        )
        dao.insertAllEmployees(initialEmployees)

        // Subscription record for genuine studio workspace
        val subscription = SubscriptionEntity(
            id = "sub_01",
            workspaceId = DEFAULT_WORKSPACE_ID,
            planId = "STUDIO",
            planName = "Studio Enterprise Plan",
            billingCycle = "YEARLY",
            amount = 14999.0,
            status = "Active",
            startDate = "01 Jan 2026",
            renewalDate = "31 Dec 2026"
        )
        dao.insertSubscription(subscription)
    }
}
