package com.example.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.model.*

object AcademicDataProvider {

    /** Active selected key in the right-side navigation drawer across all user roles */
    var activeDrawerKey by mutableStateOf("home")

    // === SHARED OBSERVABLE DATA ACROSS ALL USER LOGINS ===
    val sharedFeedPosts = mutableStateListOf<FeedPost>().apply {
        addAll(getInitialFeedPosts())
    }

    val sharedDriveDocs = mutableStateListOf<DriveDocument>().apply {
        addAll(getInitialDriveDocuments())
    }

    val sharedDriveFolders = mutableStateListOf<DriveFolder>().apply {
        addAll(getInitialDriveFolders())
    }

    val sharedExamSchedules = mutableStateListOf<AndhraExamSchedule>().apply {
        addAll(getAndhraExamSchedules())
    }

    val sharedExamResults = mutableStateListOf<AndhraExamResult>().apply {
        addAll(getAndhraExamResults())
    }

    val sharedStudyDocs = mutableStateListOf<StudyDocument>().apply {
        addAll(getStudyDocuments())
    }

    val sharedCourses = mutableStateListOf<SubjectCourse>().apply {
        addAll(getInitialCourses())
    }

    val sharedClassRoster = mutableStateListOf<EnrolledStudent>().apply {
        addAll(getFullClassRoster())
    }

    val sharedIssueReports = mutableStateListOf<AppIssueReport>().apply {
        addAll(getInitialIssueReports())
    }

    val sharedColleges = mutableStateListOf<CollegeOrganization>().apply {
        addAll(getInitialColleges())
    }

    val sharedDepartments = mutableStateListOf<DepartmentInfo>().apply {
        addAll(getInitialDepartments())
    }

    val sharedFaculty = mutableStateListOf<FacultyMember>().apply {
        addAll(getInitialFacultyMembers())
    }

    val sharedAssignments = mutableStateListOf<CampusAssignment>().apply {
        addAll(getInitialAssignments())
    }

    val sharedSubmissions = mutableStateListOf<AssignmentSubmission>().apply {
        addAll(getInitialSubmissions())
    }

    val sharedCourseResources = mutableStateListOf<CourseResource>().apply {
        addAll(getInitialCourseResources())
    }

    val sharedExamMarks = mutableStateListOf<ExamMarkRecord>().apply {
        addAll(getInitialExamMarks())
    }

    val sharedTeachingLoads = mutableStateListOf<FacultyClassTeachingLoad>().apply {
        addAll(getInitialTeachingLoads())
    }

    val sharedStudentTasks = mutableStateListOf<StudentTaskTodo>().apply {
        addAll(getInitialStudentTasks())
    }

    val sharedNotifications = mutableStateListOf<String>().apply {
        addAll(
            listOf(
                "Autonomous Exam Cell: Mid-II Timetable published for IV B.Tech II Sem",
                "Attendance Alert: Cryptography & Network Security is at 68.5% (Below 75% cutoff)",
                "Fee / Condonation Notice: Pay medical condonation by Oct 30 to avoid detention",
                "Campus Placement: Infosys & TCS Drive registration closes tonight at 11:59 PM",
                "HOD Circular: Submission of Lab Record Books mandatory before Friday",
                "Faculty Alert: New Assignment Question PDF published for 4th Year CSE Sec B",
                "Assignment Graded: Your submission for 'Deep Learning ResNet Lab' has been graded (9.5/10)"
            )
        )
    }

    private var isInitialized = false

    fun init(context: android.content.Context) {
        AppPersistenceManager.init(context)
        if (!isInitialized) {
            val loaded = AppPersistenceManager.loadAll()
            if (loaded != null) {
                if (loaded.feedPosts.isNotEmpty()) {
                    sharedFeedPosts.clear()
                    sharedFeedPosts.addAll(loaded.feedPosts)
                }
                if (loaded.examSchedules.isNotEmpty()) {
                    sharedExamSchedules.clear()
                    sharedExamSchedules.addAll(loaded.examSchedules)
                }
                if (loaded.examResults.isNotEmpty()) {
                    sharedExamResults.clear()
                    sharedExamResults.addAll(loaded.examResults)
                }
                if (loaded.driveDocs.isNotEmpty()) {
                    sharedDriveDocs.clear()
                    sharedDriveDocs.addAll(loaded.driveDocs)
                }
                if (loaded.driveFolders.isNotEmpty()) {
                    sharedDriveFolders.clear()
                    sharedDriveFolders.addAll(loaded.driveFolders)
                }
                if (loaded.courses.isNotEmpty()) {
                    sharedCourses.clear()
                    sharedCourses.addAll(loaded.courses)
                }
                if (loaded.roster.isNotEmpty()) {
                    sharedClassRoster.clear()
                    sharedClassRoster.addAll(loaded.roster)
                }
                if (loaded.issues.isNotEmpty()) {
                    sharedIssueReports.clear()
                    sharedIssueReports.addAll(loaded.issues)
                }
                if (loaded.notifications.isNotEmpty()) {
                    sharedNotifications.clear()
                    sharedNotifications.addAll(loaded.notifications)
                }
            } else {
                saveDataToDisk()
            }
            isInitialized = true
        }
    }

    fun saveDataToDisk() {
        AppPersistenceManager.saveAll(
            feedPosts = sharedFeedPosts.toList(),
            examSchedules = sharedExamSchedules.toList(),
            examResults = sharedExamResults.toList(),
            driveDocs = sharedDriveDocs.toList(),
            driveFolders = sharedDriveFolders.toList(),
            courses = sharedCourses.toList(),
            roster = sharedClassRoster.toList(),
            issues = sharedIssueReports.toList(),
            notifications = sharedNotifications.toList()
        )
    }

    /**
     * Publishes a Principal Circular or Official Announcement immediately visible to all user logins.
     */
    fun publishPrincipalPost(
        title: String,
        content: String,
        authorRole: String = "Office of the Principal",
        authorName: String = "College Principal",
        tag: String = "Principal Official Circular"
    ): FeedPost {
        val newPost = FeedPost(
            id = "post_${System.currentTimeMillis()}",
            authorName = authorName,
            authorRole = authorRole,
            date = "Today",
            title = title,
            content = content,
            upvotes = 1,
            commentsCount = 0,
            isUpvoted = false,
            tag = tag
        )
        sharedFeedPosts.add(0, newPost)
        sharedNotifications.add(0, "New Circular: $title ($tag)")
        saveDataToDisk()
        return newPost
    }

    /**
     * Publishes a Faculty Study Material or PDF document visible to all students and faculty.
     */
    fun publishFacultyDocument(
        title: String,
        folderName: String = "Class Notes & Lectures",
        fileType: String = "PDF",
        sizeFormatted: String = "3.2 MB",
        uploadedBy: String = "Dr. P. V. Kishore (CSE)",
        subjectCode: String = "2339609"
    ): DriveDocument {
        val newDoc = DriveDocument(
            id = "doc_${System.currentTimeMillis()}",
            folderId = "fld_${folderName.lowercase().replace(" ", "_")}",
            folderName = folderName,
            title = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf",
            fileType = fileType,
            sizeBytes = 3200000,
            sizeFormatted = sizeFormatted,
            uploadedBy = uploadedBy,
            uploadedDate = "Today",
            downloadUrl = "https://ksrmce.ac.in/materials/${System.currentTimeMillis()}.pdf"
        )
        sharedDriveDocs.add(0, newDoc)
        sharedStudyDocs.add(0, StudyDocument(
            id = newDoc.id,
            subjectCode = subjectCode,
            title = newDoc.title,
            fileType = fileType,
            sizeText = sizeFormatted
        ))
        sharedNotifications.add(0, "New Study Material: ${newDoc.title} uploaded by $uploadedBy")
        saveDataToDisk()
        return newDoc
    }

    /**
     * Publishes an Exam Schedule/Timetable by HOD visible to all students and faculty.
     */
    fun publishExamSchedule(
        subjectCode: String,
        subjectName: String,
        examDate: String,
        timeSlot: String = "10:00 AM - 01:00 PM",
        examType: String = "Autonomous Semester Theory Exam",
        hallTicketEligibility: Boolean = true
    ): AndhraExamSchedule {
        val schedule = AndhraExamSchedule(
            subjectCode = subjectCode,
            subjectName = subjectName,
            examDate = examDate,
            timeSlot = timeSlot,
            examType = examType,
            hallTicketEligibility = hallTicketEligibility
        )
        sharedExamSchedules.add(0, schedule)
        sharedNotifications.add(0, "Exam Timetable Published: $subjectName on $examDate")
        saveDataToDisk()
        return schedule
    }

