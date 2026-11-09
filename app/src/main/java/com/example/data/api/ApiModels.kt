package com.example.data.api

import com.example.model.UserRole

// ============================================================================
// REQUEST MODELS
// ============================================================================

data class LoginRequest(
    val email: String,
    val password: String,
    val role: String = "STUDENT"
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val institutionalId: String,
    val phone: String,
    val role: UserRole,
    val department: String,
    val password: String
)

data class OtpSendRequest(
    val email: String
)

data class OtpVerifyRequest(
    val email: String,
    val otp: String
)

data class CollegeCreateRequest(
    val name: String,
    val code: String,
    val city: String? = null,
    val adminEmail: String? = null,
    val status: String = "Active"
)

data class CollegeUpdateRequest(
    val name: String? = null,
    val code: String? = null,
    val city: String? = null,
    val adminEmail: String? = null,
    val status: String? = null
)

data class DepartmentCreateRequest(
    val collegeId: String,
    val name: String,
    val code: String,
    val hodName: String? = null,
    val hodEmail: String? = null,
    val hodId: String? = null
)

data class DepartmentUpdateRequest(
    val name: String? = null,
    val code: String? = null,
    val hodName: String? = null,
    val hodEmail: String? = null,
    val hodId: String? = null,
    val status: String? = null
)

data class AcademicYearCreateRequest(
    val collegeId: String,
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false
)

data class SemesterCreateRequest(
    val collegeId: String,
    val academicYearId: String,
    val name: String,
    val number: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false
)

data class SectionCreateRequest(
    val collegeId: String,
    val departmentId: String,
    val academicYearId: String,
    val semesterId: String,
    val name: String
)

data class SubjectCreateRequest(
    val collegeId: String,
    val departmentId: String,
    val code: String,
    val name: String,
    val credits: Int = 3,
    val year: Int,
    val semester: Int
)

data class FacultyAllocationCreateRequest(
    val collegeId: String,
    val departmentId: String,
    val facultyId: String,
    val subjectId: String,
    val academicYearId: String,
    val semesterId: String,
    val sectionId: String
)

data class StudentAllocationCreateRequest(
    val collegeId: String,
    val studentId: String,
    val departmentId: String,
    val academicYearId: String,
    val semesterId: String,
    val sectionId: String,
    val rollNumber: String
)

data class TimetableSlotCreateRequest(
    val collegeId: String,
    val departmentId: String,
    val sectionId: String,
    val subjectId: String,
    val facultyId: String,
    val day: String,
    val periodNumber: Int,
    val timeSlot: String,
    val roomNumber: String? = null,
    val academicYearId: String,
    val semesterId: String
)

data class AttendanceSessionStartRequest(
    val subjectCode: String,
    val subjectName: String,
    val departmentId: String,
    val sectionId: String,
    val periodNumber: Int = 1,
    val date: String
)

data class AttendanceRecordSubmitRequest(
    val sessionId: String,
    val subjectCode: String,
    val subjectName: String,
    val departmentId: String,
    val sectionId: String,
    val periodNumber: Int = 1,
    val date: String,
    val presentStudentIds: List<String> = emptyList(),
    val absentStudentIds: List<String> = emptyList(),
    val lateStudentIds: List<String> = emptyList(),
    val excusedStudentIds: List<String> = emptyList(),
    val method: String = "FACE_RECOGNITION"
)

data class AttendanceCorrectionRequest(
    val recordId: String,
    val studentId: String,
    val previousStatus: String,
    val newStatus: String,
    val reason: String
)

data class LeaveRequestCreateRequest(
    val leaveType: String = "CASUAL",
    val startDate: String,
    val endDate: String,
    val reason: String,
    val documentUrl: String? = null
)

data class LeaveDecisionRequest(
    val leaveId: String,
    val decision: String,
    val remarks: String? = null
)

data class AssignmentCreateRequest(
    val subjectCode: String,
    val subjectName: String,
    val departmentId: String,
    val sectionId: String,
    val title: String,
    val instructions: String,
    val deadline: String,
    val maxMarks: Int = 10,
    val permittedFileType: String = "PDF"
)

data class SubmissionCreateRequest(
    val assignmentId: String,
    val fileUrl: String,
    val fileName: String,
    val fileSizeBytes: Int
)

data class GradeSubmissionRequest(
    val submissionId: String,
    val marksObtained: Float,
    val feedback: String
)

