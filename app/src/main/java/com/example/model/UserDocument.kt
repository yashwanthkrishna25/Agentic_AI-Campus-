package com.example.model

/**
 * Firestore User Document model used to distinguish between:
 * 'ADMIN', 'FACULTY', 'STUDENT', and 'PARENT' (as well as HOD and PRINCIPAL)
 * roles in Firebase Authentication and Cloud Firestore.
 */
data class UserDocument(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "STUDENT", // 'ADMIN', 'FACULTY', 'STUDENT', 'PARENT', 'HOD', 'PRINCIPAL'
    val department: String = "Computer Science & Engineering",
    val rollNumber: String = "",
    val studentId: String = "", // For Parent role linking to ward student
    val collegeId: String = "KSRMCE",
    val phone: String = "",
    val lastLoginAt: Long = System.currentTimeMillis(),
    val photoUrl: String = "",
    val isVerified: Boolean = true
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "role" to role,
            "department" to department,
            "rollNumber" to rollNumber,
            "studentId" to studentId,
            "collegeId" to collegeId,
            "phone" to phone,
            "lastLoginAt" to lastLoginAt,
            "photoUrl" to photoUrl,
            "isVerified" to isVerified
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any?>): UserDocument {
            return UserDocument(
                uid = data["uid"] as? String ?: "",
                email = data["email"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                role = (data["role"] as? String)?.uppercase() ?: "STUDENT",
                department = data["department"] as? String ?: "Computer Science & Engineering",
                rollNumber = data["rollNumber"] as? String ?: "",
                studentId = data["studentId"] as? String ?: "",
                collegeId = data["collegeId"] as? String ?: "KSRMCE",
                phone = data["phone"] as? String ?: "",
                lastLoginAt = (data["lastLoginAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                photoUrl = data["photoUrl"] as? String ?: "",
                isVerified = data["isVerified"] as? Boolean ?: true
            )
        }
    }
}

/**
 * Attendance log pushed to Firestore when a student scans a classroom QR code
 * using CameraX and ZXing.
 */
data class AttendanceLog(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectCode: String = "",
    val subjectName: String = "",
    val sessionToken: String = "",
    val verificationMethod: String = "CAMERAX_ZXING",
    val status: String = "PRESENT",
    val collegeId: String = "KSRMCE",
    val deptId: String = "CSE",
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "studentId" to studentId,
            "studentName" to studentName,
            "subjectCode" to subjectCode,
            "subjectName" to subjectName,
            "sessionToken" to sessionToken,
            "verificationMethod" to verificationMethod,
            "status" to status,
            "collegeId" to collegeId,
            "deptId" to deptId,
            "timestamp" to timestamp,
            "dateFormatted" to dateFormatted
        )
    }
}