    /**
     * Publishes an Exam Result record by HOD visible to all students and faculty.
     */
    fun publishExamResult(
        subjectCode: String,
        subjectName: String,
        credits: Int = 4,
        internalMarks: Double,
        endTermMarks: Double,
        totalMarks: Double = internalMarks + endTermMarks,
        gradeLetter: String = if (totalMarks >= 90) "O" else if (totalMarks >= 80) "A+" else if (totalMarks >= 70) "A" else "B",
        gradePoints: Int = if (totalMarks >= 90) 10 else if (totalMarks >= 80) 9 else if (totalMarks >= 70) 8 else 7,
        resultStatus: String = if (totalMarks >= 40) "PASSED" else "FAILED"
    ): AndhraExamResult {
        val result = AndhraExamResult(
            subjectCode = subjectCode,
            subjectName = subjectName,
            credits = credits,
            internalMarks = internalMarks,
            endTermMarks = endTermMarks,
            totalMarks = totalMarks,
            gradeLetter = gradeLetter,
            gradePoints = gradePoints,
            resultStatus = resultStatus
        )
        sharedExamResults.add(0, result)
        sharedNotifications.add(0, "Results Published: $subjectName - Grade $gradeLetter ($resultStatus)")
        saveDataToDisk()
        return result
    }

    /**
     * Records attendance for an individual student (e.g. from Single Student Kiosk Face Recognition or Ledger).
     * Atomically updates the class roster status, student's subject course attendance count,
     * adds a campus notification, and persists to local encrypted disk.
     */
    fun recordSingleStudentAttendance(
        rollNumber: String,
        subjectCode: String,
        isPresent: Boolean = true
    ): Boolean {
        val newStatus = if (isPresent) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT

        // 1. Update Roster in real time
        for (i in 0 until sharedClassRoster.size) {
            if (sharedClassRoster[i].rollNumber.equals(rollNumber, ignoreCase = true)) {
                sharedClassRoster[i] = sharedClassRoster[i].copy(currentStatus = newStatus)
                break
            }
        }

        // 2. Update Student's courses if this is the student Sattu Penchala Prasad (or matching student)
        if (rollNumber.equals("239Y1A0501", ignoreCase = true) || rollNumber.isEmpty()) {
            for (i in 0 until sharedCourses.size) {
                val c = sharedCourses[i]
                if (subjectCode.contains(c.code) || subjectCode.contains(c.name, ignoreCase = true) || c.code.contains(subjectCode)) {
                    val newTotal = c.totalSessions + 1
                    val newAttended = if (isPresent) c.attendedSessions + 1 else c.attendedSessions
                    sharedCourses[i] = c.copy(
                        totalSessions = newTotal,
                        attendedSessions = newAttended
                    )
                }
            }
        }

        // 3. Dispatch notification & persist
        val statusText = if (isPresent) "PRESENT" else "ABSENT"
        val studentName = sharedClassRoster.find { it.rollNumber.equals(rollNumber, ignoreCase = true) }?.name ?: rollNumber
        sharedNotifications.add(0, "Biometric Kiosk: $studentName ($rollNumber) marked $statusText in $subjectCode")
        saveDataToDisk()
        return true
    }

    /**
     * Marks student present from biometric face scanner or kiosk, resolving rollNumber and subjectCode regardless of argument order.
     */
    fun markStudentPresent(param1: String, param2: String = ""): Boolean {
        val rollNumber = if (param1.startsWith("239Y") || param1.contains("1A05")) param1 else if (param2.startsWith("239Y") || param2.contains("1A05")) param2 else "239Y1A0501"
        val subject = if (rollNumber == param1) param2 else param1
        return recordSingleStudentAttendance(rollNumber, subject.ifEmpty { "2339609" }, isPresent = true)
    }

    fun markStudentAbsent(param1: String, param2: String = ""): Boolean {
        val rollNumber = if (param1.startsWith("239Y") || param1.contains("1A05")) param1 else if (param2.startsWith("239Y") || param2.contains("1A05")) param2 else "239Y1A0501"
        val subject = if (rollNumber == param1) param2 else param1
        return recordSingleStudentAttendance(rollNumber, subject.ifEmpty { "2339609" }, isPresent = false)
    }

    /**
     * Records an attendance session (from Group Face Scan, Single Kiosk, or Manual Ledger),
     * updating the roster, subject course totals & percentages, and persisting to disk.
     */
    fun recordAttendanceSession(
        subjectCode: String,
        attendanceMap: Map<String, Any?>
    ) {
        // 1. Update Class Roster
        for (i in 0 until sharedClassRoster.size) {
            val student = sharedClassRoster[i]
            val statusObj = attendanceMap[student.rollNumber]
            if (statusObj != null) {
                val str = statusObj.toString().uppercase()
                val newStatus = when (str) {
                    "TRUE", "PRESENT" -> AttendanceStatus.PRESENT
                    "LATE" -> AttendanceStatus.LATE
                    "ON_DUTY" -> AttendanceStatus.ON_DUTY
                    else -> AttendanceStatus.ABSENT
                }
                sharedClassRoster[i] = student.copy(currentStatus = newStatus)
            }
        }

        // 2. Update Student's courses
        val penchalaStatus = sharedClassRoster.find { it.rollNumber == "239Y1A0501" }?.currentStatus ?: AttendanceStatus.PRESENT
        val isPenchalaPresent = (penchalaStatus == AttendanceStatus.PRESENT || penchalaStatus == AttendanceStatus.LATE || penchalaStatus == AttendanceStatus.ON_DUTY)

        for (i in 0 until sharedCourses.size) {
            val c = sharedCourses[i]
            if (subjectCode.contains(c.code) || subjectCode.contains(c.name, ignoreCase = true) || c.code.contains(subjectCode)) {
                val newTotal = c.totalSessions + 1
                val newAttended = if (isPenchalaPresent) c.attendedSessions + 1 else c.attendedSessions
                sharedCourses[i] = c.copy(
                    totalSessions = newTotal,
                    attendedSessions = newAttended
                )
            }
        }

        val presentCount = attendanceMap.values.count {
            val s = it.toString().uppercase()
            s == "TRUE" || s == "PRESENT" || s == "LATE" || s == "ON_DUTY"
        }
        sharedNotifications.add(0, "Attendance Finalized: $subjectCode • $presentCount Students Present")
        saveDataToDisk()
    }

    fun enrollStudentFace(rollNumber: String) {
        for (i in 0 until sharedClassRoster.size) {
            if (sharedClassRoster[i].rollNumber.equals(rollNumber, ignoreCase = true)) {
                sharedClassRoster[i] = sharedClassRoster[i].copy(
                    faceFeaturesEnrolled = true,
                    faceDescriptor = "Vector_KNN_50_Samples"
                )
            }
        }
        sharedNotifications.add(0, "Biometric Enrollment: Student $rollNumber 50 Face Feature Vectors Enrolled")
        saveDataToDisk()
    }

    val defaultStudentProfile = UserProfile(
        id = "std_239Y1A0501",
        name = "Sattu Penchala Prasad",
        rollNumber = "239Y1A0501",
        programme = "B.Tech. - Computer Science and Engineering",
        department = "Department of Computer Science and Engineering",
        section = "4th year | Section B",
        yearSemester = "7th Semester (2026-27)",
        role = UserRole.STUDENT,
        email = "penchalaprasad70@gmail.com",
        phone = "+91 98765 43210",
        cgpa = 8.42,
        isFaceEnrolled = true,
        enrolledFaceSamplesCount = 50
    )

    val facultyProfile = UserProfile(
        id = "fac_cse_01",
        name = "Dr. P. V. Kishore",
        rollNumber = "EMP-CSE-102",
        programme = "Associate Professor & Head of Research",
        department = "Department of Computer Science and Engineering",
        section = "Faculty Mentor - CSE Sec B",
        yearSemester = "Academic Year 2026-27",
        role = UserRole.FACULTY,
        email = "pvk.cse@ksrmce.ac.in",
        phone = "+91 94401 23456",
        cgpa = 9.80,
        isFaceEnrolled = true,
        enrolledFaceSamplesCount = 50
    )

    val principalProfile = UserProfile(
        id = "admin_01",
        name = "College Principal",
        rollNumber = "ADMIN-KSRM-001",
        programme = "Office of the Principal",
        department = "KSRM College of Engineering, Kadapa",
        section = "Institutional Administration",
        yearSemester = "2026-27 Odd Semester",
        role = UserRole.PRINCIPAL,
        email = "principal@ksrmce.ac.in",
        phone = "+91 8562 201456",
        cgpa = 10.0,
        isFaceEnrolled = true,
        enrolledFaceSamplesCount = 50
    )

