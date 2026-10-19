package com.example.data.payroll

import com.example.data.model.AttendanceEntity
import com.example.data.model.LeaveEntity
import java.util.Locale

/**
 * Standard Studio Payroll & Salary Calculation Engine
 * 
 * Provides mathematically accurate, transparent, and policy-compliant payroll
 * calculations for App Boss studios. Supports both SCHEDULED_WORKING_DAYS (primary)
 * and CALENDAR_DAYS calculation bases with standard 9.0 hours per scheduled working day.
 */
object PayrollCalculationEngine {

    const val BASIS_SCHEDULED_WORKING_DAYS = "SCHEDULED_WORKING_DAYS"
    const val BASIS_CALENDAR_DAYS = "CALENDAR_DAYS"
    const val STANDARD_DAILY_HOURS = 9.0
    const val HALF_DAY_HOURS = 4.5
    const val DEFAULT_OVERTIME_MULTIPLIER = 1.5

    data class PayrollInput(
        val monthlyBaseSalary: Double,
        val monthYear: String,
        val calculationBasis: String = BASIS_SCHEDULED_WORKING_DAYS,
        val scheduledWorkingDays: Int = 26,
        val totalCalendarDays: Int = 30,
        val standardDailyHours: Double = STANDARD_DAILY_HOURS,
        val attendanceRecords: List<AttendanceEntity> = emptyList(),
        val approvedLeaves: List<LeaveEntity> = emptyList(),
        val overtimeHours: Double = 0.0,
        val overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER,
        val bonuses: Double = 0.0,
        val deductions: Double = 0.0,
        val deductionReason: String = "",
        val manualNotes: String = ""
    )

    data class PayrollResult(
        val baseMonthlySalary: Double,
        val calculationBasis: String,
        val scheduledWorkingDays: Int,
        val totalCalendarDays: Int,
        val standardDailyHours: Double,
        // Attendance Counts
        val presentDays: Double,
        val halfDays: Int,
        val absentDays: Int,
        val paidLeaveDays: Int,
        val unpaidLeaveDays: Int,
        val weeklyOffDays: Int,
        val holidayDays: Int,
        // Hours & Rates
        val actualWorkedHours: Double,
        val approvedPayableHours: Double,
        val payableDays: Double,
        val dailyRate: Double,
        val hourlyRate: Double,
        // Financial Breakdown
        val regularEarnedSalary: Double,
        val overtimeHours: Double,
        val overtimeRate: Double,
        val overtimeAmount: Double,
        val bonuses: Double,
        val grossPayable: Double,
        val deductions: Double,
        val deductionReason: String,
        val netSalary: Double,
        // Audit & Explanatory Breakdown
        val transparentBreakdown: String
    )

