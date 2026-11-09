package com.example.data.api

import android.content.Context
import com.example.data.SessionManager
import com.example.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * CampusRepository: Single source of truth for all backend API calls.
 * Bridges the UI layer with the Retrofit ApiService.
 * All network calls run on Dispatchers.IO.
 */
object CampusRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun init(context: Context) {
        ApiClient.init(context)
    }

    // ========================================================================
    // Auth helpers
    // ========================================================================
    private suspend fun bearerToken(): String {
        val token = SessionManager.getFirebaseIdToken()
        return "Bearer ${token ?: ""}"
    }

    // ========================================================================
    // OTP
    // ========================================================================
    suspend fun sendOtp(email: String): ApiResponse<OtpSendResponse> = withContext(ioDispatcher) {
        try {
            val response: Response<OtpSendResponse> = ApiClient.service.sendOtp(OtpSendRequest(email)).awaitResponse()
            if (response.isSuccessful) {
                ApiResponse(success = true, data = response.body(), message = response.body()?.message)
            } else {
                ApiResponse(success = false, error = response.errorBody()?.string() ?: "OTP send failed (${response.code()})")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, error = e.message ?: "OTP send failed")
        }
    }

    suspend fun verifyOtp(email: String, otp: String): ApiResponse<OtpVerifyResponse> = withContext(ioDispatcher) {
        try {
            val response: Response<OtpVerifyResponse> = ApiClient.service.verifyOtp(OtpVerifyRequest(email, otp)).awaitResponse()
            if (response.isSuccessful) {
                ApiResponse(success = true, data = response.body(), message = response.body()?.message)
            } else {
                ApiResponse(success = false, error = response.errorBody()?.string() ?: "OTP verify failed (${response.code()})")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, error = e.message ?: "OTP verify failed")
        }
    }

    // ========================================================================
    // COLLEGES
    // ========================================================================
    suspend fun createCollege( request: CollegeCreateRequest): ApiResponse<CollegeDto> = apiCall({ ApiClient.service.createCollege(bearerToken(), request) }) { it }

    suspend fun listColleges(): ApiResponse<CollegeListResponse> = apiCall({ ApiClient.service.listColleges(bearerToken()) }) { it }

    suspend fun getCollege( collegeId: String): ApiResponse<CollegeDto> = apiCall({ ApiClient.service.getCollege(bearerToken(), collegeId) }) { it["college"]!! }

    suspend fun updateCollege( collegeId: String, request: CollegeUpdateRequest): ApiResponse<CollegeDto> = apiCall({ ApiClient.service.updateCollege(bearerToken(), collegeId, request) }) { it["college"]!! }

    suspend fun suspendCollege( collegeId: String): ApiResponse<String> = apiCall({ ApiClient.service.suspendCollege(bearerToken(), collegeId) }) { it["status"]!! }

    suspend fun activateCollege( collegeId: String): ApiResponse<String> = apiCall({ ApiClient.service.activateCollege(bearerToken(), collegeId) }) { it["status"]!! }

    // ========================================================================
    // DEPARTMENTS
    // ========================================================================
    suspend fun createDepartment( request: DepartmentCreateRequest): ApiResponse<DepartmentDto> = apiCall({ ApiClient.service.createDepartment(bearerToken(), request) }) { it }

    suspend fun listDepartments( collegeId: String? = null): ApiResponse<DepartmentListResponse> = apiCall({ ApiClient.service.listDepartments(bearerToken(), collegeId) }) { it }

    suspend fun getDepartment( deptId: String): ApiResponse<DepartmentDto> = apiCall({ ApiClient.service.getDepartment(bearerToken(), deptId) }) { it["department"]!! }

    suspend fun updateDepartment( deptId: String, request: DepartmentUpdateRequest): ApiResponse<DepartmentDto> = apiCall({ ApiClient.service.updateDepartment(bearerToken(), deptId, request) }) { it["department"]!! }

    // ========================================================================
    // ACADEMIC YEARS
    // ========================================================================
    suspend fun createAcademicYear( request: AcademicYearCreateRequest): ApiResponse<AcademicYearDto> = apiCall({ ApiClient.service.createAcademicYear(bearerToken(), request) }) { it }

    suspend fun listAcademicYears( collegeId: String? = null): ApiResponse<AcademicYearListResponse> = apiCall({ ApiClient.service.listAcademicYears(bearerToken(), collegeId) }) { it }

    // ========================================================================
    // SEMESTERS
    // ========================================================================
    suspend fun createSemester( request: SemesterCreateRequest): ApiResponse<SemesterDto> = apiCall({ ApiClient.service.createSemester(bearerToken(), request) }) { it }

    suspend fun listSemesters( collegeId: String? = null, academicYearId: String? = null): ApiResponse<SemesterListResponse> = apiCall({ ApiClient.service.listSemesters(bearerToken(), collegeId, academicYearId) }) { it }

    // ========================================================================
    // SECTIONS
    // ========================================================================
    suspend fun createSection( request: SectionCreateRequest): ApiResponse<SectionDto> = apiCall({ ApiClient.service.createSection(bearerToken(), request) }) { it }

    suspend fun listSections( collegeId: String? = null, departmentId: String? = null, semesterId: String? = null): ApiResponse<SectionListResponse> = apiCall({ ApiClient.service.listSections(bearerToken(), collegeId, departmentId, semesterId) }) { it }

    // ========================================================================
    // SUBJECTS
    // ========================================================================
    suspend fun createSubject( request: SubjectCreateRequest): ApiResponse<SubjectDto> = apiCall({ ApiClient.service.createSubject(bearerToken(), request) }) { it }

    suspend fun listSubjects( collegeId: String? = null, departmentId: String? = null): ApiResponse<SubjectListResponse> = apiCall({ ApiClient.service.listSubjects(bearerToken(), collegeId, departmentId) }) { it }

    // ========================================================================
    // FACULTY ALLOCATIONS
    // ========================================================================
    suspend fun createFacultyAllocation( request: FacultyAllocationCreateRequest): ApiResponse<FacultyAllocationDto> = apiCall({ ApiClient.service.createFacultyAllocation(bearerToken(), request) }) { it }

    suspend fun listFacultyAllocations( facultyId: String? = null, collegeId: String? = null, departmentId: String? = null): ApiResponse<FacultyAllocationListResponse> = apiCall({ ApiClient.service.listFacultyAllocations(bearerToken(), facultyId, collegeId, departmentId) }) { it }

    // ========================================================================
    // STUDENT ALLOCATIONS
    // ========================================================================
    suspend fun createStudentAllocation( request: StudentAllocationCreateRequest): ApiResponse<StudentAllocationDto> = apiCall({ ApiClient.service.createStudentAllocation(bearerToken(), request) }) { it }

    suspend fun listStudentAllocations( studentId: String? = null, collegeId: String? = null, departmentId: String? = null): ApiResponse<StudentAllocationListResponse> = apiCall({ ApiClient.service.listStudentAllocations(bearerToken(), studentId, collegeId, departmentId) }) { it }

    // ========================================================================
    // TIMETABLE
    // ========================================================================
    suspend fun createTimetableSlot( request: TimetableSlotCreateRequest): ApiResponse<TimetableSlotDto> = apiCall({ ApiClient.service.createTimetableSlot(bearerToken(), request) }) { it }

    suspend fun listTimetableSlots( sectionId: String? = null, collegeId: String? = null, day: String? = null): ApiResponse<TimetableSlotListResponse> = apiCall({ ApiClient.service.listTimetableSlots(bearerToken(), sectionId, collegeId, day) }) { it }

    // ========================================================================
    // BIOMETRICS
    // ========================================================================
    suspend fun enrollFace(request: FaceEnrollRequest): ApiResponse<FaceEnrollResponse> = apiCall({ ApiClient.service.enrollFace(bearerToken(), request) }) { it }

    suspend fun enrollFaceAdvanced(request: FaceEnrollRequestAdvanced): ApiResponse<FaceEnrollResponseAdvanced> = apiCall({ ApiClient.service.enrollFaceAdvanced(bearerToken(), request) }) { it }

    suspend fun recognizeFaces(request: FaceRecognizeRequest): ApiResponse<FaceRecognizeResponse> = apiCall({ ApiClient.service.recognizeFaces(bearerToken(), request) }) { it }

    suspend fun recognizeFacesAdvanced(request: FaceRecognizeRequestAdvanced): ApiResponse<FaceRecognizeResponseAdvanced> = apiCall({ ApiClient.service.recognizeFacesAdvanced(bearerToken(), request) }) { it }

    suspend fun deleteFaceTemplate(studentId: String): ApiResponse<Boolean> = apiCall({ ApiClient.service.deleteFaceTemplate(bearerToken(), studentId) }) { it["deleted"]!! }

    // ========================================================================
    // ATTENDANCE
    // ========================================================================
    suspend fun submitAttendance( request: AttendanceRecordSubmitRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.submitAttendance(bearerToken(), request) }) { it }

    suspend fun correctAttendance( request: AttendanceCorrectionRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.correctAttendance(bearerToken(), request) }) { it }

    suspend fun getAttendanceHistory( subjectCode: String? = null, sectionId: String? = null, studentId: String? = null): ApiResponse<AttendanceRecordListResponse> = apiCall({ ApiClient.service.getAttendanceHistory(bearerToken(), subjectCode, sectionId, studentId) }) { it }

    suspend fun getStudentAttendance( studentId: String, subjectCode: String? = null): ApiResponse<StudentAttendanceListResponse> = apiCall({ ApiClient.service.getStudentAttendance(bearerToken(), studentId, subjectCode) }) { it }

    // ========================================================================
    // LEAVE
    // ========================================================================
    suspend fun applyLeave( request: LeaveRequestCreateRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.applyLeave(bearerToken(), request) }) { it }

    suspend fun getPendingLeaves(): ApiResponse<LeaveRequestListResponse> = apiCall({ ApiClient.service.getPendingLeaves(bearerToken()) }) { it }

    suspend fun getMyLeaves(): ApiResponse<LeaveListResponse> = apiCall({ ApiClient.service.getMyLeaves(bearerToken()) }) { it }

    suspend fun decideLeave( request: LeaveDecisionRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.decideLeave(bearerToken(), request) }) { it }

    // ========================================================================
    // ASSIGNMENTS
    // ========================================================================
    suspend fun createAssignment( request: AssignmentCreateRequest): ApiResponse<AssignmentDto> = apiCall({ ApiClient.service.createAssignment(bearerToken(), request) }) { it }

    suspend fun listAssignments( subjectCode: String? = null, sectionId: String? = null, status: String? = null): ApiResponse<AssignmentListResponse> = apiCall({ ApiClient.service.listAssignments(bearerToken(), subjectCode, sectionId, status) }) { it }

    suspend fun getAssignment( assignmentId: String): ApiResponse<AssignmentDto> = apiCall({ ApiClient.service.getAssignment(bearerToken(), assignmentId) }) { it["assignment"]!! }

    suspend fun submitAssignment( request: SubmissionCreateRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.submitAssignment(bearerToken(), request) }) { it }

    suspend fun listSubmissions( assignmentId: String? = null, studentId: String? = null): ApiResponse<SubmissionListResponse> = apiCall({ ApiClient.service.listSubmissions(bearerToken(), assignmentId, studentId) }) { it }

    suspend fun gradeSubmission( request: GradeSubmissionRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.gradeSubmission(bearerToken(), request) }) { it }

    // ========================================================================
    // ANNOUNCEMENTS
    // ========================================================================
    suspend fun createAnnouncement( request: AnnouncementCreateRequest): ApiResponse<AnnouncementDto> = apiCall({ ApiClient.service.createAnnouncement(bearerToken(), request) }) { it }

    suspend fun listAnnouncements( departmentId: String? = null, audience: String? = null): ApiResponse<AnnouncementListResponse> = apiCall({ ApiClient.service.listAnnouncements(bearerToken(), departmentId, audience) }) { it }

    // ========================================================================
    // DOCUMENTS
    // ========================================================================
    suspend fun createDocument( request: DocumentCreateRequest): ApiResponse<DocumentDto> = apiCall({ ApiClient.service.createDocument(bearerToken(), request) }) { it }

    suspend fun listDocuments( folderId: String? = null, includeShared: Boolean = true): ApiResponse<DocumentListResponse> = apiCall({ ApiClient.service.listDocuments(bearerToken(), folderId, includeShared) }) { it }

    suspend fun getDocument( docId: String): ApiResponse<DocumentDto> = apiCall({ ApiClient.service.getDocument(bearerToken(), docId) }) { it["document"]!! }

    suspend fun deleteDocument( docId: String): ApiResponse<Boolean> = apiCall({ ApiClient.service.deleteDocument(bearerToken(), docId) }) { it["deleted"]!! }

    suspend fun createDocumentFolder( request: DocumentFolderCreateRequest): ApiResponse<DocumentFolderDto> = apiCall({ ApiClient.service.createDocumentFolder(bearerToken(), request) }) { it }

    suspend fun listDocumentFolders( parentFolderId: String? = null): ApiResponse<DocumentFolderListResponse> = apiCall({ ApiClient.service.listDocumentFolders(bearerToken(), parentFolderId) }) { it }

    // ========================================================================
    // MESSAGES
    // ========================================================================
    suspend fun sendMessage( request: MessageCreateRequest): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.sendMessage(bearerToken(), request) }) { it }

    suspend fun listMessages( otherUserId: String? = null): ApiResponse<MessageListResponse> = apiCall({ ApiClient.service.listMessages(bearerToken(), otherUserId) }) { it }

    suspend fun markMessageRead( msgId: String): ApiResponse<Boolean> = apiCall({ ApiClient.service.markMessageRead(bearerToken(), msgId) }) { it["success"]!! }

    // ========================================================================
    // TASKS
    // ========================================================================
    suspend fun createTask( request: TaskCreateRequest): ApiResponse<TaskDto> = apiCall({ ApiClient.service.createTask(bearerToken(), request) }) { it }

    suspend fun listTasks( status: String? = null): ApiResponse<TaskListResponse> = apiCall({ ApiClient.service.listTasks(bearerToken(), status) }) { it }

    suspend fun updateTask( taskId: String, status: String): ApiResponse<Boolean> = apiCall({ ApiClient.service.updateTask(bearerToken(), taskId, UpdateTaskRequest(status)) }) { it["success"]!! }

    // ========================================================================
    // EXAM RESULTS
    // ========================================================================
    suspend fun createExamResult( request: ExamResultCreateRequest): ApiResponse<ExamResultDto> = apiCall({ ApiClient.service.createExamResult(bearerToken(), request) }) { it }

    suspend fun listExamResults( studentId: String? = null, subjectId: String? = null, semesterId: String? = null, published: Boolean? = null): ApiResponse<ExamResultListResponse> = apiCall({ ApiClient.service.listExamResults(bearerToken(), studentId, subjectId, semesterId, published) }) { it }

    suspend fun publishExamResult( resultId: String): ApiResponse<Map<String, Any>> = apiCall({ ApiClient.service.publishExamResult(bearerToken(), resultId) }) { it }

    // ========================================================================
    // CERTIFICATES
    // ========================================================================
    suspend fun createCertificate( request: CertificateCreateRequest): ApiResponse<CertificateDto> = apiCall({ ApiClient.service.createCertificate(bearerToken(), request) }) { it }

    suspend fun listCertificates( studentId: String? = null): ApiResponse<CertificateListResponse> = apiCall({ ApiClient.service.listCertificates(bearerToken(), studentId) }) { it }

    // ========================================================================
    // HALL TICKETS
    // ========================================================================
    suspend fun createHallTicket( request: HallTicketCreateRequest): ApiResponse<HallTicketDto> = apiCall({ ApiClient.service.createHallTicket(bearerToken(), request) }) { it }

    suspend fun listHallTickets( studentId: String? = null): ApiResponse<HallTicketListResponse> = apiCall({ ApiClient.service.listHallTickets(bearerToken(), studentId) }) { it }

    // ========================================================================
    // AI CHAT
    // ========================================================================
    suspend fun chat( message: String, history: List<Map<String, String>> = emptyList()): ApiResponse<String> = apiCall({ ApiClient.service.chat(bearerToken(), ChatRequest(message, history)) }) { it.reply }

    // ========================================================================
    // ANALYTICS
    // ========================================================================
    suspend fun getDashboardAnalytics(): ApiResponse<DashboardAnalyticsDto> = apiCall({ ApiClient.service.getDashboardAnalytics(bearerToken()) }) { it }

    suspend fun getPlatformAnalytics(): ApiResponse<PlatformAnalyticsDto> = apiCall({ ApiClient.service.getPlatformAnalytics(bearerToken()) }) { it }

    // ========================================================================
    // AUDIT LOGS
    // ========================================================================
    suspend fun getAuditLogs( collegeId: String? = null, limit: Int = 100): ApiResponse<AuditLogListResponse> = apiCall({ ApiClient.service.getAuditLogs(bearerToken(), collegeId, limit) }) { it }

    // ========================================================================
    // USERS
    // ========================================================================
    suspend fun listUsers( role: String? = null, departmentId: String? = null): ApiResponse<UserListResponse> = apiCall({ ApiClient.service.listUsers(bearerToken(), role, departmentId) }) { it }

    suspend fun getUser( uid: String): ApiResponse<UserProfileDto> = apiCall({ ApiClient.service.getUser(bearerToken(), uid) }) { it["user"]!! }

    suspend fun approveUser( uid: String): ApiResponse<String> = apiCall({ ApiClient.service.approveUser(bearerToken(), uid) }) { it["status"]!! }

    suspend fun suspendUser( uid: String): ApiResponse<String> = apiCall({ ApiClient.service.suspendUser(bearerToken(), uid) }) { it["status"]!! }

    // ========================================================================
    // QUIZZES
    // ========================================================================
    suspend fun createQuiz(request: QuizCreateRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.createQuiz(bearerToken(), request)
    }) { it }

    suspend fun listQuizzes(subjectId: String? = null, sectionId: String? = null, status: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listQuizzes(bearerToken(), subjectId, sectionId, status)
    }) { it }

    suspend fun submitQuizAttempt(request: QuizAttemptSubmit): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.submitQuizAttempt(bearerToken(), request)
    }) { it }

    suspend fun listQuizAttempts(quizId: String? = null, studentId: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listQuizAttempts(bearerToken(), quizId, studentId)
    }) { it }

    // ========================================================================
    // PLACEMENTS
    // ========================================================================
    suspend fun createPlacementDrive(request: PlacementDriveCreateRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.createPlacementDrive(bearerToken(), request)
    }) { it }

    suspend fun listPlacementDrives(department: String? = null, status: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listPlacementDrives(bearerToken(), department, status)
    }) { it }

    suspend fun applyToPlacement(request: PlacementApplicationRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.applyToPlacement(bearerToken(), request)
    }) { it }

    suspend fun listPlacementApplications(driveId: String? = null, studentId: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listPlacementApplications(bearerToken(), driveId, studentId)
    }) { it }

    // ========================================================================
    // SKILLS
    // ========================================================================
    suspend fun createSkill(request: SkillCreateRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.createSkill(bearerToken(), request)
    }) { it }

    suspend fun listSkills(category: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listSkills(bearerToken(), category)
    }) { it }

    suspend fun upsertStudentSkill(request: StudentSkillUpsertRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.upsertStudentSkill(bearerToken(), request)
    }) { it }

    suspend fun listStudentSkills(studentId: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listStudentSkills(bearerToken(), studentId)
    }) { it }

    // ========================================================================
    // CALENDAR EVENTS
    // ========================================================================
    suspend fun createCalendarEvent(request: CalendarEventCreateRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.createCalendarEvent(bearerToken(), request)
    }) { it }

    suspend fun listCalendarEvents(eventType: String? = null): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listCalendarEvents(bearerToken(), eventType)
    }) { it }

    // ========================================================================
    // APPROVAL WORKFLOWS
    // ========================================================================
    suspend fun createApprovalRequest(request: ApprovalRequestCreateRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.createApprovalRequest(bearerToken(), request)
    }) { it }

    suspend fun listPendingApprovals(): ApiResponse<Map<String, List<Any>>> = apiCall({
        ApiClient.service.listPendingApprovals(bearerToken())
    }) { it }

    suspend fun decideApproval(request: ApprovalDecisionRequest): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.decideApproval(bearerToken(), request)
    }) { it }

    // ========================================================================
    // STORAGE MONITORING
    // ========================================================================
    suspend fun getStorageUsage(): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.getStorageUsage(bearerToken())
    }) { it }

    suspend fun getPlatformStorage(): ApiResponse<Map<String, Any>> = apiCall({
        ApiClient.service.getPlatformStorage(bearerToken())
    }) { it }

        // ========================================================================
    // Internal helpers
    // ========================================================================

    private suspend fun <T, R> apiCall(
        call: suspend () -> Call<T>,
        map: (T) -> R
    ): ApiResponse<R> = withContext(ioDispatcher) {
        try {
            val response: Response<T> = call().awaitResponse()
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext ApiResponse(success = false, error = "Empty response body")
                ApiResponse(success = true, data = map(body))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Request failed (${response.code()})"
                ApiResponse(success = false, error = errorBody)
            }
        } catch (e: HttpException) {
            ApiResponse(success = false, error = "HTTP ${e.code()}: ${e.message ?: e.response()?.errorBody()?.string() ?: ""}")
        } catch (e: IOException) {
            ApiResponse(success = false, error = "Network error: ${e.message ?: e.javaClass.simpleName}")
        } catch (e: Exception) {
            ApiResponse(success = false, error = e.message ?: "Unexpected error")
        }
    }
}