data class AnnouncementCreateRequest(
    val title: String,
    val message: String,
    val audience: String = "ALL",
    val departmentId: String? = null,
    val section: String? = null,
    val priority: String = "NORMAL"
)

data class DocumentCreateRequest(
    val title: String,
    val fileUrl: String,
    val fileName: String,
    val fileType: String,
    val fileSizeBytes: Int,
    val folderId: String? = null,
    val isPrivate: Boolean = true,
    val sharedWith: List<String> = emptyList(),
    val sharedWithDepartments: List<String> = emptyList(),
    val sharedWithCollege: Boolean = false
)

data class DocumentFolderCreateRequest(
    val name: String,
    val parentFolderId: String? = null,
    val isPrivate: Boolean = true
)

data class MessageCreateRequest(
    val receiverId: String,
    val message: String
)

data class TaskCreateRequest(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: String = "MEDIUM"
)

data class ChatRequest(
    val message: String,
    val history: List<Map<String, String>> = emptyList()
)

data class FaceEnrollRequest(
    val studentId: String,
    val studentName: String,
    val departmentId: String,
    val section: String,
    val embedding: List<Float>,
    val consentConfirmed: Boolean = true
)

data class FaceRecognizeRequest(
    val subjectCode: String,
    val sectionId: String,
    val embeddings: List<List<Float>>
)

// ============================================================================
// Advanced Face Recognition Request/Response Models
// ============================================================================

data class FaceEnrollRequestAdvanced(
    val studentId: String,
    val studentName: String,
    val departmentId: String,
    val section: String,
    val embedding: List<Float>,
    val embeddings: List<List<Float>> = emptyList(),
    val confidences: List<Float> = emptyList(),
    val frameCount: Int = 1,
    val consentConfirmed: Boolean = true,
    val livenessConfidence: Float = 1.0f,
    val modelVersion: String = "arcface_v2_multi_frame"
)

data class FaceRecognizeRequestAdvanced(
    val subjectCode: String,
    val sectionId: String,
    val embeddings: List<List<Float>>,
    val frameEmbeddings: List<List<Float>> = emptyList(),
    val livenessData: Map<String, Any> = emptyMap(),
    val requireLiveness: Boolean = true
)

data class FaceMatchMetrics(
    val isCosineMatch: Boolean,
    val isEuclideanMatch: Boolean,
    val combinedScore: Float,
    val templateAgeDays: Long? = null
)

data class FaceMatchAdvanced(
    val studentId: String,
    val studentName: String,
    val confidence: Float,
    val cosineSimilarity: Float,
    val euclideanScore: Float,
    val status: String,
    val needsConfirmation: Boolean,
    val matchMetrics: FaceMatchMetrics
)

data class FaceRecognizeResponseAdvanced(
    val matches: List<FaceMatchAdvanced> = emptyList(),
    val totalDetected: Int,
    val livenessFailed: Boolean = false,
    val livenessConfidence: Float? = null,
    val message: String? = null,
    val thresholds: Map<String, Any>? = null
)

data class FaceEnrollResponseAdvanced(
    val success: Boolean,
    val model: String,
    val detector: String,
    val templateId: String? = null,
    val frameCount: Int? = null,
    val message: String
)

// Liveness data model for attendance
data class LivenessData(
    val motionScore: Float,
    val textureScore: Float,
    val blinkScore: Float,
    val reflectionScore: Float,
    val confidence: Float,
    val isLive: Boolean,
    val attackType: String? = null
)

// ============================================================================
// Quiz Models
// ============================================================================

data class QuizCreateRequest(
    val title: String,
    val subjectId: String,
    val sectionId: String,
    val departmentId: String,
    val questions: List<Map<String, Any>>,
    val maxMarks: Float,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int
)

data class QuizAttemptSubmit(
    val quizId: String,
    val answers: List<Map<String, Any>>,
    val timeTakenSeconds: Int
)

// ============================================================================
// Placement Models
// ============================================================================

data class PlacementDriveCreateRequest(
    val companyName: String,
    val jobRole: String,
    val description: String,
    val eligibleDepartments: List<String>,
    val minCgpa: Float,
    val maxBacklogs: Int,
    val applicationDeadline: String,
    val aptitudeTestDate: String,
    val technicalInterviewDate: String,
    val hrInterviewDate: String,
    val resultDate: String
)

