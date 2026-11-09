package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriveDocument
import com.example.util.PdfHelper
import com.example.ui.theme.*

/**
 * Dedicated HOD Syllabus Audit & Faculty Workload Central Workflow
 */
@Composable
fun HodSyllabusAuditProcessView(
    onNavigateToChat: (String) -> Unit = {}
) {
    var selectedCourse by remember { mutableStateOf("Deep Learning [2339609]") }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var isMid2Approved by remember { mutableStateOf(false) }

    val courseSyllabusMap = mapOf(
        "Deep Learning [2339609]" to listOf(
            Triple("Unit 1: Feedforward Networks & Backprop", 100, true),
            Triple("Unit 2: Convolutional Neural Networks", 95, true),
            Triple("Unit 3: Recurrent Networks & LSTMs", 78, true),
            Triple("Unit 4: Autoencoders & Generative Models", 30, false),
            Triple("Unit 5: Reinforcement Learning & Ethics", 0, false)
        ),
        "Cyber Security [2305701]" to listOf(
            Triple("Unit 1: Classical Cryptography & Ciphers", 100, true),
            Triple("Unit 2: Public Key Infrastructure & RSA", 90, true),
            Triple("Unit 3: Network Firewalls & IDS/IPS", 70, true),
            Triple("Unit 4: Web Application Vulnerabilities", 25, false),
            Triple("Unit 5: Incident Response & Digital Forensics", 0, false)
        ),
        "Cloud Computing [2305302]" to listOf(
            Triple("Unit 1: Virtualization & Cloud Architecture", 100, true),
            Triple("Unit 2: AWS, Azure & Microservices", 88, true),
            Triple("Unit 3: Kubernetes Container Orchestration", 80, true),
            Triple("Unit 4: Cloud Security & IAM Governance", 40, false),
            Triple("Unit 5: Serverless Computing & Lambda", 10, false)
        )
    )

    val currentUnits = courseSyllabusMap[selectedCourse] ?: courseSyllabusMap.values.first()
    val averageCoverage = currentUnits.map { it.second }.average()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Syllabus Audit & Workload Central",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                "Department of Computer Science • NBA & Autonomous Compliance",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            color = CyberEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Avg: ${averageCoverage.toInt()}% Covered",
                                color = CyberEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Course Selector Chips
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(courseSyllabusMap.keys.toList()) { courseName ->
                            val isSel = courseName == selectedCourse
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCourse = courseName },
                                label = { Text(courseName.substringBefore('[').trim(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        if (actionNotice != null) {
            item {
                Surface(
                    color = CyberSuccess.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(actionNotice!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { actionNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Unit Breakdown Cards
        item {
            Text("Course Unit Coverage Breakdown", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        items(currentUnits) { (unitName, pct, isCompleted) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCompleted) CyberEmerald.copy(alpha = 0.3f) else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(unitName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (pct >= 75) CyberEmerald else if (pct >= 30) LuminousAmber else NeonCoral,
                            trackColor = Color(0xFF0F172A)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCompleted) CyberEmerald.copy(alpha = 0.15f) else Color(0xFF1E293B)
                    ) {
                        Text(
                            text = if (isCompleted) "Verified ✓" else "In Progress",
                            color = if (isCompleted) CyberEmerald else LuminousAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Faculty Workload Matrix
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Text("Faculty Teaching Workload & Punctuality Audit", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Dr. K. Ramanathan (Professor)", "18 hrs/week • 96% Biometric Punctuality", "Compliant ✓"),
                        Triple("Prof. M. Sunitha (Assoc. Prof)", "16 hrs/week • 94% Biometric Punctuality", "Compliant ✓"),
                        Triple("Asst. Prof. D. Ravi (Asst. Prof)", "20 hrs/week • 88% Biometric Punctuality", "Overload ⚠️")
                    ).forEach { (name, details, status) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(details, color = TextSecondary, fontSize = 10.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (status.contains("Compliant")) CyberEmerald.copy(alpha = 0.15f) else NeonCoral.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    status,
                                    color = if (status.contains("Compliant")) CyberEmerald else NeonCoral,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onNavigateToChat("Perform an in-depth HOD syllabus audit and gap analysis for $selectedCourse across all 5 units, identifying units behind schedule, lab completion rates, and remediation actions.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Gap Analysis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isMid2Approved = !isMid2Approved
                        actionNotice = if (isMid2Approved) "Mid-Term 2 Practical Exam schedule validated and approved for $selectedCourse!" else "Mid-Term 2 status reset to Pending."
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMid2Approved) CyberSuccess else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMid2Approved) CyberSuccess else CyberEmerald),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (isMid2Approved) Icons.Default.CheckCircle else Icons.Default.FactCheck, contentDescription = null, tint = if (isMid2Approved) Color.White else CyberEmerald, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMid2Approved) "✓ Mid-2 Approved" else "Approve Mid-2", color = if (isMid2Approved) Color.White else CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dedicated Principal NIRF Governance & Institutional Accreditation Cockpit
 */
@Composable
fun PrincipalNirfGovernanceProcessView(
    onNavigateToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var nirfNotice by remember { mutableStateOf<String?>(null) }
    var selectedCycle by remember { mutableStateOf("NIRF 2026 Target") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NIRF Governance & Autonomous Quality", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Institutional Performance Metrics & Accreditation Ledger", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f))
                        ) {
                            Text("NAAC 'A+' • NBA Tier-1", color = CyberEmerald, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NIRF Target Rank Band", color = TextSecondary, fontSize = 10.5.sp)
                            Text("Rank 101 - 150", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Composite Institutional Score", color = TextSecondary, fontSize = 10.5.sp)
                            Text("82.4 / 100", color = CyberEmerald, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (nirfNotice != null) {
            item {
                Surface(
                    color = CyberSuccess.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(nirfNotice!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { nirfNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 5 Pillars Breakdown
        item {
            Text("NIRF 5-Pillar Performance Audit", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("TLR (Teaching, Learning & Resources)", 86.4f, CyberEmerald),
                        Triple("RPC (Research & Professional Practice)", 74.2f, ElectricIndigo),
                        Triple("GO (Graduation Outcomes & Placements)", 88.9f, NeonBlue),
                        Triple("OI (Outreach & Inclusivity)", 81.0f, LuminousAmber),
                        Triple("PR (Perception & Peer Review)", 79.5f, NeonCoral)
                    ).forEach { (pillar, score, color) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pillar, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${score}/100", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF0F172A))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (score / 100f).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onNavigateToChat("Generate a comprehensive Principal NIRF 5-Pillar Scorecard and NAAC A++ Autonomous Accreditation neural audit for KSRM College of Engineering.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Neural Audit", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        PdfHelper.downloadPdfToDevice(context, "nirf_dossier_2026.pdf", "KSRM_NIRF_Autonomous_Dossier_2026.pdf")
                        nirfNotice = "✓ Successfully generated & exported official NIRF Accreditation Dossier to device storage!"
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricIndigo),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export NIRF Dossier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dedicated College Admin Campus Operations & Facility Management
 */
@Composable
fun AdminFacilityOpsProcessView(
    onNavigateToChat: (String) -> Unit = {}
) {
    var facilityNotice by remember { mutableStateOf<String?>(null) }
    var isBroadcastDispatched by remember { mutableStateOf(false) }

    val workOrders = remember {
        mutableStateListOf(
            Triple("Central Server Room UPS Inverter", "Battery bank replacement & voltage calibration", "In-Progress"),
            Triple("Biometric Terminals - Block 2", "Camera optical calibration & sensor cleaning", "Resolved ✓"),
            Triple("IoT Solar Panel Substation", "Peak load inverter firmware upgrade v3.1", "In-Progress"),
            Triple("High-Speed Fiber Backbone", "Core switch VLAN routing audit", "Resolved ✓")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Campus Operations & Facility Management", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Physical Infrastructure, IoT Power & Network Maintenance", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonBlue.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
                        ) {
                            Text("100% Power Uptime", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        if (facilityNotice != null) {
            item {
                Surface(
                    color = CyberSuccess.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(facilityNotice!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { facilityNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("Facility Work-Order Tickets (Click to Resolve)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        items(workOrders.size) { idx ->
            val order = workOrders[idx]
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newStatus = if (order.third.contains("Resolved")) "In-Progress" else "Resolved ✓"
                        workOrders[idx] = Triple(order.first, order.second, newStatus)
                        facilityNotice = "${order.first} status toggled to $newStatus!"
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(order.first, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                        Text(order.second, color = TextSecondary, fontSize = 10.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (order.third.contains("Resolved")) CyberEmerald.copy(alpha = 0.15f) else LuminousAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            order.third,
                            color = if (order.third.contains("Resolved")) CyberEmerald else LuminousAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    isBroadcastDispatched = !isBroadcastDispatched
                    facilityNotice = if (isBroadcastDispatched) "Emergency broadcast dispatched: Campus power backup active. All biometric relays synced." else "Facility broadcast reset."
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBroadcastDispatched) CyberSuccess else NeonBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (isBroadcastDispatched) Icons.Default.CheckCircle else Icons.Default.BroadcastOnHome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBroadcastDispatched) "✓ Facility Notice Broadcasted to All Blocks" else "Broadcast Facility Notice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Dedicated Super Admin Global Cluster Infrastructure & Mesh Command Center
 */
@Composable
fun SuperAdminClusterOpsProcessView(
    onNavigateToChat: (String) -> Unit = {}
) {
    var clusterNotice by remember { mutableStateOf<String?>(null) }
    var isFailoverDrillCompleted by remember { mutableStateOf(false) }
    var isOtaDeployed by remember { mutableStateOf(false) }
    var isMeshRebalanced by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Global Cluster Infrastructure & Multi-Campus Mesh", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Cross-Tenant Kubernetes Mesh, Edge Relays & Disaster Recovery", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess.copy(alpha = 0.5f))
                        ) {
                            Text("99.99% Mesh Uptime", color = CyberSuccess, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        if (clusterNotice != null) {
            item {
                Surface(
                    color = CyberSuccess.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(clusterNotice!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clusterNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("Regional Edge Relays & Cluster Status (5 Nodes)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))

            listOf(
                Triple("KSMR Kadapa Core Cluster", "12 ms latency • 48 Terminals Online • Shard 1", "Operational ✓"),
                Triple("Vijayawada Regional Edge Node", "18 ms latency • 24 Terminals Online • Shard 2", "Operational ✓"),
                Triple("Bengaluru AWS Disaster Recovery", "26 ms latency • Standby Sync Active • Multi-AZ", "Standby Hot ✓"),
                Triple("Hyderabad AI GPU Cluster Hub", "9 ms latency • 8 A100 Tensor Nodes • Shard 3", "GPU Active ✓"),
                Triple("Tirupati Regional Gateway Node", "15 ms latency • 16 Turnstiles Synced • Edge Relay", "Operational ✓")
            ).forEach { (nodeName, telemetry, status) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nodeName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                            Text(telemetry, color = TextSecondary, fontSize = 10.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(status, color = CyberSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isFailoverDrillCompleted = true
                            clusterNotice = "✓ Cloud Node Failover Health Drill Completed: All 12 multi-campus nodes operational with 0 downtime!"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFailoverDrillCompleted) CyberSuccess else CyberViolet
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isFailoverDrillCompleted) Icons.Default.CheckCircle else Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFailoverDrillCompleted) "✓ Drill Completed" else "Run Failover Drill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            isOtaDeployed = true
                            clusterNotice = "✓ Firmware OTA update v2.4.1 dispatched and verified on all 48 Biometric Edge Terminals!"
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOtaDeployed) CyberSuccess else CyberViolet),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isOtaDeployed) CyberSuccess.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isOtaDeployed) CyberSuccess else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isOtaDeployed) Icons.Default.CheckCircle else Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isOtaDeployed) "✓ OTA v2.4 Deployed" else "Deploy OTA Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isMeshRebalanced = true
                            clusterNotice = "✓ Kubernetes Shard Mesh rebalanced across 5 regional edge relays with optimal traffic distribution."
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMeshRebalanced) Color(0xFF06B6D4) else Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMeshRebalanced) "✓ Shard Rebalanced" else "Rebalance Shard Mesh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (onNavigateToChat != null) {
                        OutlinedButton(
                            onClick = {
                                onNavigateToChat("Super Admin Cluster Health Audit: Query active node latency, telemetry SLA, and edge terminal logs.")
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF8B5CF6))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Diagnostics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Student Academic & Learning Studio (Compact, Zero Wasted Space)
 */
@Composable
fun StudentAcademicLabProcessView(
    driveDocs: List<DriveDocument> = emptyList(),
    onOpenDoc: (DriveDocument) -> Unit = {},
    onDownloadDoc: (DriveDocument) -> Unit = {},
    onNavigateToChat: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val defaultDocs = remember {
        listOf(
            DriveDocument(id = "doc_dl", title = "Deep Learning Unit 1-5 Lecture Notes & Derivations", subject = "Deep Learning [2339609]", fileType = "PDF", sizeFormatted = "4.2 MB", assetFileName = "study_material_becg.pdf"),
            DriveDocument(id = "doc_becg", title = "Engineering Economics & BECG Case Study Dossier", subject = "BECG [2305101]", fileType = "PDF", sizeFormatted = "3.1 MB", assetFileName = "study_material_becg.pdf"),
            DriveDocument(id = "doc_dbms", title = "Database Systems & SQL Lab Manual 2026", subject = "DBMS Lab [2305302]", fileType = "PDF", sizeFormatted = "2.8 MB", assetFileName = "timetable_viisem.pdf"),
            DriveDocument(id = "doc_mid2", title = "Autonomous Mid-Term 2 Blueprint & Model Solutions", subject = "Assessment", fileType = "PDF", sizeFormatted = "1.9 MB", assetFileName = "assignment_viisem_gs.pdf"),
            DriveDocument(id = "doc_qbank", title = "Autonomous End-Semester Question Bank (2024-2026)", subject = "Question Bank", fileType = "PDF", sizeFormatted = "5.4 MB", assetFileName = "timetable_viisem.pdf")
        )
    }

    val combinedDocs = remember(driveDocs, com.example.data.AcademicDataProvider.sharedDriveDocs.size, com.example.data.AcademicDataProvider.sharedStudyDocs.size) {
        val facultyUploaded = com.example.data.AcademicDataProvider.sharedDriveDocs.filter {
            it.fileType.equals("PDF", ignoreCase = true)
        }.map { doc ->
            doc.copy(
                assetFileName = if (doc.assetFileName.isNotBlank()) doc.assetFileName else "study_material_becg.pdf"
            )
        }
        (facultyUploaded + driveDocs + defaultDocs).distinctBy { it.title }
    }

    val filtered = remember(searchQuery, selectedCategory, combinedDocs) {
        combinedDocs.filter { doc ->
            val matchCategory = when (selectedCategory) {
                "Notes" -> doc.title.contains("Notes", ignoreCase = true) || doc.subject.contains("Deep", ignoreCase = true)
                "Lab Manuals" -> doc.title.contains("Lab", ignoreCase = true) || doc.title.contains("Manual", ignoreCase = true)
                "Question Banks" -> doc.title.contains("Question", ignoreCase = true) || doc.title.contains("Blueprint", ignoreCase = true)
                else -> true
            }
            val matchSearch = searchQuery.isBlank() || doc.title.contains(searchQuery, ignoreCase = true) || doc.subject.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Compact Header Card (Eliminates bulky empty screen)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Academic Learning & Resource Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Official syllabus notes, lab blueprints & exam question banks", color = TextSecondary, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f))
                        ) {
                            Text("5 Subjects Active", color = CyberEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search syllabus notes, lab blueprints, questions...", fontSize = 11.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val categories = listOf("All", "Notes", "Lab Manuals", "Question Banks")
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 10.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberEmerald,
                                    selectedLabelColor = Color(0xFF042F2E)
                                )
                            )
                        }
                    }
                }
            }
        }

        items(filtered) { doc ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF13192B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${doc.subject} • ${doc.sizeFormatted}", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onOpenDoc(doc) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onDownloadDoc(doc) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberEmerald),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onNavigateToChat?.invoke("Analyze the academic document '${doc.title}': summarize syllabus points, key derivations, and answer model questions.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LuminousAmber, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Co-Pilot", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
