package com.example.data

import androidx.compose.runtime.mutableStateListOf
import com.example.model.EmailCategory
import com.example.model.EmailNotificationLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Institutional Email Alert Dispatcher for KSRM COLLEGE OF ENGINEERING (AUTONOMOUS).
 * Dispatches automated notifications to Parents, Faculty Mentors, and Students for:
 * 1. Attendance Shortage below 75% Cutoff (Red Alert)
 * 2. Andhra JNTUA Autonomous End-Semester Exam Schedules
 * 3. Official End-Semester Grade Sheets & CGPA Results
 * 4. Hackathon & Activity Participation On-Duty (OD) Credits
 */
object EmailAlertDispatcher {

    private val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    val sentEmails = mutableStateListOf<EmailNotificationLog>(
        EmailNotificationLog(
            id = "eml_att_001",
            subject = "CRITICAL: Attendance Shortage Alert below 75% - Sattu Penchala Prasad [239Y1A0501]",
            recipients = listOf(
                "parent.penchalaprasad@gmail.com (Parent)",
                "pvk.cse@ksrmce.ac.in (Faculty Mentor)",
                "penchalaprasad70@gmail.com (Student)",
                "principal@ksrmce.ac.in (Office of the Principal)"
            ),
            recipientRoles = "Parent • Faculty Mentor • Student • Principal Office",
            category = EmailCategory.ATTENDANCE_SHORTAGE_ALERT,
            contentBody = """
                OFFICE OF THE CONTROLLER OF ATTENDANCE & DEAN ACADEMICS
                KSRM COLLEGE OF ENGINEERING (AUTONOMOUS), KADAPA - 516003
                Approved by AICTE, Affiliated to JNTUA, Accredited by NAAC 'A' Grade
                ------------------------------------------------------------------------
                REF: KSRMCE/DEAN/ATT-ALERT/2026-27/SEM7/084
                
                Dear Sri P. Srinivasa Rao (Parent) & Penchala Prasad,
                
                This is an official automated advisory from the AGENTRA AI Campus Governance Engine.
                The academic attendance audit for Odd Semester 2026-27 indicates that student SATTU PENCHALA PRASAD (Roll No: 239Y1A0501, CSE 4th Year Sec B) has fallen below or near the mandatory 75% cutoff threshold in the following instructional components:
                
                • Cyber Security [2305701]: 75.0% (30 / 40 periods attended) - Borderline Warning
                • Deep Learning [2339609]: 76.0% (38 / 50 periods attended) - Action Advised
                • Aggregate College Attendance: 80.15% (Safe margin is diminishing)
                
                AUTONOMOUS REGULATIONS NOTICE:
                1. As per JNTUA and KSRMCE Autonomous Academic Regulations, minimum 75.0% aggregate attendance is compulsory to receive End-Semester Hall Ticket.
                2. Condonation for medical reasons is permissible ONLY between 65.0% and 74.9% subject to Academic Council approval and ₹1,000 condonation fee.
                3. Attendance below 65.0% leads to DIRECT SEMESTER DETENTION without exception.
                
                REQUIRED ACTION:
                • The student must attend all remaining 28 lecture periods without unexcused absences.
                • The parent is requested to acknowledge this email and consult Faculty Mentor Dr. P. V. Kishore (+91 94401 23456).
                
                Issued with the approval of Principal & Dean Academics.
            """.trimIndent(),
            sentTimestamp = "Today, 09:30 AM",
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        ),
        EmailNotificationLog(
            id = "eml_exam_002",
            subject = "TIMETABLE RELEASE: JNTUA Autonomous B.Tech 4th Year Sem-7 End Exams - Nov 2026",
            recipients = listOf(
                "parent.penchalaprasad@gmail.com (Parent)",
                "penchalaprasad70@gmail.com (Student)"
            ),
            recipientRoles = "Parent • Student",
            category = EmailCategory.EXAM_SCHEDULE_NOTIFICATION,
            contentBody = """
                EXAMINATION BRANCH - KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)
                ------------------------------------------------------------------------
                NOTIFICATION: B.Tech IV Year I Semester End-Examinations (R20/R23 Regulations)
                
                Dear Student and Parent,
                The official timetable for the forthcoming Autonomous Semester Examinations is released:
                
                1. 18 Nov 2026 | 10:00 AM - 01:00 PM | Cyber Security [2305701]
                2. 21 Nov 2026 | 10:00 AM - 01:00 PM | Deep Learning & Neural Networks [2339609]
                3. 24 Nov 2026 | 10:00 AM - 01:00 PM | Organizational Behaviour [23HS772]
                4. 27 Nov 2026 | 02:00 PM - 05:00 PM | Prompt Engineering Lab [2339751]
                5. 30 Nov 2026 | 10:00 AM - 01:00 PM | Cloud Computing & DevOps [2332604]
                
                HALL TICKET ELIGIBILITY: VERIFIED (Current Attendance: 80.15% >= 75%)
                Examination Center: KSRMCE Main Academic Block, Kadapa.
            """.trimIndent(),
            sentTimestamp = "Yesterday, 04:15 PM",
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        ),
        EmailNotificationLog(
            id = "eml_od_003",
            subject = "ON-DUTY (OD) SANCTIONED: Smart India Hackathon 2026 - Sattu Penchala Prasad",
            recipients = listOf(
                "parent.penchalaprasad@gmail.com (Parent)",
                "pvk.cse@ksrmce.ac.in (Mentor)",
                "penchalaprasad70@gmail.com (Student)"
            ),
            recipientRoles = "Parent • Faculty Mentor • Student",
            category = EmailCategory.ACTIVITY_OD_APPROVAL,
            contentBody = """
                KSRM COLLEGE OF ENGINEERING (AUTONOMOUS) - DEAN STUDENT AFFAIRS
                ------------------------------------------------------------------------
                SUB: Sanction of 6 Instructional Periods On-Duty (OD) Attendance Credit
                
                Student Sattu Penchala Prasad (239Y1A0501) has been shortlisted for the Grand Finale of Smart India Hackathon (SIH) 2026 representing KSRMCE with project title:
                "Agentic AI Autonomous Campus ERP & Biometric Governance Engine"
                
                ACADEMIC BENEFIT:
                • 6 lecture periods granted as PRESENT under On-Duty (OD) code.
                • Verified and approved by Faculty Mentor Dr. P. V. Kishore and Principal.
            """.trimIndent(),
            sentTimestamp = "08 Oct 2026, 02:20 PM",
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        )
    )

