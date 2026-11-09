package com.example.agent

import com.example.model.AttendanceRecoveryPlan
import com.example.model.RiskLevel
import com.example.model.SubjectCourse
import kotlin.math.ceil

/**
 * Advisor Agent as formulated in Section III-D of the research paper:
 * Evaluates attendance % (days present / days held) and Risk Level to generate alerts and action plans.
 * Specifically handles the user's scenario:
 * "Attendance below 75%, student has 1 month to get above 75%, what should I do?"
 */
object AdvisorAgent {

    const val INSTITUTION_ATTENDANCE_THRESHOLD = 75.0 // 75% mandatory threshold

    /**
     * Calculates the exact mathematical sessions required to cross 75%:
     * Equation:
     *   (Attended + x) / (Total + x) >= 0.75
     *   Attended + x >= 0.75 * Total + 0.75 * x
     *   0.25 * x >= 0.75 * Total - Attended
     *   x >= (0.75 * Total - Attended) / 0.25
     */
    fun calculateAttendanceCatchupPlan(
        attendedSessions: Int,
        totalSessions: Int,
        sessionsPerDay: Int = 4,
        workingDaysInMonth: Int = 22
    ): AttendanceRecoveryPlan {
        val currentPercent = if (totalSessions > 0) (attendedSessions.toDouble() / totalSessions.toDouble()) * 100.0 else 0.0

        if (currentPercent >= INSTITUTION_ATTENDANCE_THRESHOLD) {
            return AttendanceRecoveryPlan(
                currentPercentage = currentPercent,
                targetPercentage = INSTITUTION_ATTENDANCE_THRESHOLD,
                currentAttended = attendedSessions,
                currentTotal = totalSessions,
                sessionsNeededWithoutAbsence = 0,
                estimatedDays = 0,
                roadmapMilestones = listOf(
                    "You are currently at ${"%.2f".format(currentPercent)}%, which is above the 75% cutoff.",
                    "You can afford up to ${maxOf(0, ((attendedSessions - 0.75 * totalSessions) / 0.75).toInt())} planned leaves while staying safe."
                )
            )
        }

        val requiredX = ceil((0.75 * totalSessions - attendedSessions) / 0.25).toInt().coerceAtLeast(1)
        val estimatedDays = ceil(requiredX.toDouble() / sessionsPerDay.toDouble()).toInt()
        val totalSessionsThisMonth = sessionsPerDay * workingDaysInMonth

        val milestones = mutableListOf<String>()
        milestones.add("Current status: $attendedSessions / $totalSessions sessions (${"%.2f".format(currentPercent)}%)")
        milestones.add("Minimum consecutive classes to attend with ZERO absence: $requiredX classes")
        milestones.add("Estimated time required: $estimatedDays instructional working days (~${ceil(estimatedDays / 5.0).toInt()} weeks)")
        milestones.add("Week 1 Milestone: Attend next ${minOf(20, requiredX)} sessions without leave -> Attendance rises to ~${"%.1f".format(((attendedSessions + minOf(20, requiredX)).toDouble() / (totalSessions + minOf(20, requiredX))) * 100)}%")
        if (requiredX > 20) {
            val week2Att = minOf(40, requiredX)
            milestones.add("Week 2 Milestone: Attend $week2Att sessions total -> Attendance reaches ~${"%.1f".format(((attendedSessions + week2Att).toDouble() / (totalSessions + week2Att)) * 100)}%")
        }
        milestones.add("Final Target: Once you complete $requiredX verified sessions, your attendance will safely cross ${"%.1f".format(((attendedSessions + requiredX).toDouble() / (totalSessions + requiredX)) * 100)}% (above 75%).")

        return AttendanceRecoveryPlan(
            currentPercentage = currentPercent,
            targetPercentage = INSTITUTION_ATTENDANCE_THRESHOLD,
            currentAttended = attendedSessions,
            currentTotal = totalSessions,
            sessionsNeededWithoutAbsence = requiredX,
            estimatedDays = estimatedDays,
            roadmapMilestones = milestones
        )
    }

    /**
     * Advisor alert generator based on research paper Algorithm 1
     */
    fun generateAdvisorAlerts(
        courses: List<SubjectCourse>,
        overallAttendance: Double,
        currentRiskLevel: RiskLevel
    ): List<String> {
        val alerts = mutableListOf<String>()

        if (overallAttendance < INSTITUTION_ATTENDANCE_THRESHOLD) {
            alerts.add("CRITICAL ATTENDANCE ALERT: Cumulative attendance (${"%.2f".format(overallAttendance)}%) is below the mandatory 75% University bar. Hall ticket generation will be withheld unless rectified.")
        }

        courses.filter { it.isAttendanceAtRisk }.forEach { course ->
            val plan = calculateAttendanceCatchupPlan(course.attendedSessions, course.totalSessions)
            alerts.add("Subject Alert: ${course.name} is at ${"%.1f".format(course.attendancePercentage)}%. Attend next ${plan.sessionsNeededWithoutAbsence} classes consecutively to cross 75%.")
        }

        if (currentRiskLevel == RiskLevel.HIGH) {
            alerts.add("MULTI-AGENT RISK ALERT: Academic Risk Agent classified status as HIGH RISK. Immediate Academic Counseling & remedial assignments assigned.")
        }

        return alerts
    }
}