    val superAdminProfile = UserProfile(
        id = "super_admin_01",
        name = "Platform Super Admin",
        rollNumber = "SUPER-ADMIN-01",
        programme = "Platform-Wide Multi-Campus Governance",
        department = "AGENTRA AI Central Operations",
        section = "Global Platform Oversight",
        yearSemester = "Academic Year 2026-27",
        role = UserRole.SUPER_ADMIN,
        email = "superadmin@agentra.ai",
        phone = "+91 99000 11000",
        cgpa = 10.0,
        isFaceEnrolled = true,
        enrolledFaceSamplesCount = 50
    )

    val systemAdminProfile = UserProfile(
        id = "admin_super_01",
        name = "Sri K. Ravindra Reddy",
        rollNumber = "SYS-ADMIN-01",
        programme = "College Administration & Governance",
        department = "Directorate of Technical Education",
        section = "Multi-College Administration",
        yearSemester = "Academic Year 2026-27",
        role = UserRole.ADMIN,
        email = "admin.ksrmce@gmail.com",
        phone = "+91 98480 11223",
        cgpa = 10.0,
        isFaceEnrolled = true,
        enrolledFaceSamplesCount = 50
    )

    val hodProfile = UserProfile(
        id = "hod_cse_01",
        name = "Dr. N. Ramanjaneya Reddy",
        rollNumber = "HOD-CSE-001",
        programme = "Professor & Head of Department",
        department = "Department of Computer Science and Engineering",
        section = "CSE Department Administration",
        yearSemester = "Academic Year 2026-27",
        role = UserRole.HOD,
        email = "hod.cse@ksrmce.ac.in",
        phone = "+91 94402 98765",
        cgpa = 9.90,
        isFaceEnrolled = true,
        enrolledFaceSamplesCount = 50
    )

    val defaultParentProfile = UserProfile(
        id = "par_239Y1A0501",
        name = "P. Srinivasa Rao",
        rollNumber = "PAR-239Y1A0501",
        programme = "Parent / Guardian of Sattu Penchala Prasad",
        department = "Department of Computer Science and Engineering",
        section = "Child: B.Tech 4th Year CSE Sec B (239Y1A0501)",
        yearSemester = "Academic Year 2026-27",
        role = UserRole.PARENT,
        email = "parent.penchalaprasad@gmail.com",
        phone = "+91 94405 12345",
        cgpa = 8.42,
        isFaceEnrolled = false,
        enrolledFaceSamplesCount = 0
    )

    fun getTodayChildPeriodLogs(): List<ChildDailyPeriodLog> {
        return listOf(
            ChildDailyPeriodLog(
                periodNumber = 1,
                timeSlot = "09:15 AM - 10:15 AM",
                subjectName = "Cyber Security [2305701]",
                facultyName = "Dr. M. Ramanathan",
                status = AttendanceStatus.PRESENT,
                recordedTime = "09:18 AM",
                method = AttendanceMethod.FACE_RECOGNITION_KNN
            ),
            ChildDailyPeriodLog(
                periodNumber = 2,
                timeSlot = "10:15 AM - 11:15 AM",
                subjectName = "Organizational Behaviour [23HS772]",
                facultyName = "Prof. S. R. Varma",
                status = AttendanceStatus.PRESENT,
                recordedTime = "10:17 AM",
                method = AttendanceMethod.FACE_RECOGNITION_KNN
            ),
            ChildDailyPeriodLog(
                periodNumber = 3,
                timeSlot = "11:15 AM - 12:15 PM",
                subjectName = "Deep Learning [2339609]",
                facultyName = "Dr. P. V. Kishore",
                status = AttendanceStatus.PRESENT,
                recordedTime = "11:16 AM",
                method = AttendanceMethod.FACE_RECOGNITION_KNN
            ),
            ChildDailyPeriodLog(
                periodNumber = 4,
                timeSlot = "01:15 PM - 02:15 PM",
                subjectName = "Prompt Engineering Lab [2339751]",
                facultyName = "Mr. J. Vinay Kumar",
                status = AttendanceStatus.PRESENT,
                recordedTime = "01:19 PM",
                method = AttendanceMethod.QR_CODE
            ),
            ChildDailyPeriodLog(
                periodNumber = 5,
                timeSlot = "02:15 PM - 03:15 PM",
                subjectName = "Seminar / Library Research",
                facultyName = "Dr. P. V. Kishore",
                status = AttendanceStatus.ON_DUTY,
                recordedTime = "02:15 PM",
                method = AttendanceMethod.MANUAL_ENTRY
            )
        )
    }

    fun getParentLeaveApplications(): List<ParentLeaveApplication> {
        return listOf(
            ParentLeaveApplication(
                id = "LEAVE-2026-081",
                childName = "Sattu Penchala Prasad",
                rollNumber = "239Y1A0501",
                startDate = "18 Sep 2026",
                endDate = "19 Sep 2026",
                reasonCategory = "Medical Leave (Viral Fever)",
                explanation = "Student was undergoing outpatient treatment. Doctor prescription submitted.",
                medicalProofAttached = true,
                status = "Condonation Granted"
            ),
            ParentLeaveApplication(
                id = "LEAVE-2026-112",
                childName = "Sattu Penchala Prasad",
                rollNumber = "239Y1A0501",
                startDate = "02 Oct 2026",
                endDate = "03 Oct 2026",
                reasonCategory = "Family Event / Festival",
                explanation = "Attended family ceremony with prior notification.",
                medicalProofAttached = false,
                status = "Approved by Mentor"
            )
        )
    }

    fun getMentorConversation(): List<MentorDirectMessage> {
        return listOf(
            MentorDirectMessage(
                id = "m1",
                senderName = "Dr. P. V. Kishore (Mentor)",
                messageText = "Namaste Sri Srinivasa Rao garu. Penchala Prasad is performing well in Deep Learning with 80.15% overall attendance. Please ensure he does not miss Cyber Security classes this month.",
                timestamp = "Yesterday, 04:30 PM",
                isFromMentor = true
            ),
            MentorDirectMessage(
                id = "m2",
                senderName = "P. Srinivasa Rao (Parent)",
                messageText = "Thank you sir for updating us. We are monitoring his study routine at home. We will ensure he attends all Cyber Security lectures without fail.",
                timestamp = "Yesterday, 06:15 PM",
                isFromMentor = false
            ),
            MentorDirectMessage(
                id = "m3",
                senderName = "Dr. P. V. Kishore (Mentor)",
                messageText = "Excellent. The Mid-2 internal assessments commence next month. His current CGPA is 8.42 and on track for First Class with Distinction.",
                timestamp = "Today, 10:00 AM",
                isFromMentor = true
            )
        )
    }

    fun getInitialCourses(): List<SubjectCourse> {
        return listOf(
            SubjectCourse(
                code = "2305701",
                name = "Cyber Security",
                courseType = "LECTURE",
                sectionTag = "B.TECH - CSE B/S 7th Sem 2026-27",
                facultyName = "Dr. M. Ramanathan",
                totalSessions = 48,
                attendedSessions = 36, // 75.0%
                credits = 3,
                x1 = 7.5, x2 = 8.0, x3 = 7.0, x4 = 6.5, x5 = 8.0, x6 = 7.5, x7 = 29.0
            ),
            SubjectCourse(
                code = "23HS772",
                name = "Organizational Behaviour",
                courseType = "LECTURE",
                sectionTag = "B.TECH-7th Sem 2026-27 -(OE IV_OB-23HS772) - Sec C",
                facultyName = "Prof. S. R. Varma",
                totalSessions = 36,
                attendedSessions = 32, // 88.89%
                credits = 3,
                x1 = 9.0, x2 = 8.5, x3 = 9.0, x4 = 8.0, x5 = 9.5, x6 = 9.0, x7 = 35.0
            ),
            SubjectCourse(
                code = "2339609",
                name = "Deep Learning",
                courseType = "LECTURE",
                sectionTag = "B.TECH - CSE B/S 7th Sem 2026-27",
                facultyName = "Dr. P. V. Kishore",
                totalSessions = 50,
                attendedSessions = 38, // 76.0%
                credits = 4,
                x1 = 8.0, x2 = 7.5, x3 = 8.5, x4 = 7.0, x5 = 8.0, x6 = 8.0, x7 = 32.0
            ),
            SubjectCourse(
                code = "2339751",
                name = "Prompt Engineering",
                courseType = "PRACTICAL",
                sectionTag = "B.TECH - CSE B/S 7th Sem 2026-27",
                facultyName = "Mr. J. Vinay Kumar",
                totalSessions = 36,
                attendedSessions = 36, // 100.0%
                credits = 2,
                x1 = 10.0, x2 = 10.0, x3 = 10.0, x4 = 9.5, x5 = 10.0, x6 = 10.0, x7 = 39.0
            ),
            SubjectCourse(
                code = "23HS763",
                name = "Business Ethics and Corporate Governance",
                courseType = "LECTURE",
                sectionTag = "B.TECH-7th Sem 2026-27 -(OE III BECG_23HS763) - Sec C",
                facultyName = "Dr. A. Lakshmi Devi",
                totalSessions = 37,
                attendedSessions = 33, // 89.19%
                credits = 3,
                x1 = 8.5, x2 = 9.0, x3 = 8.0, x4 = 8.5, x5 = 9.0, x6 = 8.0, x7 = 34.0
            ),
            SubjectCourse(
                code = "2332604",
                name = "Cloud Computing",
                courseType = "LECTURE",
                sectionTag = "B.TECH - CSE B/S 7th Sem 2026-27",
                facultyName = "Dr. K. Srinivas",
                totalSessions = 42,
                attendedSessions = 33, // 78.57%
                credits = 3,
                x1 = 8.0, x2 = 7.0, x3 = 7.5, x4 = 8.0, x5 = 7.5, x6 = 8.0, x7 = 30.0
            ),
            SubjectCourse(
                code = "23HS701",
                name = "Gender Sensitization",
                courseType = "LECTURE",
                sectionTag = "B.TECH - CSE B/S 7th Sem 2026-27",
                facultyName = "Mrs. B. Sunitha",
                totalSessions = 20,
                attendedSessions = 17, // 85.0%
                credits = 2,
                x1 = 9.0, x2 = 8.0, x3 = 8.5, x4 = 8.0, x5 = 9.0, x6 = 8.5, x7 = 33.0
            )
        )
    }