data class PlacementApplicationRequest(
    val driveId: String,
    val resumeUrl: String
)

// ============================================================================
// Skills Models
// ============================================================================

data class SkillCreateRequest(
    val name: String,
    val category: String,
    val description: String
)

data class StudentSkillUpsertRequest(
    val skillId: String,
    val proficiencyLevel: String,
    val yearsOfExperience: Float = 0f,
    val certification: String? = null
)

// ============================================================================
// Calendar Event Models
// ============================================================================

data class CalendarEventCreateRequest(
    val title: String,
    val description: String? = null,
    val eventDate: String,
    val eventType: String,
    val targetRoles: List<String> = emptyList(),
    val targetDepartments: List<String> = emptyList(),
    val targetSections: List<String> = emptyList()
)

// ============================================================================
// Approval Workflow Models
// ============================================================================

data class ApprovalRequestCreateRequest(
    val requestType: String,
    val referenceId: String,
    val reason: String
)

data class ApprovalDecisionRequest(
    val approvalId: String,
    val decision: String,
    val remarks: String? = null
)

data class ExamResultCreateRequest(
    val studentId: String,
    val subjectId: String,
    val academicYearId: String,
    val semesterId: String,
    val internalMarks: Float,
    val endTermMarks: Float,
    val gradeLetter: String,
    val gradePoints: Int,
    val resultStatus: String = "PASSED"
)

data class CertificateCreateRequest(
    val studentId: String,
    val title: String,
    val type: String,
    val certificateUrl: String
)

data class HallTicketCreateRequest(
    val studentId: String,
    val examName: String,
    val examDate: String,
    val hallTicketUrl: String,
    val seatNumber: String? = null,
    val roomNumber: String? = null
)

data class UpdateTaskRequest(
    val status: String
)

// ============================================================================
// RESPONSE MODELS
// ============================================================================

data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserProfileDto? = null
)

data class UserProfileDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val department: String? = null,
    val departmentId: String? = null,
    val collegeId: String? = null,
    val rollNumber: String? = null,
    val status: String? = null,
    val phone: String? = null,
    val section: String? = null,
    val yearSemester: String? = null,
    val cgpa: Double? = null,
    val isFaceEnrolled: Boolean? = null,
    val enrolledFaceSamplesCount: Int? = null,
    val avatarUri: String? = null
)

data class OtpSendResponse(
    val sent: Boolean,
    val message: String
)

data class OtpVerifyResponse(
    val verified: Boolean,
    val message: String? = null,
    val error: String? = null
)

data class CollegeDto(
    val id: String,
    val name: String,
    val code: String,
    val city: String? = null,
    val adminEmail: String? = null,
    val departmentsCount: Int = 0,
    val studentsCount: Int = 0,
    val status: String = "Active",
    val createdAt: Long? = null
)

data class DepartmentDto(
    val id: String,
    val collegeId: String,
    val name: String,
    val code: String,
    val hodName: String? = null,
    val hodEmail: String? = null,
    val hodId: String? = null,
    val facultyCount: Int = 0,
    val studentsCount: Int = 0,
    val avgAttendance: Double = 0.0,
    val status: String = "Active"
)

data class AcademicYearDto(
    val id: String,
    val collegeId: String,
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val status: String = "Active"
)

data class SemesterDto(
    val id: String,
    val collegeId: String,
    val academicYearId: String,
    val name: String,
    val number: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val status: String = "Active"
)

data class SectionDto(
    val id: String,
    val collegeId: String,
    val departmentId: String,
    val academicYearId: String,
    val semesterId: String,
    val name: String,
    val studentCount: Int = 0,
    val status: String = "Active"
)

data class SubjectDto(
    val id: String,
    val collegeId: String,
    val departmentId: String,
    val code: String,
    val name: String,
    val credits: Int = 3,
    val year: Int,
    val semester: Int,
    val status: String = "Active"
)

data class FacultyAllocationDto(
    val id: String,
    val collegeId: String,
    val departmentId: String,
    val facultyId: String,
    val subjectId: String,
    val academicYearId: String,
    val semesterId: String,
    val sectionId: String,
    val isActive: Boolean = true
)

data class StudentAllocationDto(
    val id: String,
    val collegeId: String,
    val studentId: String,
    val departmentId: String,
    val academicYearId: String,
    val semesterId: String,
    val sectionId: String,
    val rollNumber: String,
    val isActive: Boolean = true
)