    fun sendAttendanceShortageAlert(
        studentName: String = "SATTU PENCHALA PRASAD",
        rollNumber: String = "239Y1A0501",
        currentPercentage: Double = 80.15,
        triggeredBy: String = "Automated AGENTRA AI System"
    ): EmailNotificationLog {
        val nowStr = timeFormat.format(Date())
        val newEmail = EmailNotificationLog(
            id = "eml_shortage_${System.currentTimeMillis()}",
            subject = "URGENT 75% ATTENDANCE SHORTAGE ADVISORY: $studentName [$rollNumber]",
            recipients = listOf(
                "penchalaprasad70@gmail.com (Student)",
                "parent.penchalaprasad@gmail.com (Parent)",
                "principal@ksrmce.ac.in (Principal Office)"
            ),
            recipientRoles = "Parent • Faculty Mentor • Student • Principal Office",
            category = EmailCategory.ATTENDANCE_SHORTAGE_ALERT,
            contentBody = """
                KSRM COLLEGE OF ENGINEERING (AUTONOMOUS), KADAPA
                OFFICE OF ACADEMIC MONITORING & BIOMETRIC ATTENDANCE CELL
                ------------------------------------------------------------------------
                REF: KSRMCE/AUTO/ATT-WARN/${System.currentTimeMillis().toString().takeLast(6)}
                
                STUDENT PROFILE:
                • Name: $studentName
                • Roll Number: $rollNumber
                • Department: B.Tech Computer Science & Engineering (4th Year Sec B)
                • Current Aggregate Attendance: ${String.format(Locale.US, "%.2f", currentPercentage)}%
                • Triggered By: $triggeredBy
                
                STATUTORY WARNING:
                This institutional alert is dispatched because subject-wise attendance has fallen to the critical threshold (< 75.0%).
                
                SUBJECT SHORTAGE DETAILS:
                1. Cyber Security [2305701]: 75.0% - Borderline
                2. Deep Learning [2339609]: 76.0% - Critical Catch-up required
                
                CONSEQUENCES UNDER AUTONOMOUS REGULATIONS:
                Students with attendance below 75% will be detained and barred from writing semester examinations unless a valid medical condonation is approved between 65% and 75%.
                
                Please ensure immediate consultation with Faculty Mentor Dr. P. V. Kishore.
            """.trimIndent(),
            sentTimestamp = nowStr,
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        )
        sentEmails.add(0, newEmail)
        return newEmail
    }

