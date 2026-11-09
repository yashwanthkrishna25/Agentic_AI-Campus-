# AGENTRA AI CAMPUS — Database Schema Documentation

**Project Root:** `D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main`
**Database:** Google Cloud Firestore (NoSQL) + local JSON cache + FastAPI in-memory fallback
**Version:** v1.0
**Date:** 01 Nov 2026
**Preservation Status:** Existing schema preserved; new collections added via additive migrations only

---

## 1. Design Principles

- **Additive only:** New collections and fields are added; existing collections/fields are never removed or renamed
- **Tenant isolation:** Every user-owned record includes `collegeId`; department-owned records include `departmentId`
- **Backward compatible:** Existing `users`, `colleges`, `departments`, `subjects`, `announcements` collections remain unchanged
- **Audit trail:** All mutation operations create entries in `auditLogs`
- **Soft delete:** Records use `status` / `isActive` fields instead of hard deletes where appropriate

---

## 2. Existing Collections (Preserved)

### 2.1 `users`
User profile documents (one per Firebase Auth UID).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `uid` | string | Yes | Firebase Auth UID (document ID) |
| `email` | string | Yes | User email |
| `displayName` | string | Yes | Display name |
| `role` | string | Yes | STUDENT, FACULTY, HOD, PRINCIPAL, ADMIN, SUPER_ADMIN |
| `department` | string | No | Department name/code |
| `departmentId` | string | No | Department document ID |
| `rollNumber` | string | No | Roll number (students) / employee ID (staff) |
| `collegeId` | string | Yes | College document ID (tenant isolation) |
| `status` | string | Yes | PENDING, APPROVED, SUSPENDED |
| `phone` | string | No | Phone number |
| `section` | string | No | Section (e.g., "Sec B") |
| `yearSemester` | string | No | Academic year/semester |
| `cgpa` | double | No | Cumulative GPA |
| `isFaceEnrolled` | boolean | No | Face template enrolled |
| `enrolledFaceSamplesCount` | int | No | Face sample count |
| `avatarUri` | string | No | Profile image URL |
| `createdAt` | timestamp | Yes | Creation time |
| `lastLoginAt` | timestamp | No | Last login time |

Relationships: `users.collegeId` → `colleges.id`; `users.departmentId` → `departments.id`

### 2.2 `colleges`
College/institution documents.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | College document ID |
| `name` | string | Yes | College name |
| `code` | string | Yes | College code (e.g., "KSRMCE") |
| `city` | string | No | City |
| `adminEmail` | string | No | College admin email |
| `departmentsCount` | int | No | Number of departments |
| `studentsCount` | int | No | Number of students |
| `status` | string | Yes | Active, Suspended, Inactive |
| `createdAt` | timestamp | Yes | Creation time |

### 2.3 `departments`
Department documents within a college.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Department document ID |
| `collegeId` | string | Yes | Parent college ID |
| `name` | string | Yes | Department name |
| `code` | string | Yes | Department code (CSE, AIML, ECE, EEE, Civil, Mechanical, Other) |
| `hodName` | string | No | HOD name |
| `hodEmail` | string | No | HOD email |
| `hodId` | string | No | HOD user UID |
| `facultyCount` | int | No | Number of faculty |
| `studentsCount` | int | No | Number of students |
| `avgAttendance` | double | No | Average attendance % |
| `status` | string | Yes | Active, Inactive |
| `createdAt` | timestamp | Yes | Creation time |

Relationships: `departments.collegeId` → `colleges.id`; `departments.hodId` → `users.uid`

### 2.4 `subjects`
Subject/course offerings.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Subject document ID (e.g., "subj_2339609") |
| `code` | string | Yes | Subject code |
| `name` | string | Yes | Subject name |
| `departmentId` | string | Yes | Owning department ID |
| `collegeId` | string | Yes | Owning college ID |
| `credits` | int | Yes | Credit value |
| `year` | int | Yes | Academic year (1-4) |
| `semester` | int | Yes | Semester (1-8) |
| `sections` | array[string] | No | Applicable sections |
| `status` | string | Yes | Active, Inactive |
| `createdAt` | timestamp | Yes | Creation time |

