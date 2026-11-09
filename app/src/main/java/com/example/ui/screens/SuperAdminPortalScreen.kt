package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicDataProvider
import com.example.model.CollegeOrganization
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*
import com.example.ui.components.*

enum class SuperAdminTab {
    PLATFORM_OVERVIEW,
    INSTITUTIONS,
    ACCOUNT_PROVISIONING,
    AUDIT_TRAIL,
    SYSTEM_SETTINGS
}

data class AuditLogEntry(
    val id: String,
    val actor: String,
    val role: String,
    val action: String,
    val details: String,
    val timestampFormatted: String
)

/**
 * Super Admin Dashboard — Platform-Wide Multi-Campus Governance, Cloud Telemetry & Administration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminPortalScreen(
    initialTab: SuperAdminTab = SuperAdminTab.PLATFORM_OVERVIEW,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {}
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    val colleges = remember {
        mutableStateListOf<CollegeOrganization>().apply {
            addAll(AcademicDataProvider.getInitialColleges())
        }
    }

    var expandedCollegeId by remember { mutableStateOf<String?>(null) }

    val pendingAccounts = remember {
        mutableStateListOf(
            Triple("usr_faculty_201", "Dr. K. Srinivasulu", "Faculty (ECE) • Verification Pending"),
            Triple("usr_hod_302", "Dr. G. Hemalatha", "HOD (Civil) • Appointment Order Attached"),
            Triple("usr_stud_401", "V. Rajesh Kumar", "Student (239Y1A05D8) • Institutional Email Verified"),
            Triple("usr_faculty_205", "Dr. P. Suresh Babu", "Faculty (CSE) • Biometric Profile Ready")
        )
    }

    val approvedAccounts = remember {
        mutableStateListOf(
            Triple("usr_admin_101", "Principal Dr. K. Hemachandra Reddy", "Principal • KSRMCE Kadapa"),
            Triple("usr_hod_102", "Dr. M. Ramanjaneya Reddy", "HOD (CSE) • Authorized"),
            Triple("usr_fac_103", "Prof. S. Shakeel Ahamad", "Faculty (CSE) • Active"),
            Triple("usr_stud_104", "Sattu Penchala Prasad", "Student (239Y1A0501) • Active CSE Sec B"),
            Triple("usr_stud_105", "Shaik Mohammad", "Student (239Y1A05C0) • Active CSE Sec B")
        )
    }

    var provisioningRoleFilter by remember { mutableStateOf("All") }

    val auditEvents = remember {
        mutableStateListOf(
            AuditLogEntry("aud_1", "Platform Super Admin", "SUPER_ADMIN", "INSTITUTION_SYNC", "Synchronized cloud cluster telemetry across 3 campus nodes", "Today, 12:40 PM"),
            AuditLogEntry("aud_2", "College Principal", "PRINCIPAL", "ATTENDANCE_CORRECTION", "Approved autonomous biometric attendance reconciliation", "Today, 11:15 AM"),
            AuditLogEntry("aud_3", "Dr. Ramanjaneya Reddy", "HOD", "FACULTY_ALLOCATION", "Assigned DL Laboratory to Faculty 04", "Yesterday, 04:30 PM"),
            AuditLogEntry("aud_4", "System Cron Daemon", "SERVICE", "BACKUP_SNAPSHOT", "Dispatched automatic cloud disaster recovery snapshot (99.98% SLA)", "Yesterday, 06:00 PM"),
            AuditLogEntry("aud_5", "KSRMCE Admin", "ADMIN", "INFRASTRUCTURE_UPDATE", "Upgraded biometric gateway terminal firmware to v3.4.1", "2 days ago")
        )
    }
    var auditRoleFilter by remember { mutableStateOf("All") }
    var auditSearchQuery by remember { mutableStateOf("") }

    // System Settings States
    var masterClusterUrl by remember { mutableStateOf("https://agentra-cluster-ap-south.cloud.agentra.edu/v2") }
    var biometricAccuracyThreshold by remember { mutableStateOf("94.5") }
    var autoSnapshotIntervalHours by remember { mutableStateOf("6") }
    var maxConcurrentSyncThreads by remember { mutableStateOf("32") }
    var maintenanceModeEnabled by remember { mutableStateOf(false) }

    var showCreateInstitutionDialog by remember { mutableStateOf(false) }
    var editingCollege by remember { mutableStateOf<CollegeOrganization?>(null) }
    var newCollegeName by remember { mutableStateOf("") }
    var newCollegeCode by remember { mutableStateOf("") }
    var newCollegeCity by remember { mutableStateOf("") }
    var newCollegeEmail by remember { mutableStateOf("") }

    var showProvisionUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("Faculty") }
    var newUserCollege by remember { mutableStateOf("KSRMCE") }

    var actionToast by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) CyberBg else LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top Navigation Bar
            Surface(
                color = if (isDarkMode) CyberSurface else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        SuperAdminTabChip(
                            title = "Platform Overview",
                            icon = Icons.Default.Public,
                            isSelected = selectedTab == SuperAdminTab.PLATFORM_OVERVIEW,
                            activeColor = CyberViolet,
                            onClick = { selectedTab = SuperAdminTab.PLATFORM_OVERVIEW }
                        )
                    }
                    item {
                        SuperAdminTabChip(
                            title = "Institutions",
                            icon = Icons.Default.AccountBalance,
                            isSelected = selectedTab == SuperAdminTab.INSTITUTIONS,
                            activeColor = CyberSuccess,
                            badgeCount = colleges.size,
                            onClick = { selectedTab = SuperAdminTab.INSTITUTIONS }
                        )
                    }
                    item {
                        SuperAdminTabChip(
                            title = "Provisioning",
                            icon = Icons.Default.VerifiedUser,
                            isSelected = selectedTab == SuperAdminTab.ACCOUNT_PROVISIONING,
                            activeColor = CyberWarning,
                            badgeCount = pendingAccounts.size,
                            onClick = { selectedTab = SuperAdminTab.ACCOUNT_PROVISIONING }
                        )
                    }
                    item {
                        SuperAdminTabChip(
                            title = "Audit Trail",
                            icon = Icons.Default.Security,
                            isSelected = selectedTab == SuperAdminTab.AUDIT_TRAIL,
                            activeColor = CyberInfo,
                            onClick = { selectedTab = SuperAdminTab.AUDIT_TRAIL }
                        )
                    }
                    item {
                        SuperAdminTabChip(
                            title = "Settings",
                            icon = Icons.Default.Settings,
                            isSelected = selectedTab == SuperAdminTab.SYSTEM_SETTINGS,
                            activeColor = CyberAccentPink,
                            onClick = { selectedTab = SuperAdminTab.SYSTEM_SETTINGS }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                onNavigateToChat("Central Platform Super Admin AI: Multi-tenant governance, cluster telemetry, and SLA compliance.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Central AI Chatbot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notification Banner
            if (actionToast != null) {
                Surface(
                    color = CyberSuccess.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = actionToast!!, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { actionToast = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Content Area
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1080.dp)
                ) {
                    when (selectedTab) {
                        SuperAdminTab.PLATFORM_OVERVIEW -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                item {
                                    val totalStudents = colleges.sumOf { it.studentsCount }
                                    val totalFaculty = colleges.sumOf { it.departmentsCount * 8 }
                                    val totalUsers = totalStudents + totalFaculty + 45

                                    // Platform Cloud Infrastructure KPI Cards in 2x2 Grid (eliminates vertical letter stacking)
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            SuperAdminKpiCard("Active Campuses", "${colleges.size}", Icons.Default.Domain, CyberViolet, Modifier.weight(1f), isDarkMode)
                                            SuperAdminKpiCard("Global Enrolled", "$totalUsers", Icons.Default.Group, CyberInfo, Modifier.weight(1f), isDarkMode)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            SuperAdminKpiCard("Cloud Nodes", "12 Active", Icons.Default.CloudQueue, CyberSuccess, Modifier.weight(1f), isDarkMode)
                                            SuperAdminKpiCard("Cluster Uptime", "99.98%", Icons.Default.Speed, CyberWarning, Modifier.weight(1f), isDarkMode)
                                        }
                                    }
                                }

                                item {
                                    MonthlyTrendLineChart(
                                        trends = AcademicDataProvider.getInstitutionAttendanceTrends(),
                                        isDarkMode = isDarkMode
                                    )
                                }

                                item {
                                    DepartmentComparisonChart(
                                        depts = AcademicDataProvider.getDepartmentComparison(),
                                        isDarkMode = isDarkMode
                                    )
                                }

                                item {
                                    // Central AI Assistant Action Card - Clean stacked layout with ZERO text wrapping
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = CyberVioletSubtle,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(CyberViolet),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Platform AI Executive Assistant", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Text("Autonomous Cross-Campus Intelligence", color = CyberSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }

                                            Text(
                                                text = "Generate cross-campus SLA compliance reports, query face recognition accuracy telemetry, inspect active cloud cluster nodes, or automate institutional provisioning.",
                                                color = TextIceBlue,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Button(
                                                onClick = {
                                                    onNavigateToChat("Super Admin Command: Generate cross-campus biometric SLA audit and compliance diagnostic.")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Launch Executive Assistant", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SuperAdminTab.INSTITUTIONS -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Registered Institutions (${colleges.size})", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 15.sp)
                                            Text("Click any college card to inspect complete telemetry, storage quotas, and faculty hierarchy.", color = TextSecondary, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { showCreateInstitutionDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add College", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                items(colleges) { col ->
                                    val isExpanded = expandedCollegeId == col.id
                                    val isActive = col.status.equals("Active", ignoreCase = true) || col.status.contains("Verified", ignoreCase = true)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedCollegeId = if (isExpanded) null else col.id
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDarkMode) CyberElevated else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isExpanded) CyberViolet else if (isDarkMode) CyberBorder else LightBorder
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 8.dp)
                                                ) {
                                                    Text(
                                                        text = col.name,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color.White else LightTextPrimary,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${col.code} • ${col.city} • Affiliated to JNTUA",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isActive) CyberSuccess.copy(alpha = 0.2f) else CyberWarning.copy(alpha = 0.2f),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) CyberSuccess else CyberWarning),
                                                        modifier = Modifier.clickable {
                                                            val idx = colleges.indexOfFirst { it.id == col.id }
                                                            if (idx >= 0) {
                                                                val newStatus = if (isActive) "Inactive" else "Active & Verified"
                                                                colleges[idx] = col.copy(status = newStatus)
                                                                actionToast = "Campus ${col.name} status changed to $newStatus"
                                                            }
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isActive) CyberSuccess else CyberWarning)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (isActive) "Active & Verified" else "Inactive",
                                                                color = if (isActive) CyberSuccess else CyberWarning,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = { editingCollege = col },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit College", tint = CyberViolet, modifier = Modifier.size(15.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            colleges.remove(col)
                                                            actionToast = "De-registered college: ${col.name}"
                                                        },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete College", tint = CyberError, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Admin: ${col.adminEmail}", color = TextMuted, fontSize = 11.sp)
                                                Text("${col.departmentsCount} Depts • ${col.studentsCount} Students", color = CyberInfo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            // Expanded Details Section
                                            AnimatedVisibility(visible = isExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 12.dp)
                                                        .background(if (isDarkMode) CyberSurface else LightSurfaceVariant, RoundedCornerShape(10.dp))
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Comprehensive Institutional Telemetry", fontWeight = FontWeight.Bold, color = CyberViolet, fontSize = 12.sp)
                                                        Button(
                                                            onClick = { expandedCollegeId = null },
                                                            colors = ButtonDefaults.buttonColors(containerColor = CyberError.copy(alpha = 0.2f)),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text("Close Details ✕", color = CyberError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    HorizontalDivider(color = if (isDarkMode) CyberBorder else LightBorder)

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Principal Executive:", color = TextSecondary, fontSize = 11.sp)
                                                        Text("Dr. K. Hemachandra Reddy (College Principal)", color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Autonomous Accreditation:", color = TextSecondary, fontSize = 11.sp)
                                                        Text("NAAC 'A+' Grade • NBA Tier-1", color = CyberSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Face Biometric Camera Nodes:", color = TextSecondary, fontSize = 11.sp)
                                                        Text("28 High-Speed Terminals Online", color = CyberInfo, fontSize = 11.sp)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Cluster Cloud Storage Quota:", color = TextSecondary, fontSize = 11.sp)
                                                        Text("500 GB Allocated • 182 GB Used", color = CyberWarning, fontSize = 11.sp)
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("DEPARTMENT ATTENDANCE & BIOMETRIC UPTIME", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = CyberViolet, letterSpacing = 0.5.sp)

                                                    val deptData = listOf(
                                                        Triple("CSE", 0.88f, CyberSuccess),
                                                        Triple("ECE", 0.82f, CyberInfo),
                                                        Triple("AIML", 0.85f, CyberViolet),
                                                        Triple("CIVIL", 0.78f, CyberWarning),
                                                        Triple("MECH", 0.74f, CyberError)
                                                    )
                                                    deptData.forEach { (dept, pct, color) ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(dept, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, modifier = Modifier.width(42.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            LinearProgressIndicator(
                                                                progress = pct,
                                                                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                                color = color,
                                                                trackColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("${(pct * 100).toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(36.dp))
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                actionToast = "Dispatched telemetry health probe to ${col.name} gateway."
                                                            },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Health Ping", fontSize = 11.sp)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                actionToast = "Loaded full departmental roster for ${col.name} (CSE, AIML, ECE, EEE, CIVIL, MECH)."
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Inspect Faculty", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SuperAdminTab.ACCOUNT_PROVISIONING -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Account Provisioning & Identity Verification", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 15.sp)
                                            Text("Approve, reject or manually provision faculty, HOD and student user credentials.", color = TextSecondary, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { showProvisionUserDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Provision User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                item {
                                    // Filter Chips for Roles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("All", "Faculty", "HOD", "Student").forEach { role ->
                                            val isSel = provisioningRoleFilter == role
                                            Surface(
                                                modifier = Modifier.clickable { provisioningRoleFilter = role },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) CyberViolet else if (isDarkMode) CyberElevated else LightSurface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberViolet else if (isDarkMode) CyberBorder else LightBorder)
                                            ) {
                                                Text(
                                                    text = role,
                                                    color = if (isSel) Color.White else TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                val filteredPending = pendingAccounts.filter { acc ->
                                    if (provisioningRoleFilter == "All") true
                                    else acc.third.contains(provisioningRoleFilter, ignoreCase = true)
                                }

                                item {
                                    Text("Pending Approvals (${filteredPending.size})", fontWeight = FontWeight.Bold, color = CyberWarning, fontSize = 13.sp)
                                }

                                if (filteredPending.isEmpty()) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkMode) CyberElevated else LightSurface
                                        ) {
                                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                                Text("No pending accounts awaiting verification for $provisioningRoleFilter.", color = TextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                items(filteredPending) { acc ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDarkMode) CyberElevated else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(CyberVioletSubtle),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(acc.second, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 13.5.sp)
                                                Text(acc.third, color = TextSecondary, fontSize = 11.sp)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        pendingAccounts.remove(acc)
                                                        approvedAccounts.add(Triple(acc.first, acc.second, acc.third.replace("Pending", "Active")))
                                                        actionToast = "✓ Account approved & activated: ${acc.second}"
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSuccess),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        pendingAccounts.remove(acc)
                                                        actionToast = "✕ Account rejected: ${acc.second}"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = CyberError, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reject", color = CyberError, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                val filteredApproved = approvedAccounts.filter { acc ->
                                    if (provisioningRoleFilter == "All") true
                                    else acc.third.contains(provisioningRoleFilter, ignoreCase = true)
                                }

                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Approved Accounts for $provisioningRoleFilter (${filteredApproved.size})", fontWeight = FontWeight.Bold, color = CyberSuccess, fontSize = 13.sp)
                                }

                                if (filteredApproved.isEmpty()) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkMode) CyberElevated else LightSurface
                                        ) {
                                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                                Text("No approved accounts matching role filter '$provisioningRoleFilter'.", color = TextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                items(filteredApproved) { acc ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkMode) CyberSurface else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(acc.second, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 12.5.sp)
                                                Text(acc.third, color = TextMuted, fontSize = 10.5.sp)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = CyberSuccess.copy(alpha = 0.15f)
                                            ) {
                                                Text("ACTIVE", color = CyberSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SuperAdminTab.AUDIT_TRAIL -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Column {
                                        Text("Institutional Audit & Security Logs", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 15.sp)
                                        Text("Immutable tamper-evident ledger tracking multi-campus administrative actions.", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }

                                item {
                                    // Search and Filter Bar
                                    OutlinedTextField(
                                        value = auditSearchQuery,
                                        onValueChange = { auditSearchQuery = it },
                                        placeholder = { Text("Search logs by keyword or campus...", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                item {
                                    // Filter Chips for Roles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("All", "SUPER_ADMIN", "PRINCIPAL", "HOD", "SERVICE").forEach { role ->
                                            val isSel = auditRoleFilter == role
                                            Surface(
                                                modifier = Modifier.clickable { auditRoleFilter = role },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) CyberInfo else if (isDarkMode) CyberElevated else LightSurface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberInfo else if (isDarkMode) CyberBorder else LightBorder)
                                            ) {
                                                Text(
                                                    text = role,
                                                    color = if (isSel) Color.White else TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                val filteredEvents = auditEvents.filter { ev ->
                                    val matchRole = auditRoleFilter == "All" || ev.role.equals(auditRoleFilter, ignoreCase = true)
                                    val matchSearch = auditSearchQuery.isBlank() || ev.details.contains(auditSearchQuery, ignoreCase = true) || ev.actor.contains(auditSearchQuery, ignoreCase = true)
                                    matchRole && matchSearch
                                }

                                items(filteredEvents) { ev ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkMode) CyberElevated else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = when (ev.role) {
                                                            "SUPER_ADMIN" -> CyberViolet.copy(alpha = 0.2f)
                                                            "PRINCIPAL" -> CyberSuccess.copy(alpha = 0.2f)
                                                            "HOD" -> CyberWarning.copy(alpha = 0.2f)
                                                            "SERVICE" -> Color(0xFF0D9488).copy(alpha = 0.2f)
                                                            else -> CyberInfo.copy(alpha = 0.2f)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = ev.role,
                                                            color = when (ev.role) {
                                                                "SUPER_ADMIN" -> CyberViolet
                                                                "PRINCIPAL" -> CyberSuccess
                                                                "HOD" -> CyberWarning
                                                                "SERVICE" -> Color(0xFF14B8A6)
                                                                else -> CyberInfo
                                                            },
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = ev.action,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color.White else LightTextPrimary,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(ev.timestampFormatted, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(ev.details, color = if (isDarkMode) TextIceBlue else LightTextPrimary, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Actor: ${ev.actor}", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        SuperAdminTab.SYSTEM_SETTINGS -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Column {
                                        Text("Platform Governance & System Settings", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 15.sp)
                                        Text("Configure multi-tenant cloud cluster endpoints, biometric verification threshold and backup policies.", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }

                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDarkMode) CyberElevated else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Text("Cloud Cluster Architecture", fontWeight = FontWeight.Bold, color = CyberViolet, fontSize = 13.sp)

                                            OutlinedTextField(
                                                value = masterClusterUrl,
                                                onValueChange = { masterClusterUrl = it },
                                                label = { Text("Master Cluster Gateway Endpoint") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = biometricAccuracyThreshold,
                                                    onValueChange = { biometricAccuracyThreshold = it },
                                                    label = { Text("Biometric Match (%)") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = autoSnapshotIntervalHours,
                                                    onValueChange = { autoSnapshotIntervalHours = it },
                                                    label = { Text("Snapshot Interval (Hrs)") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = maxConcurrentSyncThreads,
                                                    onValueChange = { maxConcurrentSyncThreads = it },
                                                    label = { Text("Sync Workers") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            HorizontalDivider(color = if (isDarkMode) CyberBorder else LightBorder)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Platform Maintenance Mode", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 13.sp)
                                                    Text("When active, non-admin sessions will display scheduled service banner.", color = TextSecondary, fontSize = 11.sp)
                                                }
                                                Switch(
                                                    checked = maintenanceModeEnabled,
                                                    onCheckedChange = { maintenanceModeEnabled = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = CyberWarning
                                                    )
                                                )
                                            }

                                            HorizontalDivider(color = if (isDarkMode) CyberBorder else LightBorder)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Active Theme Appearance", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 13.sp)
                                                    Text("Toggle obsidian dark and clean light contrast", color = TextSecondary, fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = onToggleTheme,
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(if (isDarkMode) "Light Mode" else "Dark Mode", fontSize = 11.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Button(
                                                onClick = {
                                                    AcademicDataProvider.saveDataToDisk()
                                                    actionToast = "✓ Platform Governance Configuration successfully persisted to cloud & local storage!"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Save Platform Governance", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Provision User Dialog
    if (showProvisionUserDialog) {
        AlertDialog(
            onDismissRequest = { showProvisionUserDialog = false },
            title = { Text("Provision System Account", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserRole,
                        onValueChange = { newUserRole = it },
                        label = { Text("Designated Role (Faculty / HOD / Student)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserCollege,
                        onValueChange = { newUserCollege = it },
                        label = { Text("Campus / College Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            approvedAccounts.add(
                                Triple("usr_${System.currentTimeMillis()}", newUserName.trim(), "$newUserRole • $newUserCollege (Directly Provisioned)")
                            )
                            showProvisionUserDialog = false
                            actionToast = "✓ Successfully provisioned account for $newUserName"
                            newUserName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Provision & Authorize", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProvisionUserDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Create Institution Dialog
    if (showCreateInstitutionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateInstitutionDialog = false },
            title = { Text("Add College Institution", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCollegeName,
                        onValueChange = { newCollegeName = it },
                        label = { Text("Institution Name") },
                        placeholder = { Text("e.g. S.V. University") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCollegeCode,
                        onValueChange = { newCollegeCode = it },
                        label = { Text("Institution Code") },
                        placeholder = { Text("SVUCE") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCollegeCity,
                        onValueChange = { newCollegeCity = it },
                        label = { Text("City, State") },
                        placeholder = { Text("Tirupati, AP") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCollegeEmail,
                        onValueChange = { newCollegeEmail = it },
                        label = { Text("Admin Contact Email") },
                        placeholder = { Text("admin@svuce.edu") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollegeName.isNotBlank() && newCollegeCode.isNotBlank()) {
                            colleges.add(
                                CollegeOrganization(
                                    id = "col_${System.currentTimeMillis()}",
                                    name = newCollegeName.trim(),
                                    code = newCollegeCode.trim().uppercase(),
                                    city = newCollegeCity.trim(),
                                    adminEmail = newCollegeEmail.trim(),
                                    departmentsCount = 5,
                                    studentsCount = 1200
                                )
                            )
                            showCreateInstitutionDialog = false
                            actionToast = "Created institution: $newCollegeName"
                            newCollegeName = ""
                            newCollegeCode = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Provision College", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateInstitutionDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Edit College Dialog
    if (editingCollege != null) {
        val currCol = editingCollege!!
        var editName by remember(currCol) { mutableStateOf(currCol.name) }
        var editCode by remember(currCol) { mutableStateOf(currCol.code) }
        var editCity by remember(currCol) { mutableStateOf(currCol.city) }
        var editEmail by remember(currCol) { mutableStateOf(currCol.adminEmail) }
        var editDepts by remember(currCol) { mutableStateOf(currCol.departmentsCount.toString()) }
        var editStudents by remember(currCol) { mutableStateOf(currCol.studentsCount.toString()) }
        var editStatus by remember(currCol) { mutableStateOf(currCol.status) }

        AlertDialog(
            onDismissRequest = { editingCollege = null },
            title = { Text("Edit Institution Profile", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Institution Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("Institution Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCity,
                        onValueChange = { editCity = it },
                        label = { Text("City, State") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Admin Contact Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editDepts,
                            onValueChange = { editDepts = it },
                            label = { Text("Departments") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editStudents,
                            onValueChange = { editStudents = it },
                            label = { Text("Students") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active & Verified Status", color = TextSecondary, fontSize = 12.sp)
                        Switch(
                            checked = editStatus.equals("Active", ignoreCase = true) || editStatus.contains("Verified", ignoreCase = true),
                            onCheckedChange = { checked ->
                                editStatus = if (checked) "Active & Verified" else "Inactive"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberSuccess
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = colleges.indexOfFirst { it.id == currCol.id }
                        if (idx >= 0) {
                            colleges[idx] = currCol.copy(
                                name = editName.trim(),
                                code = editCode.trim().uppercase(),
                                city = editCity.trim(),
                                adminEmail = editEmail.trim(),
                                departmentsCount = editDepts.toIntOrNull() ?: currCol.departmentsCount,
                                studentsCount = editStudents.toIntOrNull() ?: currCol.studentsCount,
                                status = editStatus
                            )
                            actionToast = "Updated institution: $editName"
                        }
                        editingCollege = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCollege = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun PlatformTelemetryGauge(
    title: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) CyberSurface else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = fraction,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = if (isDarkMode) CyberBorder else LightBorder
            )
        }
    }
}

@Composable
private fun SuperAdminTabChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color = CyberViolet,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else CyberBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else TextSecondary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.35f) else CyberError),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SuperAdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) CyberElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(value, fontWeight = FontWeight.Black, fontSize = 17.sp, color = if (isDarkMode) Color.White else LightTextPrimary)
                Text(title, fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SuperAdminRailIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) accentColor else Color.Transparent)
                .border(1.dp, if (isSelected) accentColor else Color.Transparent, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(17.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(CyberError)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$badgeCount", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            color = if (isSelected) accentColor else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
