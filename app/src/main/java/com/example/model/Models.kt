package com.example.model

enum class UserRole(val displayName: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("College Admin"),
    PRINCIPAL("College Principal"),
    HOD("Head of Department (HOD)"),
    FACULTY("Faculty Member"),
    STUDENT("Student"),
    PARENT("Parent / Guardian"),
    PRINCIPAL_ADMIN("Principal / Admin")
}

enum class RiskLevel(val label: String) {
    LOW("Low Risk"),
    MODERATE("Moderate Risk"),
    HIGH("High Risk")
}

enum class AttendanceStatus(val symbol: String, val label: String) {
    PRESENT("P", "Present"),
    ABSENT("A", "Absent"),
    LATE("L", "Late"),
    ON_DUTY("OD", "On Duty")
}

enum class AttendanceMethod {
    FACE_RECOGNITION_KNN,
    GROUP_CAMERA_SCAN,
    QR_CODE,
    MANUAL_ENTRY
}

data class UserProfile(
    val id: String,
    val name: String,
    val rollNumber: String,
    val programme: String,
    val department: String,
    val section: String,
    val yearSemester: String,
    val role: UserRole,
    val email: String,
    val phone: String,
    val cgpa: Double,
    val isFaceEnrolled: Boolean = true,
    val enrolledFaceSamplesCount: Int = 50,
    val avatarUri: String? = null
)

data class SubjectCourse(
    val code: String,
    val name: String,
    val courseType: String, // LECTURE or PRACTICAL
    val sectionTag: String,
    val facultyName: String,
    val totalSessions: Int,
    val attendedSessions: Int,
    val credits: Int = 3,
    // Marks components from the paper (x1 to x6 internal assessments [0-10], x7 end-term [0-40])
    val x1: Double = 8.5,
    val x2: Double = 7.0,
    val x3: Double = 8.0,
    val x4: Double = 9.0,
    val x5: Double = 6.5,
    val x6: Double = 8.0,
    val x7: Double = 31.0
) {
    val attendancePercentage: Double
        get() = if (totalSessions > 0) (attendedSessions.toDouble() / totalSessions.toDouble()) * 100.0 else 0.0

    val internalTotal: Double
        get() = x1 + x2 + x3 + x4 + x5 + x6

    val grandTotal: Double
        get() = internalTotal + x7

    val isAttendanceAtRisk: Boolean
        get() = attendancePercentage < 75.0
}

data class EnrolledStudent(
    val id: String,
    val rollNumber: String,
    val name: String,
    val section: String,
    val department: String,
    val faceFeaturesEnrolled: Boolean = true,
    val faceDescriptor: String = "Vector_7500",
    var currentStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val attendancePercentage: Double = 85.0
)

data class DetectedFace(
    val id: String,
    val studentName: String,
    val rollNumber: String,
    val matchConfidence: Float,
    val boxX: Float,
    val boxY: Float,
    val boxWidth: Float,
    val boxHeight: Float,
    val isEnrolled: Boolean = true,
    val knnDistance: Float = 1420f, // Paper: < 3200 is enrolled, > 3200 is unknown stranger
    val eyesCaptured: Boolean = true,
    val leftEyeOpenProb: Float = 1.0f,
    val rightEyeOpenProb: Float = 1.0f,
    val leftEyeRelX: Float = 0.32f,
    val leftEyeRelY: Float = 0.38f,
    val rightEyeRelX: Float = 0.68f,
    val rightEyeRelY: Float = 0.38f
)

data class AcademicSurveyData(
    val stressLevel: Int, // 1 to 5
    val cgpaBand: Int, // 1 to 5
    val studyConsistency: Int, // 1 to 5
    val attendanceBand: Int // 1 to 5
)

data class RiskPredictionResult(
    val riskLevel: RiskLevel,
    val probabilityLow: Double,
    val probabilityModerate: Double,
    val probabilityHigh: Double,
    val score: Double,
    val warningFactors: List<String>,
    val advisorAction: String,
    val oneMonthPlan: AttendanceRecoveryPlan? = null
)

data class AttendanceRecoveryPlan(
    val currentPercentage: Double,
    val targetPercentage: Double = 75.0,
    val currentAttended: Int,
    val currentTotal: Int,
    val sessionsNeededWithoutAbsence: Int,
    val estimatedDays: Int,
    val roadmapMilestones: List<String>
)

data class ChatMessage(
    val id: String,
    val senderRole: String, // "User" or "ScholarBot AI" or "Advisor Agent"
    val message: String,
    val timestamp: String,
    val isFromAi: Boolean,
    val tags: List<String> = emptyList()
)

data class FeedComment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val authorName: String,
    val authorRole: String = "Campus Member",
    val text: String,
    val time: String = "Just now"
)