Relationships: `subjects.departmentId` → `departments.id`; `subjects.collegeId` → `colleges.id`

### 2.5 `announcements`
Announcements and circulars.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Announcement document ID |
| `title` | string | Yes | Title |
| `message` | string | Yes | Message body |
| `audience` | string | Yes | ALL, STUDENTS, FACULTY, HOD, PRINCIPAL, ADMIN, SUPER_ADMIN, DEPT |
| `collegeId` | string | Yes | Target college ID |
| `departmentId` | string | No | Target department ID (if DEPT audience) |
| `section` | string | No | Target section |
| `priority` | string | No | NORMAL, HIGH, URGENT |
| `authorName` | string | No | Author name |
| `authorRole` | string | No | Author role |
| `authorId` | string | No | Author user UID |
| `createdAt` | timestamp | Yes | Creation time |
| `expiresAt` | timestamp | No | Expiration time |

---

## 3. New Collections (Additive Migrations)

### 3.1 `academicYears`
Academic year definitions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Academic year document ID |
| `collegeId` | string | Yes | College ID |
| `name` | string | Yes | Year name (e.g., "2026-2027") |
| `startDate` | timestamp | No | Start date |
| `endDate` | timestamp | No | End date |
| `isCurrent` | boolean | No | Whether this is the current year |
| `status` | string | Yes | Active, Inactive |
| `createdAt` | timestamp | Yes | Creation time |

### 3.2 `semesters`
Semester definitions within an academic year.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Semester document ID |
| `collegeId` | string | Yes | College ID |
| `academicYearId` | string | Yes | Parent academic year ID |
| `name` | string | Yes | Semester name (e.g., "1st Semester", "Odd 2026-27") |
| `number` | int | Yes | Semester number (1-8) |
| `startDate` | timestamp | No | Start date |
| `endDate` | timestamp | No | End date |
| `isCurrent` | boolean | No | Whether this is the current semester |
| `status` | string | Yes | Active, Inactive |
| `createdAt` | timestamp | Yes | Creation time |

### 3.3 `sections`
Class sections within a department/year/semester.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Section document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `academicYearId` | string | Yes | Academic year ID |
| `semesterId` | string | Yes | Semester ID |
| `name` | string | Yes | Section name (e.g., "Sec A", "Sec B") |
| `studentCount` | int | No | Number of students |
| `status` | string | Yes | Active, Inactive |
| `createdAt` | timestamp | Yes | Creation time |

### 3.4 `facultyAllocations`
Faculty teaching allocations (which faculty teaches which subject to which section).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Allocation document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `facultyId` | string | Yes | Faculty user UID |
| `subjectId` | string | Yes | Subject ID |
| `academicYearId` | string | Yes | Academic year ID |
| `semesterId` | string | Yes | Semester ID |
| `sectionId` | string | Yes | Section ID |
| `isActive` | boolean | Yes | Whether allocation is active |
| `createdAt` | timestamp | Yes | Creation time |

### 3.5 `studentAllocations`
Student department/year/semester/section allocation.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Allocation document ID |
| `collegeId` | string | Yes | College ID |
| `studentId` | string | Yes | Student user UID |
| `departmentId` | string | Yes | Department ID |
| `academicYearId` | string | Yes | Academic year ID |
| `semesterId` | string | Yes | Semester ID |
| `sectionId` | string | Yes | Section ID |
| `rollNumber` | string | Yes | Student roll number |
| `isActive` | boolean | Yes | Whether allocation is active |
| `createdAt` | timestamp | Yes | Creation time |

