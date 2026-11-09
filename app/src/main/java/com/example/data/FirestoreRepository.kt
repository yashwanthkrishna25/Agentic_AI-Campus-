package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirestoreRepository {
    private const val TAG = "FirestoreRepository"
    private var firestoreDb: FirebaseFirestore? = null

    fun init(context: Context) {
        if (firestoreDb == null) {
            try {
                FirebaseApp.initializeApp(context)
                val databaseIdResId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                val databaseId = if (databaseIdResId != 0) context.getString(databaseIdResId) else ""
                firestoreDb = if (databaseId.isNotEmpty() && databaseId != "(default)") {
                    FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)
                } else {
                    FirebaseFirestore.getInstance()
                }
                Log.d(TAG, "Firestore initialized successfully with database: $databaseId")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firestore, using fallback", e)
                try {
                    firestoreDb = FirebaseFirestore.getInstance()
                } catch (ex: Exception) {
                    Log.e(TAG, "Default Firestore fallback failed", ex)
                }
            }
        }
    }

    private fun getDb(): FirebaseFirestore? = firestoreDb

    // --- COLLEGES / ORGANIZATIONS ---
    suspend fun saveCollege(college: CollegeOrganization): Boolean {
        return try {
            val db = getDb() ?: return false
            val data = hashMapOf(
                "id" to college.id,
                "name" to college.name,
                "code" to college.code,
                "city" to college.city,
                "adminEmail" to college.adminEmail,
                "departmentsCount" to college.departmentsCount,
                "studentsCount" to college.studentsCount,
                "status" to college.status
            )
            db.collection("colleges").document(college.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving college", e)
            false
        }
    }

    fun observeColleges(): Flow<List<CollegeOrganization>> = callbackFlow {
        val db = getDb()
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = db.collection("colleges").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Snapshot error observing colleges", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    CollegeOrganization(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "College",
                        code = doc.getString("code") ?: "COLL",
                        city = doc.getString("city") ?: "Campus",
                        adminEmail = doc.getString("adminEmail") ?: "",
                        departmentsCount = (doc.getLong("departmentsCount") ?: 0).toInt(),
                        studentsCount = (doc.getLong("studentsCount") ?: 0).toInt(),
                        status = doc.getString("status") ?: "Unknown"
                    )
                }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    // --- DEPARTMENTS & HODS ---
    suspend fun saveDepartment(dept: DepartmentInfo): Boolean {
        return try {
            val db = getDb() ?: return false
            val data = hashMapOf(
                "id" to dept.id,
                "collegeId" to dept.collegeId,
                "name" to dept.name,
                "code" to dept.code,
                "hodName" to dept.hodName,
                "hodEmail" to dept.hodEmail,
                "facultyCount" to dept.facultyCount,
                "studentsCount" to dept.studentsCount,
                "avgAttendance" to dept.avgAttendance
            )
            db.collection("departments").document(dept.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving department", e)
            false
        }
    }

    fun observeDepartments(): Flow<List<DepartmentInfo>> = callbackFlow {
        val db = getDb()
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = db.collection("departments").addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    DepartmentInfo(
                        id = doc.getString("id") ?: doc.id,
                        collegeId = doc.getString("collegeId") ?: "",
                        name = doc.getString("name") ?: "Department",
                        code = doc.getString("code") ?: "",
                        hodName = doc.getString("hodName") ?: "Unassigned",
                        hodEmail = doc.getString("hodEmail") ?: "",
                        facultyCount = (doc.getLong("facultyCount") ?: 0).toInt(),
                        studentsCount = (doc.getLong("studentsCount") ?: 0).toInt(),
                        avgAttendance = doc.getDouble("avgAttendance") ?: 0.0
                    )
                }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    // --- ATTENDANCE RECORDS ---
    suspend fun saveAttendanceRecord(
        subjectName: String,
        date: String,
        markedBy: String,
        method: String,
        presentCount: Int,
        totalStudents: Int
    ): Boolean {
        return try {
            val db = getDb() ?: return false
            val id = "rec_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to id,
                "subjectName" to subjectName,
                "date" to date,
                "markedBy" to markedBy,
                "method" to method,
                "presentCount" to presentCount,
                "totalStudents" to totalStudents
            )
            db.collection("attendanceRecords").document(id).set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving attendance record", e)
            false
        }
    }

    // --- CAMPUS DOCUMENTS (DRIVE) ---
    suspend fun saveCampusDocument(doc: DriveDocument): Boolean {
        return try {
            val db = getDb() ?: return false
            val data = hashMapOf(
                "id" to doc.id,
                "title" to doc.title,
                "folderName" to doc.folderName,
                "fileType" to doc.fileType,
                "sizeFormatted" to doc.sizeFormatted,
                "uploadedBy" to doc.uploadedBy,
                "uploadedDate" to doc.uploadedDate,
                "downloadUrl" to doc.downloadUrl
            )
            db.collection("campusDocuments").document(doc.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving campus document", e)
            false
        }
    }

    fun observeCampusDocuments(): Flow<List<DriveDocument>> = callbackFlow {
        val db = getDb()
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = db.collection("campusDocuments").addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { d ->
                    DriveDocument(
                        id = d.getString("id") ?: d.id,
                        folderId = d.getString("folderName")?.lowercase() ?: "general",
                        folderName = d.getString("folderName") ?: "General",
                        title = d.getString("title") ?: "Document",
                        fileType = d.getString("fileType") ?: "PDF",
                        sizeBytes = 0L,
                        sizeFormatted = d.getString("sizeFormatted") ?: "Unknown size",
                        uploadedBy = d.getString("uploadedBy") ?: "Unknown",
                        uploadedDate = d.getString("uploadedDate") ?: "",
                        downloadUrl = d.getString("downloadUrl") ?: ""
                    )
                }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    // --- SYSTEM ALERTS & ISSUE TRACKING ---
    suspend fun saveSystemAlert(report: AppIssueReport): Boolean {
        return try {
            val db = getDb() ?: return false
            val data = hashMapOf(
                "id" to report.id,
                "title" to report.title,
                "message" to report.description,
                "category" to report.locationScreen,
                "severity" to report.severity,
                "isResolved" to report.isResolved,
                "reportedBy" to report.reportedByRole,
                "dateReported" to report.dateReported
            )
            db.collection("systemAlerts").document(report.id).set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving system alert", e)
            false
        }
    }

    fun observeSystemAlerts(): Flow<List<AppIssueReport>> = callbackFlow {
        val db = getDb()
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = db.collection("systemAlerts").addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { d ->
                    AppIssueReport(
                        id = d.getString("id") ?: d.id,
                        title = d.getString("title") ?: "App Diagnostic Alert",
                        description = d.getString("message") ?: "System status normal",
                        locationScreen = d.getString("category") ?: "Core System",
                        reportedByRole = d.getString("reportedBy") ?: "System Monitor",
                        severity = d.getString("severity") ?: "Medium",
                        isResolved = d.getBoolean("isResolved") ?: false,
                        dateReported = d.getString("dateReported") ?: "Today"
                    )
                }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    // --- STUDENT MARKS (x1 to x6 Internals & x7 End Term) ---
    suspend fun saveStudentMarks(
        studentRoll: String,
        studentName: String,
        subjectCode: String,
        subjectName: String,
        x1: Double,
        x2: Double,
        x3: Double,
        x4: Double,
        x5: Double,
        x6: Double,
        x7: Double,
        updatedBy: String = "Faculty"
    ): Boolean {
        return try {
            val db = getDb() ?: return false
            val docId = "${studentRoll}_${subjectCode}"
            val internalTotal = x1 + x2 + x3 + x4 + x5 + x6
            val grandTotal = internalTotal + x7
            val data = hashMapOf(
                "docId" to docId,
                "studentRoll" to studentRoll,
                "studentName" to studentName,
                "subjectCode" to subjectCode,
                "subjectName" to subjectName,
                "x1" to x1,
                "x2" to x2,
                "x3" to x3,
                "x4" to x4,
                "x5" to x5,
                "x6" to x6,
                "x7" to x7,
                "internalTotal" to internalTotal,
                "grandTotal" to grandTotal,
                "updatedBy" to updatedBy,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("studentMarks").document(docId).set(data).await()
            Log.d(TAG, "Successfully saved marks to Firestore for $studentRoll in $subjectCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving student marks to Firestore", e)
            false
        }
    }

    // --- LIVE ML FACE ATTENDANCE CLOUD RECORD ---
    suspend fun saveFaceAttendanceBatch(
        subjectName: String,
        section: String,
        scannedCount: Int,
        verifiedCount: Int,
        unknownCount: Int,
        takenBy: String,
        recordsJson: String = ""
    ): Boolean {
        return try {
            val db = getDb() ?: return false
            val id = "face_att_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to id,
                "subjectName" to subjectName,
                "section" to section,
                "scannedCount" to scannedCount,
                "verifiedCount" to verifiedCount,
                "unknownCount" to unknownCount,
                "takenBy" to takenBy,
                "method" to "CAMERAX_DEEP_LEARNING_KNN",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("faceAttendanceBatches").document(id).set(data).await()
            Log.d(TAG, "Saved face attendance batch to Firestore: $id ($verifiedCount/$scannedCount)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving face attendance batch", e)
            false
        }
    }
}