data class FeedPost(
    val id: String,
    val authorName: String,
    val authorRole: String,
    val date: String,
    val title: String,
    val content: String,
    var upvotes: Int,
    var commentsCount: Int,
    var isUpvoted: Boolean = false,
    val tag: String = "Notice",
    val imageUrl: String? = null,
    val comments: MutableList<FeedComment> = mutableListOf()
)

data class StudyDocument(
    val id: String,
    val subjectCode: String,
    val title: String,
    val fileType: String,
    val sizeText: String,
    val isOfflineAvailable: Boolean = true
)

data class GroupChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val time: String,
    val isEncrypted: Boolean = true
)

data class FacultyMember(
    val id: String,
    val employeeId: String,
    val name: String,
    val designation: String,
    val department: String,
    val assignedSubjects: List<String>,
    var canTakeFaceAttendance: Boolean = true,
    var canEditManualAttendance: Boolean = true,
    var canEnterInternalMarks: Boolean = true,
    var canIssueCondonation: Boolean = false,
    var isActive: Boolean = true
)

data class StudentSettings(
    var notifyBelow75: Boolean = true,
    var biometricSelfCheckIn: Boolean = true,
    var dailyStudyReminders: Boolean = true,
    var autoSyncOfflineNotes: Boolean = true
)

data class FacultySettings(
    var defaultSection: String = "CSE Sec B",
    var defaultPeriod: String = "Period 1 (09:00 - 10:00 AM)",
    var antiProxyQrRefreshSecs: Int = 30,
    var notifyAbsenteeParentsSms: Boolean = true,
    var autoFlagAtRiskStudents: Boolean = true
)

data class PrincipalSettings(
    var mandatoryAttendanceBar: Double = 75.0,
    var condonationAllowedThreshold: Double = 65.0,
    var requireBiometricLivenessVerification: Boolean = true,
    var autoDetentionLockDate: String = "30 Nov 2026"
)

data class ChildDailyPeriodLog(
    val periodNumber: Int,
    val timeSlot: String,
    val subjectName: String,
    val facultyName: String,
    val status: AttendanceStatus,
    val recordedTime: String,
    val method: AttendanceMethod = AttendanceMethod.FACE_RECOGNITION_KNN
)

data class ParentLeaveApplication(
    val id: String,
    val childName: String,
    val rollNumber: String,
    val startDate: String,
    val endDate: String,
    val reasonCategory: String,
    val explanation: String,
    val medicalProofAttached: Boolean = false,
    val status: String = "Approved by Mentor" // "Pending Review", "Approved by Mentor", "Condonation Granted"
)

data class MentorDirectMessage(
    val id: String,
    val senderName: String,
    val messageText: String,
    val timestamp: String,
    val isFromMentor: Boolean
)

enum class EmailCategory(val badge: String) {
    ATTENDANCE_SHORTAGE_ALERT("75% Cutoff Shortage Alert"),
    EXAM_SCHEDULE_NOTIFICATION("Andhra JNTUA Exam Schedule"),
    EXAM_RESULTS_GRADE_CARD("End-Semester Official Grade Sheet"),
    ACTIVITY_OD_APPROVAL("Hackathon / Event OD Approval"),
    CAMPUS_ANNOUNCEMENT("Campus Security & Verification")
}

data class EmailNotificationLog(
    val id: String,
    val subject: String,
    val recipients: List<String>,
    val recipientRoles: String,
    val category: EmailCategory,
    val contentBody: String,
    val sentTimestamp: String,
    val deliveryStatus: String = "Delivered via KSRMCE Autonomous SMTP"
)

data class StudentActivityEvent(
    val id: String,
    val title: String,
    val category: String,
    val organizer: String,
    val eventDate: String,
    val projectTitle: String,
    val innovationScore: Double = 9.4,
    val odCreditsGranted: Int = 5,
    val status: String = "Verified & OD Approved"
)

data class AndhraExamSchedule(
    val subjectCode: String,
    val subjectName: String,
    val examDate: String,
    val timeSlot: String = "10:00 AM - 01:00 PM",
    val examType: String = "End-Semester Autonomous Theory",
    val hallTicketEligibility: Boolean = true
)

data class AndhraExamResult(
    val subjectCode: String,
    val subjectName: String,
    val credits: Int,
    val internalMarks: Double,
    val endTermMarks: Double,
    val totalMarks: Double,
    val gradeLetter: String,
    val gradePoints: Int,
    val resultStatus: String = "PASSED"
)

data class CollegeOrganization(
    val id: String,
    val name: String,
    val code: String,
    val city: String,
    val adminEmail: String,
    val departmentsCount: Int,
    val studentsCount: Int,
    val status: String = "Active"
)

data class DepartmentInfo(
    val id: String,
    val collegeId: String,
    val name: String,
    val code: String,
    val hodName: String,
    val hodEmail: String,
    val facultyCount: Int,
    val studentsCount: Int,
    val avgAttendance: Double
)

