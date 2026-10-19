package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BmsDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkspaceEntity::class,
        UserEntity::class,
        LeadEntity::class,
        ClientEntity::class,
        QuotationEntity::class,
        BookingEntity::class,
        ShootEntity::class,
        PhotoEditEntity::class,
        VideoEditEntity::class,
        AlbumProjectEntity::class,
        DeliveryEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        EmployeeEntity::class,
        AttendanceEntity::class,
        LeaveEntity::class,
        TaskEntity::class,
        NotificationEntity::class,
        ActivityLogEntity::class,
        SubscriptionEntity::class,
        StorageDriveEntity::class,
        ProjectFileEntity::class,
        PayrollRecordEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BmsDatabase : RoomDatabase() {
    abstract fun bmsDao(): BmsDao

    companion object {
        @Volatile
        private var INSTANCE: BmsDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 schema adjustments (non-destructive)
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 to 3 storage drives and file tracking
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 to 4 activity logs and notifications
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create payroll_records table if not exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payroll_records` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `workspaceId` TEXT NOT NULL,
                        `employeeId` TEXT NOT NULL,
                        `employeeName` TEXT NOT NULL,
                        `monthYear` TEXT NOT NULL,
                        `baseMonthlySalary` REAL NOT NULL,
                        `totalCalendarDays` INTEGER NOT NULL,
                        `scheduledWorkingDays` INTEGER NOT NULL,
                        `calculationBasis` TEXT NOT NULL,
                        `standardDailyHours` REAL NOT NULL,
                        `presentDays` REAL NOT NULL,
                        `halfDays` INTEGER NOT NULL,
                        `paidLeaveDays` INTEGER NOT NULL,
                        `unpaidLeaveDays` INTEGER NOT NULL,
                        `weeklyOffDays` INTEGER NOT NULL,
                        `holidayDays` INTEGER NOT NULL,
                        `payableDays` REAL NOT NULL,
                        `totalWorkingHours` REAL NOT NULL,
                        `dailyRate` REAL NOT NULL,
                        `grossPayable` REAL NOT NULL,
                        `deductions` REAL NOT NULL,
                        `deductionReason` TEXT NOT NULL,
                        `overtimeHours` REAL NOT NULL,
                        `overtimeAmount` REAL NOT NULL,
                        `netSalary` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `approvedBy` TEXT NOT NULL,
                        `approvalDate` TEXT NOT NULL,
                        `paymentDate` TEXT NOT NULL,
                        `calculationNotes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Deduplicate attendance entries if any pre-exist to prevent SQLiteConstraintException on unique index
                try {
                    db.execSQL("""
                        DELETE FROM `attendance` WHERE rowid NOT IN (
                            SELECT MIN(rowid) FROM `attendance` GROUP BY `workspaceId`, `employeeId`, `date`
                        )
                    """.trimIndent())
                } catch (_: Exception) {}

                // Create unique index for payroll records by employee and month
                try {
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payroll_records_workspaceId_employeeId_monthYear` ON `payroll_records` (`workspaceId`, `employeeId`, `monthYear`)")
                } catch (_: Exception) {}

                // Create unique index for attendance by employee and date
                try {
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_workspaceId_employeeId_date` ON `attendance` (`workspaceId`, `employeeId`, `date`)")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): BmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BmsDatabase::class.java,
                    "bms_database.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(BmsDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class BmsDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        SeedData.populateInitialData(database.bmsDao())
                    } catch (e: Exception) {
                        android.util.Log.w("BmsDatabase", "Notice: initial database seed in callback: ${e.message}")
                    }
                }
            }
        }
    }
}