### 3.6 `timetableSlots`
Timetable entries.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Timetable slot document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `sectionId` | string | Yes | Section ID |
| `subjectId` | string | Yes | Subject ID |
| `facultyId` | string | Yes | Faculty user UID |
| `day` | string | Yes | Day of week |
| `periodNumber` | int | Yes | Period number (1-8) |
| `timeSlot` | string | Yes | Time range (e.g., "09:00-10:00 AM") |
| `roomNumber` | string | No | Room/lab number |
| `academicYearId` | string | Yes | Academic year ID |
| `semesterId` | string | Yes | Semester ID |
| `isActive` | boolean | Yes | Whether slot is active |
| `createdAt` | timestamp | Yes | Creation time |

### 3.7 `attendanceRecords`
Attendance session records.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Attendance record document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `sectionId` | string | Yes | Section ID |
| `subjectId` | string | Yes | Subject ID |
| `facultyId` | string | Yes | Faculty who marked attendance |
| `sessionId` | string | Yes | Session identifier |
| `date` | timestamp | Yes | Attendance date |
| `periodNumber` | int | Yes | Period number |
| `method` | string | Yes | FACE_RECOGNITION, MANUAL, QR_CODE |
| `presentStudentIds` | array[string] | No | Present student UIDs |
| `absentStudentIds` | array[string] | No | Absent student UIDs |
| `lateStudentIds` | array[string] | No | Late student UIDs |
| `excusedStudentIds` | array[string] | No | Excused student UIDs |
| `totalStudents` | int | Yes | Total students in session |
| `markedBy` | string | Yes | Faculty UID |
| `markedByRole` | string | Yes | Faculty role |
| `createdAt` | timestamp | Yes | Creation time |
| `updatedAt` | timestamp | No | Last update time |

### 3.8 `studentAttendance`
Per-student attendance summary (denormalized for quick lookup).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Composite: studentId_subjectId |
| `collegeId` | string | Yes | College ID |
| `studentId` | string | Yes | Student UID |
| `subjectId` | string | Yes | Subject ID |
| `departmentId` | string | Yes | Department ID |
| `totalSessions` | int | Yes | Total sessions |
| `attendedSessions` | int | Yes | Attended sessions |
| `absentSessions` | int | Yes | Absent sessions |
| `lateSessions` | int | Yes | Late sessions |
| `attendancePercentage` | double | Yes | Calculated percentage |
| `lastUpdated` | timestamp | Yes | Last update time |

### 3.9 `faceTemplates`
Face biometric templates.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Template document ID (student UID) |
| `studentId` | string | Yes | Student UID |
| `studentName` | string | Yes | Student name |
| `departmentId` | string | Yes | Department ID |
| `collegeId` | string | Yes | College ID |
| `embedding` | array[float] | Yes | 128/512-dimensional facial embedding |
| `modelName` | string | No | Face model used |
| `detectorBackend` | string | No | Detector backend used |
| `enrolledBy` | string | Yes | Faculty/admin UID who enrolled |
| `enrolledAt` | timestamp | Yes | Enrollment time |
| `consentConfirmed` | boolean | Yes | Biometric consent given |
| `isActive` | boolean | Yes | Template active |

### 3.10 `assignments`
Assignments created by faculty.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Assignment document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `sectionId` | string | Yes | Section ID |
| `subjectId` | string | Yes | Subject ID |
| `facultyId` | string | Yes | Creating faculty UID |
| `title` | string | Yes | Assignment title |
| `instructions` | string | Yes | Assignment instructions |
| `deadline` | timestamp | Yes | Submission deadline |
| `maxMarks` | double | Yes | Maximum marks |
| `permittedFileType` | string | Yes | Allowed file type (PDF, etc.) |
| `autoClose` | boolean | Yes | Auto-close after deadline |
| `status` | string | Yes | ACTIVE, CLOSED, DRAFT |
| `createdAt` | timestamp | Yes | Creation time |
| `updatedAt` | timestamp | No | Last update time |