data class TimetableSlotDto(
    val id: String,
    val collegeId: String,
    val departmentId: String,
    val sectionId: String,
    val subjectId: String,
    val facultyId: String,
    val day: String,
    val periodNumber: Int,
    val timeSlot: String,
    val roomNumber: String? = null,
    val academicYearId: String,
    val semesterId: String,
    val isActive: Boolean = true
)

data class AttendanceRecordDto(
    val id: String,
    val sessionId: String,
    val subjectCode: String,
    val subjectName: String,
    val departmentId: String,
    val sectionId: String,
    val collegeId: String,
    val periodNumber: Int,
    val date: String,
    val presentCount: Int,
    val absentCount: Int,
    val totalStudents: Int,
    val presentStudentIds: List<String> = emptyList(),
    val absentStudentIds: List<String> = emptyList(),
    val lateStudentIds: List<String> = emptyList(),
    val excusedStudentIds: List<String> = emptyList(),
    val markedBy: String,
    val markedByRole: String,
    val method: String,
    val createdAt: Long
)

data class StudentAttendanceDto(
    val id: String,
    val collegeId: String,
    val studentId: String,
    val subjectCode: String,
    val departmentId: String,
    val totalSessions: Int,
    val attendedSessions: Int,
    val absentSessions: Int,
    val lateSessions: Int,
    val attendancePercentage: Double,
    val lastUpdated: Long
)

data class LeaveRequestDto(
    val id: String,
    val applicantId: String,
    val applicantName: String,
    val applicantRole: String,
    val departmentId: String,
    val collegeId: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val reason: String,
    val documentUrl: String? = null,
    val status: String,
    val decidedBy: String? = null,
    val decisionRemarks: String? = null,
    val createdAt: Long
)

data class AssignmentDto(
    val id: String,
    val collegeId: String,
    val departmentId: String,
    val sectionId: String,
    val subjectCode: String,
    val subjectName: String,
    val title: String,
    val instructions: String,
    val deadline: String,
    val maxMarks: Int,
    val permittedFileType: String,
    val createdBy: String,
    val status: String,
    val createdAt: Long
)

data class AssignmentSubmissionDto(
    val id: String,
    val collegeId: String,
    val assignmentId: String,
    val studentId: String,
    val studentName: String,
    val fileUrl: String,
    val fileName: String,
    val fileSizeBytes: Int,
    val submittedAt: Long,
    val status: String,
    val marksObtained: Float? = null,
    val feedback: String? = null,
    val gradedBy: String? = null,
    val gradedAt: Long? = null
)

data class AnnouncementDto(
    val id: String,
    val collegeId: String,
    val departmentId: String? = null,
    val title: String,
    val message: String,
    val audience: String,
    val section: String? = null,
    val priority: String,
    val authorName: String,
    val authorRole: String,
    val authorId: String,
    val createdAt: Long
)

data class DocumentDto(
    val id: String,
    val ownerId: String,
    val collegeId: String,
    val departmentId: String? = null,
    val title: String,
    val fileUrl: String,
    val fileName: String,
    val fileType: String,
    val fileSizeBytes: Int,
    val folderId: String? = null,
    val isPrivate: Boolean,
    val sharedWith: List<String> = emptyList(),
    val sharedWithDepartments: List<String> = emptyList(),
    val sharedWithCollege: Boolean = false,
    val uploadedAt: Long,
    val updatedAt: Long
)

data class DocumentFolderDto(
    val id: String,
    val ownerId: String,
    val collegeId: String,
    val departmentId: String? = null,
    val name: String,
    val parentFolderId: String? = null,
    val isPrivate: Boolean,
    val createdAt: Long
)

data class MessageDto(
    val id: String,
    val collegeId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val receiverId: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)

data class TaskDto(
    val id: String,
    val collegeId: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: String,
    val status: String,
    val createdAt: Long
)

data class ChatResponse(
    val reply: String
)

data class FaceEnrollResponse(
    val success: Boolean,
    val model: String,
    val detector: String,
    val message: String
)

data class FaceRecognizeResponse(
    val matches: List<FaceMatch>,
    val totalDetected: Int
)

data class FaceMatch(
    val studentId: String,
    val studentName: String,
    val confidence: Float,
    val status: String,
    val needsConfirmation: Boolean
)

