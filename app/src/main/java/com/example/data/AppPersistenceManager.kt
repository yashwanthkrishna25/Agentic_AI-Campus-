package com.example.data

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * AppPersistenceManager:
 * Provides persistent local JSON storage for all AGENTRA AI CAMPUS dynamic records:
 * - Shared Feed Posts & Official Circulars (Principal & HOD posts)
 * - Exam Schedules & Timetables (published by HOD)
 * - Exam Results & Grade Memos (published by HOD)
 * - Study Materials & PDF Documents (uploaded by Faculty)
 * - Subject Courses & Attendance percentages (updated by Face & Manual attendance)
 * - Class Roster & Face Enrolment status
 * - Notifications & System Issues
 *
 * Ensures all updates persist across app restarts and reboots.
 */
object AppPersistenceManager {
    private const val FILE_NAME = "agentra_campus_data.json"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getStorageFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, FILE_NAME)
    }

    /**
     * Saves all shared campus data collections into the local persistent file.
     */
    @Synchronized
    fun saveAll(
        feedPosts: List<FeedPost>,
        examSchedules: List<AndhraExamSchedule>,
        examResults: List<AndhraExamResult>,
        driveDocs: List<DriveDocument>,
        driveFolders: List<DriveFolder>,
        courses: List<SubjectCourse>,
        roster: List<EnrolledStudent>,
        issues: List<AppIssueReport>,
        notifications: List<String>
    ) {
        val file = getStorageFile() ?: return
        try {
            val root = JSONObject()

            // 1. Feed Posts
            val postsArray = JSONArray()
            for (p in feedPosts) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("authorName", p.authorName)
                obj.put("authorRole", p.authorRole)
                obj.put("date", p.date)
                obj.put("title", p.title)
                obj.put("content", p.content)
                obj.put("upvotes", p.upvotes)
                obj.put("commentsCount", p.commentsCount)
                obj.put("isUpvoted", p.isUpvoted)
                obj.put("tag", p.tag)
                if (p.imageUrl != null) obj.put("imageUrl", p.imageUrl)
                postsArray.put(obj)
            }
            root.put("feedPosts", postsArray)

            // 2. Exam Schedules
            val schedulesArray = JSONArray()
            for (s in examSchedules) {
                val obj = JSONObject()
                obj.put("subjectCode", s.subjectCode)
                obj.put("subjectName", s.subjectName)
                obj.put("examDate", s.examDate)
                obj.put("timeSlot", s.timeSlot)
                obj.put("examType", s.examType)
                obj.put("hallTicketEligibility", s.hallTicketEligibility)
                schedulesArray.put(obj)
            }
            root.put("examSchedules", schedulesArray)

            // 3. Exam Results
            val resultsArray = JSONArray()
            for (r in examResults) {
                val obj = JSONObject()
                obj.put("subjectCode", r.subjectCode)
                obj.put("subjectName", r.subjectName)
                obj.put("credits", r.credits)
                obj.put("internalMarks", r.internalMarks)
                obj.put("endTermMarks", r.endTermMarks)
                obj.put("totalMarks", r.totalMarks)
                obj.put("gradeLetter", r.gradeLetter)
                obj.put("gradePoints", r.gradePoints)
                obj.put("resultStatus", r.resultStatus)
                resultsArray.put(obj)
            }
            root.put("examResults", resultsArray)

            // 4. Drive Documents
            val docsArray = JSONArray()
            for (d in driveDocs) {
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("folderId", d.folderId)
                obj.put("folderName", d.folderName)
                obj.put("title", d.title)
                obj.put("fileType", d.fileType)
                obj.put("sizeBytes", d.sizeBytes)
                obj.put("sizeFormatted", d.sizeFormatted)
                obj.put("uploadedBy", d.uploadedBy)
                obj.put("uploadedDate", d.uploadedDate)
                obj.put("downloadUrl", d.downloadUrl)
                docsArray.put(obj)
            }
            root.put("driveDocs", docsArray)

            // 5. Drive Folders
            val foldersArray = JSONArray()
            for (f in driveFolders) {
                val obj = JSONObject()
                obj.put("id", f.id)
                obj.put("name", f.name)
                obj.put("createdDate", f.createdDate)
                obj.put("colorHex", f.colorHex)
                obj.put("fileCount", f.fileCount)
                foldersArray.put(obj)
            }
            root.put("driveFolders", foldersArray)

            // 6. Subject Courses (Attendance & Marks)
            val coursesArray = JSONArray()
            for (c in courses) {
                val obj = JSONObject()
                obj.put("code", c.code)
                obj.put("name", c.name)
                obj.put("courseType", c.courseType)
                obj.put("sectionTag", c.sectionTag)
                obj.put("facultyName", c.facultyName)
                obj.put("totalSessions", c.totalSessions)
                obj.put("attendedSessions", c.attendedSessions)
                obj.put("credits", c.credits)
                obj.put("x1", c.x1)
                obj.put("x2", c.x2)
                obj.put("x3", c.x3)
                obj.put("x4", c.x4)
                obj.put("x5", c.x5)
                obj.put("x6", c.x6)
                obj.put("x7", c.x7)
                coursesArray.put(obj)
            }
            root.put("courses", coursesArray)

            // 7. Class Roster
            val rosterArray = JSONArray()
            for (st in roster) {
                val obj = JSONObject()
                obj.put("id", st.id)
                obj.put("rollNumber", st.rollNumber)
                obj.put("name", st.name)
                obj.put("section", st.section)
                obj.put("department", st.department)
                obj.put("faceFeaturesEnrolled", st.faceFeaturesEnrolled)
                obj.put("faceDescriptor", st.faceDescriptor)
                obj.put("currentStatus", st.currentStatus.name)
                rosterArray.put(obj)
            }
            root.put("roster", rosterArray)

            // 8. Issues
            val issuesArray = JSONArray()
            for (iss in issues) {
                val obj = JSONObject()
                obj.put("id", iss.id)
                obj.put("title", iss.title)
                obj.put("description", iss.description)
                obj.put("locationScreen", iss.locationScreen)
                obj.put("reportedByRole", iss.reportedByRole)
                obj.put("severity", iss.severity)
                obj.put("isResolved", iss.isResolved)
                obj.put("dateReported", iss.dateReported)
                issuesArray.put(obj)
            }
            root.put("issues", issuesArray)

            // 9. Notifications
            val notifsArray = JSONArray()
            for (n in notifications) {
                notifsArray.put(n)
            }
            root.put("notifications", notifsArray)

            file.writeText(root.toString(2))
        } catch (_: Exception) {
        }
    }

    /**
     * Loads saved campus data from local persistent storage.
     * Returns null if file does not exist yet.
     */
    @Synchronized
    fun loadAll(): LoadedCampusData? {
        val file = getStorageFile() ?: return null
        if (!file.exists()) return null

        try {
            val content = file.readText()
            if (content.isBlank()) return null
            val root = JSONObject(content)

            // 1. Feed Posts
            val feedPosts = mutableListOf<FeedPost>()
            val postsArray = root.optJSONArray("feedPosts")
            if (postsArray != null) {
                for (i in 0 until postsArray.length()) {
                    val obj = postsArray.getJSONObject(i)
                    feedPosts.add(
                        FeedPost(
                            id = obj.optString("id", "p_$i"),
                            authorName = obj.optString("authorName", "Campus Admin"),
                            authorRole = obj.optString("authorRole", "Administration"),
                            date = obj.optString("date", "Today"),
                            title = obj.optString("title", "Notice"),
                            content = obj.optString("content", ""),
                            upvotes = obj.optInt("upvotes", 0),
                            commentsCount = obj.optInt("commentsCount", 0),
                            isUpvoted = obj.optBoolean("isUpvoted", false),
                            tag = obj.optString("tag", "Notice"),
                            imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl").ifBlank { null } else null
                        )
                    )
                }
            }

            // 2. Exam Schedules
            val examSchedules = mutableListOf<AndhraExamSchedule>()
            val schedulesArray = root.optJSONArray("examSchedules")
            if (schedulesArray != null) {
                for (i in 0 until schedulesArray.length()) {
                    val obj = schedulesArray.getJSONObject(i)
                    examSchedules.add(
                        AndhraExamSchedule(
                            subjectCode = obj.optString("subjectCode", ""),
                            subjectName = obj.optString("subjectName", ""),
                            examDate = obj.optString("examDate", ""),
                            timeSlot = obj.optString("timeSlot", "10:00 AM - 01:00 PM"),
                            examType = obj.optString("examType", "Autonomous Semester Theory"),
                            hallTicketEligibility = obj.optBoolean("hallTicketEligibility", true)
                        )
                    )
                }
            }

            // 3. Exam Results
            val examResults = mutableListOf<AndhraExamResult>()
            val resultsArray = root.optJSONArray("examResults")
            if (resultsArray != null) {
                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    examResults.add(
                        AndhraExamResult(
                            subjectCode = obj.optString("subjectCode", ""),
                            subjectName = obj.optString("subjectName", ""),
                            credits = obj.optInt("credits", 3),
                            internalMarks = obj.optDouble("internalMarks", 25.0),
                            endTermMarks = obj.optDouble("endTermMarks", 55.0),
                            totalMarks = obj.optDouble("totalMarks", 80.0),
                            gradeLetter = obj.optString("gradeLetter", "A"),
                            gradePoints = obj.optInt("gradePoints", 8),
                            resultStatus = obj.optString("resultStatus", "PASSED")
                        )
                    )
                }
            }

            // 4. Drive Docs
            val driveDocs = mutableListOf<DriveDocument>()
            val docsArray = root.optJSONArray("driveDocs")
            if (docsArray != null) {
                for (i in 0 until docsArray.length()) {
                    val obj = docsArray.getJSONObject(i)
                    driveDocs.add(
                        DriveDocument(
                            id = obj.optString("id", "doc_$i"),
                            folderId = obj.optString("folderId", "fld_notes"),
                            folderName = obj.optString("folderName", "Class Notes & Lectures"),
                            title = obj.optString("title", "Document.pdf"),
                            fileType = obj.optString("fileType", "PDF"),
                            sizeBytes = obj.optLong("sizeBytes", 2500000L),
                            sizeFormatted = obj.optString("sizeFormatted", "2.5 MB"),
                            uploadedBy = obj.optString("uploadedBy", "Faculty"),
                            uploadedDate = obj.optString("uploadedDate", "Today"),
                            downloadUrl = obj.optString("downloadUrl", "https://ksrmce.ac.in")
                        )
                    )
                }
            }

            // 5. Drive Folders
            val driveFolders = mutableListOf<DriveFolder>()
            val foldersArray = root.optJSONArray("driveFolders")
            if (foldersArray != null) {
                for (i in 0 until foldersArray.length()) {
                    val obj = foldersArray.getJSONObject(i)
                    driveFolders.add(
                        DriveFolder(
                            id = obj.optString("id", "fld_$i"),
                            name = obj.optString("name", "Folder"),
                            createdDate = obj.optString("createdDate", "Today"),
                            colorHex = obj.optLong("colorHex", 0xFF10B981),
                            fileCount = obj.optInt("fileCount", 0)
                        )
                    )
                }
            }

            // 6. Courses
            val courses = mutableListOf<SubjectCourse>()
            val coursesArray = root.optJSONArray("courses")
            if (coursesArray != null) {
                for (i in 0 until coursesArray.length()) {
                    val obj = coursesArray.getJSONObject(i)
                    courses.add(
                        SubjectCourse(
                            code = obj.optString("code", ""),
                            name = obj.optString("name", ""),
                            courseType = obj.optString("courseType", "LECTURE"),
                            sectionTag = obj.optString("sectionTag", "B.TECH - CSE"),
                            facultyName = obj.optString("facultyName", "Faculty"),
                            totalSessions = obj.optInt("totalSessions", 40),
                            attendedSessions = obj.optInt("attendedSessions", 32),
                            credits = obj.optInt("credits", 3),
                            x1 = obj.optDouble("x1", 8.0),
                            x2 = obj.optDouble("x2", 8.0),
                            x3 = obj.optDouble("x3", 8.0),
                            x4 = obj.optDouble("x4", 8.0),
                            x5 = obj.optDouble("x5", 8.0),
                            x6 = obj.optDouble("x6", 8.0),
                            x7 = obj.optDouble("x7", 32.0)
                        )
                    )
                }
            }

            // 7. Roster
            val roster = mutableListOf<EnrolledStudent>()
            val rosterArray = root.optJSONArray("roster")
            if (rosterArray != null) {
                for (i in 0 until rosterArray.length()) {
                    val obj = rosterArray.getJSONObject(i)
                    val statusStr = obj.optString("currentStatus", "PRESENT")
                    val status = try {
                        AttendanceStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        AttendanceStatus.PRESENT
                    }
                    roster.add(
                        EnrolledStudent(
                            id = obj.optString("id", "$i"),
                            rollNumber = obj.optString("rollNumber", ""),
                            name = obj.optString("name", ""),
                            section = obj.optString("section", "Sec B"),
                            department = obj.optString("department", "CSE"),
                            faceFeaturesEnrolled = obj.optBoolean("faceFeaturesEnrolled", true),
                            faceDescriptor = obj.optString("faceDescriptor", "Vector_7500"),
                            currentStatus = status
                        )
                    )
                }
            }

            // 8. Issues
            val issues = mutableListOf<AppIssueReport>()
            val issuesArray = root.optJSONArray("issues")
            if (issuesArray != null) {
                for (i in 0 until issuesArray.length()) {
                    val obj = issuesArray.getJSONObject(i)
                    issues.add(
                        AppIssueReport(
                            id = obj.optString("id", "iss_$i"),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            locationScreen = obj.optString("locationScreen", ""),
                            reportedByRole = obj.optString("reportedByRole", ""),
                            severity = obj.optString("severity", "Medium"),
                            isResolved = obj.optBoolean("isResolved", false),
                            dateReported = obj.optString("dateReported", "Today")
                        )
                    )
                }
            }

            // 9. Notifications
            val notifications = mutableListOf<String>()
            val notifsArray = root.optJSONArray("notifications")
            if (notifsArray != null) {
                for (i in 0 until notifsArray.length()) {
                    notifications.add(notifsArray.getString(i))
                }
            }

            return LoadedCampusData(
                feedPosts = feedPosts,
                examSchedules = examSchedules,
                examResults = examResults,
                driveDocs = driveDocs,
                driveFolders = driveFolders,
                courses = courses,
                roster = roster,
                issues = issues,
                notifications = notifications
            )
        } catch (_: Exception) {
            return null
        }
    }
}

data class LoadedCampusData(
    val feedPosts: List<FeedPost>,
    val examSchedules: List<AndhraExamSchedule>,
    val examResults: List<AndhraExamResult>,
    val driveDocs: List<DriveDocument>,
    val driveFolders: List<DriveFolder>,
    val courses: List<SubjectCourse>,
    val roster: List<EnrolledStudent>,
    val issues: List<AppIssueReport>,
    val notifications: List<String>
)
