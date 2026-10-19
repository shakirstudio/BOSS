package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val businessType: String,
    val city: String,
    val phone: String,
    val email: String,
    val address: String,
    val gstNumber: String,
    val tagline: String = "Manage. Track. Grow.",
    val planId: String = "STUDIO",
    val isTrialActive: Boolean = true,
    val trialDaysRemaining: Int = 7,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val email: String,
    val mobile: String,
    val role: String, // Owner/Admin, Manager, HOD, Photographer, Videographer, Photo Editor, Video Editor, Album Designer, Accountant, Staff
    val avatarUrl: String = "",
    val isLoggedIn: Boolean = true,
    val loginMethod: String = "GOOGLE" // GOOGLE or OTP
)

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val mobile: String,
    val email: String,
    val eventType: String,
    val eventDate: String,
    val venue: String,
    val source: String, // Instagram, Referral, Website, Walk-in, Google
    val estimatedBudget: Double,
    val assignedStaff: String,
    val status: String, // New, Contacted, Follow-up, Quotation Sent, Negotiation, Won, Lost
    val notes: String,
    val followUpDate: String,
    val isConverted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val mobile: String,
    val email: String,
    val address: String,
    val city: String,
    val eventType: String,
    val totalBookingValue: Double = 0.0,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val notes: String = "",
    val timelineStage: String = "Booking", // Lead, Contacted, Quotation, Booking, Event, Editing, Album, Delivery, Completed
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val quotationNumber: String,
    val clientId: String,
    val clientName: String,
    val contact: String,
    val eventDate: String,
    val eventLocation: String,
    val services: String, // Comma separated: Photography, 4K Cinematic Video, Drone, Pre-Wedding
    val packageDetails: String,
    val subtotal: Double,
    val discount: Double,
    val taxRate: Double = 18.0, // 18% GST
    val taxAmount: Double,
    val totalAmount: Double,
    val advanceRequired: Double,
    val balanceAmount: Double,
    val status: String, // Draft, Sent, Viewed, Accepted, Rejected, Expired
    val terms: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingNumber: String,
    val clientId: String,
    val clientName: String,
    val eventType: String,
    val eventDate: String,
    val eventTime: String = "09:00 AM",
    val venue: String,
    val packageName: String,
    val totalAmount: Double,
    val advancePaid: Double,
    val balanceAmount: Double,
    val assignedTeam: String,
    val status: String, // Confirmed, Upcoming, In Progress, Completed, Cancelled
    val notes: String = "",
    val hasConflict: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shoots")
data class ShootEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingId: String,
    val clientName: String,
    val eventType: String,
    val shootDate: String,
    val callTime: String,
    val venue: String,
    val leadPhotographer: String,
    val videographer: String,
    val equipmentList: String, // Camera bodies, lenses, gimbal, lights, mic
    val status: String, // Planned, Assigned, Shooting, Data Backup, Completed
    val dataBackupDone: Boolean = false,
    val specialInstructions: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "photo_edits")
data class PhotoEditEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingId: String,
    val clientName: String,
    val projectName: String,
    val editorName: String,
    val totalPhotos: Int,
    val editedPhotos: Int,
    val deadline: String,
    val priority: String, // Low, Medium, High, Urgent
    val status: String, // Shoot Completed, Data Backup, Selection, In Progress, Quality Check, Client Review, Approved, Delivered
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_edits")
data class VideoEditEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingId: String,
    val clientName: String,
    val videoType: String, // Teaser, Cinematic Highlights, Full Traditional, Reels
    val duration: String, // 3 mins, 25 mins, 60 secs
    val editorName: String,
    val deadline: String,
    val revisionCount: Int = 0,
    val status: String, // Footage Received, Backup, Rough Cut, Review, Revision, Final Edit, Approved, Delivered
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "album_projects")
data class AlbumProjectEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingId: String,
    val clientName: String,
    val albumSize: String, // 12x36, 12x30, 14x40, Custom Velvet
    val numberOfPages: Int,
    val designerName: String,
    val deadline: String,
    val revisionCount: Int = 0,
    val status: String, // Photos Selected, Assigned, Designing, Internal Review, Client Review, Approved, Printing, Delivered
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingId: String,
    val clientName: String,
    val deliveryType: String, // Digital Photos, Digital Videos, Hard Drive, Pen Drive, Album Box, Online Gallery
    val recipient: String,
    val trackingOrLink: String = "",
    val deliveryDate: String,
    val status: String, // Pending, Ready, Delivered
    val notes: String = "",
    val confirmedByClient: Boolean = false
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val bookingId: String,
    val clientName: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String, // Cash, UPI, Bank Transfer, Card, Razorpay
    val transactionRef: String,
    val paymentType: String, // Advance, Second Installment, Final Settlement
    val status: String, // Paid, Pending, Overdue, Partially Paid
    val notes: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val category: String, // Travel, Petrol, Equipment, Maintenance, Salary, Food, Printing, Marketing, Software, Office, Other
    val amount: Double,
    val date: String,
    val description: String,
    val employeeName: String,
    val projectName: String = "",
    val paymentMethod: String,
    val receiptRef: String = ""
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val employeeCode: String,
    val name: String,
    val mobile: String,
    val email: String,
    val department: String, // Photography, Videography, Editing, Album Designing, Management, Accounts
    val designation: String,
    val role: String,
    val joiningDate: String,
    val basicSalary: Double,
    val status: String = "Active", // Active, On Leave, Inactive
    val emergencyContact: String = "",
    val responsibilities: String = "Shoots, Editing & Studio Operations",
    val assignedPermissions: String = "ALL",
    val avatarUrl: String = "",
    val loginUsername: String = "",
    val loginPassword: String = ""
)

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["workspaceId", "employeeId", "date"], unique = true)]
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val status: String, // Present, Absent, Half-Day, Leave, Weekly Off, Holiday
    val workingHours: Double = 9.0, // Standard 9 hours per working day
    val checkInTime: String = "09:30 AM",
    val checkOutTime: String = "06:30 PM",
    val notes: String = "",
    val updatedBy: String = "Admin",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payroll_records",
    indices = [Index(value = ["workspaceId", "employeeId", "monthYear"], unique = true)]
)
data class PayrollRecordEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val employeeId: String,
    val employeeName: String,
    val monthYear: String, // e.g. "Oct 2026"
    val baseMonthlySalary: Double,
    val totalCalendarDays: Int = 31,
    val scheduledWorkingDays: Int = 26,
    val calculationBasis: String = "CALENDAR_DAYS", // "CALENDAR_DAYS" or "SCHEDULED_WORKING_DAYS"
    val standardDailyHours: Double = 9.0,
    val presentDays: Double = 0.0,
    val halfDays: Int = 0,
    val paidLeaveDays: Int = 0,
    val unpaidLeaveDays: Int = 0,
    val weeklyOffDays: Int = 0,
    val holidayDays: Int = 0,
    val payableDays: Double = 0.0,
    val totalWorkingHours: Double = 0.0,
    val dailyRate: Double = 0.0,
    val grossPayable: Double = 0.0,
    val deductions: Double = 0.0,
    val deductionReason: String = "",
    val overtimeHours: Double = 0.0,
    val overtimeAmount: Double = 0.0,
    val netSalary: Double = 0.0,
    val status: String = "Draft", // Draft, Approved, Paid
    val approvedBy: String = "",
    val approvalDate: String = "",
    val paymentDate: String = "",
    val calculationNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaves")