    /**
     * Executes the payroll calculation according to configured rules
     */
    fun calculate(input: PayrollInput): PayrollResult {
        val safeBaseSalary = maxOf(0.0, input.monthlyBaseSalary)
        val safeStandardHours = if (input.standardDailyHours > 0.0) input.standardDailyHours else STANDARD_DAILY_HOURS
        val safeCalendarDays = maxOf(1, input.totalCalendarDays)
        val safeScheduledDays = maxOf(1, input.scheduledWorkingDays)

        // 1. Calculate Daily and Hourly Rates
        val isWorkingDaysBasis = input.calculationBasis == BASIS_SCHEDULED_WORKING_DAYS
        val dailyRate = if (isWorkingDaysBasis) {
            safeBaseSalary / safeScheduledDays.toDouble()
        } else {
            safeBaseSalary / safeCalendarDays.toDouble()
        }

        val hourlyRate = if (safeStandardHours > 0.0) dailyRate / safeStandardHours else 0.0

        // 2. Aggregate Attendance Records for the target payroll month
        val relevantAttendance = input.attendanceRecords.filter { att ->
            isDateInMonth(att.date, input.monthYear)
        }

        // Distinct dates to prevent double-counting
        val distinctAttendanceByDate = relevantAttendance.associateBy { it.date }

        var presentCount = 0.0
        var halfDayCount = 0
        var absentCount = 0
        var paidLeaveCount = 0
        var weeklyOffCount = 0
        var holidayCount = 0
        var actualWorkedHoursSum = 0.0
        var payableHoursSum = 0.0

        for ((_, record) in distinctAttendanceByDate) {
            when (record.status) {
                "Present" -> {
                    presentCount += 1.0
                    val hrs = if (record.workingHours > 0.0) record.workingHours else safeStandardHours
                    actualWorkedHoursSum += hrs
                    payableHoursSum += hrs
                }
                "Half-Day", "Half Day" -> {
                    halfDayCount += 1
                    val hrs = if (record.workingHours > 0.0) record.workingHours else HALF_DAY_HOURS
                    actualWorkedHoursSum += hrs
                    payableHoursSum += hrs
                }
                "Leave" -> {
                    paidLeaveCount += 1
                    payableHoursSum += safeStandardHours
                }
                "Weekly Off" -> {
                    weeklyOffCount += 1
                    payableHoursSum += safeStandardHours
                }
                "Holiday" -> {
                    holidayCount += 1
                    payableHoursSum += safeStandardHours
                }
                "Absent" -> {
                    absentCount += 1
                    // Contributes 0.0 payable hours
                }
                else -> {
                    // Custom / Unmarked records contribute according to recorded hours
                    if (record.workingHours > 0.0) {
                        actualWorkedHoursSum += record.workingHours
                        payableHoursSum += record.workingHours
                    }
                }
            }
        }

        // 3. Reconcile standalone approved leaves not already marked in attendance
        for (leave in input.approvedLeaves) {
            if (leave.status.equals("Approved", ignoreCase = true)) {
                // If leave date is in month and not already counted
                if (isDateInMonth(leave.fromDate, input.monthYear) && !distinctAttendanceByDate.containsKey(leave.fromDate)) {
                    paidLeaveCount += 1
                    payableHoursSum += safeStandardHours
                }
            }
        }

        // 4. Calculate Effective Payable Days
        val payableDays = presentCount + (halfDayCount * 0.5) + paidLeaveCount + weeklyOffCount + holidayCount

        // 5. Regular Earned Salary
        val regularEarnedSalary = if (isWorkingDaysBasis) {
            hourlyRate * payableHoursSum
        } else {
            payableDays * dailyRate
        }

        // 6. Overtime Calculation
        val safeOvertimeHours = maxOf(0.0, input.overtimeHours)
        val overtimeMultiplier = if (input.overtimeMultiplier > 0.0) input.overtimeMultiplier else DEFAULT_OVERTIME_MULTIPLIER
        val overtimeRate = hourlyRate * overtimeMultiplier
        val overtimeAmount = safeOvertimeHours * overtimeRate

        // 7. Bonuses, Gross, Deductions & Net Salary
        val safeBonuses = maxOf(0.0, input.bonuses)
        val safeDeductions = maxOf(0.0, input.deductions)
        val grossPayable = regularEarnedSalary + overtimeAmount + safeBonuses
        val netSalary = maxOf(0.0, grossPayable - safeDeductions)

        // 8. Generate Audit Explanation Breakdown String
        val basisLabel = if (isWorkingDaysBasis) {
            "Scheduled Working Days ($safeScheduledDays days)"
        } else {
            "Calendar Days ($safeCalendarDays days)"
        }

        val breakdown = buildString {
            appendLine("=== SALARY CALCULATION BREAKDOWN (${input.monthYear.uppercase(Locale.getDefault())}) ===")
            appendLine("Policy Basis: $basisLabel • Standard: ${safeStandardHours.toInt()}h/day")
            appendLine(String.format(Locale.getDefault(), "Monthly Base Salary: ₹%,.2f", safeBaseSalary))
            appendLine(String.format(Locale.getDefault(), "Daily Rate: ₹%,.2f/day (Base ÷ %s)", dailyRate, if (isWorkingDaysBasis) "$safeScheduledDays days" else "$safeCalendarDays days"))
            appendLine()
            appendLine("--- ATTENDANCE SUMMARY ---")
            appendLine("• Present Days: ${String.format(Locale.getDefault(), "%.1f", presentCount)} days")
            appendLine("• Half Days: $halfDayCount (contributing ${halfDayCount * 0.5} payable days)")
            appendLine("• Approved Paid Leaves: $paidLeaveCount days")
            appendLine("• Weekly Offs (Paid): $weeklyOffCount days")
            appendLine("• Official Holidays (Paid): $holidayCount days")
            appendLine("• Absent / Unpaid Days: $absentCount days")
            appendLine(String.format(Locale.getDefault(), "Actual Worked Hours: %.1f hrs", actualWorkedHoursSum))
            appendLine(String.format(Locale.getDefault(), "Total Approved Payable Hours: %.1f hrs", payableHoursSum))
            appendLine(String.format(Locale.getDefault(), "Effective Payable Days: %.1f days", payableDays))
            appendLine()
            appendLine("--- EARNINGS & SETTLEMENT ---")
            appendLine(String.format(Locale.getDefault(), "• Regular Earned Salary: ₹%,.2f", regularEarnedSalary))
            if (safeOvertimeHours > 0.0) {
                appendLine(String.format(Locale.getDefault(), "• Overtime (%.1fh @ %.1fx): +₹%,.2f", safeOvertimeHours, overtimeMultiplier, overtimeAmount))
            }
            if (safeBonuses > 0.0) {
                appendLine(String.format(Locale.getDefault(), "• Performance Bonus / Incentives: +₹%,.2f", safeBonuses))
            }
            appendLine(String.format(Locale.getDefault(), "Gross Earnings: ₹%,.2f", grossPayable))
            if (safeDeductions > 0.0) {
                val reasonText = if (input.deductionReason.isNotBlank()) " (${input.deductionReason})" else ""
                appendLine(String.format(Locale.getDefault(), "• Authorized Deductions%s: -₹%,.2f", reasonText, safeDeductions))
            }
            appendLine("────────────────────────────────────────────")
            appendLine(String.format(Locale.getDefault(), "NET PAYABLE SALARY: ₹%,.2f", netSalary))
            if (input.manualNotes.isNotBlank()) {
                appendLine("Notes: ${input.manualNotes}")
            }
        }

        return PayrollResult(
            baseMonthlySalary = safeBaseSalary,
            calculationBasis = input.calculationBasis,
            scheduledWorkingDays = safeScheduledDays,
            totalCalendarDays = safeCalendarDays,
            standardDailyHours = safeStandardHours,
            presentDays = presentCount,
            halfDays = halfDayCount,
            absentDays = absentCount,
            paidLeaveDays = paidLeaveCount,
            unpaidLeaveDays = absentCount,
            weeklyOffDays = weeklyOffCount,
            holidayDays = holidayCount,
            actualWorkedHours = actualWorkedHoursSum,
            approvedPayableHours = payableHoursSum,
            payableDays = payableDays,
            dailyRate = dailyRate,
            hourlyRate = hourlyRate,
            regularEarnedSalary = regularEarnedSalary,
            overtimeHours = safeOvertimeHours,
            overtimeRate = overtimeRate,
            overtimeAmount = overtimeAmount,
            bonuses = safeBonuses,
            grossPayable = grossPayable,
            deductions = safeDeductions,
            deductionReason = input.deductionReason,
            netSalary = netSalary,
            transparentBreakdown = breakdown
        )
    }

