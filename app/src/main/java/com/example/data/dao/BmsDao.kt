package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BmsDao {
    // Workspaces
    @Query("SELECT * FROM workspaces WHERE id = :workspaceId")
    fun getWorkspace(workspaceId: String): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM workspaces")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT COUNT(*) FROM workspaces")
    suspend fun getWorkspaceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    // Current User
    @Query("SELECT * FROM users WHERE workspaceId = :workspaceId LIMIT 1")
    fun getCurrentUser(workspaceId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Leads
    @Query("SELECT * FROM leads WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getLeads(workspaceId: String): Flow<List<LeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity)

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Delete
    suspend fun deleteLead(lead: LeadEntity)

    // Clients
    @Query("SELECT * FROM clients WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getClients(workspaceId: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE workspaceId = :workspaceId AND id = :clientId")
    fun getClientById(workspaceId: String, clientId: String): Flow<ClientEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    // Quotations
    @Query("SELECT * FROM quotations WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getQuotations(workspaceId: String): Flow<List<QuotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: QuotationEntity)

    @Update
    suspend fun updateQuotation(quotation: QuotationEntity)

    // Bookings
    @Query("SELECT * FROM bookings WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getBookings(workspaceId: String): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    // Shoots
    @Query("SELECT * FROM shoots WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getShoots(workspaceId: String): Flow<List<ShootEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoot(shoot: ShootEntity)

    @Update
    suspend fun updateShoot(shoot: ShootEntity)

    // Photo Edits
    @Query("SELECT * FROM photo_edits WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun getPhotoEdits(workspaceId: String): Flow<List<PhotoEditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoEdit(edit: PhotoEditEntity)

    @Update
    suspend fun updatePhotoEdit(edit: PhotoEditEntity)

    // Video Edits
    @Query("SELECT * FROM video_edits WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun getVideoEdits(workspaceId: String): Flow<List<VideoEditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoEdit(edit: VideoEditEntity)

    @Update
    suspend fun updateVideoEdit(edit: VideoEditEntity)

    // Albums
    @Query("SELECT * FROM album_projects WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun getAlbumProjects(workspaceId: String): Flow<List<AlbumProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumProject(album: AlbumProjectEntity)

    @Update
    suspend fun updateAlbumProject(album: AlbumProjectEntity)

    // Deliveries
    @Query("SELECT * FROM deliveries WHERE workspaceId = :workspaceId")
    fun getDeliveries(workspaceId: String): Flow<List<DeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    // Payments
    @Query("SELECT * FROM payments WHERE workspaceId = :workspaceId")
    fun getPayments(workspaceId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    // Expenses
    @Query("SELECT * FROM expenses WHERE workspaceId = :workspaceId")
    fun getExpenses(workspaceId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    // Employees
    @Query("SELECT * FROM employees WHERE workspaceId = :workspaceId")
    fun getEmployees(workspaceId: String): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeeById(id: String)

    @Query("UPDATE tasks SET assignedTo = :newName WHERE assignedTo = :oldName AND workspaceId = :workspaceId")
    suspend fun updateTaskAssignee(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE tasks SET completedBy = :newName WHERE completedBy = :oldName AND workspaceId = :workspaceId")
    suspend fun updateTaskCompletedBy(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE tasks SET assignedBy = :newName WHERE assignedBy = :oldName AND workspaceId = :workspaceId")
    suspend fun updateTaskAssignedBy(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE attendance SET employeeName = :newName WHERE employeeId = :employeeId OR employeeName = :oldName")
    suspend fun updateAttendanceEmployeeName(employeeId: String, newName: String, oldName: String)

    @Query("UPDATE activity_logs SET userName = :newName WHERE userName = :oldName")
    suspend fun updateActivityLogUserName(oldName: String, newName: String)

    @Query("UPDATE leaves SET employeeName = :newName WHERE employeeName = :oldName")
    suspend fun updateLeaveEmployeeName(oldName: String, newName: String)

    @Query("UPDATE expenses SET employeeName = :newName WHERE employeeName = :oldName")
    suspend fun updateExpenseEmployeeName(oldName: String, newName: String)

    @Query("UPDATE shoots SET leadPhotographer = :newName WHERE leadPhotographer = :oldName AND workspaceId = :workspaceId")
    suspend fun updateShootLeadPhotographer(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE shoots SET videographer = :newName WHERE videographer = :oldName AND workspaceId = :workspaceId")
    suspend fun updateShootVideographer(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE photo_edits SET editorName = :newName WHERE editorName = :oldName AND workspaceId = :workspaceId")
    suspend fun updatePhotoEditEditor(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE video_edits SET editorName = :newName WHERE editorName = :oldName AND workspaceId = :workspaceId")
    suspend fun updateVideoEditEditor(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE album_projects SET designerName = :newName WHERE designerName = :oldName AND workspaceId = :workspaceId")
    suspend fun updateAlbumDesigner(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE leads SET assignedStaff = :newName WHERE assignedStaff = :oldName AND workspaceId = :workspaceId")
    suspend fun updateLeadAssignedStaff(oldName: String, newName: String, workspaceId: String)

    @Query("UPDATE users SET name = :newName WHERE name = :oldName")
    suspend fun updateUserName(oldName: String, newName: String)

    // Attendance
    @Query("SELECT * FROM attendance WHERE workspaceId = :workspaceId ORDER BY date DESC")
    fun getAttendance(workspaceId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workspaceId = :workspaceId AND employeeId = :employeeId ORDER BY date DESC")
    fun getEmployeeAttendance(workspaceId: String, employeeId: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendance(id: String)

    @Query("DELETE FROM attendance WHERE workspaceId = :workspaceId AND employeeId = :employeeId AND date = :date")
    suspend fun deleteAttendanceByEmployeeDate(workspaceId: String, employeeId: String, date: String)

    // Payroll Records
    @Query("SELECT * FROM payroll_records WHERE workspaceId = :workspaceId ORDER BY monthYear DESC, employeeName ASC")
    fun getPayrollRecords(workspaceId: String): Flow<List<PayrollRecordEntity>>

    @Query("SELECT * FROM payroll_records WHERE workspaceId = :workspaceId AND monthYear = :monthYear")
    fun getPayrollForMonth(workspaceId: String, monthYear: String): Flow<List<PayrollRecordEntity>>

    @Query("SELECT * FROM payroll_records WHERE workspaceId = :workspaceId AND employeeId = :employeeId ORDER BY monthYear DESC")
    fun getEmployeePayrollHistory(workspaceId: String, employeeId: String): Flow<List<PayrollRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollRecord(payroll: PayrollRecordEntity)

    @Update
    suspend fun updatePayrollRecord(payroll: PayrollRecordEntity)

    @Query("DELETE FROM payroll_records WHERE id = :id")
    suspend fun deletePayrollRecord(id: String)

    @Query("UPDATE payroll_records SET status = :status, approvedBy = :approvedBy, approvalDate = :approvalDate WHERE id = :id")
    suspend fun approvePayrollRecord(id: String, status: String, approvedBy: String, approvalDate: String)

    // Leaves
    @Query("SELECT * FROM leaves WHERE workspaceId = :workspaceId")
    fun getLeaves(workspaceId: String): Flow<List<LeaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveEntity)

    @Update
    suspend fun updateLeave(leave: LeaveEntity)

    // Tasks
    @Query("SELECT * FROM tasks WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun getTasks(workspaceId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE workspaceId = :workspaceId ORDER BY id DESC")
    fun getNotifications(workspaceId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE workspaceId = :workspaceId")
    suspend fun markAllNotificationsAsRead(workspaceId: String)

    // Activity Logs
    @Query("SELECT * FROM activity_logs WHERE workspaceId = :workspaceId ORDER BY timeMillis DESC")
    fun getActivityLogs(workspaceId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    // Subscriptions
    @Query("SELECT * FROM subscriptions WHERE workspaceId = :workspaceId LIMIT 1")
    fun getSubscription(workspaceId: String): Flow<SubscriptionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    // Bulk inserts for seeding demo data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLeads(leads: List<LeadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllClients(clients: List<ClientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuotations(quotations: List<QuotationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBookings(bookings: List<BookingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllShoots(shoots: List<ShootEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPhotoEdits(edits: List<PhotoEditEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVideoEdits(edits: List<VideoEditEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAlbums(albums: List<AlbumProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDeliveries(deliveries: List<DeliveryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<PaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEmployees(employees: List<EmployeeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(attendance: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLeaves(leaves: List<LeaveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotifications(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllActivityLogs(logs: List<ActivityLogEntity>)

    // Storage Drives
    @Query("SELECT * FROM storage_drives WHERE workspaceId = :workspaceId ORDER BY driveLabel ASC")
    fun getStorageDrives(workspaceId: String): Flow<List<StorageDriveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageDrive(drive: StorageDriveEntity)

    @Update
    suspend fun updateStorageDrive(drive: StorageDriveEntity)

    @Delete
    suspend fun deleteStorageDrive(drive: StorageDriveEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStorageDrives(drives: List<StorageDriveEntity>)

    // Project Files
    @Query("SELECT * FROM project_files WHERE workspaceId = :workspaceId ORDER BY clientName ASC")
    fun getProjectFiles(workspaceId: String): Flow<List<ProjectFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectFile(file: ProjectFileEntity)

    @Update
    suspend fun updateProjectFile(file: ProjectFileEntity)

    @Delete
    suspend fun deleteProjectFile(file: ProjectFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProjectFiles(files: List<ProjectFileEntity>)

    // Demo Data Purge Queries (Preserving genuine Shakir Tamboli admin records)
    @Query("DELETE FROM leads WHERE id IN ('lead_01', 'lead_02', 'lead_03', 'lead_04')")
    suspend fun purgeDemoLeads()

    @Query("DELETE FROM clients WHERE id IN ('client_01', 'client_02', 'client_03')")
    suspend fun purgeDemoClients()

    @Query("DELETE FROM quotations WHERE id IN ('quot_01', 'quot_02', 'quot_03')")
    suspend fun purgeDemoQuotations()

    @Query("DELETE FROM bookings WHERE id IN ('book_01', 'book_02', 'book_03')")
    suspend fun purgeDemoBookings()

    @Query("DELETE FROM shoots WHERE id IN ('shoot_01', 'shoot_02')")
    suspend fun purgeDemoShoots()

    @Query("DELETE FROM photo_edits WHERE id IN ('pe_01', 'pe_02', 'pe_03', 'pedit_01', 'pedit_02')")
    suspend fun purgeDemoPhotoEdits()

    @Query("DELETE FROM video_edits WHERE id IN ('ve_01', 've_02', 'vedit_01', 'vedit_02')")
    suspend fun purgeDemoVideoEdits()

    @Query("DELETE FROM album_projects WHERE id IN ('alb_01', 'alb_02')")
    suspend fun purgeDemoAlbumProjects()

    @Query("DELETE FROM deliveries WHERE id IN ('del_01', 'del_02')")
    suspend fun purgeDemoDeliveries()

    @Query("DELETE FROM payments WHERE id IN ('pay_01', 'pay_02', 'pay_03', 'pay_04')")
    suspend fun purgeDemoPayments()

    @Query("DELETE FROM expenses WHERE id IN ('exp_01', 'exp_02', 'exp_03', 'exp_04', 'exp_05')")
    suspend fun purgeDemoExpenses()

    @Query("DELETE FROM employees WHERE id IN ('emp_rahul', 'emp_priya')")
    suspend fun purgeDemoEmployees()

    @Query("DELETE FROM attendance WHERE id IN ('att_01', 'att_02', 'att_03') OR employeeId IN ('emp_rahul', 'emp_priya')")
    suspend fun purgeDemoAttendance()

    @Query("DELETE FROM leaves WHERE id IN ('leave_01') OR employeeName IN ('Rahul Verma', 'Priya Sharma')")
    suspend fun purgeDemoLeaves()

    @Query("DELETE FROM tasks WHERE id IN ('task_01', 'task_02', 'task_03', 'task_04')")
    suspend fun purgeDemoTasks()

    @Query("DELETE FROM notifications WHERE id IN ('notif_01', 'notif_02', 'notif_03', 'notif_04')")
    suspend fun purgeDemoNotifications()

    @Query("DELETE FROM activity_logs WHERE id IN ('log_01', 'log_02', 'log_03', 'log_04')")
    suspend fun purgeDemoActivityLogs()

    @Query("DELETE FROM storage_drives WHERE id IN ('drv_01', 'drv_02', 'drv_03', 'drv_04', 'drive_01', 'drive_02', 'drive_03', 'drive_04')")
    suspend fun purgeDemoStorageDrives()

    @Query("DELETE FROM project_files WHERE id IN ('pf_01', 'pf_02', 'pf_03', 'pfile_01', 'pfile_02', 'pfile_03')")
    suspend fun purgeDemoProjectFiles()

    // Sync Snapshot Query Methods
    @Query("SELECT * FROM workspaces WHERE id = :workspaceId LIMIT 1")
    suspend fun getWorkspaceOnce(workspaceId: String): WorkspaceEntity?

    @Query("SELECT * FROM employees WHERE workspaceId = :workspaceId")
    suspend fun getEmployeesOnce(workspaceId: String): List<EmployeeEntity>

    @Query("SELECT * FROM attendance WHERE workspaceId = :workspaceId")
    suspend fun getAttendanceOnce(workspaceId: String): List<AttendanceEntity>

    @Query("SELECT * FROM payroll_records WHERE workspaceId = :workspaceId")
    suspend fun getPayrollRecordsOnce(workspaceId: String): List<PayrollRecordEntity>

    @Query("SELECT * FROM clients WHERE workspaceId = :workspaceId")
    suspend fun getClientsOnce(workspaceId: String): List<ClientEntity>

    @Query("SELECT * FROM leads WHERE workspaceId = :workspaceId")
    suspend fun getLeadsOnce(workspaceId: String): List<LeadEntity>

    @Query("SELECT * FROM quotations WHERE workspaceId = :workspaceId")
    suspend fun getQuotationsOnce(workspaceId: String): List<QuotationEntity>

    @Query("SELECT * FROM bookings WHERE workspaceId = :workspaceId")
    suspend fun getBookingsOnce(workspaceId: String): List<BookingEntity>

    @Query("SELECT * FROM shoots WHERE workspaceId = :workspaceId")
    suspend fun getShootsOnce(workspaceId: String): List<ShootEntity>

    @Query("SELECT * FROM photo_edits WHERE workspaceId = :workspaceId")
    suspend fun getPhotoEditsOnce(workspaceId: String): List<PhotoEditEntity>

    @Query("SELECT * FROM video_edits WHERE workspaceId = :workspaceId")
    suspend fun getVideoEditsOnce(workspaceId: String): List<VideoEditEntity>

    @Query("SELECT * FROM album_projects WHERE workspaceId = :workspaceId")
    suspend fun getAlbumProjectsOnce(workspaceId: String): List<AlbumProjectEntity>

    @Query("SELECT * FROM deliveries WHERE workspaceId = :workspaceId")
    suspend fun getDeliveriesOnce(workspaceId: String): List<DeliveryEntity>

    @Query("SELECT * FROM payments WHERE workspaceId = :workspaceId")
    suspend fun getPaymentsOnce(workspaceId: String): List<PaymentEntity>

    @Query("SELECT * FROM expenses WHERE workspaceId = :workspaceId")
    suspend fun getExpensesOnce(workspaceId: String): List<ExpenseEntity>

    @Query("SELECT * FROM tasks WHERE workspaceId = :workspaceId")
    suspend fun getTasksOnce(workspaceId: String): List<TaskEntity>

    @Query("SELECT * FROM leaves WHERE workspaceId = :workspaceId")
    suspend fun getLeavesOnce(workspaceId: String): List<LeaveEntity>

    @Query("SELECT * FROM subscriptions WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getSubscriptionOnce(workspaceId: String): SubscriptionEntity?
}