data class ExamResultDto(
    val id: String,
    val collegeId: String,
    val studentId: String,
    val subjectId: String,
    val departmentId: String,
    val academicYearId: String,
    val semesterId: String,
    val internalMarks: Float,
    val endTermMarks: Float,
    val totalMarks: Float,
    val gradeLetter: String,
    val gradePoints: Int,
    val resultStatus: String,
    val published: Boolean,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val createdAt: Long
)

data class CertificateDto(
    val id: String,
    val collegeId: String,
    val studentId: String,
    val title: String,
    val type: String,
    val certificateUrl: String,
    val issuedBy: String,
    val issuedAt: Long,
    val verificationCode: String,
    val isVerified: Boolean,
    val createdAt: Long
)

data class HallTicketDto(
    val id: String,
    val collegeId: String,
    val studentId: String,
    val examName: String,
    val examDate: String,
    val hallTicketUrl: String,
    val seatNumber: String? = null,
    val roomNumber: String? = null,
    val status: String,
    val generatedBy: String,
    val generatedAt: Long,
    val createdAt: Long
)

data class DashboardAnalyticsDto(
    val collegeId: String,
    val role: String,
    val totalStudents: Int,
    val totalFaculty: Int,
    val totalDepartments: Int,
    val totalAssignments: Int,
    val totalSubmissions: Int,
    val gradedSubmissions: Int,
    val pendingSubmissions: Int,
    val attendanceRate: Double,
    val departmentComparison: List<DepartmentComparisonDto>,
    val recentAttendance: List<AttendanceRecordDto>,
    val timestamp: Long
)

data class DepartmentComparisonDto(
    val dept: String,
    val attendance: Double,
    val students: Int
)

data class PlatformAnalyticsDto(
    val totalColleges: Int,
    val totalUsers: Int,
    val totalAttendanceRecords: Int,
    val colleges: List<CollegeDto>,
    val timestamp: Long
)

data class AuditLogDto(
    val id: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val targetType: String,
    val targetId: String,
    val details: String? = null,
    val timestamp: Long
)

// ============================================================================
// LIST RESPONSE WRAPPERS
// ============================================================================

data class CollegeListResponse(val colleges: List<CollegeDto> = emptyList())
data class DepartmentListResponse(val departments: List<DepartmentDto> = emptyList())
data class AcademicYearListResponse(val academicYears: List<AcademicYearDto> = emptyList())
data class SemesterListResponse(val semesters: List<SemesterDto> = emptyList())
data class SectionListResponse(val sections: List<SectionDto> = emptyList())
data class SubjectListResponse(val subjects: List<SubjectDto> = emptyList())
data class FacultyAllocationListResponse(val allocations: List<FacultyAllocationDto> = emptyList())
data class StudentAllocationListResponse(val allocations: List<StudentAllocationDto> = emptyList())
data class TimetableSlotListResponse(val slots: List<TimetableSlotDto> = emptyList())
data class AttendanceRecordListResponse(val records: List<AttendanceRecordDto> = emptyList())
data class StudentAttendanceListResponse(val records: List<StudentAttendanceDto> = emptyList())
data class LeaveRequestListResponse(val pendingLeaves: List<LeaveRequestDto> = emptyList())
data class LeaveListResponse(val leaves: List<LeaveRequestDto> = emptyList())
data class AssignmentListResponse(val assignments: List<AssignmentDto> = emptyList())
data class SubmissionListResponse(val submissions: List<AssignmentSubmissionDto> = emptyList())
data class AnnouncementListResponse(val announcements: List<AnnouncementDto> = emptyList())
data class DocumentListResponse(val documents: List<DocumentDto> = emptyList())
data class DocumentFolderListResponse(val folders: List<DocumentFolderDto> = emptyList())
data class MessageListResponse(val messages: List<MessageDto> = emptyList())
data class TaskListResponse(val tasks: List<TaskDto> = emptyList())
data class ExamResultListResponse(val results: List<ExamResultDto> = emptyList())
data class CertificateListResponse(val certificates: List<CertificateDto> = emptyList())
data class HallTicketListResponse(val hallTickets: List<HallTicketDto> = emptyList())
data class UserListResponse(val users: List<UserProfileDto> = emptyList())
data class AuditLogListResponse(val auditLogs: List<AuditLogDto> = emptyList())