### 3.11 `assignmentSubmissions`
Student assignment submissions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Submission document ID |
| `collegeId` | string | Yes | College ID |
| `assignmentId` | string | Yes | Parent assignment ID |
| `studentId` | string | Yes | Submitting student UID |
| `fileUrl` | string | Yes | Uploaded file URL |
| `fileName` | string | Yes | Original file name |
| `fileSizeBytes` | int | Yes | File size |
| `submittedAt` | timestamp | Yes | Submission time |
| `status` | string | Yes | SUBMITTED, GRADED, LATE |
| `marksObtained` | double | No | Marks obtained |
| `maxMarks` | double | No | Maximum marks |
| `feedback` | string | No | Faculty feedback |
| `gradedBy` | string | No | Faculty UID who graded |
| `gradedAt` | timestamp | No | Grading time |

### 3.12 `quizzes`
Quiz definitions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Quiz document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `sectionId` | string | Yes | Section ID |
| `subjectId` | string | Yes | Subject ID |
| `facultyId` | string | Yes | Creating faculty UID |
| `title` | string | Yes | Quiz title |
| `questions` | array[object] | Yes | Quiz questions |
| `maxMarks` | double | Yes | Maximum marks |
| `startTime` | timestamp | Yes | Quiz start time |
| `endTime` | timestamp | Yes | Quiz end time |
| `status` | string | Yes | DRAFT, ACTIVE, CLOSED |
| `createdAt` | timestamp | Yes | Creation time |

### 3.13 `questionBank`
Question bank entries.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Question document ID |
| `collegeId` | string | Yes | College ID |
| `departmentId` | string | Yes | Department ID |
| `subjectId` | string | Yes | Subject ID |
| `facultyId` | string | Yes | Creating faculty UID |
| `questionText` | string | Yes | Question text |
| `questionType` | string | Yes | MCQ, SHORT_ANSWER, LONG_ANSWER, CODE |
| `options` | array[string] | No | MCQ options |
| `correctAnswer` | string | No | Correct answer |
| `marks` | double | Yes | Question marks |
| `difficulty` | string | No | EASY, MEDIUM, HARD |
| `topic` | string | No | Topic tag |
| `createdAt` | timestamp | Yes | Creation time |

### 3.14 `examResults`
Examination results.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Result document ID |
| `collegeId` | string | Yes | College ID |
| `studentId` | string | Yes | Student UID |
| `subjectId` | string | Yes | Subject ID |
| `departmentId` | string | Yes | Department ID |
| `academicYearId` | string | Yes | Academic year ID |
| `semesterId` | string | Yes | Semester ID |
| `internalMarks` | double | Yes | Internal assessment marks |
| `endTermMarks` | double | Yes | End-term marks |
| `totalMarks` | double | Yes | Total marks |
| `gradeLetter` | string | Yes | Grade letter (O, A+, A, B, etc.) |
| `gradePoints` | int | Yes | Grade points |
| `resultStatus` | string | Yes | PASSED, FAILED |
| `sgpa` | double | No | Semester GPA |
| `cgpa` | double | No | Cumulative GPA |
| `published` | boolean | Yes | Whether result is published |
| `approvedBy` | string | No | Approver UID |
| `approvedAt` | timestamp | No | Approval time |
| `createdAt` | timestamp | Yes | Creation time |

### 3.15 `certificates`
Digital certificates.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Certificate document ID |
| `collegeId` | string | Yes | College ID |
| `studentId` | string | Yes | Student UID |
| `title` | string | Yes | Certificate title |
| `type` | string | Yes | Course completion, merit, participation, etc. |
| `certificateUrl` | string | Yes | PDF certificate URL |
| `issuedBy` | string | Yes | Issuing authority UID |
| `issuedAt` | timestamp | Yes | Issue date |
| `verificationCode` | string | Yes | Unique verification code |
| `isVerified` | boolean | Yes | Verification status |
| `createdAt` | timestamp | Yes | Creation time |

### 3.16 `hallTickets`
Hall tickets/exam admit cards.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Hall ticket document ID |
| `collegeId` | string | Yes | College ID |
| `studentId` | string | Yes | Student UID |
| `examName` | string | Yes | Exam name |
| `examDate` | timestamp | Yes | Exam date |
| `hallTicketUrl` | string | Yes | PDF hall ticket URL |
| `seatNumber` | string | No | Seat number |
| `roomNumber` | string | No | Exam room |
| `status` | string | Yes | GENERATED, DOWNLOADED, USED |
| `generatedBy` | string | Yes | Generator UID |
| `generatedAt` | timestamp | Yes | Generation time |
| `createdAt` | timestamp | Yes | Creation time |