    fun getInitialFeedPosts(): List<FeedPost> {
        return listOf(
            FeedPost(
                id = "post_1",
                authorName = "Mr. J. Vinay kumar",
                authorRole = "Assistant Professor, CSE",
                date = "05 Oct",
                title = "Happy World Teacher's Day & Agentic AI Symposium",
                content = "On this World Teacher's Day, we celebrate the tireless dedication of our mentors shaping the innovators of tomorrow. Special guest lecture on Agentic AI Systems in the Main Auditorium tomorrow at 10:30 AM.",
                upvotes = 24,
                commentsCount = 2,
                isUpvoted = true,
                tag = "KSRMCE Official",
                imageUrl = "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=800",
                comments = mutableListOf(
                    FeedComment(authorName = "P. Rajesh", authorRole = "Student (CSE)", text = "Looking forward to the Agentic AI lecture!"),
                    FeedComment(authorName = "Dr. M. Sreenivasulu", authorRole = "HOD CSE", text = "Attendance is compulsory for all 3rd and 4th year students.")
                )
            ),
            FeedPost(
                id = "post_2",
                authorName = "KSRMCE Examination Cell",
                authorRole = "Dean of Evaluations",
                date = "15 Sept",
                title = "Notification: Mid-Term Examination Schedule",
                content = "The ODD Semester Mid-2 Assessment timetables for 4th year B.Tech are released. Component marks x1 through x6 must be submitted by faculty via ScholarBot by end of this week.",
                upvotes = 18,
                commentsCount = 1,
                isUpvoted = false,
                tag = "Academics",
                imageUrl = "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800",
                comments = mutableListOf(
                    FeedComment(authorName = "K. Ananya", authorRole = "Student (ECE)", text = "Where can we download the hall ticket?")
                )
            ),
            FeedPost(
                id = "post_3",
                authorName = "Training & Placement Cell",
                authorRole = "Placement Officer",
                date = "10 Oct",
                title = "Campus Recruitment Drive - Tier-1 Tech Companies",
                content = "Pre-placement training sessions for Deep Learning, Prompt Engineering and Cloud architectures scheduled from Monday. Minimum 75% attendance mandatory for registration eligibility.",
                upvotes = 35,
                commentsCount = 2,
                isUpvoted = false,
                tag = "Placements",
                imageUrl = "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800",
                comments = mutableListOf(
                    FeedComment(authorName = "S. Penchala Prasad", authorRole = "Student (CSE)", text = "Is resume verification completed by mentors?"),
                    FeedComment(authorName = "Placement Officer", authorRole = "Faculty Coordinator", text = "Yes, please check your placement portal dashboard.")
                )
            )
        )
    }

    fun getStudyDocuments(): List<StudyDocument> {
        return listOf(
            StudyDocument("doc_1", "2339609", "Deep Learning - Unit 1: Perceptrons & Backpropagation.pdf", "PDF", "4.8 MB"),
            StudyDocument("doc_2", "2339609", "Multi-Agent Systems & Face Recognition Architectures.pdf", "PDF", "6.2 MB"),
            StudyDocument("doc_3", "2305701", "Cyber Security - Cryptography & Network Security Lab Manual.pdf", "PDF", "3.4 MB"),
            StudyDocument("doc_4", "2339751", "Prompt Engineering - In-Context Learning & Few-Shot Templates.pdf", "PDF", "2.1 MB"),
            StudyDocument("doc_5", "2332604", "Cloud Computing - AWS & Firebase Serverless Architecture.pdf", "PDF", "5.1 MB"),
            StudyDocument("doc_6", "23HS763", "Corporate Governance Syllabus & Case Studies 2026-27.pdf", "PDF", "1.9 MB")
        )
    }

    fun getGroupChatMessages(): List<GroupChatMessage> {
        return listOf(
            GroupChatMessage("m1", "Bojja Prasad Reddy", "Has everyone reviewed the Deep Learning Haar cascade dataset?", "10:14 AM"),
            GroupChatMessage("m2", "Chaganti Mythili", "Yes, the KNN block cross-validation reached 90.7% on the time block folds.", "10:16 AM"),
            GroupChatMessage("m3", "Chatakondu Subbarayudu", "Don't forget tomorrow's mid-exam component submission for Prompt Engineering.", "10:20 AM"),
            GroupChatMessage("m4", "SATTU PENCHALA PRASAD", "I ran the AuraCampus 1-month recovery plan, my attendance is projected to reach 84.5% if I maintain 0 absences.", "10:22 AM")
        )
    }

    val studentSettings = StudentSettings()
    val facultySettings = FacultySettings()
    val principalSettings = PrincipalSettings()

