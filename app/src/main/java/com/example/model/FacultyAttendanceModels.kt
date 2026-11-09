package com.example.model

/**
 * Data models for Faculty Attendance Visual Summary & Analytics Charts.
 */
data class FacultySubjectSummary(
    val subjectCode: String,
    val subjectName: String,
    val section: String,
    val semester: String = "7th Semester (Odd 2026-27)",
    val totalClassesConducted: Int,
    val enrolledStudents: Int,
    val averageAttendancePercentage: Double,
    val studentsAbove75: Int,
    val studentsBelow75: Int,
    val weeklyAttendanceTrend: List<Double>, // 10-12 weeks percentage
    val tierDistinctionCount: Int, // > 85%
    val tierGoodStandingCount: Int, // 75% - 85%
    val tierWarningZoneCount: Int, // 65% - 74%
    val tierCriticalDetentionCount: Int, // < 65%
    val periodAverages: List<Pair<String, Double>>, // Period name to average %
    val highestDayInfo: String,
    val lowestDayInfo: String,
    val lastUpdated: String = "Today, 11:30 AM"
) {
    val isOverallAboveTarget: Boolean
        get() = averageAttendancePercentage >= 75.0
}

data class FacultyAttendanceOverview(
    val facultyName: String = "Dr. P. V. Kishore",
    val designation: String = "Associate Professor",
    val department: String = "Computer Science & Engineering",
    val academicYear: String = "2026 - 2027 (Autonomous)",
    val subjects: List<FacultySubjectSummary>
) {
    val totalClassesConducted: Int
        get() = subjects.sumOf { it.totalClassesConducted }

    val totalEnrolledStudents: Int
        get() = subjects.sumOf { it.enrolledStudents }

    val totalStudentsAbove75: Int
        get() = subjects.sumOf { it.studentsAbove75 }

    val totalStudentsBelow75: Int
        get() = subjects.sumOf { it.studentsBelow75 }

    val overallAttendancePercentage: Double
        get() = if (subjects.isNotEmpty()) {
            subjects.map { it.averageAttendancePercentage }.average()
        } else 0.0

    val overallTierDistinction: Int
        get() = subjects.sumOf { it.tierDistinctionCount }

    val overallTierGood: Int
        get() = subjects.sumOf { it.tierGoodStandingCount }

    val overallTierWarning: Int
        get() = subjects.sumOf { it.tierWarningZoneCount }

    val overallTierCritical: Int
        get() = subjects.sumOf { it.tierCriticalDetentionCount }
}