data class LeaveEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val employeeName: String,
    val leaveType: String, // Casual, Sick, Annual, Emergency
    val fromDate: String,
    val toDate: String,
    val reason: String,
    val status: String // Pending, Approved, Rejected
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val description: String,
    val assignedTo: String,
    val projectName: String,
    val priority: String, // Low, Medium, High, Urgent
    val deadline: String,
    val status: String, // Pending, In Progress, Completed, Overdue
    val isOverdue: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val assignedBy: String = "Admin",
    val dueTime: String = "06:00 PM",
    val completionDate: String = "",
    val completionTime: String = "",
    val completedBy: String = "",
    val remarks: String = ""
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val message: String,
    val priority: String, // INFO, WARNING, URGENT
    val timestamp: String,
    val isRead: Boolean = false,
    val category: String // Booking, Payment, Shoot, Editing, Leave, Lead
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val userName: String,
    val action: String,
    val module: String = "System",
    val details: String = "",
    val timestamp: String,
    val timeMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val planId: String, // FREE, PRO, STUDIO, BUSINESS
    val planName: String,
    val billingCycle: String, // MONTHLY, YEARLY
    val amount: Double,
    val status: String, // Active, Trial, Expired, Cancelled
    val startDate: String,
    val renewalDate: String,
    val razorpayOrderId: String = "",
    val gstRate: Double = 18.0
)

@Entity(tableName = "storage_drives")
data class StorageDriveEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val driveLabel: String, // "HDD-RED-01", "SANDISK-SSD-02", "NAS-STUDIO-8TB"
    val driveModel: String, // "WD My Passport 4TB", "SanDisk Extreme 2TB", "Synology NAS"
    val totalCapacityGb: Int,
    val freeSpaceGb: Int,
    val driveType: String, // External HDD, Portable SSD, Studio NAS, Cloud Storage
    val location: String, // Shelf A - Locker 1, Editing Bay 2, Offsite Safe
    val assignedClients: String, // Rahul & Priya Wedding, Sunburn 2026
    val status: String = "Active", // Active, Full / Archived, In Transit, Faulty
    val lastBackupDate: String = "Today",
    val notes: String = ""
)

@Entity(tableName = "project_files")
data class ProjectFileEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val clientName: String,
    val eventType: String,
    val projectFolder: String, // "2026/Wedding_Rahul_Pooja"
    val rawDriveLabel: String, // "HDD-RED-01"
    val backupDriveLabel: String, // "NAS-STUDIO-8TB"
    val googleDriveRawLink: String = "", // "https://drive.google.com/..."
    val clientGalleryLink: String = "", // "https://gallery.boss-studios.in/..."
    val finalDeliverablesLink: String = "", // "https://drive.google.com/..."
    val canveraPrintPdfLink: String = "", // "https://canvera.com/..."
    val isDualBackupVerified: Boolean = true,
    val rawFootageGb: Double = 250.0,
    val status: String = "RAW Ingested" // RAW Ingested, In Editing, Ready for Delivery, Delivered & Archived
)
