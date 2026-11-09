package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserProfile
import com.example.model.UserRole

data class RegisteredAccount(
    val email: String,
    val institutionalId: String,
    val name: String,
    val role: UserRole,
    val department: String,
    val section: String = "Sec A",
    val password: String
)

object SessionManager {
    private const val PREF_NAME = "agentra_ai_session_pref"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_EMAIL = "key_user_email"
    private const val KEY_USER_ROLL = "key_user_roll"
    private const val KEY_USER_ROLE = "key_user_role"
    private const val KEY_USER_DEPT = "key_user_dept"
    private const val KEY_USER_SECTION = "key_user_section"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_FIREBASE_ID_TOKEN = "key_firebase_id_token"
    private const val KEY_USER_COLLEGE_ID = "key_user_college_id"

    private var sharedPrefs: SharedPreferences? = null

    fun getThemeMode(): com.example.ui.theme.ThemeMode {
        val raw = sharedPrefs?.getString(KEY_THEME_MODE, com.example.ui.theme.ThemeMode.DARK.name)
            ?: com.example.ui.theme.ThemeMode.DARK.name
        return try {
            com.example.ui.theme.ThemeMode.valueOf(raw)
        } catch (e: Exception) {
            com.example.ui.theme.ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: com.example.ui.theme.ThemeMode) {
        sharedPrefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }


    // Real authentication is provided by Firebase. No demo credentials are stored in the APK.
    private val accounts = mutableListOf<RegisteredAccount>()

    fun init(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPrefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    fun getProfile(): UserProfile = getCurrentProfile()

    fun getCurrentProfile(): UserProfile {
        val prefs = sharedPrefs
        if (prefs != null && prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            val roleStr = prefs.getString(KEY_USER_ROLE, UserRole.STUDENT.name) ?: UserRole.STUDENT.name
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                UserRole.STUDENT
            }

            return UserProfile(
                id = prefs.getString(KEY_USER_ID, "std_user") ?: "std_user",
                name = prefs.getString(KEY_USER_NAME, "User") ?: "User",
                rollNumber = prefs.getString(KEY_USER_ROLL, "ID-000") ?: "ID-000",
                programme = when (role) {
                    UserRole.SUPER_ADMIN -> "Platform-Wide Central Administration"
                    UserRole.ADMIN -> "Multi-College Administration & Governance"
                    UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> "College Principal & Executive Campus Administrator"
                    UserRole.HOD -> "Professor & Head of Department"
                    UserRole.FACULTY -> "Professor & Faculty Mentor"
                    UserRole.STUDENT -> "B.Tech. - Computer Science and Engineering"
                    UserRole.PARENT -> "Parent / Guardian of Sattu Penchala Prasad"
                },
                department = prefs.getString(KEY_USER_DEPT, "Computer Science & Engineering") ?: "Computer Science & Engineering",
                section = prefs.getString(KEY_USER_SECTION, "Section B") ?: "Section B",
                yearSemester = "Academic Year 2026-27",
                role = role,
                email = prefs.getString(KEY_USER_EMAIL, "user@agentra.ai") ?: "user@agentra.ai",
                phone = "+91 98765 43210",
                cgpa = if (role == UserRole.STUDENT || role == UserRole.PARENT) 8.42 else 10.0,
                isFaceEnrolled = role != UserRole.PARENT,
                enrolledFaceSamplesCount = if (role != UserRole.PARENT) 50 else 0
            )
        }
        return AcademicDataProvider.defaultStudentProfile
    }

    @Deprecated("Use FirebaseRoleAuthManager; local password authentication is disabled.")
    fun login(emailOrId: String, password: String): Pair<Boolean, String> =
        Pair(false, "Local/demo login is disabled. Sign in with a verified Firebase account.")

    @Deprecated("Role shortcuts are disabled; use authenticated Firebase sign-in.")
    fun quickRoleLogin(role: UserRole) { /* Intentionally disabled for security. */ }

    fun register(
        name: String,
        email: String,
        institutionalId: String,
        role: UserRole,
        department: String,
        password: String
    ): Pair<Boolean, String> {
        if (name.isBlank() || email.isBlank() || institutionalId.isBlank()) {
            return Pair(false, "All required fields must be filled.")
        }

        val existing = accounts.find { it.email.equals(email.trim(), ignoreCase = true) }
        if (existing != null) {
            return Pair(false, "An account with email '$email' already exists. Please log in.")
        }

        val newAccount = RegisteredAccount(
            email = email.trim(),
            institutionalId = institutionalId.trim(),
            name = name.trim(),
            role = role,
            department = department.trim(),
            section = when (role) {
                UserRole.STUDENT -> "4th Year Sec B"
                UserRole.PARENT -> "Child: 4th Year Sec B"
                else -> "Department Roster"
            },
            password = password
        )
        accounts.add(newAccount)

        saveSession(
            name = newAccount.name,
            email = newAccount.email,
            roll = newAccount.institutionalId,
            role = newAccount.role,
            department = newAccount.department,
            section = newAccount.section
        )

        return Pair(true, "Account created successfully for ${newAccount.name}!")
    }

    @Deprecated("Configure Google Sign-In with Firebase before enabling this action.")
    fun googleSignIn(role: UserRole = UserRole.STUDENT): UserProfile {
        throw IllegalStateException("Google Sign-In is not configured. Use verified Firebase email sign-in.")
    }

    fun logout() {
        sharedPrefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_FIREBASE_ID_TOKEN)
            apply()
        }
    }

    fun saveFirebaseIdToken(token: String?) {
        sharedPrefs?.edit()?.putString(KEY_FIREBASE_ID_TOKEN, token ?: "")?.apply()
    }

    fun getFirebaseIdToken(): String? {
        val t = sharedPrefs?.getString(KEY_FIREBASE_ID_TOKEN, null)
        return if (t.isNullOrEmpty()) null else t
    }

    fun saveCollegeId(collegeId: String?) {
        sharedPrefs?.edit()?.putString(KEY_USER_COLLEGE_ID, collegeId ?: "")?.apply()
    }

    fun getCollegeId(): String? {
        val c = sharedPrefs?.getString(KEY_USER_COLLEGE_ID, null)
        return if (c.isNullOrEmpty()) null else c
    }

    fun saveProfile(profile: UserProfile) {
        saveSession(
            name = profile.name,
            email = profile.email,
            roll = profile.rollNumber,
            role = profile.role,
            department = profile.department,
            section = profile.section
        )
    }

    private fun saveSession(
        name: String,
        email: String,
        roll: String,
        role: UserRole,
        department: String,
        section: String
    ) {
        sharedPrefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, "usr_${System.currentTimeMillis()}")
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLL, roll)
            putString(KEY_USER_ROLE, role.name)
            putString(KEY_USER_DEPT, department)
            putString(KEY_USER_SECTION, section)
            apply()
        }
    }
}
