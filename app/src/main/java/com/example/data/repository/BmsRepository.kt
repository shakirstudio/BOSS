package com.example.data.repository

import com.example.data.dao.BmsDao
import com.example.data.database.SeedData
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BmsRepository(private val dao: BmsDao) {

    fun getWorkspace(workspaceId: String): Flow<WorkspaceEntity?> = dao.getWorkspace(workspaceId)
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>> = dao.getAllWorkspaces()
    suspend fun saveWorkspace(workspace: WorkspaceEntity) = dao.insertWorkspace(workspace)

    fun getCurrentUser(workspaceId: String): Flow<UserEntity?> = dao.getCurrentUser(workspaceId)
    suspend fun saveUser(user: UserEntity) = dao.insertUser(user)

    // Leads
    fun getLeads(workspaceId: String): Flow<List<LeadEntity>> = dao.getLeads(workspaceId)
    suspend fun saveLead(lead: LeadEntity) = dao.insertLead(lead)
    suspend fun updateLead(lead: LeadEntity) = dao.updateLead(lead)
    suspend fun deleteLead(lead: LeadEntity) = dao.deleteLead(lead)

    // Convert Lead to Client & Quotation
    suspend fun convertLeadToClient(lead: LeadEntity, userName: String): String {
        val clientId = "client_" + UUID.randomUUID().toString().take(8)
        val client = ClientEntity(
            id = clientId,
            workspaceId = lead.workspaceId,
            name = lead.name,
            mobile = lead.mobile,
            email = lead.email,
            address = lead.venue,
            city = "Pune",
            eventType = lead.eventType,
            totalBookingValue = lead.estimatedBudget,
            paidAmount = 0.0,
            pendingAmount = lead.estimatedBudget,
            notes = "Converted from Lead: ${lead.notes}",
            timelineStage = "Quotation"
        )
        dao.insertClient(client)

        // Mark lead as won & converted
        val updatedLead = lead.copy(status = "Won", isConverted = true)
        dao.updateLead(updatedLead)

        // Create initial draft quotation
        val quotId = "quot_" + UUID.randomUUID().toString().take(8)
        val quotNumber = "QT-2026-" + (1050..1999).random()
        val subtotal = lead.estimatedBudget
        val discount = 0.0
        val tax = (subtotal - discount) * 0.18
        val total = (subtotal - discount) + tax
        val quotation = QuotationEntity(
            id = quotId,
            workspaceId = lead.workspaceId,
            quotationNumber = quotNumber,
            clientId = clientId,
            clientName = lead.name,
            contact = lead.mobile,
            eventDate = lead.eventDate,
            eventLocation = lead.venue,
            services = "Full Photography & Videography Coverage",
            packageDetails = "Custom Package based on lead inquiry",
            subtotal = subtotal,
            discount = discount,
            taxAmount = tax,
            totalAmount = total,
            advanceRequired = total * 0.3,
            balanceAmount = total * 0.7,
            status = "Draft",
            terms = "1. 30% advance on confirmation. 2. 50% on shoot date. 3. Balance on delivery."
        )
        dao.insertQuotation(quotation)

        // Log activity
        dao.insertActivityLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = lead.workspaceId,
                userName = userName,
                action = "Converted lead ${lead.name} to active Client and generated quotation $quotNumber",
                module = "CRM",
                timestamp = "Just now"
            )
        )

        return clientId
    }

    // Clients
    fun getClients(workspaceId: String): Flow<List<ClientEntity>> = dao.getClients(workspaceId)
    fun getClientById(workspaceId: String, clientId: String): Flow<ClientEntity?> = dao.getClientById(workspaceId, clientId)
    suspend fun saveClient(client: ClientEntity) = dao.insertClient(client)
    suspend fun updateClient(client: ClientEntity) = dao.updateClient(client)

    // Quotations
    fun getQuotations(workspaceId: String): Flow<List<QuotationEntity>> = dao.getQuotations(workspaceId)
    suspend fun saveQuotation(quotation: QuotationEntity) = dao.insertQuotation(quotation)
    suspend fun updateQuotation(quotation: QuotationEntity) = dao.updateQuotation(quotation)

    // Convert Quotation to Booking
    suspend fun convertQuotationToBooking(quotation: QuotationEntity, userName: String): String {
        val bookingId = "book_" + UUID.randomUUID().toString().take(8)
        val bookingNumber = "BK-2026-" + (100..999).random()

        val booking = BookingEntity(
            id = bookingId,
            workspaceId = quotation.workspaceId,
            bookingNumber = bookingNumber,
            clientId = quotation.clientId,
            clientName = quotation.clientName,
            eventType = quotation.services.take(30),
            eventDate = quotation.eventDate,
            venue = quotation.eventLocation,
            packageName = quotation.packageDetails,
            totalAmount = quotation.totalAmount,
            advancePaid = quotation.advanceRequired,
            balanceAmount = quotation.balanceAmount,
            assignedTeam = "Team Assignment Pending",
            status = "Confirmed",
            notes = "Auto-created from accepted quotation ${quotation.quotationNumber}"
        )
        dao.insertBooking(booking)

        // Update quotation status to Accepted
        dao.updateQuotation(quotation.copy(status = "Accepted"))

        // Create shoot item
        val shoot = ShootEntity(
            id = "shoot_" + UUID.randomUUID().toString().take(8),
            workspaceId = quotation.workspaceId,
            bookingId = bookingId,
            clientName = quotation.clientName,
            eventType = quotation.services.take(30),
            shootDate = quotation.eventDate,
            callTime = "08:00 AM",
            venue = quotation.eventLocation,
            leadPhotographer = "To Be Assigned",
            videographer = "To Be Assigned",
            equipmentList = "Standard Dual Camera + Lighting Kit",
            status = "Planned"
        )
        dao.insertShoot(shoot)

        // Record initial advance payment
        dao.insertPayment(
            PaymentEntity(
                id = "pay_" + UUID.randomUUID().toString().take(8),
                workspaceId = quotation.workspaceId,
                bookingId = bookingId,
                clientName = quotation.clientName,
                amount = quotation.advanceRequired,
                paymentDate = quotation.eventDate,
                paymentMethod = "Bank Transfer",
                transactionRef = "ADV-${quotation.quotationNumber}",
                paymentType = "Advance Booking Amount",
                status = "Paid"
            )
        )

        // Log activity
        dao.insertActivityLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = quotation.workspaceId,
                userName = userName,
                action = "Confirmed Quotation ${quotation.quotationNumber} -> Booking $bookingNumber for ${quotation.clientName}",
                module = "Sales",
                timestamp = "Just now"
            )
        )

        return bookingId
    }

    // Bookings
    fun getBookings(workspaceId: String): Flow<List<BookingEntity>> = dao.getBookings(workspaceId)
    suspend fun saveBooking(booking: BookingEntity) = dao.insertBooking(booking)
    suspend fun updateBooking(booking: BookingEntity) = dao.updateBooking(booking)

    // Shoots
    fun getShoots(workspaceId: String): Flow<List<ShootEntity>> = dao.getShoots(workspaceId)
    suspend fun saveShoot(shoot: ShootEntity) = dao.insertShoot(shoot)
    suspend fun updateShoot(shoot: ShootEntity) = dao.updateShoot(shoot)

    // Photo & Video Edits
    fun getPhotoEdits(workspaceId: String): Flow<List<PhotoEditEntity>> = dao.getPhotoEdits(workspaceId)
    suspend fun savePhotoEdit(edit: PhotoEditEntity) = dao.insertPhotoEdit(edit)
    suspend fun updatePhotoEdit(edit: PhotoEditEntity) = dao.updatePhotoEdit(edit)

    fun getVideoEdits(workspaceId: String): Flow<List<VideoEditEntity>> = dao.getVideoEdits(workspaceId)
    suspend fun saveVideoEdit(edit: VideoEditEntity) = dao.insertVideoEdit(edit)
    suspend fun updateVideoEdit(edit: VideoEditEntity) = dao.updateVideoEdit(edit)

    // Albums
    fun getAlbumProjects(workspaceId: String): Flow<List<AlbumProjectEntity>> = dao.getAlbumProjects(workspaceId)
    suspend fun saveAlbumProject(album: AlbumProjectEntity) = dao.insertAlbumProject(album)
    suspend fun updateAlbumProject(album: AlbumProjectEntity) = dao.updateAlbumProject(album)

    // Deliveries
    fun getDeliveries(workspaceId: String): Flow<List<DeliveryEntity>> = dao.getDeliveries(workspaceId)
    suspend fun saveDelivery(delivery: DeliveryEntity) = dao.insertDelivery(delivery)
    suspend fun updateDelivery(delivery: DeliveryEntity) = dao.updateDelivery(delivery)

    // Payments & Expenses
    fun getPayments(workspaceId: String): Flow<List<PaymentEntity>> = dao.getPayments(workspaceId)
    suspend fun savePayment(payment: PaymentEntity) = dao.insertPayment(payment)
    suspend fun updatePayment(payment: PaymentEntity) = dao.updatePayment(payment)

    fun getExpenses(workspaceId: String): Flow<List<ExpenseEntity>> = dao.getExpenses(workspaceId)
    suspend fun saveExpense(expense: ExpenseEntity) = dao.insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = dao.updateExpense(expense)

    // Employees, Attendance, Leaves
    fun getEmployees(workspaceId: String): Flow<List<EmployeeEntity>> = dao.getEmployees(workspaceId)
    suspend fun saveEmployee(employee: EmployeeEntity) = dao.insertEmployee(employee)
    suspend fun updateEmployee(employee: EmployeeEntity) = dao.updateEmployee(employee)
    suspend fun deleteEmployee(employeeId: String) = dao.deleteEmployeeById(employeeId)

    suspend fun cascadeEmployeeNameUpdate(oldName: String, newName: String, workspaceId: String, employeeId: String) {
        if (oldName.isNotBlank() && oldName != newName) {
            dao.updateTaskAssignee(oldName, newName, workspaceId)
            dao.updateTaskCompletedBy(oldName, newName, workspaceId)
            dao.updateTaskAssignedBy(oldName, newName, workspaceId)
            dao.updateAttendanceEmployeeName(employeeId, newName, oldName)
            dao.updateActivityLogUserName(oldName, newName)
            dao.updateLeaveEmployeeName(oldName, newName)
            dao.updateExpenseEmployeeName(oldName, newName)
            dao.updateShootLeadPhotographer(oldName, newName, workspaceId)
            dao.updateShootVideographer(oldName, newName, workspaceId)
            dao.updatePhotoEditEditor(oldName, newName, workspaceId)
            dao.updateVideoEditEditor(oldName, newName, workspaceId)
            dao.updateAlbumDesigner(oldName, newName, workspaceId)
            dao.updateLeadAssignedStaff(oldName, newName, workspaceId)
            dao.updateUserName(oldName, newName)
        }
    }

    fun getAttendance(workspaceId: String): Flow<List<AttendanceEntity>> = dao.getAttendance(workspaceId)
    fun getEmployeeAttendance(workspaceId: String, employeeId: String): Flow<List<AttendanceEntity>> = dao.getEmployeeAttendance(workspaceId, employeeId)
    suspend fun saveAttendance(attendance: AttendanceEntity) = dao.insertAttendance(attendance)
    suspend fun updateAttendance(attendance: AttendanceEntity) = dao.updateAttendance(attendance)
    suspend fun deleteAttendance(id: String) = dao.deleteAttendance(id)
    suspend fun deleteAttendanceByEmployeeDate(workspaceId: String, employeeId: String, date: String) = dao.deleteAttendanceByEmployeeDate(workspaceId, employeeId, date)

    // Payroll Records
    fun getPayrollRecords(workspaceId: String): Flow<List<PayrollRecordEntity>> = dao.getPayrollRecords(workspaceId)
    fun getPayrollForMonth(workspaceId: String, monthYear: String): Flow<List<PayrollRecordEntity>> = dao.getPayrollForMonth(workspaceId, monthYear)
    fun getEmployeePayrollHistory(workspaceId: String, employeeId: String): Flow<List<PayrollRecordEntity>> = dao.getEmployeePayrollHistory(workspaceId, employeeId)
    suspend fun savePayrollRecord(payroll: PayrollRecordEntity) = dao.insertPayrollRecord(payroll)
    suspend fun updatePayrollRecord(payroll: PayrollRecordEntity) = dao.updatePayrollRecord(payroll)
    suspend fun deletePayrollRecord(id: String) = dao.deletePayrollRecord(id)
    suspend fun approvePayrollRecord(id: String, status: String, approvedBy: String, approvalDate: String) = dao.approvePayrollRecord(id, status, approvedBy, approvalDate)

    // Demo Data Purge
    suspend fun purgeAllDemoData() {
        dao.purgeDemoLeads()
        dao.purgeDemoClients()
        dao.purgeDemoQuotations()
        dao.purgeDemoBookings()
        dao.purgeDemoShoots()
        dao.purgeDemoPhotoEdits()
        dao.purgeDemoVideoEdits()
        dao.purgeDemoAlbumProjects()
        dao.purgeDemoDeliveries()
        dao.purgeDemoPayments()
        dao.purgeDemoExpenses()
        dao.purgeDemoEmployees()
        dao.purgeDemoAttendance()
        dao.purgeDemoLeaves()
        dao.purgeDemoTasks()
        dao.purgeDemoNotifications()
        dao.purgeDemoActivityLogs()
        dao.purgeDemoStorageDrives()
        dao.purgeDemoProjectFiles()
    }

    fun getLeaves(workspaceId: String): Flow<List<LeaveEntity>> = dao.getLeaves(workspaceId)
    suspend fun saveLeave(leave: LeaveEntity) = dao.insertLeave(leave)
    suspend fun updateLeave(leave: LeaveEntity) = dao.updateLeave(leave)

    // Tasks
    fun getTasks(workspaceId: String): Flow<List<TaskEntity>> = dao.getTasks(workspaceId)
    suspend fun saveTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(taskId: String) = dao.deleteTaskById(taskId)

    // Notifications
    fun getNotifications(workspaceId: String): Flow<List<NotificationEntity>> = dao.getNotifications(workspaceId)
    suspend fun saveNotification(notif: NotificationEntity) = dao.insertNotification(notif)
    suspend fun markAllNotificationsAsRead(workspaceId: String) = dao.markAllNotificationsAsRead(workspaceId)

    // Activity Logs
    fun getActivityLogs(workspaceId: String): Flow<List<ActivityLogEntity>> = dao.getActivityLogs(workspaceId)
    suspend fun logActivity(workspaceId: String, user: String, action: String, module: String) {
        dao.insertActivityLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                userName = user,
                action = action,
                module = module,
                timestamp = "Just now"
            )
        )
    }

    // Subscriptions
    fun getSubscription(workspaceId: String): Flow<SubscriptionEntity?> = dao.getSubscription(workspaceId)
    suspend fun saveSubscription(sub: SubscriptionEntity) = dao.insertSubscription(sub)
    suspend fun updateSubscription(sub: SubscriptionEntity) = dao.updateSubscription(sub)

    // Feature permission checks based on plan
    fun canUseFeature(planId: String, featureKey: String): Boolean {
        return when (planId.uppercase()) {
            "FREE" -> featureKey in listOf("leads", "clients", "basic_quotations", "basic_tasks")
            "PRO" -> featureKey in listOf("leads", "clients", "basic_quotations", "pdf_quotations", "basic_tasks", "shoots", "expenses", "team")
            "STUDIO" -> true // All studio features unlocked
            "BUSINESS" -> true
            else -> true
        }
    }

    // Storage Drives
    fun getStorageDrives(workspaceId: String): Flow<List<StorageDriveEntity>> = dao.getStorageDrives(workspaceId)
    suspend fun saveStorageDrive(drive: StorageDriveEntity) = dao.insertStorageDrive(drive)
    suspend fun updateStorageDrive(drive: StorageDriveEntity) = dao.updateStorageDrive(drive)
    suspend fun deleteStorageDrive(drive: StorageDriveEntity) = dao.deleteStorageDrive(drive)

    // Project Files
    fun getProjectFiles(workspaceId: String): Flow<List<ProjectFileEntity>> = dao.getProjectFiles(workspaceId)
    suspend fun saveProjectFile(file: ProjectFileEntity) = dao.insertProjectFile(file)
    suspend fun updateProjectFile(file: ProjectFileEntity) = dao.updateProjectFile(file)
    suspend fun deleteProjectFile(file: ProjectFileEntity) = dao.deleteProjectFile(file)

    suspend fun ensureInitialData() {
        if (dao.getWorkspaceCount() == 0) {
            SeedData.populateInitialData(dao)
        }
    }

    suspend fun resetWithSeedData() {
        SeedData.populateInitialData(dao)
    }
}
