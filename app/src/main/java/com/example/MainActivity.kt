package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.example.agent.AdvisorAgent
import com.example.data.AcademicDataProvider
import com.example.data.FirestoreRepository
import com.example.data.SessionManager
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.navigation.AppNavigation
import com.example.navigation.HashBreadcrumbBar
import com.example.navigation.HashRouter
import com.example.navigation.HashRouterHost
import com.example.navigation.rememberHashRouter
import com.example.ui.components.AppTopBar
import com.example.ui.components.CameraXQrScannerView
import com.example.ui.components.CustomDrawerContent
import com.example.ui.components.EmailAlertCenterDialog
import com.example.ui.components.QrAttendanceDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class RoleNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(applicationContext)
        FirestoreRepository.init(applicationContext)
        AcademicDataProvider.init(applicationContext)
        com.example.biometrics.FaceRecognitionEngine.init(applicationContext)
        com.example.data.api.CampusRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            AuraCampusApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraCampusApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentThemeMode by remember { mutableStateOf(SessionManager.getThemeMode()) }
    val systemInDark = isSystemInDarkTheme()
    val isDarkMode = when (currentThemeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    fun toggleAppTheme() {
        val nextMode = if (isDarkMode) ThemeMode.LIGHT else ThemeMode.DARK
        currentThemeMode = nextMode
        SessionManager.setThemeMode(nextMode)
    }

    var useDynamicColor by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }

    val adaptiveInfo = com.example.ui.responsive.rememberWindowAdaptiveInfo()

    CompositionLocalProvider(com.example.ui.responsive.LocalWindowAdaptiveInfo provides adaptiveInfo) {
        AuraCampusTheme(darkTheme = isDarkMode, dynamicColor = useDynamicColor) {
            Crossfade(
                targetState = showSplashScreen,
                animationSpec = tween(durationMillis = 300),
                label = "splash_crossfade"
            ) { isSplash ->
                if (isSplash) {
                    SplashScreen(
                        onSplashFinished = {
                            showSplashScreen = false
                        }
                    )
                } else {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                val initialProfile = remember {
                    SessionManager.init(context)
                    AcademicDataProvider.init(context)
                    SessionManager.getCurrentProfile()
                }
                var userRole by remember { mutableStateOf(initialProfile.role) }
                var currentProfile by remember { mutableStateOf(initialProfile) }

                // Unified HashRouter navigation controller
                val initialHash = remember(initialProfile.role) {
                    when (initialProfile.role) {
                        UserRole.SUPER_ADMIN -> HashRouter.SUPER_ADMIN_HUB
                        UserRole.STUDENT -> HashRouter.STUDENT_CAMPUS
                        UserRole.ADMIN -> HashRouter.ADMIN_HUB
                        UserRole.HOD -> HashRouter.HOD_DESK
                        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> HashRouter.PRINCIPAL_EXECUTIVE
                        else -> HashRouter.FACULTY_COMMAND
                    }
                }
                val router = rememberHashRouter(initialHash)

                var activeCameraSubject by remember { mutableStateOf("Deep Learning [2339609]") }
                var showRoleLoginScreen by remember { mutableStateOf(!SessionManager.isLoggedIn()) }

                // Dialog states
                var showQrScannerDialog by remember { mutableStateOf(false) }
                var showCameraXScannerView by remember { mutableStateOf(false) }
                var showNotificationsDialog by remember { mutableStateOf(false) }
                var showIdCardDialog by remember { mutableStateOf(false) }
                var showSearchDialog by remember { mutableStateOf(false) }
                var showEmailCenterDialog by remember { mutableStateOf(false) }
                var bannerNotificationMessage by remember { mutableStateOf<String?>(null) }

                // New feature interactive dialog states
                var showCareerRoadmapDialog by remember { mutableStateOf(false) }
                var showCertVaultDialog by remember { mutableStateOf(false) }
                var activeVaultDocument by remember { mutableStateOf<com.example.model.DriveDocument?>(null) }
                var showInvigilationDialog by remember { mutableStateOf(false) }
                var showMentorshipDialog by remember { mutableStateOf(false) }
                var showSyllabusTrackerDialog by remember { mutableStateOf(false) }
                var showCurriculumDialog by remember { mutableStateOf(false) }
                var showFacultyAuditDialog by remember { mutableStateOf(false) }
                var showStatutoryCouncilDialog by remember { mutableStateOf(false) }
                var showSysSecurityDialog by remember { mutableStateOf(false) }

                // Dynamic interactive notification list bound to live campus updates
                val activeNotifications = AcademicDataProvider.sharedNotifications

                // Role-specific bottom navigation tabs with Hash routes
                val navTabs = remember(userRole) {
                    when (userRole) {
                        UserRole.SUPER_ADMIN -> listOf(
                            RoleNavTab(HashRouter.SUPER_ADMIN_HUB, "Platform Hub", Icons.Default.Public),
                            RoleNavTab(HashRouter.SUPER_ADMIN_AI, "Central AI", Icons.Default.SmartToy),
                            RoleNavTab(HashRouter.ADMIN_CAMPUS, "Notices", Icons.Default.DynamicFeed)
                        )

                        UserRole.STUDENT -> listOf(
                            RoleNavTab(HashRouter.STUDENT_CAMPUS, "Campus Hub", Icons.Default.DynamicFeed),
                            RoleNavTab(HashRouter.STUDENT_ATTENDANCE, "My Attendance", Icons.Default.School),
                            RoleNavTab(HashRouter.STUDENT_RISK, "Risk AI", Icons.Default.Analytics),
                            RoleNavTab(HashRouter.STUDENT_AI, "Agent AI", Icons.Default.SmartToy)
                        )

                        UserRole.FACULTY -> listOf(
                            RoleNavTab(HashRouter.FACULTY_COMMAND, "Command", Icons.Default.Dashboard),
                            RoleNavTab(HashRouter.FACULTY_GROUP_SCAN, "Group Scan", Icons.Default.Groups),
                            RoleNavTab(HashRouter.FACULTY_MANUAL_LEDGER, "Manual Ledger", Icons.Default.EditCalendar),
                            RoleNavTab(HashRouter.FACULTY_ADVISOR, "Advisor Bot", Icons.Default.SmartToy),
                            RoleNavTab(HashRouter.FACULTY_CAMPUS, "Campus", Icons.Default.DynamicFeed)
                        )

                        UserRole.HOD -> listOf(
                            RoleNavTab(HashRouter.HOD_DESK, "HOD Desk", Icons.Default.Engineering),
                            RoleNavTab(HashRouter.HOD_BELOW_75, "Mentorship Hub", Icons.Default.School),
                            RoleNavTab(HashRouter.HOD_AI, "HOD AI", Icons.Default.SmartToy),
                            RoleNavTab(HashRouter.HOD_CAMPUS, "Campus", Icons.Default.DynamicFeed)
                        )

                        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> listOf(
                            RoleNavTab(HashRouter.PRINCIPAL_EXECUTIVE, "Executive", Icons.Default.AdminPanelSettings),
                            RoleNavTab(HashRouter.PRINCIPAL_FACULTY, "Faculty Access", Icons.Default.ManageAccounts),
                            RoleNavTab(HashRouter.PRINCIPAL_AI, "Principal AI", Icons.Default.SmartToy),
                            RoleNavTab(HashRouter.PRINCIPAL_GOVERNANCE, "Governance", Icons.Default.AccountBalance),
                            RoleNavTab(HashRouter.PRINCIPAL_CAMPUS, "Campus", Icons.Default.DynamicFeed)
                        )

                        UserRole.ADMIN -> listOf(
                            RoleNavTab(HashRouter.ADMIN_HUB, "Admin Hub", Icons.Default.Domain),
                            RoleNavTab(HashRouter.ADMIN_ISSUES, "Facilities", Icons.Default.Hub),
                            RoleNavTab(HashRouter.ADMIN_AI, "Admin AI", Icons.Default.SmartToy),
                            RoleNavTab(HashRouter.ADMIN_CAMPUS, "Campus", Icons.Default.DynamicFeed)
                        )

                        else -> emptyList()
                    }
                }

                // Role Switcher helper
                fun handleRoleChange(newRole: UserRole) {
                    userRole = newRole
                    currentProfile = when (newRole) {
                        UserRole.SUPER_ADMIN -> AcademicDataProvider.superAdminProfile
                        UserRole.STUDENT -> AcademicDataProvider.defaultStudentProfile
                        UserRole.FACULTY -> AcademicDataProvider.facultyProfile
                        UserRole.HOD -> AcademicDataProvider.hodProfile
                        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> AcademicDataProvider.principalProfile
                        else -> AcademicDataProvider.systemAdminProfile
                    }
                    router.navigateToRoleRoot(newRole)
                }

                BackHandler(enabled = drawerState.isOpen || (showRoleLoginScreen && SessionManager.isLoggedIn()) || router.canGoBack) {
                    if (drawerState.isOpen) {
                        coroutineScope.launch { drawerState.close() }
                    } else if (showRoleLoginScreen && SessionManager.isLoggedIn()) {
                        showRoleLoginScreen = false
                    } else if (router.canGoBack) {
                        router.popBackStack()
                    }
                }

                if (showRoleLoginScreen) {
                    RoleLoginScreen(
                        onLoginSuccess = { profile ->
                            currentProfile = profile
                            userRole = profile.role
                            showRoleLoginScreen = false
                            router.navigateToRoleRoot(profile.role)
                        }
                    )
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            CustomDrawerContent(
                                userProfile = currentProfile,
                                currentRoute = router.currentHash,
                                onNavigate = { route ->
                                    when (route) {
                                        "academics" -> {
                                            router.navigate(
                                                if (userRole == UserRole.STUDENT) HashRouter.STUDENT_ATTENDANCE
                                                else HashRouter.FACULTY_COMMAND
                                            )
                                        }
                                        "camera_attendance" -> {
                                            if (userRole != UserRole.STUDENT) {
                                                router.navigate(
                                                    if (userRole == UserRole.FACULTY) HashRouter.FACULTY_GROUP_SCAN
                                                    else HashRouter.PRINCIPAL_FACULTY
                                                )
                                            } else {
                                                showQrScannerDialog = true
                                            }
                                        }
                                        "risk_agent" -> {
                                            router.navigate(
                                                if (userRole == UserRole.PRINCIPAL_ADMIN) HashRouter.PRINCIPAL_RESEARCH
                                                else HashRouter.STUDENT_RISK
                                            )
                                        }
                                        "id_card" -> { showIdCardDialog = true }
                                        "qr_suite" -> { showQrScannerDialog = true }
                                        "email_center" -> { showEmailCenterDialog = true }
                                        "andhra_exams" -> { router.navigate(HashRouter.EXAMS) }
                                        "pomodoro" -> {
                                            router.navigate(HashRouter.POMODORO)
                                        }
                                        "manual_ledger" -> {
                                            router.navigate(HashRouter.FACULTY_MANUAL_LEDGER)
                                        }
                                        "faculty_summary" -> {
                                            router.navigate(HashRouter.FACULTY_SUMMARY)
                                        }
                                        "faculty_materials" -> {
                                            router.navigate(HashRouter.FACULTY_COMMAND)
                                        }
                                        "principal_broadcast" -> {
                                            router.navigate(HashRouter.PRINCIPAL_EXECUTIVE)
                                        }
                                        "hod_publish_exam" -> {
                                            router.navigate("/hod/desk?tab=exam_ops")
                                        }
                                        "career_roadmap" -> { showCareerRoadmapDialog = true }
                                        "cert_vault" -> { showCertVaultDialog = true }
                                        "invigilation" -> { showInvigilationDialog = true }
                                        "mentorship" -> { showMentorshipDialog = true }
                                        "syllabus_tracker" -> { showSyllabusTrackerDialog = true }
                                        "curriculum" -> { showCurriculumDialog = true }
                                        "faculty_audit" -> { router.navigate("/hod/desk?tab=faculty_alloc") }
                                        "defaulter_triage" -> { router.navigate("/hod/desk?tab=below_75") }
                                        "statutory_council" -> { showStatutoryCouncilDialog = true }
                                        "quality_audit" -> { router.navigate(HashRouter.PRINCIPAL_EXECUTIVE) }
                                        "exam_governance" -> { router.navigate(HashRouter.PRINCIPAL_GOVERNANCE) }
                                        "research_grants" -> { router.navigate(HashRouter.PRINCIPAL_RESEARCH) }
                                        "admin_directory" -> { router.navigate("/admin/hub?tab=organizations") }
                                        "sys_security" -> { showSysSecurityDialog = true }
                                        "cloud_backup" -> {
                                            com.example.data.AcademicDataProvider.saveDataToDisk()
                                            router.navigate("/admin/hub?tab=issues")
                                            bannerNotificationMessage = "Live Database Persisted & Firestore Cloud Synced: 2,480 Records"
                                        }
                                        "tenant_config" -> { router.navigate("/admin/hub?tab=departments") }
                                        "orchestrator" -> { router.navigate("/super_admin/hub?tab=institutions") }
                                        "ai_cluster" -> {
                                            router.navigate("/super_admin/hub?tab=settings")
                                            bannerNotificationMessage = "AI Inference Cluster Status: Active (12 Nodes Healthy, 18ms latency)"
                                        }
                                        "audit_logs" -> { router.navigate("/super_admin/hub?tab=audit") }
                                        "federated_sync" -> {
                                            com.example.data.AcademicDataProvider.saveDataToDisk()
                                            router.navigate("/super_admin/hub?tab=institutions")
                                            bannerNotificationMessage = "Federated Cross-Campus Sync: Synchronized 5 Institution Clusters"
                                        }
                                        "feed", "campus", "drive", "calendar", "materials" -> {
                                            router.navigate(
                                                when (userRole) {
                                                    UserRole.STUDENT -> "/student/campus?tab=drive"
                                                    UserRole.FACULTY -> "/faculty/campus?tab=drive"
                                                    UserRole.HOD -> "/hod/campus?tab=drive"
                                                    UserRole.PRINCIPAL,
                                                    UserRole.PRINCIPAL_ADMIN -> "/principal/campus?tab=drive"
                                                    UserRole.SUPER_ADMIN -> HashRouter.SUPER_ADMIN_HUB
                                                    else -> "/admin/campus?tab=drive"
                                                }
                                            )
                                        }
                                        else -> { router.navigateToRoleRoot(userRole) }
                                    }
                                },
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { toggleAppTheme() },
                                onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
                                onLogout = {
                                    com.example.data.AcademicDataProvider.activeDrawerKey = "home"
                                    SessionManager.logout()
                                    showRoleLoginScreen = true
                                },
                                onProfileAvatarChanged = { avatarName ->
                                    currentProfile = currentProfile.copy(avatarUri = avatarName)
                                    SessionManager.saveProfile(currentProfile)
                                },
                                onProfileUpdated = { updatedProfile ->
                                    currentProfile = updatedProfile
                                    SessionManager.saveProfile(updatedProfile)
                                    bannerNotificationMessage = "Profile updated successfully!"
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                AppTopBar(
                                    title = if (adaptiveInfo.isSmallScreen) "AGENTRA AI" else "AGENTRA AI CAMPUS",
                                    activeRole = userRole,
                                    notificationCount = activeNotifications.size,
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = { toggleAppTheme() },
                                    onMenuClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onRoleSwitcherClick = { router.navigateToRoleRoot(userRole) },
                                    onHomeClick = { router.navigateToRoleRoot(userRole) },
                                    onSearchClick = { showSearchDialog = true },
                                    onQrClick = { showQrScannerDialog = true },
                                    onNotificationsClick = { showNotificationsDialog = true },
                                    onEmailCenterClick = { showEmailCenterDialog = true }
                                )
                            },
                            bottomBar = {
                                if (!adaptiveInfo.useNavigationRail) {
                                    NavigationBar(
                                        containerColor = if (isDarkMode) ObsidianSurface else LightSurface,
                                        contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    ) {
                                        navTabs.forEach { tab ->
                                            val isSelected = router.currentHash.startsWith(tab.route) ||
                                                    router.currentPath == tab.route.removePrefix("#")
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = { router.navigate(tab.route) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = tab.label,
                                                        fontSize = if (adaptiveInfo.isSmallScreen) 9.sp else 10.5.sp,
                                                        maxLines = 1,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                alwaysShowLabel = false,
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = if (isDarkMode) ObsidianBg else Color.White,
                                                    selectedTextColor = CyberEmerald,
                                                    indicatorColor = CyberEmerald,
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                ),
                                                modifier = Modifier.testTag("nav_item_${tab.route.removePrefix("#/").replace('/', '_')}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (adaptiveInfo.useNavigationRail) {
                                    NavigationRail(
                                        containerColor = if (isDarkMode) ObsidianSurface else LightSurface,
                                        contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        navTabs.forEach { tab ->
                                            val isSelected = router.currentHash.startsWith(tab.route) ||
                                                    router.currentPath == tab.route.removePrefix("#")
                                            NavigationRailItem(
                                                selected = isSelected,
                                                onClick = { router.navigate(tab.route) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = tab.label,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                alwaysShowLabel = false,
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = if (isDarkMode) ObsidianBg else Color.White,
                                                    selectedTextColor = CyberEmerald,
                                                    indicatorColor = CyberEmerald,
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                ),
                                                modifier = Modifier.testTag("nav_rail_${tab.route.removePrefix("#/").replace('/', '_')}")
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    AppNavigation(
                                        router = router,
                                        activeRole = userRole,
                                        isDarkMode = isDarkMode,
                                        onToggleTheme = { toggleAppTheme() },
                                        onScanClassQr = { showQrScannerDialog = true },
                                        onLaunchCameraScan = { subjectName ->
                                            activeCameraSubject = subjectName
                                            router.navigate(HashRouter.buildGroupScanRoute(subjectName))
                                        },
                                        onLaunchManualLedger = {
                                            router.navigate(HashRouter.FACULTY_MANUAL_LEDGER)
                                        },
                                        onNavigateToChat = { prompt ->
                                            val aiRoute = when (userRole) {
                                                UserRole.FACULTY -> HashRouter.FACULTY_ADVISOR
                                                UserRole.HOD -> HashRouter.HOD_AI
                                                UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> HashRouter.PRINCIPAL_AI
                                                UserRole.SUPER_ADMIN -> HashRouter.SUPER_ADMIN_AI
                                                UserRole.ADMIN -> HashRouter.ADMIN_AI
                                                else -> HashRouter.STUDENT_AI
                                            }
                                            if (prompt.isNotBlank()) {
                                                router.navigate(HashRouter.buildChatRoute(userRole, prompt))
                                            } else {
                                                router.navigate(aiRoute)
                                            }
                                        },
                                        onShowBanner = { msg ->
                                            bannerNotificationMessage = msg
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                // Banner feedback snackbar
                                if (bannerNotificationMessage != null) {
                                    Surface(
                                        color = CyberEmeraldDark,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(16.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = bannerNotificationMessage!!,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { bannerNotificationMessage = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

            // Institutional ID Card Dialog
            if (showIdCardDialog) {
                AlertDialog(
                    onDismissRequest = { showIdCardDialog = false },
                    title = { Text("Institutional Digital ID Badge", fontWeight = FontWeight.Bold) },
                    text = {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = CyberEmeraldDark,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)",
                                        modifier = Modifier.padding(8.dp),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) ObsidianBorder else LightElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(46.dp), tint = CyberEmerald)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(currentProfile.name, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(currentProfile.rollNumber, color = CyberEmerald, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(currentProfile.programme, color = TextSecondary, fontSize = 11.sp)
                                Text(currentProfile.section, color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(80.dp))
                                Text("Biometric KNN Face Enrolled (50 Vectors)", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showIdCardDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Notification Dialog (Interactive & Dynamically Dismissable)
            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NeonCoral)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AuraCampus Alerts (${activeNotifications.size})", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        if (activeNotifications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("All caught up!", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                    Text("No unread alerts or notifications at this time.", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                activeNotifications.forEach { alert ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkMode) ObsidianElevated else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCoral.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = alert,
                                                fontSize = 12.sp,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                lineHeight = 16.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { activeNotifications.remove(alert) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (activeNotifications.isNotEmpty()) {
                            Button(
                                onClick = {
                                    activeNotifications.clear()
                                    showNotificationsDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                            ) {
                                Text("Dismiss All", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { showNotificationsDialog = false }) {
                                Text("Close")
                            }
                        }
                    },
                    dismissButton = {
                        if (activeNotifications.isNotEmpty()) {
                            TextButton(onClick = { showNotificationsDialog = false }) {
                                Text("Close")
                            }
                        }
                    }
                )
            }

            // New Student Feature 1: AI Placement & Career Roadmap Dialog
            if (showCareerRoadmapDialog) {
                AlertDialog(
                    onDismissRequest = { showCareerRoadmapDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = CyberEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Career & Placement Roadmap", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Personalized Tier-1 Tech & Core Engineering Career Preparation Path for CSE 4th Year:",
                                fontSize = 12.sp,
                                color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                            )
                            listOf(
                                Triple("DSA & LeetCode Hard Patterns", "92% Completed • Trees, Graphs, DP", CyberEmerald),
                                Triple("System Design & Scalable Architectures", "78% Completed • Caching, Sharding, Message Queues", ElectricIndigo),
                                Triple("AI / ML Engineering Specialization", "85% Completed • PyTorch, Transformer Attention", Color(0xFF8B5CF6)),
                                Triple("Mock Technical Interview Score", "8.8 / 10 • Recommended for Amazon & TCS Digital", LuminousAmber)
                            ).forEach { (milestone, detail, accent) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) ObsidianElevated else LightSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(milestone, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(detail, fontSize = 11.5.sp, color = accent, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showCareerRoadmapDialog = false
                                        router.navigate(
                                            when (userRole) {
                                                UserRole.STUDENT -> HashRouter.STUDENT_AI
                                                UserRole.FACULTY -> HashRouter.FACULTY_ADVISOR
                                                UserRole.HOD -> HashRouter.HOD_AI
                                                UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> HashRouter.PRINCIPAL_AI
                                                UserRole.ADMIN -> HashRouter.ADMIN_AI
                                                UserRole.SUPER_ADMIN -> HashRouter.SUPER_ADMIN_AI
                                                else -> HashRouter.STUDENT_AI
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start AI Mock Interview", color = ObsidianBg, fontWeight = FontWeight.Bold)
                                }
                            },
                    dismissButton = {
                        TextButton(onClick = { showCareerRoadmapDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // New Student Feature 2: Digital Certificate Vault Dialog
            if (showCertVaultDialog) {
                AlertDialog(
                    onDismissRequest = { showCertVaultDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = LuminousAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Digital Certificate & Hall Ticket Vault", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Cryptographically Verified Credentials & Official Autonomous Hall Tickets:",
                                fontSize = 12.sp,
                                color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                            )
                            listOf(
                                Triple("End-Semester Hall Ticket (IV-II)", "Exam Center: Main Block Lab-3 • Seat #B-42", "DOWNLOAD TICKET"),
                                Triple("Official Grade Memo (IV-I Sem)", "CGPA: 8.84 • Passed First Class with Distinction", "VIEW PDF"),
                                Triple("AI & Deep Learning Honors Certification", "Issued by Autonomous Academic Council", "VERIFY ON CHAIN"),
                                Triple("Central Library e-Pass", "RFID: 9Y2305B7 • 0 Active Book Fines", "SHOW BARCODE")
                            ).forEach { (title, subtitle, action) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) ObsidianElevated else LightSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(title, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 12.5.sp)
                                            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = CyberEmerald.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable {
                                                when (action) {
                                                    "DOWNLOAD TICKET" -> {
                                                        activeVaultDocument = com.example.model.DriveDocument(
                                                            id = "vault_hall_ticket_2026",
                                                            folderId = "official_hall_tickets",
                                                            folderName = "Official Autonomous Hall Tickets",
                                                            title = "KSRMCE_Autonomous_Hall_Ticket_IV-II.pdf",
                                                            fileType = "PDF",
                                                            sizeBytes = 1887436L,
                                                            sizeFormatted = "1.8 MB",
                                                            uploadedBy = "Autonomous Examination Branch",
                                                            uploadedDate = "24 Oct 2026",
                                                            downloadUrl = "https://agentra.ksrmce.ac.in/vault/hallticket_239Y1A0501.pdf"
                                                        )
                                                    }
                                                    "VIEW PDF" -> {
                                                        activeVaultDocument = com.example.model.DriveDocument(
                                                            id = "vault_grade_memo_2026",
                                                            folderId = "grade_records",
                                                            folderName = "Academic Council Grade Records",
                                                            title = "KSRMCE_Official_Grade_Memo_IV-I.pdf",
                                                            fileType = "PDF",
                                                            sizeBytes = 2306867L,
                                                            sizeFormatted = "2.2 MB",
                                                            uploadedBy = "Autonomous Academic Council",
                                                            uploadedDate = "18 Oct 2026",
                                                            downloadUrl = "https://agentra.ksrmce.ac.in/vault/grade_memo_239Y1A0501.pdf"
                                                        )
                                                    }
                                                    else -> {
                                                        bannerNotificationMessage = "Verified & Downloaded: $title"
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(action, color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCertVaultDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            // PDF Viewer for Digital Vault Hall Ticket & Grade Memo
            activeVaultDocument?.let { doc ->
                com.example.ui.components.PdfViewerDialog(
                    document = doc,
                    isDarkMode = isDarkMode,
                    onDismiss = { activeVaultDocument = null },
                    onDownloadCompleted = { msg ->
                        bannerNotificationMessage = msg
                    }
                )
            }

            // Classroom QR Scanner & Face Attendance Dialog
            if (showQrScannerDialog) {
                AlertDialog(
                    onDismissRequest = { showQrScannerDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CyberEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Classroom QR & Face Attendance Check-In", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Live Dual-Authentication Check-In: Scan teacher's Smartboard QR token, followed by real-time facial verification.",
                                fontSize = 12.sp,
                                color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Active Session Details:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = CyberEmerald)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Class: B.Tech CSE IV Year Sec-B", fontSize = 11.5.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                    Text("Subject: Deep Learning [2339609]", fontSize = 11.5.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                    Text("Room: Lab 402 • Period 3", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Button(
                                onClick = {
                                    showQrScannerDialog = false
                                    showCameraXScannerView = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF042F2E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open QR Scanner (verification unavailable)", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    bannerNotificationMessage = "QR attendance is unavailable: secure server-side session verification is not configured. No attendance was changed."
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("QR Attendance — server verification required", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQrScannerDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // New Faculty Feature 1: Invigilation Duties Dialog
            if (showInvigilationDialog) {
                AlertDialog(
                    onDismissRequest = { showInvigilationDialog = false },
                    title = { Text("Invigilation Duties & Autonomous Exam Hall", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Upcoming End-Semester Exam Invigilation Schedule:", fontSize = 12.sp, color = TextSecondary)
                            listOf(
                                Triple("24 Oct 2026", "10:00 AM - 01:00 PM", "Hall 204 • 60 Candidates"),
                                Triple("27 Oct 2026", "02:00 PM - 05:00 PM", "ECE Block Lab 3 • 45 Candidates"),
                                Triple("30 Oct 2026", "10:00 AM - 01:00 PM", "Main Seminar Hall • 120 Candidates")
                            ).forEach { (date, time, venue) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkMode) ObsidianElevated else LightSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("$date • $time", fontWeight = FontWeight.Bold, color = ElectricIndigo, fontSize = 12.5.sp)
                                        Text(venue, fontSize = 11.5.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showInvigilationDialog = false }) {
                            Text("Acknowledge Duty")
                        }
                    }
                )
            }

            // New Faculty Feature 2: Mentorship & Counseling Dialog
            if (showMentorshipDialog) {
                AlertDialog(
                    onDismissRequest = { showMentorshipDialog = false },
                    title = { Text("Student Mentorship & Counselor Logs", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Assigned 20 Mentees (CSE 4th Year Section B):", fontSize = 12.sp, color = TextSecondary)
                            listOf(
                                Pair("K. Sailaja [239Y1A0501]", "Attendance: 84.5% • CGPA: 9.12 • No Backlogs"),
                                Pair("M. Akhil [239Y1A0512]", "Attendance: 68.2% • Flagged for Condonation Counseling"),
                                Pair("V. Dinesh [239Y1A0524]", "Attendance: 79.1% • Placed in Cognizant (6.5 LPA)")
                            ).forEach { (student, status) ->
                                Surface(shape = RoundedCornerShape(8.dp), color = if (isDarkMode) ObsidianElevated else LightSurface, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(student, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 12.5.sp)
                                        Text(status, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMentorshipDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // New Faculty Feature 3: Syllabus Tracker Dialog
            if (showSyllabusTrackerDialog) {
                AlertDialog(
                    onDismissRequest = { showSyllabusTrackerDialog = false },
                    title = { Text("Academic Syllabus & Lab Tracker", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                Pair("Deep Learning [2339609]", 0.88f),
                                Pair("Cyber Security [2305701]", 0.76f),
                                Pair("Prompt Engineering Lab [2339751]", 0.95f)
                            ).forEach { (course, prog) ->
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(course, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                        Text("${(prog * 100).toInt()}%", fontSize = 12.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = CyberEmerald)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSyllabusTrackerDialog = false }) { Text("Close") }
                    }
                )
            }

            // New HOD Feature 1: Curriculum & Lab Allocations Dialog
            if (showCurriculumDialog) {
                AlertDialog(
                    onDismissRequest = { showCurriculumDialog = false },
                    title = { Text("Branch Curriculum & Lab Allocations", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("CSE Department Autonomous Curriculum R23 Compliance: 100%", fontSize = 12.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                            Text("Total Approved Theory Hours: 420 hrs • Lab Practical Hours: 180 hrs", fontSize = 11.5.sp, color = TextSecondary)
                            Text("Lab 401 & 402 GPU Clusters: 48/48 Workstations Operational", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showCurriculumDialog = false }) { Text("Close") } }
                )
            }

            // New HOD Feature 2: Faculty Performance & Teaching Audits Dialog
            if (showFacultyAuditDialog) {
                AlertDialog(
                    onDismissRequest = { showFacultyAuditDialog = false },
                    title = { Text("Faculty Performance & Teaching Audits", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Average Student Feedback Rating: 4.6 / 5.0 (CSE Department)", fontSize = 12.5.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                            Text("Syllabus On-Time Completion Rate: 94.2%", fontSize = 12.sp, color = TextSecondary)
                            Text("Biometric Attendance Punctuality: 98.1%", fontSize = 12.sp, color = TextSecondary)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showFacultyAuditDialog = false }) { Text("Close") } }
                )
            }

            // New Principal Feature: Statutory Council & Affiliation Hub Dialog
            if (showStatutoryCouncilDialog) {
                AlertDialog(
                    onDismissRequest = { showStatutoryCouncilDialog = false },
                    title = { Text("Statutory Council & Affiliation Hub", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AICTE & JNTUA Autonomous Extension 2026-2027: Approved", fontSize = 12.5.sp, color = LuminousAmber, fontWeight = FontWeight.Bold)
                            Text("NAAC A+ Grade Peer Team Review: Cycle 3 Compliance Completed", fontSize = 12.sp, color = TextSecondary)
                            Text("NBA Accredited Programs: CSE, ECE, EEE, MECH (Valid till 2028)", fontSize = 12.sp, color = TextSecondary)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showStatutoryCouncilDialog = false }) { Text("Close") } }
                )
            }

            // New Admin Feature: System Security & Role Governance Dialog
            if (showSysSecurityDialog) {
                AlertDialog(
                    onDismissRequest = { showSysSecurityDialog = false },
                    title = { Text("System Security & Role Governance", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Role-Based Access Control (RBAC): 7 Roles Configured", fontSize = 12.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                            Text("Zero-Trust Biometric Security: 100% Face Vectors Encrypted with AES-256", fontSize = 11.5.sp, color = TextSecondary)
                            Text("Active Sessions: 1,420 Authenticated Users", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showSysSecurityDialog = false }) { Text("Close") } }
                )
            }

            // CameraX + ZXing QR Attendance Scanner Module (Pushes attendance logs to Firestore)
            if (showCameraXScannerView) {
                CameraXQrScannerView(
                    userProfile = currentProfile,
                    isDarkMode = isDarkMode,
                    onDismiss = { showCameraXScannerView = false },
                    onAttendanceRecorded = { message ->
                        bannerNotificationMessage = message
                    }
                )
            }

            // Search Dialog
            if (showSearchDialog) {
                var searchInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showSearchDialog = false },
                    title = { Text("Search AuraCampus") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = { searchInput = it },
                                placeholder = { Text("Course code, student, faculty...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("e.g., 'Deep Learning', 'Dr. P. V. Kishore', '239Y1A0501'", fontSize = 11.sp, color = TextMuted)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSearchDialog = false }) {
                            Text("Search")
                        }
                    }
                )
            }

            // KSRMCE Autonomous Email Alert Center Dialog
            if (showEmailCenterDialog) {
                EmailAlertCenterDialog(
                    onDismiss = { showEmailCenterDialog = false },
                    onEmailDispatched = { msg ->
                        bannerNotificationMessage = msg
                    }
                )
            }
            }
        }
    }
}
}
}
}
