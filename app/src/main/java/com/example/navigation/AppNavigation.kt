package com.example.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.UserRole
import com.example.ui.screens.*

/**
 * AppNavigation:
 * A unified navigation controller using mutableStateOf based router logic to manage
 * role-specific dashboards ('Student', 'Faculty', 'HOD', 'Principal', etc.),
 * ensuring smooth transitions between screens and strictly avoiding standard navigation libraries.
 */
@Composable
fun AppNavigation(
    router: HashRouter,
    activeRole: UserRole,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onScanClassQr: () -> Unit,
    onLaunchCameraScan: (String) -> Unit,
    onLaunchManualLedger: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onShowBanner: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeCameraSubject by remember { mutableStateOf("Deep Learning [2339609]") }

    HashRouterHost(
        router = router,
        modifier = modifier.fillMaxSize()
    ) { currentPath, queryParams ->
        when (currentPath) {
            // === 1. STUDENT DASHBOARD MODULE ===
            "/student/campus", "/faculty/campus", "/hod/campus", "/principal/campus", "/parent/campus", "/admin/campus", "/super_admin/campus" -> {
                val tabParam = queryParams["tab"]
                val initialTab = when (tabParam) {
                    "drive", "materials" -> CampusLifeTab.DRIVE_MATERIALS
                    "pomodoro" -> CampusLifeTab.POMODORO_TIMER
                    else -> CampusLifeTab.FEED
                }
                CampusFeedScreen(
                    initialTab = initialTab,
                    userRole = activeRole,
                    isStudent = (activeRole == UserRole.STUDENT),
                    onNavigateToAttendance = {
                        router.navigate(HashRouter.STUDENT_ATTENDANCE)
                    },
                    onNavigateToChat = onNavigateToChat
                )
            }

            "/student/attendance" -> {
                StudentPortalView(
                    onScanClassQr = onScanClassQr,
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/student/risk", "/principal/research" -> {
                AcademicRiskScreen(
                    userRole = activeRole,
                    onNavigateToChat = onNavigateToChat
                )
            }

            // === 2. FACULTY DASHBOARD MODULE ===
            "/faculty/command" -> {
                FacultyPortalScreen(
                    onLaunchCameraScan = { subjectName ->
                        activeCameraSubject = subjectName
                        onLaunchCameraScan(subjectName)
                        router.navigate(HashRouter.buildGroupScanRoute(subjectName))
                    },
                    onLaunchManualLedger = {
                        onLaunchManualLedger()
                        router.navigate(HashRouter.FACULTY_MANUAL_LEDGER)
                    },
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/faculty/summary" -> {
                FacultyAttendanceSummaryScreen(
                    isDarkMode = isDarkMode,
                    onBack = { router.popBackStack() },
                    onNavigateToChat = onNavigateToChat,
                    onTriggerToast = onShowBanner
                )
            }

            "/faculty/group_scan", "/principal/biometrics", "/student/qr_scanner", "/qr_scanner" -> {
                val subject = queryParams["subject"] ?: activeCameraSubject
                FaceAttendanceScreen(
                    currentSubject = subject,
                    onAttendanceRecorded = { count, total ->
                        onShowBanner("Marked $count / $total students PRESENT via Group Camera KNN!")
                    },
                    onNavigateHome = {
                        router.popBackStack()
                    }
                )
            }

            "/faculty/manual_ledger" -> {
                ManualAttendanceScreen(
                    onBack = {
                        router.navigateToRoleRoot(UserRole.FACULTY)
                    },
                    onSaveSuccess = { msg ->
                        onShowBanner(msg)
                        router.navigateToRoleRoot(UserRole.FACULTY)
                    }
                )
            }

            // === 3. HOD DASHBOARD MODULE ===
            "/hod/desk" -> {
                val tabParam = queryParams["tab"]
                val initialTab = when (tabParam) {
                    "exam_ops", "publish_exam" -> HODTab.EXAM_OPERATIONS
                    "below_75", "defaulter_triage" -> HODTab.BELOW_75_TRIAGE
                    "faculty_alloc", "faculty_audit" -> HODTab.FACULTY_ALLOCATION
                    "circulars" -> HODTab.DEPARTMENT_CIRCULARS
                    else -> HODTab.DEPARTMENT_OVERVIEW
                }
                HODPortalScreen(
                    initialTab = initialTab,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onNavigateToChat = onNavigateToChat
                )
            }

            "/hod/below_75" -> {
                HODPortalScreen(
                    initialTab = HODTab.BELOW_75_TRIAGE,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onNavigateToChat = onNavigateToChat
                )
            }

            // === 4. PRINCIPAL DASHBOARD MODULE ===
            "/principal/executive" -> {
                PrincipalPortalScreen(
                    initialSectionTab = PrincipalSectionTab.EXECUTIVE_AUDIT,
                    onBroadcastSuccess = onShowBanner,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/principal/faculty", "/principal/biometrics" -> {
                PrincipalPortalScreen(
                    initialSectionTab = PrincipalSectionTab.FACULTY_MANAGEMENT,
                    onBroadcastSuccess = onShowBanner,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/principal/governance" -> {
                PrincipalPortalScreen(
                    initialSectionTab = PrincipalSectionTab.DETENTION_ENFORCER,
                    onBroadcastSuccess = onShowBanner,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            // === 5. PARENT & ADMIN MODULES ===
            "/parent/hub" -> {
                val tabParam = queryParams["tab"]
                val initialTab = when (tabParam) {
                    "marks", "risk" -> ParentSectionTab.MARKS_AND_RISK
                    "leave" -> ParentSectionTab.LEAVE_APPLICATIONS
                    "connect", "mentor" -> ParentSectionTab.MENTOR_CONNECT
                    "settings" -> ParentSectionTab.PARENT_SETTINGS
                    else -> ParentSectionTab.CHILD_ATTENDANCE
                }
                ParentDashboardScreen(
                    initialTab = initialTab,
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/parent/risk" -> {
                ParentDashboardScreen(
                    initialTab = ParentSectionTab.MARKS_AND_RISK,
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/parent/leave" -> {
                ParentDashboardScreen(
                    initialTab = ParentSectionTab.LEAVE_APPLICATIONS,
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/parent/connect" -> {
                ParentDashboardScreen(
                    initialTab = ParentSectionTab.MENTOR_CONNECT,
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/parent/settings" -> {
                ParentDashboardScreen(
                    initialTab = ParentSectionTab.PARENT_SETTINGS,
                    onNavigateToChat = onNavigateToChat,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }

            "/super_admin/hub" -> {
                val tabParam = queryParams["tab"]
                val initialTab = when (tabParam) {
                    "institutions", "orchestrator" -> SuperAdminTab.INSTITUTIONS
                    "provisioning" -> SuperAdminTab.ACCOUNT_PROVISIONING
                    "audit", "audit_logs" -> SuperAdminTab.AUDIT_TRAIL
                    "settings" -> SuperAdminTab.SYSTEM_SETTINGS
                    else -> SuperAdminTab.PLATFORM_OVERVIEW
                }
                SuperAdminPortalScreen(
                    initialTab = initialTab,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onNavigateToChat = onNavigateToChat
                )
            }

            "/admin/hub" -> {
                val tabParam = queryParams["tab"]
                val initialTab = when (tabParam) {
                    "departments", "courses", "tenant_config" -> AdminTab.DEPARTMENTS_COURSES
                    "timetable" -> AdminTab.TIMETABLE_ALLOCATIONS
                    "analytics" -> AdminTab.CAMPUS_ANALYTICS
                    "organizations", "directory" -> AdminTab.ORGANIZATIONS
                    "issues", "cloud_backup" -> AdminTab.DIAGNOSTICS_ISSUES
                    "settings" -> AdminTab.ADMIN_SETTINGS
                    else -> AdminTab.INSTITUTION_PROFILE
                }
                AdminPortalScreen(
                    initialTab = initialTab,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onNavigateToChat = onNavigateToChat
                )
            }

            "/admin/issues" -> {
                AdminPortalScreen(
                    initialTab = AdminTab.DIAGNOSTICS_ISSUES,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onNavigateToChat = onNavigateToChat
                )
            }

            // === 6. SHARED EXAMS ===
            "/shared/exams" -> {
                AndhraExamsAndActivitiesScreen(
                    onTriggerToast = onShowBanner,
                    isDarkMode = isDarkMode
                )
            }

            "/pomodoro" -> {
                PomodoroScreen(
                    onNavigateBack = { router.popBackStack() },
                    isDarkMode = isDarkMode
                )
            }

            // === 7. AI CHATBOT SCREEN FOR ALL USER ROLES ===
            "/student/chat", "student/chat",
            "/faculty/chat", "faculty/chat",
            "/hod/chat", "hod/chat",
            "/principal/chat", "principal/chat",
            "/admin/chat", "admin/chat",
            "/super_admin/chat", "super_admin/chat",
            "/chat", "chat" -> {
                val promptParam = queryParams["prompt"]
                val roleParam = queryParams["role"]
                val cleanPath = "/" + currentPath.trim().trimStart('/')
                val effectiveRole = if (roleParam != null) {
                    try { UserRole.valueOf(roleParam) } catch (_: Exception) { activeRole }
                } else when (cleanPath) {
                    "/faculty/chat" -> UserRole.FACULTY
                    "/hod/chat" -> UserRole.HOD
                    "/principal/chat" -> UserRole.PRINCIPAL
                    "/admin/chat" -> UserRole.ADMIN
                    "/super_admin/chat" -> UserRole.SUPER_ADMIN
                    "/student/chat" -> UserRole.STUDENT
                    else -> activeRole
                }
                ChatbotScreen(
                    userRole = effectiveRole,
                    initialPrompt = promptParam,
                    isDarkMode = isDarkMode,
                    onNavigateBack = {
                        if (router.canGoBack) {
                            router.popBackStack()
                        } else {
                            router.navigateToRoleRoot(activeRole)
                        }
                    }
                )
            }

            else -> {
                // Default fallback to role root
                CampusFeedScreen(
                    userRole = activeRole,
                    isStudent = (activeRole == UserRole.STUDENT),
                    onNavigateToAttendance = {
                        router.navigate(HashRouter.STUDENT_ATTENDANCE)
                    },
                    onNavigateToChat = onNavigateToChat
                )
            }
        }
    }
}