    private fun isDateInMonth(dateStr: String, monthYearStr: String): Boolean {
        if (dateStr.isBlank() || monthYearStr.isBlank()) return false
        val cleanDate = dateStr.trim().lowercase(Locale.getDefault())
        val cleanMonth = monthYearStr.trim().lowercase(Locale.getDefault())

        // Check full match, e.g. "Oct 2026", "October 2026"
        if (cleanDate.contains(cleanMonth)) return true

        // Check month abbreviation (first 3 chars, e.g. "oct")
        val monthAbbr = cleanMonth.take(3)
        val yearPart = cleanMonth.takeLast(4)
        if (cleanDate.contains(monthAbbr) && (yearPart.toIntOrNull() == null || cleanDate.contains(yearPart))) {
            return true
        }

        // Check numerical month mapping for ISO format (e.g. 2026-10-01)
        val monthNumber = when (monthAbbr) {
            "jan" -> "01"
            "feb" -> "02"
            "mar" -> "03"
            "apr" -> "04"
            "may" -> "05"
            "jun" -> "06"
            "jul" -> "07"
            "aug" -> "08"
            "sep" -> "09"
            "oct" -> "10"
            "nov" -> "11"
            "dec" -> "12"
            else -> ""
        }
        if (monthNumber.isNotBlank() && yearPart.toIntOrNull() != null) {
            if (cleanDate.contains("$yearPart-$monthNumber") || cleanDate.contains("$monthNumber-$yearPart")) {
                return true
            }
        }

        // Check YYYY-MM format
        if (cleanMonth.contains("-")) {
            val parts = cleanMonth.split("-")
            if (parts.size >= 2 && cleanDate.startsWith("${parts[0]}-${parts[1]}")) return true
        }

        return false
    }
}