data class DriveFolder(
    val id: String,
    val name: String,
    val createdDate: String,
    val colorHex: Long = 0xFF10B981,
    var fileCount: Int = 0
)

data class DriveDocument(
    val id: String,
    val folderId: String = "docs",
    val folderName: String = "Campus Documents",
    val title: String,
    val fileType: String = "PDF",
    val sizeBytes: Long = 524288L,
    val sizeFormatted: String = "512 KB",
    val uploadedBy: String = "Autonomous Examination Cell",
    val uploadedDate: String = "Verified",
    val downloadUrl: String = "",
    val localUri: String? = null,
    val ownerId: String = "",
    val collegeId: String = "col_ksrm_01",
    val assetFileName: String = "BECG_CASE_STUDY.pdf",
    val fileName: String = "document.pdf",
    val subject: String = "",
    val subjectCode: String = ""
)

data class AppIssueReport(
    val id: String,
    val title: String,
    val description: String,
    val locationScreen: String,
    val reportedByRole: String,
    val severity: String = "Medium",
    var isResolved: Boolean = false,
    val dateReported: String = "Today"
)

data class HODSettings(
    var departmentCode: String = "CSE",
    var notifyFacultyBelow75: Boolean = true,
    var autoEmailParents: Boolean = true,
    var sendWeeklyPdfToFaculty: Boolean = true,
    var lockAttendanceThreshold: Double = 75.0
)

data class AdminSettings(
    var activeCollegeId: String = "col_ksrm_01",
    var receiveIssueAlertsViaEmail: Boolean = true,
    var adminNotificationEmail: String = "admin.ksrmce@gmail.com",
    var autoBackupFirestoreDaily: Boolean = true
)

data class TimetableSlot(
    val id: String,
    val day: String,
    val period: Int,
    val timeSlot: String,
    val subjectCode: String,
    val subjectName: String,
    val facultyName: String,
    val roomNumber: String
)

data class CampusAssignment(
    val id: String,
    val collegeId: String = "col_ksrm_01",
    val department: String,      // CSE, AIML, ECE, EEE, CIVIL, MECH
    val year: Int,               // 1, 2, 3, 4
    val section: String = "All", // A, B, All
    val subjectCode: String,
    val subjectName: String,
    val title: String,
    val description: String,
    val facultyName: String,
    val deadlineTimestamp: Long,
    val deadlineFormatted: String,
    val questionPdfName: String = "Assignment_Question.pdf",
    val maxMarks: Int = 10,
    var isClosed: Boolean = false
) {
    val isExpired: Boolean
        get() = isClosed || (deadlineTimestamp > 0 && System.currentTimeMillis() > deadlineTimestamp)
}

data class AssignmentSubmission(
    val id: String,
    val assignmentId: String,
    val studentId: String,
    val studentName: String,
    val rollNumber: String,
    val department: String,
    val year: Int,
    val section: String,
    val submittedPdfName: String,
    val submissionTimestamp: Long,
    val submissionDateFormatted: String,
    var marksAwarded: Double? = null,
    var feedback: String = "",
    var isGraded: Boolean = false
)

data class CourseResource(
    val id: String,
    val collegeId: String = "col_ksrm_01",
    val department: String,
    val year: Int,
    val subjectCode: String,
    val subjectName: String,
    val title: String,
    val type: String, // "Notes PDF", "Syllabus", "Question Bank", "Lab Manual"
    val facultyName: String,
    val uploadedDate: String = "Recently",
    val fileSizeFormatted: String = "2.4 MB"
)

data class ExamMarkRecord(
    val id: String,
    val rollNumber: String,
    val subjectCode: String,
    val subjectName: String,
    val mid1Marks: Double,    // Max 30
    val mid2Marks: Double,    // Max 30
    val assignmentMarks: Double, // Max 10 (from graded assignment)
    val semResultMarks: Double,  // Max 70
    val totalCredits: Int = 3
) {
    val grandTotal: Double
        get() = ((mid1Marks + mid2Marks) / 2.0) + assignmentMarks + semResultMarks

    val gradeLetter: String
        get() = when {
            grandTotal >= 90 -> "A+"
            grandTotal >= 80 -> "A"
            grandTotal >= 70 -> "B"
            grandTotal >= 60 -> "C"
            grandTotal >= 50 -> "D"
            else -> "F"
        }
}

data class FacultyClassTeachingLoad(
    val id: String,
    val department: String,   // e.g. CSE, ECE
    val year: Int,            // 1, 2, 3, 4
    val section: String,      // Sec A, Sec B
    val subjectCode: String,
    val subjectName: String,
    val period: String = "Period 1 (09:00 - 10:00 AM)",
    val totalEnrolledStudents: Int = 68
)

data class StudentTaskTodo(
    val id: String,
    val title: String,
    val dueDate: String,
    val category: String = "Exam Prep",
    var isCompleted: Boolean = false
)

