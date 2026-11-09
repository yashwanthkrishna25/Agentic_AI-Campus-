package com.example.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ========================================================================
    // HEALTH
    // ========================================================================
    @GET("/health")
    fun healthCheck(): Call<Map<String, Any>>

    // ========================================================================
    // AUTH / OTP
    // ========================================================================
    @POST("/api/otp/send")
    fun sendOtp(@Body request: OtpSendRequest): Call<OtpSendResponse>

    @POST("/api/otp/verify")
    fun verifyOtp(@Body request: OtpVerifyRequest): Call<OtpVerifyResponse>

    // ========================================================================
    // COLLEGES
    // ========================================================================
    @POST("/api/colleges")
    fun createCollege(@Header("Authorization") auth: String, @Body request: CollegeCreateRequest): Call<CollegeDto>

    @GET("/api/colleges")
    fun listColleges(@Header("Authorization") auth: String): Call<CollegeListResponse>

    @GET("/api/colleges/{collegeId}")
    fun getCollege(@Header("Authorization") auth: String, @Path("collegeId") collegeId: String): Call<Map<String, CollegeDto>>

    @PUT("/api/colleges/{collegeId}")
    fun updateCollege(@Header("Authorization") auth: String, @Path("collegeId") collegeId: String, @Body request: CollegeUpdateRequest): Call<Map<String, CollegeDto>>

    @POST("/api/colleges/{collegeId}/suspend")
    fun suspendCollege(@Header("Authorization") auth: String, @Path("collegeId") collegeId: String): Call<Map<String, String>>

    @POST("/api/colleges/{collegeId}/activate")
    fun activateCollege(@Header("Authorization") auth: String, @Path("collegeId") collegeId: String): Call<Map<String, String>>

    // ========================================================================
    // DEPARTMENTS
    // ========================================================================
    @POST("/api/departments")
    fun createDepartment(@Header("Authorization") auth: String, @Body request: DepartmentCreateRequest): Call<DepartmentDto>

    @GET("/api/departments")
    fun listDepartments(@Header("Authorization") auth: String, @Query("collegeId") collegeId: String? = null): Call<DepartmentListResponse>

    @GET("/api/departments/{deptId}")
    fun getDepartment(@Header("Authorization") auth: String, @Path("deptId") deptId: String): Call<Map<String, DepartmentDto>>

    @PUT("/api/departments/{deptId}")
    fun updateDepartment(@Header("Authorization") auth: String, @Path("deptId") deptId: String, @Body request: DepartmentUpdateRequest): Call<Map<String, DepartmentDto>>

    // ========================================================================
    // ACADEMIC YEARS
    // ========================================================================
    @POST("/api/academic-years")
    fun createAcademicYear(@Header("Authorization") auth: String, @Body request: AcademicYearCreateRequest): Call<AcademicYearDto>

    @GET("/api/academic-years")
    fun listAcademicYears(@Header("Authorization") auth: String, @Query("collegeId") collegeId: String? = null): Call<AcademicYearListResponse>

    // ========================================================================
    // SEMESTERS
    // ========================================================================
    @POST("/api/semesters")
    fun createSemester(@Header("Authorization") auth: String, @Body request: SemesterCreateRequest): Call<SemesterDto>

    @GET("/api/semesters")
    fun listSemesters(@Header("Authorization") auth: String, @Query("collegeId") collegeId: String? = null, @Query("academicYearId") academicYearId: String? = null): Call<SemesterListResponse>

    // ========================================================================
    // SECTIONS
    // ========================================================================
    @POST("/api/sections")
    fun createSection(@Header("Authorization") auth: String, @Body request: SectionCreateRequest): Call<SectionDto>

    @GET("/api/sections")
    fun listSections(@Header("Authorization") auth: String, @Query("collegeId") collegeId: String? = null, @Query("departmentId") departmentId: String? = null, @Query("semesterId") semesterId: String? = null): Call<SectionListResponse>

    // ========================================================================
    // SUBJECTS
    // ========================================================================
    @POST("/api/subjects")
    fun createSubject(@Header("Authorization") auth: String, @Body request: SubjectCreateRequest): Call<SubjectDto>

    @GET("/api/subjects")
    fun listSubjects(@Header("Authorization") auth: String, @Query("collegeId") collegeId: String? = null, @Query("departmentId") departmentId: String? = null): Call<SubjectListResponse>

    // ========================================================================
    // FACULTY ALLOCATIONS
    // ========================================================================
    @POST("/api/faculty-allocations")
    fun createFacultyAllocation(@Header("Authorization") auth: String, @Body request: FacultyAllocationCreateRequest): Call<FacultyAllocationDto>

    @GET("/api/faculty-allocations")
    fun listFacultyAllocations(@Header("Authorization") auth: String, @Query("facultyId") facultyId: String? = null, @Query("collegeId") collegeId: String? = null, @Query("departmentId") departmentId: String? = null): Call<FacultyAllocationListResponse>

    // ========================================================================
    // STUDENT ALLOCATIONS
    // ========================================================================
    @POST("/api/student-allocations")
    fun createStudentAllocation(@Header("Authorization") auth: String, @Body request: StudentAllocationCreateRequest): Call<StudentAllocationDto>

    @GET("/api/student-allocations")
    fun listStudentAllocations(@Header("Authorization") auth: String, @Query("studentId") studentId: String? = null, @Query("collegeId") collegeId: String? = null, @Query("departmentId") departmentId: String? = null): Call<StudentAllocationListResponse>

    // ========================================================================
    // TIMETABLE
    // ========================================================================
    @POST("/api/timetable-slots")
    fun createTimetableSlot(@Header("Authorization") auth: String, @Body request: TimetableSlotCreateRequest): Call<TimetableSlotDto>

    @GET("/api/timetable-slots")
    fun listTimetableSlots(@Header("Authorization") auth: String, @Query("sectionId") sectionId: String? = null, @Query("collegeId") collegeId: String? = null, @Query("day") day: String? = null): Call<TimetableSlotListResponse>

    // ========================================================================
    // BIOMETRICS
    // ========================================================================
    @POST("/api/biometrics/enroll")
    fun enrollFace(@Header("Authorization") auth: String, @Body request: FaceEnrollRequest): Call<FaceEnrollResponse>

    @POST("/api/biometrics/enroll/advanced")
    fun enrollFaceAdvanced(@Header("Authorization") auth: String, @Body request: FaceEnrollRequestAdvanced): Call<FaceEnrollResponseAdvanced>

    @POST("/api/biometrics/recognize")
    fun recognizeFaces(@Header("Authorization") auth: String, @Body request: FaceRecognizeRequest): Call<FaceRecognizeResponse>

    @POST("/api/biometrics/recognize/advanced")
    fun recognizeFacesAdvanced(@Header("Authorization") auth: String, @Body request: FaceRecognizeRequestAdvanced): Call<FaceRecognizeResponseAdvanced>

    @DELETE("/api/biometrics/delete/{studentId}")
    fun deleteFaceTemplate(@Header("Authorization") auth: String, @Path("studentId") studentId: String): Call<Map<String, Boolean>>

    // ========================================================================
    // ATTENDANCE
    // ========================================================================
    @POST("/api/attendance/submit")
    fun submitAttendance(@Header("Authorization") auth: String, @Body request: AttendanceRecordSubmitRequest): Call<Map<String, Any>>

    @POST("/api/attendance/correct")
    fun correctAttendance(@Header("Authorization") auth: String, @Body request: AttendanceCorrectionRequest): Call<Map<String, Any>>

    @GET("/api/attendance/history")
    fun getAttendanceHistory(@Header("Authorization") auth: String, @Query("subjectCode") subjectCode: String? = null, @Query("sectionId") sectionId: String? = null, @Query("studentId") studentId: String? = null): Call<AttendanceRecordListResponse>

    @GET("/api/attendance/student/{studentId}")
    fun getStudentAttendance(@Header("Authorization") auth: String, @Path("studentId") studentId: String, @Query("subjectCode") subjectCode: String? = null): Call<StudentAttendanceListResponse>

    // ========================================================================
    // LEAVE
    // ========================================================================
    @POST("/api/leave/apply")
    fun applyLeave(@Header("Authorization") auth: String, @Body request: LeaveRequestCreateRequest): Call<Map<String, Any>>

    @GET("/api/leave/pending")
    fun getPendingLeaves(@Header("Authorization") auth: String): Call<LeaveRequestListResponse>

    @GET("/api/leave/my")
    fun getMyLeaves(@Header("Authorization") auth: String): Call<LeaveListResponse>

    @POST("/api/leave/decide")
    fun decideLeave(@Header("Authorization") auth: String, @Body request: LeaveDecisionRequest): Call<Map<String, Any>>

    // ========================================================================
    // ASSIGNMENTS
    // ========================================================================
    @POST("/api/assignments/create")
    fun createAssignment(@Header("Authorization") auth: String, @Body request: AssignmentCreateRequest): Call<AssignmentDto>

    @GET("/api/assignments/list")
    fun listAssignments(@Header("Authorization") auth: String, @Query("subjectCode") subjectCode: String? = null, @Query("sectionId") sectionId: String? = null, @Query("status") status: String? = null): Call<AssignmentListResponse>

    @GET("/api/assignments/{assignmentId}")
    fun getAssignment(@Header("Authorization") auth: String, @Path("assignmentId") assignmentId: String): Call<Map<String, AssignmentDto>>

    @POST("/api/assignments/submit")
    fun submitAssignment(@Header("Authorization") auth: String, @Body request: SubmissionCreateRequest): Call<Map<String, Any>>

    @GET("/api/assignments/submissions")
    fun listSubmissions(@Header("Authorization") auth: String, @Query("assignmentId") assignmentId: String? = null, @Query("studentId") studentId: String? = null): Call<SubmissionListResponse>

    @POST("/api/assignments/grade")
    fun gradeSubmission(@Header("Authorization") auth: String, @Body request: GradeSubmissionRequest): Call<Map<String, Any>>

    // ========================================================================
    // ANNOUNCEMENTS
    // ========================================================================
    @POST("/api/announcements/create")
    fun createAnnouncement(@Header("Authorization") auth: String, @Body request: AnnouncementCreateRequest): Call<AnnouncementDto>

    @GET("/api/announcements/list")
    fun listAnnouncements(@Header("Authorization") auth: String, @Query("departmentId") departmentId: String? = null, @Query("audience") audience: String? = null): Call<AnnouncementListResponse>

    // ========================================================================
    // DOCUMENTS
    // ========================================================================
    @POST("/api/documents")
    fun createDocument(@Header("Authorization") auth: String, @Body request: DocumentCreateRequest): Call<DocumentDto>

    @GET("/api/documents")
    fun listDocuments(@Header("Authorization") auth: String, @Query("folderId") folderId: String? = null, @Query("includeShared") includeShared: Boolean = true): Call<DocumentListResponse>

    @GET("/api/documents/{docId}")
    fun getDocument(@Header("Authorization") auth: String, @Path("docId") docId: String): Call<Map<String, DocumentDto>>

    @DELETE("/api/documents/{docId}")
    fun deleteDocument(@Header("Authorization") auth: String, @Path("docId") docId: String): Call<Map<String, Boolean>>

    @POST("/api/document-folders")
    fun createDocumentFolder(@Header("Authorization") auth: String, @Body request: DocumentFolderCreateRequest): Call<DocumentFolderDto>

    @GET("/api/document-folders")
    fun listDocumentFolders(@Header("Authorization") auth: String, @Query("parentFolderId") parentFolderId: String? = null): Call<DocumentFolderListResponse>

    // ========================================================================
    // MESSAGES
    // ========================================================================
    @POST("/api/messages")
    fun sendMessage(@Header("Authorization") auth: String, @Body request: MessageCreateRequest): Call<Map<String, Any>>

    @GET("/api/messages")
    fun listMessages(@Header("Authorization") auth: String, @Query("otherUserId") otherUserId: String? = null): Call<MessageListResponse>

    @POST("/api/messages/{msgId}/read")
    fun markMessageRead(@Header("Authorization") auth: String, @Path("msgId") msgId: String): Call<Map<String, Boolean>>

    // ========================================================================
    // TASKS
    // ========================================================================
    @POST("/api/tasks")
    fun createTask(@Header("Authorization") auth: String, @Body request: TaskCreateRequest): Call<TaskDto>

    @GET("/api/tasks")
    fun listTasks(@Header("Authorization") auth: String, @Query("status") status: String? = null): Call<TaskListResponse>

    @PUT("/api/tasks/{taskId}")
    fun updateTask(@Header("Authorization") auth: String, @Path("taskId") taskId: String, @Body request: UpdateTaskRequest): Call<Map<String, Boolean>>

    // ========================================================================
    // EXAM RESULTS
    // ========================================================================
    @POST("/api/exam-results")
    fun createExamResult(@Header("Authorization") auth: String, @Body request: ExamResultCreateRequest): Call<ExamResultDto>

    @GET("/api/exam-results")
    fun listExamResults(@Header("Authorization") auth: String, @Query("studentId") studentId: String? = null, @Query("subjectId") subjectId: String? = null, @Query("semesterId") semesterId: String? = null, @Query("published") published: Boolean? = null): Call<ExamResultListResponse>

    @POST("/api/exam-results/{resultId}/publish")
    fun publishExamResult(@Header("Authorization") auth: String, @Path("resultId") resultId: String): Call<Map<String, Any>>

    // ========================================================================
    // CERTIFICATES
    // ========================================================================
    @POST("/api/certificates")
    fun createCertificate(@Header("Authorization") auth: String, @Body request: CertificateCreateRequest): Call<CertificateDto>

    @GET("/api/certificates")
    fun listCertificates(@Header("Authorization") auth: String, @Query("studentId") studentId: String? = null): Call<CertificateListResponse>

    // ========================================================================
    // HALL TICKETS
    // ========================================================================
    @POST("/api/hall-tickets")
    fun createHallTicket(@Header("Authorization") auth: String, @Body request: HallTicketCreateRequest): Call<HallTicketDto>

    @GET("/api/hall-tickets")
    fun listHallTickets(@Header("Authorization") auth: String, @Query("studentId") studentId: String? = null): Call<HallTicketListResponse>

    // ========================================================================
    // AI CHAT
    // ========================================================================
    @POST("/api/chat")
    fun chat(@Header("Authorization") auth: String, @Body request: ChatRequest): Call<ChatResponse>

    // ========================================================================
    // ANALYTICS
    // ========================================================================
    @GET("/api/analytics/dashboard")
    fun getDashboardAnalytics(@Header("Authorization") auth: String): Call<DashboardAnalyticsDto>

    @GET("/api/analytics/platform")
    fun getPlatformAnalytics(@Header("Authorization") auth: String): Call<PlatformAnalyticsDto>

    // ========================================================================
    // AUDIT LOGS
    // ========================================================================
    @GET("/api/audit-logs")
    fun getAuditLogs(@Header("Authorization") auth: String, @Query("collegeId") collegeId: String? = null, @Query("limit") limit: Int = 100): Call<AuditLogListResponse>

    // ========================================================================
    // USERS
    // ========================================================================
    @GET("/api/users")
    fun listUsers(@Header("Authorization") auth: String, @Query("role") role: String? = null, @Query("departmentId") departmentId: String? = null): Call<UserListResponse>

    @GET("/api/users/{uid}")
    fun getUser(@Header("Authorization") auth: String, @Path("uid") uid: String): Call<Map<String, UserProfileDto>>

    @PUT("/api/users/{uid}/approve")
    fun approveUser(@Header("Authorization") auth: String, @Path("uid") uid: String): Call<Map<String, String>>

    @PUT("/api/users/{uid}/suspend")
    fun suspendUser(@Header("Authorization") auth: String, @Path("uid") uid: String): Call<Map<String, String>>

    // ========================================================================
    // QUIZZES
    // ========================================================================
    @POST("/api/quizzes")
    fun createQuiz(@Header("Authorization") auth: String, @Body request: QuizCreateRequest): Call<Map<String, Any>>

    @GET("/api/quizzes")
    fun listQuizzes(@Header("Authorization") auth: String, @Query("subjectId") subjectId: String? = null, @Query("sectionId") sectionId: String? = null, @Query("status") status: String? = null): Call<Map<String, List<Any>>>

    @POST("/api/quizzes/attempt")
    fun submitQuizAttempt(@Header("Authorization") auth: String, @Body request: QuizAttemptSubmit): Call<Map<String, Any>>

    @GET("/api/quizzes/attempts")
    fun listQuizAttempts(@Header("Authorization") auth: String, @Query("quizId") quizId: String? = null, @Query("studentId") studentId: String? = null): Call<Map<String, List<Any>>>

    // ========================================================================
    // PLACEMENTS
    // ========================================================================
    @POST("/api/placements/drives")
    fun createPlacementDrive(@Header("Authorization") auth: String, @Body request: PlacementDriveCreateRequest): Call<Map<String, Any>>

    @GET("/api/placements/drives")
    fun listPlacementDrives(@Header("Authorization") auth: String, @Query("department") department: String? = null, @Query("status") status: String? = null): Call<Map<String, List<Any>>>

    @POST("/api/placements/apply")
    fun applyToPlacement(@Header("Authorization") auth: String, @Body request: PlacementApplicationRequest): Call<Map<String, Any>>

    @GET("/api/placements/applications")
    fun listPlacementApplications(@Header("Authorization") auth: String, @Query("driveId") driveId: String? = null, @Query("studentId") studentId: String? = null): Call<Map<String, List<Any>>>

    // ========================================================================
    // SKILLS
    // ========================================================================
    @POST("/api/skills")
    fun createSkill(@Header("Authorization") auth: String, @Body request: SkillCreateRequest): Call<Map<String, Any>>

    @GET("/api/skills")
    fun listSkills(@Header("Authorization") auth: String, @Query("category") category: String? = null): Call<Map<String, List<Any>>>

    @POST("/api/students/skills")
    fun upsertStudentSkill(@Header("Authorization") auth: String, @Body request: StudentSkillUpsertRequest): Call<Map<String, Any>>

    @GET("/api/students/skills")
    fun listStudentSkills(@Header("Authorization") auth: String, @Query("studentId") studentId: String? = null): Call<Map<String, List<Any>>>

    // ========================================================================
    // CALENDAR EVENTS
    // ========================================================================
    @POST("/api/calendar-events")
    fun createCalendarEvent(@Header("Authorization") auth: String, @Body request: CalendarEventCreateRequest): Call<Map<String, Any>>

    @GET("/api/calendar-events")
    fun listCalendarEvents(@Header("Authorization") auth: String, @Query("eventType") eventType: String? = null): Call<Map<String, List<Any>>>

    // ========================================================================
    // APPROVAL WORKFLOWS
    // ========================================================================
    @POST("/api/approvals/request")
    fun createApprovalRequest(@Header("Authorization") auth: String, @Body request: ApprovalRequestCreateRequest): Call<Map<String, Any>>

    @GET("/api/approvals/pending")
    fun listPendingApprovals(@Header("Authorization") auth: String): Call<Map<String, List<Any>>>

    @POST("/api/approvals/decide")
    fun decideApproval(@Header("Authorization") auth: String, @Body request: ApprovalDecisionRequest): Call<Map<String, Any>>

    // ========================================================================
    // STORAGE MONITORING
    // ========================================================================
    @GET("/api/storage/usage")
    fun getStorageUsage(@Header("Authorization") auth: String): Call<Map<String, Any>>

    @GET("/api/storage/platform")
    fun getPlatformStorage(@Header("Authorization") auth: String): Call<Map<String, Any>>
}