    fun sendAndhraExamScheduleAlert(
        examCount: Int = 5,
        semester: String = "7th Semester (2026-27)"
    ): EmailNotificationLog {
        val nowStr = timeFormat.format(Date())
        val newEmail = EmailNotificationLog(
            id = "eml_sched_${System.currentTimeMillis()}",
            subject = "OFFICIAL EXAM TIMETABLE: KSRMCE Autonomous $semester End Exams",
            recipients = listOf(
                "parent.penchalaprasad@gmail.com (Parent)",
                "penchalaprasad70@gmail.com (Student)"
            ),
            recipientRoles = "Parent • Student",
            category = EmailCategory.EXAM_SCHEDULE_NOTIFICATION,
            contentBody = """
                KSRM COLLEGE OF ENGINEERING (AUTONOMOUS), KADAPA
                EXAMINATION CELL - AFFILIATED TO JNTUA ANANTAPURAMU
                ------------------------------------------------------------------------
                Official Timetable for $semester Theory & Practical Examinations.
                Total $examCount Scheduled Papers starting 18 Nov 2026 to 30 Nov 2026.
                
                Timing: 10:00 AM - 01:00 PM
                Eligibility: Verified and Approved via AGENTRA AI Biometrics.
            """.trimIndent(),
            sentTimestamp = nowStr,
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        )
        sentEmails.add(0, newEmail)
        return newEmail
    }

    fun sendAndhraExamResultsAlert(
        sgpa: Double = 8.65,
        cgpa: Double = 8.42
    ): EmailNotificationLog {
        val nowStr = timeFormat.format(Date())
        val newEmail = EmailNotificationLog(
            id = "eml_res_${System.currentTimeMillis()}",
            subject = "OFFICIAL SEMESTER GRADE REPORT: Sattu Penchala Prasad [SGPA: $sgpa]",
            recipients = listOf(
                "parent.penchalaprasad@gmail.com (Parent)",
                "penchalaprasad70@gmail.com (Student)",
                "pvk.cse@ksrmce.ac.in (Mentor)"
            ),
            recipientRoles = "Parent • Student • Faculty Mentor",
            category = EmailCategory.EXAM_RESULTS_GRADE_CARD,
            contentBody = """
                KSRM COLLEGE OF ENGINEERING (AUTONOMOUS), KADAPA
                CONTROLLER OF EXAMINATIONS - OFFICIAL PROVISIONAL GRADE MEMO
                ------------------------------------------------------------------------
                STUDENT: SATTU PENCHALA PRASAD (239Y1A0501)
                BRANCH: Computer Science and Engineering (B.Tech 4th Year)
                
                ACADEMIC PERFORMANCE SUMMARY:
                • Semester SGPA: $sgpa
                • Cumulative CGPA: $cgpa
                • Result: PASSED FIRST CLASS WITH DISTINCTION
                • Total Credits Earned: 22 / 22
                
                Congratulations from the Dean of Academics and Faculty of CSE!
            """.trimIndent(),
            sentTimestamp = nowStr,
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        )
        sentEmails.add(0, newEmail)
        return newEmail
    }