    fun getInitialFacultyMembers(): List<FacultyMember> {
        return listOf(
            FacultyMember(
                id = "fac_1",
                employeeId = "KSRM-CSE-01",
                name = "Dr. P. V. Kishore",
                designation = "Associate Professor",
                department = "CSE",
                assignedSubjects = listOf("Deep Learning [2339609]", "Prompt Engineering [2339751]"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true,
                canIssueCondonation = true
            ),
            FacultyMember(
                id = "fac_2",
                employeeId = "KSRM-CSE-02",
                name = "Mr. J. Vinay Kumar",
                designation = "Assistant Professor",
                department = "CSE",
                assignedSubjects = listOf("Prompt Engineering [2339751]", "Cloud Computing [2332604]"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true,
                canIssueCondonation = false
            ),
            FacultyMember(
                id = "fac_3",
                employeeId = "KSRM-CSE-03",
                name = "Dr. M. Ramanathan",
                designation = "Professor",
                department = "CSE",
                assignedSubjects = listOf("Cyber Security [2305701]"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true,
                canIssueCondonation = false
            ),
            FacultyMember(
                id = "fac_4",
                employeeId = "KSRM-HBS-01",
                name = "Prof. S. R. Varma",
                designation = "Professor",
                department = "Humanities & Sciences",
                assignedSubjects = listOf("Organizational Behaviour [23HS772]"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true,
                canIssueCondonation = false
            )
        )
    }

    fun getAndhraExamSchedules(): List<AndhraExamSchedule> {
        return listOf(
            AndhraExamSchedule(
                subjectCode = "2305701",
                subjectName = "Cyber Security",
                examDate = "18 Nov 2026",
                timeSlot = "10:00 AM - 01:00 PM",
                examType = "Autonomous End-Semester Theory",
                hallTicketEligibility = true
            ),
            AndhraExamSchedule(
                subjectCode = "2339609",
                subjectName = "Deep Learning & Neural Networks",
                examDate = "21 Nov 2026",
                timeSlot = "10:00 AM - 01:00 PM",
                examType = "Autonomous End-Semester Theory",
                hallTicketEligibility = true
            ),
            AndhraExamSchedule(
                subjectCode = "23HS772",
                subjectName = "Organizational Behaviour",
                examDate = "24 Nov 2026",
                timeSlot = "10:00 AM - 01:00 PM",
                examType = "Autonomous End-Semester Theory",
                hallTicketEligibility = true
            ),
            AndhraExamSchedule(
                subjectCode = "2339751",
                subjectName = "Prompt Engineering & GenAI Lab",
                examDate = "27 Nov 2026",
                timeSlot = "02:00 PM - 05:00 PM",
                examType = "Autonomous Practical Lab Exam",
                hallTicketEligibility = true
            ),
            AndhraExamSchedule(
                subjectCode = "2332604",
                subjectName = "Cloud Computing & DevOps",
                examDate = "30 Nov 2026",
                timeSlot = "10:00 AM - 01:00 PM",
                examType = "Autonomous End-Semester Theory",
                hallTicketEligibility = true
            )
        )
    }

    fun getAndhraExamResults(): List<AndhraExamResult> {
        return listOf(
            AndhraExamResult(
                subjectCode = "2305701",
                subjectName = "Cyber Security",
                credits = 3,
                internalMarks = 26.5,
                endTermMarks = 58.0,
                totalMarks = 84.5,
                gradeLetter = "A+",
                gradePoints = 9,
                resultStatus = "PASSED"
            ),
            AndhraExamResult(
                subjectCode = "2339609",
                subjectName = "Deep Learning & Neural Networks",
                credits = 4,
                internalMarks = 28.0,
                endTermMarks = 61.5,
                totalMarks = 89.5,
                gradeLetter = "O",
                gradePoints = 10,
                resultStatus = "PASSED"
            ),
            AndhraExamResult(
                subjectCode = "23HS772",
                subjectName = "Organizational Behaviour",
                credits = 3,
                internalMarks = 25.0,
                endTermMarks = 54.0,
                totalMarks = 79.0,
                gradeLetter = "A",
                gradePoints = 8,
                resultStatus = "PASSED"
            ),
            AndhraExamResult(
                subjectCode = "2339751",
                subjectName = "Prompt Engineering Lab",
                credits = 2,
                internalMarks = 29.0,
                endTermMarks = 66.0,
                totalMarks = 95.0,
                gradeLetter = "O",
                gradePoints = 10,
                resultStatus = "PASSED"
            ),
            AndhraExamResult(
                subjectCode = "2332604",
                subjectName = "Cloud Computing",
                credits = 3,
                internalMarks = 24.5,
                endTermMarks = 56.0,
                totalMarks = 80.5,
                gradeLetter = "A",
                gradePoints = 8,
                resultStatus = "PASSED"
            )
        )
    }

    fun getStudentActivities(): List<StudentActivityEvent> {
        return listOf(
            StudentActivityEvent(
                id = "act_sih_2026",
                title = "Smart India Hackathon (SIH) 2026 - AI Hardware & Software Edition",
                category = "National Level Hackathon",
                organizer = "Ministry of Education Innovation Cell (MIC) & AICTE",
                eventDate = "04-06 Nov 2026",
                projectTitle = "Agentic AI Autonomous Campus ERP & Biometric Governance",
                innovationScore = 9.8,
                odCreditsGranted = 6,
                status = "Grand Finale Shortlisted • OD Sanctioned"
            ),
            StudentActivityEvent(
                id = "act_jntua_expo",
                title = "JNTUA State Level Technical Paper & Project Symposium",
                category = "Project Expo",
                organizer = "JNTU Anantapur College of Engineering",
                eventDate = "22 Oct 2026",
                projectTitle = "Decentralized Blockchain Attendance Ledger with KNN Face Recognition",
                innovationScore = 9.5,
                odCreditsGranted = 4,
                status = "First Prize Winner (₹15,000) • OD Verified"
            ),
            StudentActivityEvent(
                id = "act_ieee_kadapa",
                title = "IEEE Hyderabad Section Student Congress & Hack-a-bit",
                category = "Hackathon & Workshop",
                organizer = "IEEE Student Branch KSRMCE",
                eventDate = "12 Oct 2026",
                projectTitle = "Predictive Academic Risk Early Warning System with LightGBM",
                innovationScore = 9.2,
                odCreditsGranted = 4,
                status = "Verified & OD Approved"
            )
        )
    }

    val hodSettings = HODSettings()
    val adminSettings = AdminSettings()

    fun getInitialColleges(): List<CollegeOrganization> {
        return listOf(
            CollegeOrganization(
                id = "col_ksrm_01",
                name = "KSRM College of Engineering (Autonomous)",
                code = "KSRMCE",
                city = "Kadapa, Andhra Pradesh",
                adminEmail = "admin.ksrmce@gmail.com",
                departmentsCount = 7,
                studentsCount = 2850,
                status = "Active & Verified"
            ),
            CollegeOrganization(
                id = "col_klm_02",
                name = "K.L.M. College of Engineering for Women",
                code = "KLMCEW",
                city = "Kadapa, Andhra Pradesh",
                adminEmail = "admin.klm@ksrmce.ac.in",
                departmentsCount = 4,
                studentsCount = 1420,
                status = "Active"
            ),
            CollegeOrganization(
                id = "col_svec_03",
                name = "Sree Vidyanikethan Engineering College",
                code = "SVEC",
                city = "Tirupati, Andhra Pradesh",
                adminEmail = "governance@svec.edu.in",
                departmentsCount = 8,
                studentsCount = 3600,
                status = "Active"
            )
        )
    }

    fun getInitialDepartments(): List<DepartmentInfo> {
        return listOf(
            DepartmentInfo(
                id = "dept_cse",
                collegeId = "col_ksrm_01",
                name = "Computer Science and Engineering",
                code = "CSE",
                hodName = "Dr. N. Ramanjaneya Reddy",
                hodEmail = "hod.cse@ksrmce.ac.in",
                facultyCount = 28,
                studentsCount = 480,
                avgAttendance = 81.4
            ),
            DepartmentInfo(
                id = "dept_ece",
                collegeId = "col_ksrm_01",
                name = "Electronics and Communication Engineering",
                code = "ECE",
                hodName = "Dr. M. Venkata Narayana",
                hodEmail = "hod.ece@ksrmce.ac.in",
                facultyCount = 24,
                studentsCount = 420,
                avgAttendance = 78.9
            ),
            DepartmentInfo(
                id = "dept_eee",
                collegeId = "col_ksrm_01",
                name = "Electrical and Electronics Engineering",
                code = "EEE",
                hodName = "Dr. C. Gangi Reddy",
                hodEmail = "hod.eee@ksrmce.ac.in",
                facultyCount = 18,
                studentsCount = 310,
                avgAttendance = 82.1
            ),
            DepartmentInfo(
                id = "dept_mech",
                collegeId = "col_ksrm_01",
                name = "Mechanical Engineering",
                code = "MECH",
                hodName = "Dr. D. Ravikanth",
                hodEmail = "hod.mech@ksrmce.ac.in",
                facultyCount = 19,
                studentsCount = 290,
                avgAttendance = 76.2
            ),
            DepartmentInfo(
                id = "dept_civil",
                collegeId = "col_ksrm_01",
                name = "Civil Engineering",
                code = "CIVIL",
                hodName = "Dr. V. Giridhar",
                hodEmail = "hod.civil@ksrmce.ac.in",
                facultyCount = 16,
                studentsCount = 260,
                avgAttendance = 77.8
            ),
            DepartmentInfo(
                id = "dept_ai_ds",
                collegeId = "col_ksrm_01",
                name = "Artificial Intelligence & Data Science",
                code = "AI & DS",
                hodName = "Dr. S. Khaja Khizar",
                hodEmail = "hod.aids@ksrmce.ac.in",
                facultyCount = 14,
                studentsCount = 240,
                avgAttendance = 84.5
            )
        )
    }

    fun getInitialDriveFolders(): List<DriveFolder> {
        return listOf(
            DriveFolder("fld_notes", "Class Notes & Lectures", "10 Oct 2026", 0xFF10B981, 4),
            DriveFolder("fld_qb", "Previous Question Banks", "08 Oct 2026", 0xFF6366F1, 3),
            DriveFolder("fld_lab", "Lab Manuals & Codes", "05 Oct 2026", 0xFFF59E0B, 5),
            DriveFolder("fld_syl", "Syllabus & Regulations", "01 Oct 2026", 0xFFEC4899, 2),
            DriveFolder("fld_assign", "Student Assignment Submissions", "12 Oct 2026", 0xFF06B6D4, 6)
        )
    }

    fun getInitialDriveDocuments(): List<DriveDocument> {
        return listOf(
            DriveDocument(
                id = "doc_dl_unit1",
                folderId = "fld_notes",
                folderName = "Class Notes & Lectures",
                title = "Deep Learning - Unit 1 Convolutional Networks & ResNet.pdf",
                fileType = "PDF",
                sizeBytes = 2450000,
                sizeFormatted = "2.4 MB",
                uploadedBy = "Dr. P.V. Kishore (CSE)",
                uploadedDate = "10 Oct 2026",
                downloadUrl = "https://ksrmce.ac.in/notes/dl_unit1.pdf"
            ),
            DriveDocument(
                id = "doc_cyber_notes",
                folderId = "fld_notes",
                folderName = "Class Notes & Lectures",
                title = "Cyber Security - Cryptography & AES / RSA Implementation.pdf",
                fileType = "PDF",
                sizeBytes = 3850000,
                sizeFormatted = "3.8 MB",
                uploadedBy = "Dr. M. Ramanathan",
                uploadedDate = "09 Oct 2026",
                downloadUrl = "https://ksrmce.ac.in/notes/cyber_aes.pdf"
            ),
            DriveDocument(
                id = "doc_mid2_qb",
                folderId = "fld_qb",
                folderName = "Previous Question Banks",
                title = "Mid-2 Examination Autonomous CSE Question Bank (2025-26).pdf",
                fileType = "PDF",
                sizeBytes = 1650000,
                sizeFormatted = "1.6 MB",
                uploadedBy = "Academic Exam Cell",
                uploadedDate = "08 Oct 2026",
                downloadUrl = "https://ksrmce.ac.in/exams/mid2_qb.pdf"
            ),
            DriveDocument(
                id = "doc_prompt_lab",
                folderId = "fld_lab",
                folderName = "Lab Manuals & Codes",
                title = "Prompt Engineering Lab Experiments Manual & Test Cases.pdf",
                fileType = "PDF",
                sizeBytes = 4120000,
                sizeFormatted = "4.1 MB",
                uploadedBy = "Mr. J. Vinay Kumar",
                uploadedDate = "05 Oct 2026",
                downloadUrl = "https://ksrmce.ac.in/labs/prompt_lab.pdf"
            )
        )
    }

    fun getInitialIssueReports(): List<AppIssueReport> {
        return listOf(
            AppIssueReport(
                id = "iss_01",
                title = "QR Scanner Camera Lens Detection Delay",
                description = "Google Scanner format compatibility resolved with high-density standard ISO matrix format.",
                locationScreen = "QR Attendance Suite",
                reportedByRole = "Faculty CSE",
                severity = "Medium",
                isResolved = true,
                dateReported = "13 Oct 2026"
            ),
            AppIssueReport(
                id = "iss_02",
                title = "ECE Dept 3rd Year Attendance Threshold Sync",
                description = "Automated Firestore live sync configured for 420 ECE students under JNTUA regulation.",
                locationScreen = "HOD Executive Portal",
                reportedByRole = "HOD ECE",
                severity = "Low",
                isResolved = false,
                dateReported = "12 Oct 2026"
            ),
            AppIssueReport(
                id = "iss_03",
                title = "Proxy Check-In Prevention Policy",
                description = "Self QR check-in disabled for students; biometric group camera verification mandated.",
                locationScreen = "Student Portal Security",
                reportedByRole = "Principal Office",
                severity = "High",
                isResolved = true,
                dateReported = "11 Oct 2026"
            )
        )
    }

    fun getFacultyDirectory(): List<FacultyMember> {
        return listOf(
            FacultyMember(
                id = "fac_01",
                employeeId = "EMP-CSE-102",
                name = "Dr. P. V. Kishore",
                designation = "Associate Professor & Mentor",
                department = "CSE",
                assignedSubjects = listOf("Deep Learning (R20CS701)", "Natural Language Processing (R20CS702)"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true
            ),
            FacultyMember(
                id = "fac_02",
                employeeId = "EMP-CSE-108",
                name = "Prof. S. Khaja Hussain",
                designation = "Assistant Professor",
                department = "CSE",
                assignedSubjects = listOf("Cloud Computing Architecture (R20CS703)"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true
            ),
            FacultyMember(
                id = "fac_03",
                employeeId = "EMP-CSE-114",
                name = "Dr. M. Venkata Ramana",
                designation = "Professor & Head - AI Labs",
                department = "CSE",
                assignedSubjects = listOf("Generative AI & LLM Systems (R20CS705)"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true
            ),
            FacultyMember(
                id = "fac_04",
                employeeId = "EMP-CSE-120",
                name = "Mrs. B. Swathi",
                designation = "Assistant Professor",
                department = "CSE",
                assignedSubjects = listOf("Software Engineering Lab (R20CS708)"),
                canTakeFaceAttendance = true,
                canEditManualAttendance = true,
                canEnterInternalMarks = true
            )
        )
    }

    fun getStudentsBelow75Attendance(): List<EnrolledStudent> {
        return listOf(
            EnrolledStudent("19", "239Y1A05D5", "D. Navya", "Sec B", "CSE", currentStatus = AttendanceStatus.ABSENT),
            EnrolledStudent("23", "239Y1A05D9", "J. Thanmai", "Sec B", "CSE", currentStatus = AttendanceStatus.ABSENT),
            EnrolledStudent("31", "239Y1A05E7", "T. Divya", "Sec B", "CSE", currentStatus = AttendanceStatus.ABSENT),
            EnrolledStudent("37", "239Y1A05F3", "D. Jaswanth", "Sec B", "CSE", currentStatus = AttendanceStatus.ABSENT),
            EnrolledStudent("44", "239Y1A05G0", "M. Sneha", "Sec B", "CSE", currentStatus = AttendanceStatus.ABSENT)
        )
    }

    fun getFullClassRoster(): List<EnrolledStudent> {
        val names = listOf(
            "Sattu Penchala Prasad", "B. Sai Penchala", "Chatakondu Subbarayudu",
            "Shaik Mohammad Abdur Rahman", "Bojja Prasad Reddy", "V. Dinesh Kumar",
            "K. Sai Praneeth", "M. Tejaswini", "G. Harshitha", "B. Anusha",
            "D. Navya", "J. Thanmai", "T. Divya", "D. Jaswanth", "M. Sneha",
            "A. Karthik", "K. Upendra", "P. Akhil", "L. Varshini", "C. Harish",
            "N. Venkata Sai", "P. Sravani", "R. Bhanu Prakash", "M. Charan Teja", "S. Khaja Hussain",
            "T. Rajesh", "V. Lavanya", "B. Pavan Kumar", "G. Sai Kumar", "K. Manohar",
            "M. Deepa", "P. Hemanth", "S. Ayesha", "T. Mahesh", "V. Sandeep",
            "A. Vinay", "B. Sowmya", "C. Rohith", "D. Manoj", "E. Keerthi",
            "G. Prudhvi", "H. Tharun", "J. Archana", "K. Bharath", "L. Madhu",
            "M. Kalyan", "N. Swetha", "P. Rakesh", "S. Jagadeesh", "T. Naresh"
        )
        val roster = mutableListOf<EnrolledStudent>()
        for (i in names.indices) {
            val rollSuffix = when {
                i < 3 -> "B" + (7 + i)
                i < 13 -> "C" + (i - 3)
                i < 23 -> "D" + (i - 13)
                i < 33 -> "E" + (i - 23)
                i < 43 -> "F" + (i - 33)
                else -> "G" + (i - 43)
            }
            val studentRoll = if (i == 0) "239Y1A0501" else "239Y1A05$rollSuffix"
            val status = when {
                i in listOf(10, 11, 12, 14, 17) -> AttendanceStatus.ABSENT
                i in listOf(13, 25) -> AttendanceStatus.LATE
                else -> AttendanceStatus.PRESENT
            }
            roster.add(
                EnrolledStudent(
                    id = String.format("%02d", i + 1),
                    rollNumber = studentRoll,
                    name = names[i],
                    section = "Sec B",
                    department = "CSE",
                    faceFeaturesEnrolled = true,
                    faceDescriptor = "Vector_KNN_${1000 + i}",
                    currentStatus = status
                )
            )
        }
        return roster
    }

    fun getInstitutionDepartments(): List<DepartmentInfo> {
        return listOf(
            DepartmentInfo("dept_01", "col_ksrm_01", "Computer Science & Engineering", "CSE", "Dr. P. V. Kishore", "hod.cse@ksrmce.ac.in", 42, 780, 81.2),
            DepartmentInfo("dept_02", "col_ksrm_01", "Electronics & Communication Engineering", "ECE", "Dr. G. Hemalatha", "hod.ece@ksrmce.ac.in", 36, 620, 78.4),
            DepartmentInfo("dept_03", "col_ksrm_01", "Mechanical Engineering", "MECH", "Dr. D. Ravikanth", "hod.mech@ksrmce.ac.in", 28, 480, 74.2),
            DepartmentInfo("dept_04", "col_ksrm_01", "Civil Engineering", "CIVIL", "Dr. N. Ramanjaneyulu", "hod.civil@ksrmce.ac.in", 26, 440, 76.8),
            DepartmentInfo("dept_05", "col_ksrm_01", "Artificial Intelligence & Data Science", "AI&DS", "Dr. M. Sreenivasulu", "hod.aids@ksrmce.ac.in", 24, 460, 83.5),
            DepartmentInfo("dept_06", "col_ksrm_01", "Electrical & Electronics Engineering", "EEE", "Dr. K. Amaresh", "hod.eee@ksrmce.ac.in", 25, 460, 77.1)
        )
    }

    fun getDepartmentComparison(): List<Triple<String, Float, Double>> {
        return if (sharedDepartments.isNotEmpty()) {
            sharedDepartments.map { dept ->
                Triple(dept.code, dept.avgAttendance.toFloat(), 8.2)
            }
        } else {
            listOf(
                Triple("CSE", 81.2f, 8.42),
                Triple("ECE", 78.4f, 7.95),
                Triple("MECH", 74.2f, 7.15),
                Triple("CIVIL", 76.8f, 7.30),
                Triple("AI&DS", 83.5f, 8.65),
                Triple("EEE", 77.1f, 7.55)
            )
        }
    }

    fun getInstitutionAttendanceTrends(): List<Pair<String, Float>> {
        return listOf(
            Pair("Jun", 84.5f),
            Pair("Jul", 82.0f),
            Pair("Aug", 79.2f),
            Pair("Sep", 81.6f),
            Pair("Oct", 83.4f)
        )
    }

    fun getWeeklyTimetable(): List<TimetableSlot> {
        return listOf(
            TimetableSlot("tt_1", "Monday", 1, "09:00 - 10:00 AM", "2305701", "Cyber Security", "Dr. M. Ramanathan", "Room 401"),
            TimetableSlot("tt_2", "Monday", 2, "10:00 - 11:00 AM", "2339609", "Deep Learning", "Dr. P. V. Kishore", "Lab 402"),
            TimetableSlot("tt_3", "Monday", 3, "11:15 - 12:15 PM", "2332604", "Cloud Computing", "Dr. K. Srinivas", "Room 401"),
            TimetableSlot("tt_4", "Tuesday", 1, "09:00 - 10:00 AM", "23HS772", "Organizational Behaviour", "Prof. S. R. Varma", "Room 403"),
            TimetableSlot("tt_5", "Tuesday", 2, "10:00 - 11:00 AM", "2339751", "Prompt Engineering Lab", "Mr. J. Vinay Kumar", "AI Lab 1"),
            TimetableSlot("tt_6", "Wednesday", 1, "09:00 - 10:00 AM", "2339609", "Deep Learning", "Dr. P. V. Kishore", "Room 401"),
            TimetableSlot("tt_7", "Wednesday", 2, "10:00 - 11:00 AM", "2305701", "Cyber Security", "Dr. M. Ramanathan", "Room 401"),
            TimetableSlot("tt_8", "Thursday", 1, "09:00 - 10:00 AM", "23HS763", "Business Ethics", "Dr. A. Lakshmi Devi", "Room 402"),
            TimetableSlot("tt_9", "Friday", 1, "09:00 - 10:00 AM", "2332604", "Cloud Computing", "Dr. K. Srinivas", "Room 401")
        )
    }

    fun getInitialAssignments(): List<CampusAssignment> {
        val now = System.currentTimeMillis()
        val twoDaysLater = now + (2 * 24 * 60 * 60 * 1000)
        val passedYesterday = now - (1 * 24 * 60 * 60 * 1000)
        return listOf(
            CampusAssignment(
                id = "asgn_dl_01",
                collegeId = "col_ksrm_01",
                department = "CSE",
                year = 4,
                section = "Sec B",
                subjectCode = "2339609",
                subjectName = "Deep Learning",
                title = "Assignment 1: ResNet vs VGG Architecture & Backprop",
                description = "Derive backpropagation equations for CNN pooling layers and summarize residual connections. Upload typed/handwritten scanned PDF.",
                facultyName = "Dr. P. V. Kishore",
                deadlineTimestamp = twoDaysLater,
                deadlineFormatted = "Tomorrow, 11:59 PM",
                questionPdfName = "DL_Assignment_01_Questions.pdf",
                maxMarks = 10,
                isClosed = false
            ),
            CampusAssignment(
                id = "asgn_cyber_02",
                collegeId = "col_ksrm_01",
                department = "CSE",
                year = 4,
                section = "Sec B",
                subjectCode = "2305701",
                subjectName = "Cyber Security",
                title = "Assignment 2: AES Key Expansion & RSA Proof",
                description = "Submit comprehensive analysis of AES MixColumns transformation with working numerical example.",
                facultyName = "Dr. M. Ramanathan",
                deadlineTimestamp = twoDaysLater + (24 * 60 * 60 * 1000),
                deadlineFormatted = "In 3 Days, 05:00 PM",
                questionPdfName = "CyberSec_Assignment_02_Questions.pdf",
                maxMarks = 10,
                isClosed = false
            ),
            CampusAssignment(
                id = "asgn_cloud_03",
                collegeId = "col_ksrm_01",
                department = "CSE",
                year = 4,
                section = "Sec B",
                subjectCode = "2332604",
                subjectName = "Cloud Computing",
                title = "Assignment 3: Microservices & Kubernetes Deployment",
                description = "Document deployment architecture with YAML manifests and ingress routing.",
                facultyName = "Dr. K. Srinivas",
                deadlineTimestamp = passedYesterday,
                deadlineFormatted = "Yesterday (Closed)",
                questionPdfName = "Cloud_Assignment_03_Questions.pdf",
                maxMarks = 10,
                isClosed = true
            ),
            CampusAssignment(
                id = "asgn_ece_01",
                collegeId = "col_ksrm_01",
                department = "ECE",
                year = 3,
                section = "Sec A",
                subjectCode = "2304501",
                subjectName = "Digital Signal Processing",
                title = "DSP Lab Assignment 1: FFT Implementation",
                description = "Design 8-point radix-2 DIT FFT algorithm and compute twiddle factors.",
                facultyName = "Dr. P. V. Kishore",
                deadlineTimestamp = twoDaysLater,
                deadlineFormatted = "Tomorrow, 11:59 PM",
                questionPdfName = "DSP_Assignment_01_Questions.pdf",
                maxMarks = 10,
                isClosed = false
            )
        )
    }

    fun getInitialSubmissions(): List<AssignmentSubmission> {
        val now = System.currentTimeMillis()
        return listOf(
            AssignmentSubmission(
                id = "sub_01",
                assignmentId = "asgn_dl_01",
                studentId = "usr_student_01",
                studentName = "Sattu Penchala Prasad",
                rollNumber = "239Y1A0501",
                department = "CSE",
                year = 4,
                section = "Sec B",
                submittedPdfName = "239Y1A0501_DeepLearning_Asgn1.pdf",
                submissionTimestamp = now - 3600000,
                submissionDateFormatted = "Today at 02:15 PM",
                marksAwarded = 9.5,
                feedback = "Excellent derivation of residual skip connections and gradient flow.",
                isGraded = true
            ),
            AssignmentSubmission(
                id = "sub_02",
                assignmentId = "asgn_dl_01",
                studentId = "usr_student_02",
                studentName = "B. Sai Penchala",
                rollNumber = "239Y1A05B8",
                department = "CSE",
                year = 4,
                section = "Sec B",
                submittedPdfName = "239Y1A05B8_DeepLearning_Asgn1.pdf",
                submissionTimestamp = now - 7200000,
                submissionDateFormatted = "Today at 01:10 PM",
                marksAwarded = 8.0,
                feedback = "Good work, include more intermediate steps for pooling layer backprop.",
                isGraded = true
            ),
            AssignmentSubmission(
                id = "sub_03",
                assignmentId = "asgn_cyber_02",
                studentId = "usr_student_01",
                studentName = "Sattu Penchala Prasad",
                rollNumber = "239Y1A0501",
                department = "CSE",
                year = 4,
                section = "Sec B",
                submittedPdfName = "239Y1A0501_CyberSec_Asgn2.pdf",
                submissionTimestamp = now - 1800000,
                submissionDateFormatted = "Today at 03:30 PM",
                marksAwarded = null,
                feedback = "Pending Faculty Review",
                isGraded = false
            )
        )
    }

    fun getInitialCourseResources(): List<CourseResource> {
        return listOf(
            CourseResource("cr_1", "col_ksrm_01", "CSE", 4, "2339609", "Deep Learning", "Unit 1-4 Full Lecture Notes & CNN Slides", "Notes PDF", "Dr. P. V. Kishore", "Oct 12", "4.8 MB"),
            CourseResource("cr_2", "col_ksrm_01", "CSE", 4, "2305701", "Cyber Security", "Autonomous R23 Academic Syllabus & Outcomes", "Syllabus", "Dr. M. Ramanathan", "Oct 08", "1.1 MB"),
            CourseResource("cr_3", "col_ksrm_01", "CSE", 4, "2332604", "Cloud Computing", "Previous 5 Years Autonomous Exam Question Bank", "Question Bank", "Dr. K. Srinivas", "Oct 04", "3.2 MB"),
            CourseResource("cr_4", "col_ksrm_01", "CSE", 4, "2339751", "Prompt Engineering Lab", "Prompt Lab Experiments, Datasets & Code Snippets", "Lab Manual", "Mr. J. Vinay Kumar", "Oct 02", "5.1 MB"),
            CourseResource("cr_5", "col_ksrm_01", "AIML", 2, "2331301", "Data Structures & Algorithms", "DSA Lecture Notes, Binary Trees & Graphs", "Notes PDF", "Dr. S. Khaja Khizar", "Oct 10", "3.6 MB"),
            CourseResource("cr_6", "col_ksrm_01", "ECE", 3, "2304501", "Digital Signal Processing", "DSP Filter Design & MATLAB Lab Manual", "Lab Manual", "Dr. M. Venkata Narayana", "Oct 05", "4.0 MB")
        )
    }

    fun getInitialExamMarks(): List<ExamMarkRecord> {
        return listOf(
            ExamMarkRecord("em_1", "239Y1A0501", "2339609", "Deep Learning", mid1Marks = 28.5, mid2Marks = 27.0, assignmentMarks = 9.5, semResultMarks = 61.0, totalCredits = 3),
            ExamMarkRecord("em_2", "239Y1A0501", "2305701", "Cyber Security", mid1Marks = 26.0, mid2Marks = 25.5, assignmentMarks = 9.0, semResultMarks = 58.0, totalCredits = 3),
            ExamMarkRecord("em_3", "239Y1A0501", "2332604", "Cloud Computing", mid1Marks = 29.0, mid2Marks = 28.0, assignmentMarks = 10.0, semResultMarks = 63.5, totalCredits = 3),
            ExamMarkRecord("em_4", "239Y1A0501", "23HS772", "Organizational Behaviour", mid1Marks = 24.0, mid2Marks = 25.0, assignmentMarks = 8.5, semResultMarks = 52.0, totalCredits = 2),
            ExamMarkRecord("em_5", "239Y1A0501", "2339751", "Prompt Engineering Lab", mid1Marks = 29.5, mid2Marks = 30.0, assignmentMarks = 10.0, semResultMarks = 68.0, totalCredits = 2)
        )
    }

    fun getInitialTeachingLoads(): List<FacultyClassTeachingLoad> {
        return listOf(
            FacultyClassTeachingLoad("tl_1", "CSE", 4, "Sec B", "2339609", "Deep Learning", "Period 2 (10:00 - 11:00 AM)", 64),
            FacultyClassTeachingLoad("tl_2", "CSE", 1, "Sec A", "2305101", "Problem Solving with C", "Period 4 (01:30 - 02:30 PM)", 70),
            FacultyClassTeachingLoad("tl_3", "ECE", 3, "Sec A", "2304501", "Digital Signal Processing", "Period 1 (09:00 - 10:00 AM)", 62),
            FacultyClassTeachingLoad("tl_4", "AIML", 2, "Sec B", "2331302", "Neural Networks Foundations", "Period 5 (02:30 - 03:30 PM)", 58)
        )
    }

    fun getInitialStudentTasks(): List<StudentTaskTodo> {
        return listOf(
            StudentTaskTodo("tsk_1", "Revise Backprop derivation for Deep Learning Mid-2", "Tomorrow 09:00 AM", "Exam Prep", false),
            StudentTaskTodo("tsk_2", "Submit Cyber Security AES Numerical Assignment PDF", "In 2 Days", "Assignment", false),
            StudentTaskTodo("tsk_3", "Practice LeetCode Tree Traversals for TCS Placement Round", "Sunday", "Placement", false),
            StudentTaskTodo("tsk_4", "Download End-Semester Hall Ticket & Print verification", "Nov 15", "Admin", true)
        )
    }

    // === INTERACTION HELPERS ===
    fun submitAssignment(submission: AssignmentSubmission) {
        val existingIdx = sharedSubmissions.indexOfFirst { it.assignmentId == submission.assignmentId && it.rollNumber == submission.rollNumber }
        if (existingIdx >= 0) {
            sharedSubmissions[existingIdx] = submission
        } else {
            sharedSubmissions.add(0, submission)
        }
        sharedNotifications.add(0, "Submission Logged: ${submission.studentName} uploaded ${submission.submittedPdfName}")
    }

    fun gradeAssignment(submissionId: String, marks: Double, feedback: String) {
        val idx = sharedSubmissions.indexOfFirst { it.id == submissionId }
        if (idx >= 0) {
            val updated = sharedSubmissions[idx].copy(
                marksAwarded = marks,
                feedback = feedback,
                isGraded = true
            )
            sharedSubmissions[idx] = updated
            // Also sync to exam marks if applicable
            val markIdx = sharedExamMarks.indexOfFirst { it.rollNumber == updated.rollNumber }
            if (markIdx >= 0) {
                val mark = sharedExamMarks[markIdx]
                sharedExamMarks[markIdx] = mark.copy(assignmentMarks = marks)
            }
            sharedNotifications.add(0, "Marks Assigned: ${updated.studentName} received $marks/10 for ${updated.submittedPdfName}")
            saveDataToDisk()
        }
    }

    fun publishAssignment(assignment: CampusAssignment) {
        sharedAssignments.add(0, assignment)
        sharedNotifications.add(0, "New Assignment: ${assignment.title} published by ${assignment.facultyName}")
        saveDataToDisk()
    }

    fun uploadCourseMaterial(material: CourseResource) {
        sharedCourseResources.add(0, material)
        sharedNotifications.add(0, "Study Resource: ${material.title} uploaded for ${material.department} Year ${material.year}")
        saveDataToDisk()
    }

    fun toggleStudentTask(taskId: String) {
        val idx = sharedStudentTasks.indexOfFirst { it.id == taskId }
        if (idx >= 0) {
            val current = sharedStudentTasks[idx]
            sharedStudentTasks[idx] = current.copy(isCompleted = !current.isCompleted)
        }
    }

    fun addStudentTask(title: String, dueDate: String, category: String = "Exam Prep") {
        sharedStudentTasks.add(0, StudentTaskTodo(java.util.UUID.randomUUID().toString(), title, dueDate, category, false))
        saveDataToDisk()
    }

    fun deleteStudentTask(taskId: String) {
        sharedStudentTasks.removeAll { it.id == taskId }
        saveDataToDisk()
    }

    fun markAllTasksCompleted() {
        for (i in sharedStudentTasks.indices) {
            sharedStudentTasks[i] = sharedStudentTasks[i].copy(isCompleted = true)
        }
        saveDataToDisk()
    }

    // Isolated Private Drive: Only documents belonging to ownerId are returned!
    fun getPrivateDriveDocsForUser(ownerId: String): List<DriveDocument> {
        val safeOwner = if (ownerId.isBlank()) "usr_student_01" else ownerId
        return sharedDriveDocs.filter { doc ->
            doc.ownerId == safeOwner || (doc.ownerId.isEmpty() && safeOwner == "usr_student_01")
        }
    }

    fun uploadPrivateDriveDoc(ownerId: String, title: String, fileType: String = "PDF", sizeFormatted: String = "1.8 MB"): DriveDocument {
        val safeOwner = if (ownerId.isBlank()) "usr_student_01" else ownerId
        val newDoc = DriveDocument(
            id = "pdoc_" + System.currentTimeMillis(),
            folderId = "fld_private",
            folderName = "My Private Vault",
            title = title,
            fileType = fileType,
            sizeBytes = 1800000,
            sizeFormatted = sizeFormatted,
            uploadedBy = "Me",
            uploadedDate = "Today",
            downloadUrl = "https://ksrmce.ac.in/private/$title",
            ownerId = safeOwner,
            collegeId = "col_ksrm_01"
        )
        sharedDriveDocs.add(0, newDoc)
        return newDoc
    }

    fun deletePrivateDriveDoc(docId: String, ownerId: String): Boolean {
        val safeOwner = if (ownerId.isBlank()) "usr_student_01" else ownerId
        val item = sharedDriveDocs.firstOrNull { it.id == docId && (it.ownerId == safeOwner || it.ownerId.isEmpty()) }
        return if (item != null) {
            sharedDriveDocs.remove(item)
            true
        } else false
    }
}