object FacultyAttendanceDataProvider {
    fun getFacultyOverview(): FacultyAttendanceOverview {
        val subjects = listOf(
            FacultySubjectSummary(
                subjectCode = "2339609",
                subjectName = "Deep Learning & Neural Networks",
                section = "CSE - 4th Year Sec B",
                totalClassesConducted = 52,
                enrolledStudents = 48,
                averageAttendancePercentage = 76.5,
                studentsAbove75 = 39,
                studentsBelow75 = 9,
                weeklyAttendanceTrend = listOf(84.0, 81.5, 78.0, 75.5, 79.0, 74.0, 71.5, 76.0, 80.5, 78.0, 76.5, 77.0),
                tierDistinctionCount = 18,
                tierGoodStandingCount = 21,
                tierWarningZoneCount = 6,
                tierCriticalDetentionCount = 3,
                periodAverages = listOf(
                    "P1 (09:15)" to 88.0,
                    "P2 (10:15)" to 84.5,
                    "P3 (11:15)" to 79.0,
                    "P4 (01:15)" to 73.5,
                    "P5 (02:15)" to 71.0,
                    "P6 (03:15)" to 69.5
                ),
                highestDayInfo = "Thu, 15 Oct (93.7% Attendance)",
                lowestDayInfo = "Fri, 02 Oct (66.7% Attendance)"
            ),
            FacultySubjectSummary(
                subjectCode = "2339751",
                subjectName = "Prompt Engineering & GenAI Lab",
                section = "CSE - 4th Year Sec B",
                totalClassesConducted = 38,
                enrolledStudents = 48,
                averageAttendancePercentage = 92.4,
                studentsAbove75 = 46,
                studentsBelow75 = 2,
                weeklyAttendanceTrend = listOf(95.0, 94.0, 91.0, 93.5, 90.0, 92.0, 94.5, 91.0, 93.0, 92.5, 93.0, 94.0),
                tierDistinctionCount = 36,
                tierGoodStandingCount = 10,
                tierWarningZoneCount = 2,
                tierCriticalDetentionCount = 0,
                periodAverages = listOf(
                    "P1 (09:15)" to 96.0,
                    "P2 (10:15)" to 94.0,
                    "P3 (11:15)" to 93.0,
                    "P4 (01:15)" to 91.5,
                    "P5 (02:15)" to 89.0,
                    "P6 (03:15)" to 88.0
                ),
                highestDayInfo = "Wed, 07 Oct (97.9% Attendance)",
                lowestDayInfo = "Mon, 21 Sep (87.5% Attendance)"
            ),
            FacultySubjectSummary(
                subjectCode = "2332604",
                subjectName = "Cloud Computing & Distributed Systems",
                section = "CSE - 4th Year Sec A",
                totalClassesConducted = 44,
                enrolledStudents = 46,
                averageAttendancePercentage = 81.2,
                studentsAbove75 = 40,
                studentsBelow75 = 6,
                weeklyAttendanceTrend = listOf(86.0, 83.0, 82.5, 79.0, 80.0, 78.5, 76.0, 82.0, 84.5, 81.0, 80.5, 82.0),
                tierDistinctionCount = 22,
                tierGoodStandingCount = 18,
                tierWarningZoneCount = 5,
                tierCriticalDetentionCount = 1,
                periodAverages = listOf(
                    "P1 (09:15)" to 89.5,
                    "P2 (10:15)" to 85.0,
                    "P3 (11:15)" to 82.0,
                    "P4 (01:15)" to 77.0,
                    "P5 (02:15)" to 76.5,
                    "P6 (03:15)" to 74.0
                ),
                highestDayInfo = "Tue, 13 Oct (91.3% Attendance)",
                lowestDayInfo = "Wed, 30 Sep (71.7% Attendance)"
            ),
            FacultySubjectSummary(
                subjectCode = "2339610",
                subjectName = "Computer Vision & Autonomous Lab",
                section = "CSE - 4th Year Sec A",
                totalClassesConducted = 36,
                enrolledStudents = 46,
                averageAttendancePercentage = 88.5,
                studentsAbove75 = 43,
                studentsBelow75 = 3,
                weeklyAttendanceTrend = listOf(91.0, 89.5, 87.0, 88.0, 86.5, 90.0, 88.0, 87.5, 89.0, 88.5, 89.0, 89.5),
                tierDistinctionCount = 28,
                tierGoodStandingCount = 15,
                tierWarningZoneCount = 2,
                tierCriticalDetentionCount = 1,
                periodAverages = listOf(
                    "P1 (09:15)" to 92.0,
                    "P2 (10:15)" to 90.0,
                    "P3 (11:15)" to 89.0,
                    "P4 (01:15)" to 87.0,
                    "P5 (02:15)" to 86.0,
                    "P6 (03:15)" to 85.0
                ),
                highestDayInfo = "Mon, 19 Oct (95.6% Attendance)",
                lowestDayInfo = "Thu, 24 Sep (80.4% Attendance)"
            ),
            FacultySubjectSummary(
                subjectCode = "2339612",
                subjectName = "Technical Capstone Seminar",
                section = "CSE - 4th Year Sec B",
                totalClassesConducted = 24,
                enrolledStudents = 48,
                averageAttendancePercentage = 84.8,
                studentsAbove75 = 42,
                studentsBelow75 = 6,
                weeklyAttendanceTrend = listOf(88.0, 85.0, 83.0, 86.0, 82.0, 84.0, 81.0, 85.0, 86.0, 84.5, 85.0, 85.5),
                tierDistinctionCount = 24,
                tierGoodStandingCount = 18,
                tierWarningZoneCount = 4,
                tierCriticalDetentionCount = 2,
                periodAverages = listOf(
                    "P1 (09:15)" to 90.0,
                    "P2 (10:15)" to 87.0,
                    "P3 (11:15)" to 85.0,
                    "P4 (01:15)" to 81.0,
                    "P5 (02:15)" to 80.0,
                    "P6 (03:15)" to 78.0
                ),
                highestDayInfo = "Fri, 16 Oct (91.6% Attendance)",
                lowestDayInfo = "Wed, 16 Sep (75.0% Attendance)"
            )
        )
        return FacultyAttendanceOverview(subjects = subjects)
    }
}