    fun sendActivityOdApprovalAlert(
        eventName: String,
        projectTitle: String,
        odPeriods: Int = 5
    ): EmailNotificationLog {
        val nowStr = timeFormat.format(Date())
        val newEmail = EmailNotificationLog(
            id = "eml_act_${System.currentTimeMillis()}",
            subject = "ON-DUTY ATTENDANCE CREDITED ($odPeriods periods): $eventName",
            recipients = listOf(
                "parent.penchalaprasad@gmail.com (Parent)",
                "pvk.cse@ksrmce.ac.in (Mentor)",
                "penchalaprasad70@gmail.com (Student)"
            ),
            recipientRoles = "Parent • Mentor • Student",
            category = EmailCategory.ACTIVITY_OD_APPROVAL,
            contentBody = """
                KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)
                DEAN OF STUDENT AFFAIRS & RESEARCH INNOVATION CELL
                ------------------------------------------------------------------------
                EVENT PARTICIPATION & ON-DUTY CREDIT CONFIRMATION
                
                Student Sattu Penchala Prasad (239Y1A0501) has successfully presented project:
                "$projectTitle"
                at $eventName.
                
                Attendance Credit: $odPeriods periods marked ON-DUTY (OD) in institutional biometric ledger.
            """.trimIndent(),
            sentTimestamp = nowStr,
            deliveryStatus = "DELIVERED • SMTP 250 OK (KSRMCE Gateway)"
        )
        sentEmails.add(0, newEmail)
        return newEmail
    }

    /**
     * Launches the official Android email/Gmail client pre-addressed to Student and Parent
     * with the official low-attendance warning notice.
     */
    fun launchGmailLowAttendanceIntent(
        context: android.content.Context,
        studentEmail: String = "penchalaprasad70@gmail.com",
        parentEmail: String = "parent.penchalaprasad@gmail.com",
        studentName: String = "Sattu Penchala Prasad",
        rollNumber: String = "239Y1A0501",
        subjectName: String = "Deep Learning [2339609]",
        currentAttendance: Double = 68.5,
        facultyName: String = "Dr. P. V. Kishore"
    ) {
        val subject = "URGENT: Low Attendance Warning (<75% Cutoff) - $studentName [$rollNumber]"
        val body = """
OFFICIAL NOTICE: KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)
DEPARTMENT OF COMPUTER SCIENCE & ENGINEERING
------------------------------------------------------------------------
To: $studentName ($studentEmail)
Cc: Parent / Guardian ($parentEmail)
From: Faculty In-Charge $facultyName (pvk.cse@ksrmce.ac.in)

Subject: Critical Attendance Deficit Warning in $subjectName

Dear Student and Respected Parent,

This official notification serves to inform you that your current attendance in $subjectName has dropped to ${"%.1f".format(currentAttendance)}%, which is critically BELOW the 75.0% mandatory autonomous threshold prescribed by university academic regulations.

REGULATORY IMPLICATIONS:
1. Minimum 75% attendance is compulsory to receive End-Semester Examination Hall Tickets.
2. Attendance between 65.0% and 74.9% requires formal Condonation approval by the Principal & Academic Council upon submission of medical proof.
3. Attendance below 65.0% results in immediate Semester Detention.

MANDATORY ACTION:
You are required to attend a remedial counseling session with Faculty In-Charge $facultyName and submit an undertaking signed by your parent.

Issued by:
Faculty In-Charge & Dean Academics
KSRM College of Engineering (Autonomous), Kadapa
        """.trimIndent()

        // Also record in internal log
        sentEmails.add(
            0,
            EmailNotificationLog(
                id = "eml_gmail_${System.currentTimeMillis()}",
                subject = subject,
                recipients = listOf("$studentEmail (Student)", "$parentEmail (Parent)", "principal@ksrmce.ac.in (Principal)"),
                recipientRoles = "Student • Parent • Principal Office",
                category = EmailCategory.ATTENDANCE_SHORTAGE_ALERT,
                contentBody = body,
                sentTimestamp = timeFormat.format(Date()),
                deliveryStatus = "DELIVERED • SMTP 250 OK (Dispatched via KSRMCE Gateway)"
            )
        )

        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(studentEmail))
                putExtra(android.content.Intent.EXTRA_CC, arrayOf(parentEmail))
                putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                putExtra(android.content.Intent.EXTRA_TEXT, body)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Dispatch Official Low Attendance Notice via Gmail").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.util.Log.e("EmailAlertDispatcher", "Failed to launch mail client intent", e)
        }
    }
}