### 3.17 `documents` (Personal document drive)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Document document ID |
| `ownerId` | string | Yes | Owner user UID |
| `collegeId` | string | Yes | Owner's college ID |
| `departmentId` | string | No | Owner's department ID |
| `title` | string | Yes | Document title |
| `fileUrl` | string | Yes | File storage URL |
| `fileName` | string | Yes | Original file name |
| `fileType` | string | Yes | File type (PDF, DOC, etc.) |
| `fileSizeBytes` | int | Yes | File size |
| `folderId` | string | No | Parent folder ID |
| `isPrivate` | boolean | Yes | Whether document is private |
| `sharedWith` | array[string] | No | User UIDs with explicit access |
| `sharedWithDepartments` | array[string] | No | Department IDs with access |
| `sharedWithCollege` | boolean | No | Whether visible to entire college |
| `uploadedAt` | timestamp | Yes | Upload time |
| `updatedAt` | timestamp | No | Last update time |

### 3.18 `documentFolders`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Folder document ID |
| `ownerId` | string | Yes | Owner user UID |
| `collegeId` | string | Yes | Owner's college ID |
| `name` | string | Yes | Folder name |
| `parentFolderId` | string | No | Parent folder ID (null for root) |
| `createdAt` | timestamp | Yes | Creation time |

### 3.19 `notifications`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Notification document ID |
| `collegeId` | string | Yes | Target college ID |
| `departmentId` | string | No | Target department ID |
| `recipientId` | string | No | Specific recipient UID (null = broadcast) |
| `recipientRole` | string | No | Target role (null = all) |
| `title` | string | Yes | Notification title |
| `message` | string | Yes | Notification message |
| `type` | string | Yes | INFO, ALERT, ASSIGNMENT, EXAM, ATTENDANCE, LEAVE, RESULT |
| `relatedId` | string | No | Related record ID (assignment, exam, etc.) |
| `isRead` | boolean | Yes | Read status |
| `createdAt` | timestamp | Yes | Creation time |

### 3.20 `messages` (Faculty-student / HOD communication)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Message document ID |
| `collegeId` | string | Yes | College ID |
| `senderId` | string | Yes | Sender UID |
| `receiverId` | string | Yes | Receiver UID |
| `message` | string | Yes | Message text |
| `isRead` | boolean | Yes | Read status |
| `createdAt` | timestamp | Yes | Creation time |

### 3.21 `leaveRequests`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Leave request document ID |
| `collegeId` | string | Yes | College ID |
| `applicantId` | string | Yes | Applicant UID |
| `applicantName` | string | Yes | Applicant name |
| `applicantRole` | string | Yes | Applicant role |
| `departmentId` | string | Yes | Department ID |
| `leaveType` | string | Yes | CASUAL, MEDICAL, DUTY, ACADEMIC |
| `startDate` | timestamp | Yes | Start date |
| `endDate` | timestamp | Yes | End date |
| `reason` | string | Yes | Reason |
| `documentUrl` | string | No | Supporting document URL |
| `status` | string | Yes | PENDING, APPROVED, REJECTED |
| `decidedBy` | string | No | Decider UID |
| `decisionRemarks` | string | No | Decision remarks |
| `decidedAt` | timestamp | No | Decision time |
| `createdAt` | timestamp | Yes | Creation time |

### 3.22 `auditLogs`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Audit log document ID |
| `collegeId` | string | Yes | College ID |
| `action` | string | Yes | Action type (CREATE, UPDATE, DELETE, APPROVE, etc.) |
| `actorId` | string | Yes | User who performed action |
| `actorRole` | string | Yes | Actor role |
| `targetType` | string | Yes | Type of record affected |
| `targetId` | string | Yes | ID of record affected |
| `details` | string | No | Human-readable description |
| `timestamp` | timestamp | Yes | Action time |

