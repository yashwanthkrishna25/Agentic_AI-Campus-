package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.data.AcademicDataProvider
import com.example.ui.theme.*

/**
 * Role-tailored Navigation Drawer for AGENTRA AI CAMPUS.
 * Complete adaptive light/dark theming and working-based workflow options for every role.
 * - Single Sign Out / Logout button pinned at the bottom of the drawer.
 * - Selected drawer item receives royal purple accent highlight (Color(0xFF8B5CF6)).
 * - Profile card supports avatar/photo upload and editing.
 * - Cleaned and consolidated navigation items tailored per user role.
 */
@Composable
fun CustomDrawerContent(
    userProfile: UserProfile,
    currentRoute: String = "home",
    onNavigate: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    onProfileAvatarChanged: ((String) -> Unit)? = null,
    onProfileUpdated: ((UserProfile) -> Unit)? = null
) {
    var showAvatarDialog by remember { mutableStateOf(false) }
    var selectedAvatarName by remember { mutableStateOf(userProfile.avatarUri ?: "avatar_default") }

    val roleThemeColor = when (userProfile.role) {
        UserRole.STUDENT -> CyberEmerald
        UserRole.FACULTY -> ElectricIndigo
        UserRole.HOD -> Color(0xFF8B5CF6)
        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> LuminousAmber
        UserRole.ADMIN -> Color(0xFFEF4444)
        UserRole.SUPER_ADMIN -> Color(0xFF8B5CF6)
        else -> CyberEmerald
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedAvatarName = uri.toString()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.82f)
            .widthIn(min = 260.dp, max = 320.dp),
        color = if (isDarkMode) ObsidianSurface else LightBg
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable Menu Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
            ) {
                // Profile Card with Photo Edit & ID Badge (Quick logout removed to keep single logout)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDarkMode) ObsidianElevated else LightSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                    shadowElevation = if (isDarkMode) 4.dp else 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar with interactive edit badge overlay
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { showAvatarDialog = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(roleThemeColor.copy(alpha = 0.2f))
                                        .border(2.dp, roleThemeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val customBitmap = remember(userProfile.avatarUri) {
                                        if (userProfile.avatarUri?.startsWith("content://") == true || userProfile.avatarUri?.startsWith("file://") == true) {
                                            try {
                                                val uri = android.net.Uri.parse(userProfile.avatarUri)
                                                val stream = context.contentResolver.openInputStream(uri)
                                                android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                            } catch (_: Exception) {
                                                null
                                            }
                                        } else null
                                    }

                                    if (customBitmap != null) {
                                        Image(
                                            bitmap = customBitmap,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        val avatarIcon = when (userProfile.avatarUri) {
                                            "Scholar (Male)" -> Icons.Default.School
                                            "Scholar (Female)" -> Icons.Default.Face
                                            "Faculty Mentor (Male)" -> Icons.Default.SupervisorAccount
                                            "Faculty Mentor (Female)" -> Icons.Default.PersonPin
                                            "Head of Department (Male)" -> Icons.Default.Engineering
                                            "Head of Department (Female)" -> Icons.Default.Psychology
                                            "College Principal" -> Icons.Default.AdminPanelSettings
                                            "College Administrator" -> Icons.Default.Domain
                                            "Super Administrator" -> Icons.Default.Language
                                            else -> when (userProfile.role) {
                                                UserRole.STUDENT -> Icons.Default.School
                                                UserRole.FACULTY -> Icons.Default.SupervisorAccount
                                                UserRole.HOD -> Icons.Default.Engineering
                                                UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> Icons.Default.AdminPanelSettings
                                                UserRole.ADMIN -> Icons.Default.Domain
                                                UserRole.SUPER_ADMIN -> Icons.Default.Language
                                                else -> Icons.Default.AccountCircle
                                            }
                                        }
                                        Icon(
                                            imageVector = avatarIcon,
                                            contentDescription = "Profile Avatar",
                                            modifier = Modifier.size(28.dp),
                                            tint = roleThemeColor
                                        )
                                    }
                                }
                                // Edit Camera Badge
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6))
                                        .border(1.5.dp, if (isDarkMode) ObsidianElevated else LightSurface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Edit Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${userProfile.rollNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDarkMode) TextMuted else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onNavigate("id_card"); onCloseDrawer() },
                                modifier = Modifier.testTag("student_id_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = "ID Badge",
                                    tint = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = roleThemeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = userProfile.role.displayName.uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = roleThemeColor,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Text(
                                text = userProfile.department,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDarkMode) TextSecondary else Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Navigation Sections tailored to Current Role's Daily Workflow
                DrawerSectionHeader(title = "${userProfile.role.displayName} Workspace", isDarkMode = isDarkMode)

                                // Dynamic drawer selection tracking that moves royal purple highlight to whichever item is clicked
                val selectedDrawerKey = AcademicDataProvider.activeDrawerKey

                LaunchedEffect(currentRoute) {
                    val matchingKey = when {
                        currentRoute.contains("pomodoro") -> "pomodoro"
                        currentRoute.contains("andhra_exams") || currentRoute.contains("exams") -> "andhra_exams"
                        currentRoute.contains("career_roadmap") -> "career_roadmap"
                        currentRoute.contains("cert_vault") -> "cert_vault"
                        currentRoute.contains("qr_suite") || currentRoute.contains("qr") -> "qr_suite"
                        currentRoute.contains("faculty_summary") -> "faculty_summary"
                        currentRoute.contains("faculty_materials") -> "faculty_materials"
                        currentRoute.contains("manual_ledger") -> "manual_ledger"
                        currentRoute.contains("camera_attendance") || currentRoute.contains("group_scan") -> "camera_attendance"
                        currentRoute.contains("invigilation") -> "invigilation"
                        currentRoute.contains("mentorship") -> "mentorship"
                        currentRoute.contains("syllabus_tracker") -> "syllabus_tracker"
                        currentRoute.contains("hod_publish_exam") || currentRoute.contains("exam_ops") -> "hod_publish_exam"
                        currentRoute.contains("curriculum") -> "curriculum"
                        currentRoute.contains("faculty_audit") || currentRoute.contains("faculty_alloc") -> "faculty_audit"
                        currentRoute.contains("defaulter_triage") || currentRoute.contains("below_75") -> "defaulter_triage"
                        currentRoute.contains("principal_broadcast") -> "principal_broadcast"
                        currentRoute.contains("statutory_council") -> "statutory_council"
                        currentRoute.contains("exam_governance") || currentRoute.contains("governance") -> "exam_governance"
                        currentRoute.contains("research_grants") || currentRoute.contains("research") -> "research_grants"
                        currentRoute.contains("quality_audit") -> "quality_audit"
                        currentRoute.contains("admin_directory") || currentRoute.contains("organizations") -> "admin_directory"
                        currentRoute.contains("sys_security") -> "sys_security"
                        currentRoute.contains("cloud_backup") -> "cloud_backup"
                        currentRoute.contains("tenant_config") || currentRoute.contains("departments") -> "tenant_config"
                        currentRoute.contains("issues") -> "issues"
                        currentRoute.contains("orchestrator") || currentRoute.contains("institutions") -> "orchestrator"
                        currentRoute.contains("ai_cluster") || currentRoute.contains("settings") -> "ai_cluster"
                        currentRoute.contains("audit_logs") || currentRoute.contains("audit") -> "audit_logs"
                        currentRoute.contains("federated_sync") -> "federated_sync"
                        currentRoute.contains("drive") || currentRoute.contains("materials") -> "drive"
                        currentRoute.contains("risk") -> "risk_agent"
                        else -> null
                    }
                    if (matchingKey != null) {
                        AcademicDataProvider.activeDrawerKey = matchingKey
                    }
                }

                fun selectItem(key: String) {
                    AcademicDataProvider.activeDrawerKey = key
                    onNavigate(key)
                    onCloseDrawer()
                }

                DrawerItem(
                    icon = Icons.Default.Dashboard,
                    title = "${userProfile.role.displayName} Command Home",
                    isSelected = selectedDrawerKey == "home",
                    isDarkMode = isDarkMode,
                    onClick = { selectItem("home") }
                )

                when (userProfile.role) {
                    UserRole.STUDENT -> {
                        DrawerItem(
                            icon = Icons.Default.Timer,
                            title = "Pomodoro Study Sessions",
                            isSelected = selectedDrawerKey == "pomodoro",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("pomodoro") }
                        )
                        DrawerItem(
                            icon = Icons.Default.AssignmentTurnedIn,
                            title = "Autonomous Academic & Exam Portal",
                            isSelected = selectedDrawerKey == "andhra_exams",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("andhra_exams") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Psychology,
                            title = "AI Career & Placement Roadmap",
                            isSelected = selectedDrawerKey == "career_roadmap",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("career_roadmap") }
                        )
                        DrawerItem(
                            icon = Icons.Default.WorkspacePremium,
                            title = "Digital Certificate & Hall Ticket Vault",
                            isSelected = selectedDrawerKey == "cert_vault",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("cert_vault") }
                        )
                        DrawerItem(
                            icon = Icons.Default.CameraAlt,
                            title = "Classroom QR Scanner (CameraX)",
                            isSelected = selectedDrawerKey == "qr_suite",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("qr_suite") }
                        )
                        DrawerItem(
                            icon = Icons.Default.PictureAsPdf,
                            title = "Study Materials & Faculty PDFs",
                            isSelected = selectedDrawerKey == "drive",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("drive") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Analytics,
                            title = "Attendance Recovery Simulator (<75% Alert)",
                            isSelected = selectedDrawerKey == "risk_agent",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("risk_agent") }
                        )
                        DrawerItem(
                            icon = Icons.Default.DynamicFeed,
                            title = "Campus Feed & Student Discussion",
                            isSelected = selectedDrawerKey == "feed",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("feed") }
                        )
                    }

                    UserRole.FACULTY -> {
                        DrawerItem(
                            icon = Icons.Default.BarChart,
                            title = "Subject Attendance Summary & Charts",
                            isSelected = selectedDrawerKey == "faculty_summary",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("faculty_summary") }
                        )
                        DrawerItem(
                            icon = Icons.Default.PictureAsPdf,
                            title = "Study Materials & Upload PDFs",
                            isSelected = selectedDrawerKey == "faculty_materials",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("faculty_materials") }
                        )
                        DrawerItem(
                            icon = Icons.Default.QrCode2,
                            title = "Class Attendance & Smartboard QR",
                            isSelected = selectedDrawerKey == "qr_suite",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("qr_suite") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Groups,
                            title = "Group AI Face Attendance (40-50 Students)",
                            isSelected = selectedDrawerKey == "camera_attendance",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("camera_attendance") }
                        )
                        DrawerItem(
                            icon = Icons.Default.EditCalendar,
                            title = "Manual Attendance Ledger (Edit P/A)",
                            isSelected = selectedDrawerKey == "manual_ledger",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("manual_ledger") }
                        )
                        DrawerItem(
                            icon = Icons.Default.FactCheck,
                            title = "Invigilation Duties & Exam Hall Seating",
                            isSelected = selectedDrawerKey == "invigilation",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("invigilation") }
                        )
                        DrawerItem(
                            icon = Icons.Default.SupportAgent,
                            title = "Student Mentorship & Counseling Logs",
                            isSelected = selectedDrawerKey == "mentorship",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("mentorship") }
                        )
                        DrawerItem(
                            icon = Icons.Default.MenuBook,
                            title = "Academic Syllabus & Lab Tracker",
                            isSelected = selectedDrawerKey == "syllabus_tracker",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("syllabus_tracker") }
                        )
                        DrawerItem(
                            icon = Icons.Default.DynamicFeed,
                            title = "Campus Feed & College Notices",
                            isSelected = selectedDrawerKey == "drive",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("drive") }
                        )
                    }

                    UserRole.HOD -> {
                        DrawerItem(
                            icon = Icons.Default.DateRange,
                            title = "Publish Exam Schedules & Results",
                            isSelected = selectedDrawerKey == "hod_publish_exam",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("hod_publish_exam") }
                        )
                        DrawerItem(
                            icon = Icons.Default.AccountTree,
                            title = "Branch Curriculum & Lab Allocations",
                            isSelected = selectedDrawerKey == "curriculum",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("curriculum") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Assessment,
                            title = "Faculty Performance & Teaching Audits",
                            isSelected = selectedDrawerKey == "faculty_audit",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("faculty_audit") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Warning,
                            title = "AI Department Defaulter Triage (< 75%)",
                            isSelected = selectedDrawerKey == "defaulter_triage",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("defaulter_triage") }
                        )
                        DrawerItem(
                            icon = Icons.Default.FolderShared,
                            title = "Department Study Materials & PDFs",
                            isSelected = selectedDrawerKey == "drive",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("drive") }
                        )
                        DrawerItem(
                            icon = Icons.Default.DynamicFeed,
                            title = "Campus Feed & Notices",
                            isSelected = selectedDrawerKey == "campus",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("campus") }
                        )
                    }

                    UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> {
                        DrawerItem(
                            icon = Icons.Default.ManageAccounts,
                            title = "Faculty Access & Department Oversight",
                            isSelected = selectedDrawerKey == "camera_attendance",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("camera_attendance") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Campaign,
                            title = "Broadcast Official College Circular",
                            isSelected = selectedDrawerKey == "principal_broadcast",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("principal_broadcast") }
                        )
                        DrawerItem(
                            icon = Icons.Default.AccountBalance,
                            title = "Statutory Council & Affiliation Hub",
                            isSelected = selectedDrawerKey == "statutory_council",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("statutory_council") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Gavel,
                            title = "End-Semester Examination Governance",
                            isSelected = selectedDrawerKey == "exam_governance",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("exam_governance") }
                        )
                        DrawerItem(
                            icon = Icons.Default.VerifiedUser,
                            title = "Institutional Research & NAAC / NBA Hub",
                            isSelected = selectedDrawerKey == "research_grants",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("research_grants") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Verified,
                            title = "Institutional Quality & Executive Audit",
                            isSelected = selectedDrawerKey == "quality_audit",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("quality_audit") }
                        )
                        DrawerItem(
                            icon = Icons.Default.DynamicFeed,
                            title = "Campus Feed & Public Notice Board",
                            isSelected = selectedDrawerKey == "drive",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("drive") }
                        )
                    }

                    UserRole.ADMIN -> {
                        DrawerItem(
                            icon = Icons.Default.CorporateFare,
                            title = "Multi-College Directory & Add College",
                            isSelected = selectedDrawerKey == "admin_directory",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("admin_directory") }
                        )
                        DrawerItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "System Security & Role Governance",
                            isSelected = selectedDrawerKey == "sys_security",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("sys_security") }
                        )
                        DrawerItem(
                            icon = Icons.Default.CloudSync,
                            title = "Cloud Database Backup & Migration",
                            isSelected = selectedDrawerKey == "cloud_backup",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("cloud_backup") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Tune,
                            title = "Multi-Tenant Institution Configurator",
                            isSelected = selectedDrawerKey == "tenant_config",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("tenant_config") }
                        )
                        DrawerItem(
                            icon = Icons.Default.ReportProblem,
                            title = "System Issues Triage & Admin Gmail Alerts",
                            isSelected = selectedDrawerKey == "issues",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("issues") }
                        )
                    }

                    UserRole.SUPER_ADMIN -> {
                        DrawerItem(
                            icon = Icons.Default.Language,
                            title = "Multi-Tenant Campus Orchestrator",
                            isSelected = selectedDrawerKey == "orchestrator",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("orchestrator") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Memory,
                            title = "Global AI Inference Engine & Cluster Metrics",
                            isSelected = selectedDrawerKey == "ai_cluster",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("ai_cluster") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Policy,
                            title = "Enterprise Audit Logs & Forensic Vault",
                            isSelected = selectedDrawerKey == "audit_logs",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("audit_logs") }
                        )
                        DrawerItem(
                            icon = Icons.Default.Hub,
                            title = "Cross-Campus Federated Sync",
                            isSelected = selectedDrawerKey == "federated_sync",
                            isDarkMode = isDarkMode,
                            onClick = { selectItem("federated_sync") }
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(10.dp))

                DrawerSectionHeader(title = "App Preferences", isDarkMode = isDarkMode)

                // Day / Night Mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (isDarkMode) "Dark Theme" else "Light Theme",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }
            }

            // PINNED ALWAYS-VISIBLE BOTTOM BAR - EXACTLY ONE LOGOUT BUTTON IN THE DRAWER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 28.dp)
                    .clickable {
                        onCloseDrawer()
                        onLogout()
                    }
                    .testTag("drawer_pinned_logout_btn"),
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkMode) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEE2E2),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = if (isDarkMode) 0.6f else 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign Out / Switch Role",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDarkMode) Color.White else Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }

    // Interactive Profile Edit & Photo Selector Dialog
    if (showAvatarDialog) {
        var editDisplayName by remember { mutableStateOf(userProfile.name) }
        var editEmail by remember { mutableStateOf(userProfile.email) }
        var editPhone by remember { mutableStateOf(userProfile.phone) }
        var profileErrorMsg by remember { mutableStateOf<String?>(null) }

        val avatarPresets = listOf(
            Pair("Scholar (Male)", Icons.Default.School),
            Pair("Scholar (Female)", Icons.Default.Face),
            Pair("Faculty Mentor (Male)", Icons.Default.SupervisorAccount),
            Pair("Faculty Mentor (Female)", Icons.Default.PersonPin),
            Pair("Head of Department (Male)", Icons.Default.Engineering),
            Pair("Head of Department (Female)", Icons.Default.Psychology),
            Pair("College Principal", Icons.Default.AdminPanelSettings),
            Pair("College Administrator", Icons.Default.Domain),
            Pair("Super Administrator", Icons.Default.Language),
            Pair("Parent / Guardian", Icons.Default.FamilyRestroom),
            Pair("Device Gallery / Camera Photo", Icons.Default.AddPhotoAlternate)
        )

        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF8B5CF6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile & Change Photo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Update your institutional details and profile avatar:",
                        fontSize = 12.sp,
                        color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                    )

                    if (profileErrorMsg != null) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = profileErrorMsg!!,
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it; profileErrorMsg = null },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it; profileErrorMsg = null },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it; profileErrorMsg = null },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Phone Gallery Photo Upload Button
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = roleThemeColor)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedAvatarName.startsWith("content://") || selectedAvatarName.startsWith("file://")) "Phone Photo Attached ✓" else "Choose Photo from Phone Gallery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Or Select Avatar Preset:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    avatarPresets.forEach { (name, icon) ->
                        val isSelected = selectedAvatarName == name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAvatarName = name },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.2f) else if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF8B5CF6) else if (isDarkMode) ObsidianBorder else LightElevated
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF8B5CF6) else ObsidianBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (isDarkMode) Color(0xFFDDD6FE) else Color(0xFF6D28D9)) else (if (isDarkMode) Color.White else Color(0xFF0F172A))
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDisplayName.isBlank()) {
                            profileErrorMsg = "Display name cannot be blank."
                            return@Button
                        }
                        if (!editEmail.contains("@")) {
                            profileErrorMsg = "Please enter a valid institutional email."
                            return@Button
                        }
                        val updated = userProfile.copy(
                            name = editDisplayName.trim(),
                            email = editEmail.trim(),
                            phone = editPhone.trim(),
                            avatarUri = selectedAvatarName
                        )
                        onProfileUpdated?.invoke(updated)
                        onProfileAvatarChanged?.invoke(selectedAvatarName)
                        showAvatarDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DrawerSectionHeader(title: String, isDarkMode: Boolean = true) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = if (isDarkMode) TextMuted else Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    )
}

/**
 * Drawer item with prominent Electric Indigo / Royal Purple highlight when selected.
 */
@Composable
fun DrawerItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean = false,
    isDarkMode: Boolean = true,
    tintOverride: Color? = null,
    onClick: () -> Unit
) {
    // Royal Electric Purple color for selected state
    val purpleSelectedColor = Color(0xFF8B5CF6)
    val itemTint = tintOverride ?: (if (isSelected) purpleSelectedColor else (if (isDarkMode) TextSecondary else Color(0xFF64748B)))
    val textColor = tintOverride ?: (if (isSelected) (if (isDarkMode) Color(0xFFDDD6FE) else Color(0xFF6D28D9)) else (if (isDarkMode) Color.White else Color(0xFF0F172A)))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) purpleSelectedColor.copy(alpha = if (isDarkMode) 0.22f else 0.16f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) purpleSelectedColor.copy(alpha = if (isDarkMode) 0.65f else 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = itemTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || tintOverride != null) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            )
        }
    }
}
