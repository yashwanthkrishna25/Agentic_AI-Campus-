package com.example.data

import android.util.Log
import com.example.model.AttendanceLog
import com.example.model.UserDocument
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



/**
 * Firebase Authentication & Firestore Role Management Service.
 * Implements role distinction between 'Admin', 'Faculty', 'Student', and 'Parent'
 * (as well as HOD and Principal) using Firebase Auth custom claims and Firestore user documents.
 * Also handles pushing student classroom QR attendance logs to Firestore.
 */
object FirebaseRoleAuthManager {
    private const val TAG = "FirebaseRoleAuth"
    private const val USERS_COLLECTION = "users"
    private const val ATTENDANCE_LOGS_COLLECTION = "attendanceLogs"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Resolves the effective role of a user using:
     * 1. Firebase Auth custom claims ("role") if present.
     * 2. Firestore user document (/users/{uid}) role field.
     * 3. Falls back to defaultRole or STUDENT.
     */
    suspend fun resolveUserRole(
        uid: String,
        defaultRole: UserRole = UserRole.STUDENT
    ): UserRole {
        try {
            // Step 1: Check Firebase Auth Custom Claims if user is signed in
            val user = auth.currentUser
            if (user != null && user.uid == uid) {
                try {
                    val tokenResult = user.getIdToken(false).await()
                    val claimRole = tokenResult.claims["role"] as? String
                    if (!claimRole.isNullOrBlank()) {
                        Log.d(TAG, "Resolved role from Firebase Auth custom claim: $claimRole")
                        return mapStringToUserRole(claimRole)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking custom claims: ${e.message}")
                }
            }

            // Step 2: Query Firestore user document
            val docSnapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (docSnapshot.exists()) {
                val roleString = docSnapshot.getString("role")
                if (!roleString.isNullOrBlank()) {
                    Log.d(TAG, "Resolved role from Firestore document: $roleString")
                    return mapStringToUserRole(roleString)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve role from Firestore/Auth for $uid: ${e.message}")
        }
        return defaultRole
    }

    /**
     * Authenticates via Firebase Auth with seamless fallback to SessionManager credentials.
     * When online and using email, authenticates with FirebaseAuth and loads user document from Firestore.
     * When using Institutional ID or offline, validates with SessionManager and syncs to Firestore in background.
     */
    suspend fun signInWithFirebaseOrLocal(
        identifier: String,
        password: String
    ): Result<UserProfile> {
        val trimmed = identifier.trim()
        if (!trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Sign in with the email address linked to your Firebase account. Institutional-ID sign-in requires a configured server-side lookup."))
        }
        return try {
            val authResult = auth.signInWithEmailAndPassword(trimmed, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(IllegalStateException("Authentication did not return a user."))
            if (!firebaseUser.isEmailVerified) {
                auth.signOut()
                return Result.failure(IllegalStateException("Please verify your email address before signing in."))
            }
            val uid = firebaseUser.uid
            val userDoc = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (!userDoc.exists()) {
                auth.signOut()
                return Result.failure(IllegalStateException("Your campus account has not been approved yet. Contact the administrator."))
            }
            val accountStatus = userDoc.getString("status") ?: "PENDING"
            if (accountStatus != "APPROVED") {
                auth.signOut()
                return Result.failure(IllegalStateException("Your account is pending campus administrator approval."))
            }
            val roleValue = userDoc.getString("role")
                ?: return Result.failure(IllegalStateException("Your campus role is not configured. Contact the administrator."))
            val effectiveRole = mapStringToUserRole(roleValue)
            val profile = UserProfile(
                id = uid,
                name = userDoc.getString("displayName") ?: firebaseUser.displayName ?: "Campus User",
                rollNumber = userDoc.getString("rollNumber") ?: "ID-PENDING",
                programme = "AGENTRA AI CAMPUS",
                department = userDoc.getString("department") ?: "Not assigned",
                section = userDoc.getString("section") ?: "Not assigned",
                yearSemester = userDoc.getString("yearSemester") ?: "Not assigned",
                role = effectiveRole,
                email = firebaseUser.email ?: trimmed,
                phone = firebaseUser.phoneNumber.orEmpty(),
                cgpa = 0.0,
                isFaceEnrolled = false,
                enrolledFaceSamplesCount = 0
            )
            SessionManager.saveProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-in failed", e)
            Result.failure(IllegalStateException("Sign-in failed. Check your credentials, internet connection, and Firebase configuration."))
        }
    }


    /**
     * Signs in with a Google ID token, then applies campus approval rules.
     * Google sign-in does not create or approve campus users.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserProfile> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google did not return an ID token. Please try again."))
        }

        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Google sign-in did not return a Firebase user.")

            if (!firebaseUser.isEmailVerified) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException("Please use a Google account with a verified email address.")
                )
            }

            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException(
                        "No campus account is registered for this Google account. Contact your campus administrator."
                    )
                )
            }

            val status = userDoc.getString("status") ?: "PENDING"
            if (!status.equals("APPROVED", ignoreCase = true)) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException("Your campus account is pending administrator approval.")
                )
            }

            val roleValue = userDoc.getString("role")?.trim()
            if (roleValue.isNullOrBlank()) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException("Your campus role is not configured. Contact your administrator.")
                )
            }

            val normalizedRole = roleValue.uppercase(Locale.ROOT)
            val allowedRoles = setOf(
                "ADMIN", "FACULTY", "STUDENT", "PARENT", "HOD",
                "PRINCIPAL", "PRINCIPAL_ADMIN", "DEAN"
            )
            if (normalizedRole !in allowedRoles) {
                auth.signOut()
                return Result.failure(
                    IllegalStateException("Your campus role is invalid. Contact your administrator.")
                )
            }

            val profile = UserProfile(
                id = firebaseUser.uid,
                name = userDoc.getString("displayName")
                    ?: firebaseUser.displayName
                    ?: "Campus User",
                rollNumber = userDoc.getString("rollNumber") ?: "ID-PENDING",
                programme = "AGENTRA AI CAMPUS",
                department = userDoc.getString("department") ?: "Not assigned",
                section = userDoc.getString("section") ?: "Not assigned",
                yearSemester = userDoc.getString("yearSemester") ?: "Not assigned",
                role = mapStringToUserRole(roleValue),
                email = firebaseUser.email.orEmpty(),
                phone = firebaseUser.phoneNumber.orEmpty(),
                cgpa = 0.0,
                isFaceEnrolled = false,
                enrolledFaceSamplesCount = 0
            )

            SessionManager.saveProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-in failed", e)
            auth.signOut()
            Result.failure(
                IllegalStateException(
                    e.message ?: "Google sign-in failed. Check your internet connection and Firebase configuration."
                )
            )
        }
    }


    /**
     * Registers a new user with Firebase Authentication and Firestore User Document.
     */
    suspend fun signUpWithFirebase(
        name: String,
        email: String,
        institutionalId: String,
        phone: String,
        role: UserRole,
        department: String,
        password: String
    ): Result<UserProfile> {
        val trimmedEmail = email.trim()
        if (role != UserRole.STUDENT && role != UserRole.PARENT) {
            return Result.failure(IllegalArgumentException("Privileged campus accounts must be provisioned by an authorized administrator."))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must contain at least 8 characters."))
        }
        try {
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user
            firebaseUser?.sendEmailVerification()?.await()
            val uid = firebaseUser?.uid
                ?: return Result.failure(IllegalStateException("Firebase did not create an account."))
            val pendingUser = hashMapOf<String, Any>(
                "uid" to uid,
                "email" to trimmedEmail,
                "displayName" to name.trim(),
                "role" to role.name,
                "department" to department.trim(),
                "rollNumber" to institutionalId.trim(),
                "phone" to phone.trim(),
                "collegeId" to "KSRMCE",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION).document(uid).set(pendingUser).await()
            // Do not leave the newly created, unverified account signed in.
            auth.signOut()
            // Do not create a local session: the email must be verified and an admin must approve it.
            return Result.failure(IllegalStateException(
                "Account created. Check your email for Firebase verification, then wait for campus administrator approval before signing in."
            ))
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth registration failed", e)
            return Result.failure(IllegalStateException("Registration failed. Check Firebase setup and try again."))
        }
    }

    /**
     * Registers or updates a user document in Firestore to persist their role
     * ('Admin', 'Faculty', 'Student', 'Parent', etc.).
     */
    suspend fun syncUserDocument(
        uid: String,
        email: String,
        displayName: String,
        role: UserRole,
        department: String,
        rollNumber: String = "",
        studentId: String = ""
    ): Boolean {
        return try {
            val userDoc = UserDocument(
                uid = uid,
                email = email,
                displayName = displayName,
                role = role.name,
                department = department,
                rollNumber = rollNumber,
                studentId = studentId,
                collegeId = "KSRMCE",
                lastLoginAt = System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(userDoc.toMap(), SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully synced user document in Firestore: ${userDoc.email} (${userDoc.role})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user document to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Pushes a verified QR attendance scan log to Firestore collection 'attendanceLogs'.
     * Called by CameraX + ZXing QR scanner when a student scans a classroom projector code.
     */
    suspend fun pushClassroomAttendanceLog(
        studentId: String,
        studentName: String,
        subjectCode: String,
        subjectName: String,
        sessionToken: String,
        verificationMethod: String = "CAMERAX_ZXING"
    ): Result<AttendanceLog> {
        // Do not accept client-provided QR contents as authorization. This project does not yet
        // have a trusted backend endpoint that verifies signed session, expiry, enrollment,
        // identity, class scope, and duplicate attendance atomically.
        return Result.failure(
            IllegalStateException(
                "QR attendance is unavailable: server-side signed-session validation is not configured. No attendance was recorded."
            )
        )
    }

    /**
     * Maps role string from Firestore or Custom Claims to UserRole enum.
     */
    fun mapStringToUserRole(roleStr: String): UserRole {
        return when (roleStr.trim().uppercase(Locale.ROOT)) {
            "SUPER_ADMIN" -> UserRole.SUPER_ADMIN
            "ADMIN" -> UserRole.ADMIN
            "FACULTY" -> UserRole.FACULTY
            "STUDENT" -> UserRole.STUDENT
            "PARENT" -> UserRole.PARENT
            "HOD" -> UserRole.HOD
            "PRINCIPAL", "PRINCIPAL_ADMIN", "DEAN" -> UserRole.PRINCIPAL
            else -> UserRole.STUDENT
        }
    }

    /**
     * Sends password reset email through Firebase Auth.
     */
    suspend fun sendPasswordReset(email: String): Result<String> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        return try {
            auth.sendPasswordResetEmail(trimmed).await()
            Result.success("Password reset link has been dispatched to $trimmed. Please check your inbox.")
        } catch (e: Exception) {
            Log.w(TAG, "Password reset failed: ${e.message}")
            Result.failure(IllegalStateException(e.message ?: "Unable to send password reset email."))
        }
    }

    /**
     * Helper to sign out from Firebase Auth and clear local session.
     */
    fun signOut() {
        try {
            auth.signOut()
            SessionManager.logout()
            Log.d(TAG, "Signed out from Firebase and cleared session")
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut: ${e.message}")
        }
    }
}