### 3.23 `aiUsageLogs`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Log document ID |
| `collegeId` | string | Yes | College ID |
| `userId` | string | Yes | User who made request |
| `userRole` | string | Yes | User role |
| `requestType` | string | Yes | CHAT, RAG, QUIZ_GEN, ASSIGNMENT_GEN, etc. |
| `tokensUsed` | int | No | Tokens consumed |
| `modelUsed` | string | No | AI model used |
| `responseTimeMs` | int | No | Response time |
| `timestamp` | timestamp | Yes | Request time |

### 3.24 `placements` (Placement preparation & applications)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Placement record ID |
| `collegeId` | string | Yes | College ID |
| `studentId` | string | Yes | Student UID |
| `companyName` | string | Yes | Company name |
| `jobRole` | string | Yes | Job role |
| `applicationStatus` | string | Yes | APPLIED, SHORTLISTED, INTERVIEW, SELECTED, REJECTED |
| `appliedAt` | timestamp | Yes | Application time |
| `resumeUrl` | string | No | Resume URL |
| `createdAt` | timestamp | Yes | Creation time |

### 3.25 `tasks` (Daily tasks / to-do)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Task document ID |
| `collegeId` | string | Yes | College ID |
| `userId` | string | Yes | Task owner UID |
| `title` | string | Yes | Task title |
| `description` | string | No | Task description |
| `dueDate` | timestamp | No | Due date |
| `priority` | string | No | LOW, MEDIUM, HIGH |
| `status` | string | Yes | PENDING, COMPLETED |
| `createdAt` | timestamp | Yes | Creation time |

---

## 4. Security Rules Summary (Firestore)

All collections must enforce:
1. **Authentication required** for all read/write (except public announcements)
2. **College isolation:** `request.resource.data.collegeId == request.auth.token.collegeId` OR Super Admin
3. **Department isolation:** HOD/Faculty restricted to their `departmentId`
4. **Student self-access:** Students can only read their own attendance, marks, documents
5. **Faculty access:** Faculty can only access subjects/sections they are allocated to
6. **Admin/Super Admin:** Broad access within their college / across all colleges

Firestore rules must be tested in Firebase Emulator Suite before production use.

---

## 5. Indexes Required (Composite)

| Collection | Fields | Query Type |
|------------|--------|------------|
| `users` | `collegeId`, `role` | Role-based user listing per college |
| `users` | `collegeId`, `departmentId` | Department user listing |
| `attendanceRecords` | `collegeId`, `subjectId`, `date` | Attendance history by subject/date |
| `studentAttendance` | `collegeId`, `studentId` | Student attendance summary |
| `assignments` | `collegeId`, `sectionId`, `status` | Active assignments per section |
| `assignmentSubmissions` | `collegeId`, `assignmentId`, `studentId` | Submissions per assignment/student |
| `examResults` | `collegeId`, `studentId`, `semesterId` | Student results per semester |
| `documents` | `collegeId`, `ownerId` | Personal documents per user |
| `notifications` | `collegeId`, `recipientId`, `isRead` | Unread notifications per user |
| `timetableSlots` | `collegeId`, `sectionId`, `day` | Timetable per section/day |
| `facultyAllocations` | `collegeId`, `facultyId` | Faculty allocations |
| `studentAllocations` | `collegeId`, `studentId` | Student allocation lookup |
| `auditLogs` | `collegeId`, `timestamp` | Audit log chronological |

---

## 6. Migration Safety Rules

1. **NEVER** execute `DROP DATABASE`, `DROP TABLE`, `TRUNCATE`, or destructive schema replacement
2. **ALWAYS** create a Firestore backup before migration
3. **ALWAYS** use additive changes (new collections, new fields)
4. **NEVER** rename or remove existing fields
5. **VERIFY** existing data integrity after migration
6. **TEST** migrations on a safe copy when possible


