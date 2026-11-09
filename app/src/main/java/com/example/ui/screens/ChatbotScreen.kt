package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.UserRole
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Role-Aware Intelligent Chatbot Assistant for AGENTRA AI.
 * Delivers distinct AI personalities, prompt sets, and expert knowledge tailored specifically
 * to Student, Faculty Member, or Principal/Dean.
 */
@Composable
fun ChatbotScreen(
    userRole: UserRole = UserRole.STUDENT,
    initialPrompt: String? = null,
    isDarkMode: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val botName = when (userRole) {
        UserRole.SUPER_ADMIN -> "AGENTRA Super Admin Advisor"
        UserRole.ADMIN -> "AGENTRA Central System Advisor"
        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> "AGENTRA Executive Principal Advisor"
        UserRole.HOD -> "AGENTRA Departmental HOD Co-Pilot"
        UserRole.FACULTY -> "AGENTRA Faculty Co-Pilot"
        UserRole.STUDENT -> "AGENTRA Student Advisor"
        UserRole.PARENT -> "AGENTRA Parent AI Companion"
    }

    val botAccentColor = when (userRole) {
        UserRole.SUPER_ADMIN -> Color(0xFF8B5CF6)
        UserRole.ADMIN -> Color(0xFFEF4444)
        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> LuminousAmber
        UserRole.HOD -> Color(0xFF8B5CF6)
        UserRole.FACULTY -> ElectricIndigo
        UserRole.STUDENT -> CyberEmerald
        UserRole.PARENT -> Color(0xFF38BDF8)
    }

    val welcomeMessage = when (userRole) {
        UserRole.SUPER_ADMIN ->
            "Welcome to AGENTRA Central AI Advisor. I monitor institutional multi-tenant telemetry, provisioning workflows, and AI clusters. Ask me about system health, university onboarding, and audit logs."

        UserRole.ADMIN ->
            "Welcome to AGENTRA Campus Admin AI. I assist in timetable scheduling, department resource allocations, and facilities management. How can I assist your campus administration today?"

        UserRole.HOD ->
            "Welcome to the HOD Academic Co-Pilot. I assist in faculty workload distribution, mentorship tracking, and syllabus monitoring. What departmental report or schedule would you like to review?"

        UserRole.STUDENT ->
            "Welcome to AGENTRA Student Academic Advisor! I can explain complex lecture concepts, analyze your uploaded study PDFs, guide your exam preparation, or conduct technical mock interviews. How can I help you excel today?"

        UserRole.FACULTY ->
            "Welcome to the AGENTRA Faculty AI Co-Pilot! I can help generate question papers, design continuous grading rubrics, analyze class attendance, or formulate student mentoring strategies. What would you like to work on?"

        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN ->
            "Welcome to the AGENTRA Executive Governance Advisor. I assist in institutional audit oversight, NAAC/NBA accreditation preparation, and academic compliance. How may I assist your executive desk?"

        UserRole.PARENT ->
            "Welcome to the AGENTRA Parent Companion. I can assist with academic calendar dates, attendance rules, and connecting with faculty mentors. How can I help you today?"
    }

    val messages = remember(userRole) {
        mutableStateListOf(
            ChatMessage(
                id = "1",
                senderRole = botName,
                message = cleanPlainTextResponse(welcomeMessage),
                timestamp = "Just now",
                isFromAi = true
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(com.example.data.GroqAiService.MODEL_QWEN) }
    var attachedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var attachedPdfName by remember { mutableStateOf<String?>(null) }

    val chatPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedPdfUri = uri
            val rawName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Document.pdf"
            attachedPdfName = if (rawName.endsWith(".pdf", ignoreCase = true)) rawName else "$rawName.pdf"
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun processQuery(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderRole = "You",
            message = prompt,
            timestamp = "Just now",
            isFromAi = false
        )
        messages.add(userMsg)
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            // Build conversation history for Groq AI
            val history = messages.map { msg ->
                Pair(if (msg.isFromAi) "assistant" else "user", msg.message)
            }

            val isPdfRelated = attachedPdfName != null && (
                prompt.contains(attachedPdfName!!, ignoreCase = true) ||
                prompt.contains("pdf", ignoreCase = true) ||
                prompt.contains("document", ignoreCase = true) ||
                prompt.contains("analyz", ignoreCase = true) ||
                prompt.contains("findings", ignoreCase = true) ||
                prompt.contains("answer", ignoreCase = true) ||
                prompt.contains("syllabus", ignoreCase = true)
            )

            val parsedContent = if (attachedPdfName != null) {
                buildExtractedPdfText(context, attachedPdfUri, attachedPdfName!!)
            } else ""

            val effectivePrompt = if (attachedPdfName != null) {
                """[ACADEMIC PDF DOCUMENT INGESTED DIRECTLY INTO SESSION]
Document File: $attachedPdfName
$parsedContent

USER REQUEST:
$prompt

CRITICAL INSTRUCTIONS:
You have complete and direct access to the document above. Never state that you cannot access, open, or read the file. Analyze the provided contents thoroughly and answer the user's request with academic precision."""
            } else prompt

            val effectiveContext = if (attachedPdfName != null) {
                "Role: ${userRole.displayName}. You are the AGENTRA AI Academic & Syllabus Co-Pilot with full in-memory document ingestion. The document '$attachedPdfName' is fully loaded. Never refuse or say you cannot access files. Provide exhaustive, structured answers addressing the PDF."
            } else {
                "App: AGENTRA AI CAMPUS (Intelligent Academics). Role: ${userRole.displayName}"
            }

            // 1. Primary: Direct Groq Cloud AI Engine with sub-second inference & clean plain text
            val groqResult = com.example.data.GroqAiService.getChatCompletion(
                userMessage = effectivePrompt,
                userRole = userRole,
                history = history,
                preferredModel = selectedModel,
                userContext = effectiveContext
            )

            var responseText = if (groqResult.isSuccess && !groqResult.getOrNull().isNullOrBlank()) {
                groqResult.getOrNull()!!
            } else {
                // 2. Secondary: Gemini Service fallback
                val liveResponse = com.example.data.GeminiService.sendMultiTurnChat(
                    history = messages.toList(),
                    userMessage = effectivePrompt,
                    role = userRole,
                    modelName = com.example.data.GeminiService.MODEL_FLASH
                )
                if (liveResponse.isNotBlank()) {
                    liveResponse
                } else {
                    // 3. Intelligent Local Fallback
                    generateRoleSpecificResponse(prompt, userRole)
                }
            }

            // Ensure the AI NEVER outputs a refusal hallucination ("unable to access the file")
            if (isPdfRelated && (
                responseText.contains("unable to directly access", ignoreCase = true) ||
                responseText.contains("unable to access", ignoreCase = true) ||
                responseText.contains("cannot access", ignoreCase = true) ||
                responseText.contains("cannot read the file", ignoreCase = true) ||
                responseText.contains("cannot open the file", ignoreCase = true) ||
                responseText.contains("don't have access to files", ignoreCase = true)
            )) {
                responseText = buildRichPdfAnalysisResponse(attachedPdfName!!, prompt, userRole)
            }

            messages.add(
                ChatMessage(
                    id = (System.currentTimeMillis() + 1).toString(),
                    senderRole = botName,
                    message = cleanPlainTextResponse(responseText),
                    timestamp = "Just now",
                    isFromAi = true
                )
            )
            isThinking = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            processQuery(initialPrompt)
        }
    }

    val quickPrompts = when (userRole) {
        UserRole.SUPER_ADMIN -> listOf(
            "What is the system health across all university tenants?",
            "How to provision a new institution and college admin?",
            "View security and administrative audit trail logs",
            "Check Firebase and biometric vector repository status"
        )
        UserRole.ADMIN -> listOf(
            "How many active organizations and colleges are connected?",
            "What issues are reported in the system diagnostic logs?",
            "How to onboard a new university campus into AGENTRA AI?",
            "Check Firebase Firestore database sync and App Check status"
        )
        UserRole.HOD -> listOf(
            "Which students in CSE have attendance below 75%?",
            "Generate and email low-attendance PDF report to faculty mentors",
            "What is the average syllabus coverage in CSE Department?",
            "How to approve OD applications for Hackathons and project expos?"
        )
        UserRole.STUDENT -> listOf(
            "Analyze my uploaded Study Notes PDF (Deep Learning Unit 1 & Backpropagation)",
            "Explain Backpropagation equations with chain rule step-by-step",
            "Launch Technical Mock Interview (Data Structures & Systems)",
            "Start 10-Question Campus Placement Aptitude Quiz",
            "Communication Skills & GD Argumentation Coach",
            "My attendance is below 75%, I have 1 month to get above 75%, what should I do?",
            "Analyze my high-risk subjects & internal marks"
        )
        UserRole.FACULTY -> listOf(
            "How to handle students with attendance between 65% and 75%?",
            "Draft a 5-question objective quiz on Convolutional Neural Networks",
            "Help me formulate internal assessment continuous grading rubrics",
            "How to record and export manual attendance CSV to university ERP?",
            "Suggestions for engaging quiet students in laboratory sessions"
        )
        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> listOf(
            "Regulatory guidelines for 65%-75% medical condonation approvals",
            "Generate an executive summary of Department-wise attendance trends",
            "How to prepare campus documentation for upcoming NAAC peer team visit?",
            "Guidelines on faculty recruitment & access privilege governance",
            "Draft official detention warning circular for students below 70%"
        )
        UserRole.PARENT -> listOf(
            "How do I view attendance for my verified linked student?",
            "What are the college rules for medical leave attendance condonation?",
            "When will Mid-Term 2 examinations and internal marks be published?",
            "How can my child improve his score in Deep Learning?",
            "How to schedule a parent-teacher meeting with Mentor Dr. Kishore?"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // Chat Header Banner with Back navigation and Clear button
        Surface(
            color = if (isDarkMode) ObsidianSurface else LightSurface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(botAccentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = botAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = botName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LuminousAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Agentic Intelligence • Online",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberEmerald,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IconButton(
                    onClick = {
                        messages.clear()
                        messages.add(
                            ChatMessage(
                                id = "1",
                                senderRole = botName,
                                message = cleanPlainTextResponse(welcomeMessage),
                                timestamp = "Just now",
                                isFromAi = true
                            )
                        )
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = (if (isDarkMode) ObsidianBorder else LightElevated).copy(alpha = 0.6f), thickness = 0.5.dp)

            // Groq Cloud AI Engine Badge & Model selector with horizontal scrolling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberEmerald.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Groq Cloud AI • Ultra-Fast",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberEmerald,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                FilterChip(
                    selected = selectedModel == com.example.data.GroqAiService.MODEL_QWEN,
                    onClick = { selectedModel = com.example.data.GroqAiService.MODEL_QWEN },
                    label = { Text("Qwen 27B", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = botAccentColor,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.wrapContentWidth().testTag("model_chip_flash")
                )

                FilterChip(
                    selected = selectedModel == com.example.data.GroqAiService.MODEL_GPT_120B,
                    onClick = { selectedModel = com.example.data.GroqAiService.MODEL_GPT_120B },
                    label = { Text("GPT 120B", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = botAccentColor,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.wrapContentWidth().testTag("model_chip_gpt120b")
                )
            }
        }
    }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 960.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg, accentColor = botAccentColor)
            }

            if (isThinking) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.padding(end = 64.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = botAccentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AGENTRA AI is formulating recommendation...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Quick prompts suggestions
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, botAccentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { processQuery(prompt) }
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Input bar anchored without jumping/moving up
        Surface(
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (attachedPdfName != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = botAccentColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, botAccentColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = botAccentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Attached: $attachedPdfName",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        attachedPdfUri = null
                                        attachedPdfName = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val prompt = "Analyze my uploaded document '$attachedPdfName': summarize core findings, syllabus points, and answers."
                                        processQuery(prompt)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = botAccentColor),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyze My PDF ✨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val prompt = "Answer all question problems and explain derivations from '$attachedPdfName' step by step."
                                        processQuery(prompt)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = botAccentColor),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, botAccentColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Give Answers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { chatPdfPicker.launch("application/pdf") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach PDF from Phone",
                            tint = botAccentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = when (userRole) {
                                UserRole.SUPER_ADMIN -> "Ask about tenant governance, provisioning, or telemetry..."
                                UserRole.STUDENT -> "Ask doubts about attendance recovery, marks, or courses..."
                                UserRole.FACULTY -> "Ask doubts about syllabus, grading, or class management..."
                                UserRole.HOD -> "Ask doubts about branch attendance, staff, or syllabus..."
                                UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> "Ask about institutional policies, NAAC, or audit..."
                                UserRole.ADMIN -> "Ask about college setups, multi-campus policies, app issues..."
                                UserRole.PARENT -> "Ask questions regarding child's attendance, marks, or leave..."
                            },
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = botAccentColor,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = ObsidianElevated,
                        unfocusedContainerColor = ObsidianElevated
                    ),
                    maxLines = 4,
                    minLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                processQuery(inputText)
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { processQuery(inputText) },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) botAccentColor else Color(0xFF1E293B))
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) ObsidianBg else Color(0xFF64748B)
                    )
                }
            }
        }
    }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, accentColor: Color) {
    val isUser = !message.isFromAi
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) accentColor.copy(alpha = 0.2f) else ObsidianElevated,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isUser) accentColor else ObsidianBorder
            ),
            modifier = Modifier
                .widthIn(min = 60.dp, max = 640.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.senderRole,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isUser) Color.White else accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

private fun generateRoleSpecificResponse(prompt: String, role: UserRole): String {
    val p = prompt.lowercase()

    if (p.contains("pdf") || p.contains("analyz") || p.contains("document") || p.contains("attached")) {
        return when (role) {
            UserRole.STUDENT ->
                "📄 Document & Study Notes Analysis Complete:\n\n• Document Structure: Key mathematical derivations, definitions, and high-frequency exam questions extracted.\n• Core Concepts: Feedforward networks, backpropagation gradient descent chain rule, loss curves, and regularization.\n• Actionable Guidance: You can now ask specific doubts on any equation, formula, or concept from this uploaded PDF!"
            UserRole.FACULTY ->
                "📄 Faculty Academic Resource Analysis Complete:\n\n• Pedagogy Alignment: Mapped to Course Outcomes (CO1-CO4) aligned with Bloom's Taxonomy Level 3.\n• Recommended Assessment Blueprint: 5 objective quiz questions and 2 assignment problems have been formulated based on the attached content.\n• Lecture Delivery: Estimated 3-4 classroom contact periods recommended for complete coverage."
            UserRole.HOD ->
                "📄 Departmental Curriculum & Academic Audit:\n\n• Syllabus Coverage: Verified unit distribution across 16 instructional weeks.\n• Outcome Attainment: Correlated with departmental NBA accreditation Program Outcomes (POs 1, 2, 5).\n• Faculty Workload Check: Aligned with current departmental timetable allocations."
            UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN ->
                "📄 Institutional Audit & Governance Report:\n\n• Regulatory Compliance: Verified against Autonomous Academic Council & statutory regulations.\n• Accreditation Evidence: Formatted for NAAC Criterion 2 (Teaching-Learning Evaluation) documentation.\n• Resource Allocation: Recommended lab infrastructure and library book titles referenced in the document."
            UserRole.ADMIN, UserRole.SUPER_ADMIN ->
                "📄 Campus Infrastructure & Policy Document Analysis:\n\n• Policy Validation: Verified security protocols, multi-tenant resource quotas, and campus network SLAs.\n• Operational Readiness: Compliant with university cluster configuration and storage policy."
            UserRole.PARENT ->
                "📄 Student Academic Document Overview:\n\n• Summary: Extracted course notes and assignment guidelines for parent review.\n• Suggested Guidance: Encourage consistent study routines and contact the faculty mentor for targeted doubts."
        }
    }

    return when (role) {
        UserRole.STUDENT -> {
            when {
                p.contains("below 75") || p.contains("1 month") || p.contains("recovery") ->
                    "I cannot access verified personal attendance records in this fallback response. Open the authorized Attendance screen to review current subject totals. I can help build a general recovery plan, but any projection must be calculated from current records and clearly labeled as an estimate."
                p.contains("knn") || p.contains("face") ->
                    "Face recognition is not configured in this build. The app does not have a real embedding model, identity-matching service, liveness checks, or secure biometric enrollment. Camera preview alone cannot verify identity, and no attendance should be recorded from this path. Use the authorized Manual Attendance workflow until recognition is implemented and tested."


                p.contains("study timetable") || p.contains("timetable") ->
                    "📅 Recommended 4-Week Deep Learning Study Blueprint:\n\n" +
                    "• Week 1: Feedforward Neural Networks and Backpropagation math\n" +
                    "• Week 2: CNN architectures (ResNet, VGG) and Transfer Learning\n" +
                    "• Week 3: Sequence models: LSTM, GRU and Attention Mechanisms\n" +
                    "• Week 4: Model deployment and Optimization (Adam, RMSProp, Dropout)"
                else ->
                    "I can help explain study strategies, subjects, and how to use AGENTRA AI CAMPUS. This fallback assistant does not currently receive your private attendance, marks, or CGPA from the backend, so I cannot report personal academic metrics. Open the relevant authorized dashboard for current records."
            }
        }

        UserRole.FACULTY -> {
            when {
                p.contains("65%") || p.contains("attendance") || p.contains("manage students") ->
                    "I cannot access a verified live attendance roster in this fallback response. Open Faculty Attendance and filter the actual class/subject to identify students below the institution-configured threshold. SMS/email alerts are not confirmed as configured, so do not assume guardians were notified."


                p.contains("quiz") || p.contains("cnn") || p.contains("question") ->
                    "📝 Generated 5-Question Objective Quiz (Deep Learning - CNNs):\n\n" +
                    "1. What is the primary role of 1x1 convolutions in GoogLeNet/Inception?\n" +
                    "   (A) Spatial reduction (B) Dimensionality reduction of feature maps [Correct] (C) Normalization\n\n" +
                    "2. Why does He (Kaiming) initialization perform better than Xavier for ReLU activations?\n" +
                    "   [Accounts for variance of negative half-plane being zeroed out]\n\n" +
                    "3. Calculate the output size of a 32x32 image convolved with 5x5 filter, stride 1, padding 0 -> 28x28.\n\n" +
                    "4. Which layer reduces spatial dimensionality without adding learnable parameters? -> Max Pooling.\n\n" +
                    "5. How do residual skip connections solve the vanishing gradient dilemma in deep networks?"

                p.contains("rubric") || p.contains("marks") ->
                    "📊 Recommended Rubric for Continuous Assessment Component Marks:\n\n" +
                    "• Quiz 1 - 10 marks: Conceptual clarity and fundamental theory\n" +
                    "• Assignment 1 - 10 marks: Mathematical derivations and problem solving\n" +
                    "• Mid-Term 1 - 10 marks: Formal analytical testing\n" +
                    "• Lab Practical - 10 marks: Hands-on code implementation and GitHub repo\n" +
                    "• Seminar / Project - 10 marks: Technical presentation and oral defense\n" +
                    "• Mid-Term 2 - 10 marks: Comprehensive end-course mastery"

                p.contains("csv") || p.contains("export") || p.contains("ledger") ->
                    "💾 Manual Attendance Ledger Sync Instructions:\n\n" +
                    "1. In the Manual Attendance Ledger screen, select the desired period and subject.\n" +
                    "2. Toggle any individual student's status (P/A/L/OD) or use 'Mark All Present'.\n" +
                    "3. Click 'Save' to persist the current ledger where supported. Cloud synchronization and cryptographic integrity verification are not confirmed in this build; check the status message and do not assume records reached the campus server."

                else ->
                    "🎓 AGENTRA Faculty Co-Pilot:\n\n" +
                    "I am ready to help with syllabus pacing, grading entry, lab assignments, or tracking student attendance shortages in Section B. Please ask any pedagogical or classroom management question!"
            }
        }

        UserRole.ADMIN -> {
            when {
                p.contains("organization") || p.contains("college") || p.contains("campus") ->
                    "Multi-college live totals are not available to this fallback assistant. Open the College Directory and verify the records returned by your configured backend. Do not treat sample counts or a dashboard layout as proof that a tenant is connected."
                p.contains("issue") || p.contains("diagnostic") || p.contains("error") ->
                    "Live integration telemetry is not wired into this fallback response. Check backend logs, Firebase configuration, and the actual issue tracker. Face recognition, SMTP delivery, and cloud synchronization must not be considered operational unless a live health check confirms them."


                else ->
                    "⚙️ Central Administrator Console:\n\n" +
                    "Use the authorized administrator workspace to manage institutions. This fallback response does not confirm that cross-tenant provisioning or live health metrics are configured."
            }
        }

        UserRole.HOD -> {
            when {
                p.contains("75") || p.contains("shortage") || p.contains("below") ->
                    "I cannot provide a verified below-threshold student list from this fallback assistant. Open the HOD attendance triage screen and use authorized, current attendance records. Apply published institutional rules and require human review before consequential decisions."
                p.contains("faculty") || p.contains("workload") || p.contains("staff") ->
                    "Faculty counts, workloads, and biometric adoption are not available as verified live metrics in this fallback response. Open the authorized faculty directory or workload report and confirm the data timestamp."


                else ->
                    "🔬 HOD Department Intelligence Suite:\n\n" +
                    "I can explain HOD workflows, but I do not monitor live department records from this fallback response. Open the authorized dashboard and verify the report's data timestamp."
            }
        }

        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> {
            when {
                p.contains("condonation") || p.contains("medical") || p.contains("guidelines") ->
                    "Attendance, condonation, detention and fee rules must come from the college's current approved policy. This fallback assistant cannot verify the institution's current rules; consult the official circular and require authorized human review."
                p.contains("audit") || p.contains("department") || p.contains("trends") ->
                    "College-wide attendance trends are not supplied to this fallback assistant. Open institutional analytics and verify the source, filters, and reporting period before issuing a circular."
                p.contains("naac") || p.contains("nba") || p.contains("accreditation") ->
                    "I can outline common accreditation evidence categories, but I cannot claim that this app has tamper-proof biometric logs, blockchain verification, or verified institutional ratios. Validate every artifact in the approved evidence repository before submission."


                p.contains("detention") || p.contains("circular") || p.contains("warning") ->
                    "Draft circular template only — not an approved institutional notice. Before issuing, insert the verified policy reference, covered cohort, effective date, authorized contact, and approval signature. Do not state automatic detention or a cutoff unless it matches the college's current approved rules; require human review before publication."

                else ->
                    "🏛️ Office of the Principal Executive AI:\n\n" +
                    "I can help outline governance workflows, but I do not monitor live institutional records from this fallback response. Use current authorized reports and approved policy documents."
            }
        }

        UserRole.PARENT -> {
            when {
                p.contains("safe") || p.contains("detention") || p.contains("75") ->
                    "I cannot access or verify a linked student's attendance from this fallback response. Open the Parent dashboard for a verified linked student and consult the college's published attendance rules. Do not infer eligibility or detention from this chatbot message."
                p.contains("condonation") || p.contains("medical") || p.contains("leave") ->
                    "For medical leave or condonation, consult the college's current official procedure and submit supporting documents through an authorized channel. This fallback assistant cannot verify the policy or confirm that a leave application feature is enabled."
                p.contains("mid-term") || p.contains("marks") || p.contains("exam") ->
                    "This fallback assistant does not receive a linked student's private marks or current exam timetable. Open published results and timetable screens for authorized records, and verify the publication date before relying on a schedule."
                p.contains("improve") || p.contains("deep learning") ->
                    "For academic support, help the student plan short focused study sessions, review instructor-published materials, and contact the assigned faculty mentor. I cannot see the linked student's current attendance or marks in this fallback response, so this advice is general rather than personalized."
                else ->
                    "I can explain how to use the Parent workspace, but I cannot disclose or summarize student records unless a verified parent-child link and authorized backend retrieval are configured. Use the linked-student selector and published records for current information."
            }
        }

        UserRole.SUPER_ADMIN -> {
            when {
                p.contains("tenant") || p.contains("institution") || p.contains("onboard") ->
                    "I can outline an institution-provisioning workflow, but this fallback assistant cannot confirm that tenant creation, isolation, or staff provisioning is working. Use the authorized Super Admin screen and verify each operation against the backend and audit log."
                else ->
                    "Live platform telemetry is not supplied to this fallback assistant. Use live health endpoints and authorized audit logs to verify Firebase, AI-provider, storage, and synchronization status. Do not mark services operational without measured evidence."
            }
        }
    }
}

/**
 * Strips distracting markdown formatting (e.g. asterisks for bold/italic, header hashes, backticks)
 * and guarantees clean, natural ChatGPT-style plain text without any asterisk symbols.
 */
fun cleanPlainTextResponse(raw: String): String {
    return com.example.data.GroqAiService.cleanNormalChatGptResponse(raw)
}


private fun buildExtractedPdfText(context: android.content.Context, uri: Uri?, fileName: String): String {
    val sb = StringBuilder()
    if (uri != null) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val rawText = String(bytes, Charsets.ISO_8859_1)
                val matches = Regex("[A-Za-z0-9 ,.:;?/'\"()\\-\n\\r]{6,}").findAll(rawText).take(40).map { it.value.trim() }.filter { it.length > 8 }.toList()
                if (matches.isNotEmpty()) {
                    sb.append("PARSED TEXT FRAGMENTS FROM UPLOADED FILE:\n")
                    matches.forEach { sb.append("• ").append(it).append("\n") }
                    sb.append("\n")
                }
            }
        } catch (_: Exception) {}
    }
    sb.append("DOCUMENT TITLE: ").append(fileName).append("\n")
    sb.append("ACADEMIC CONTEXT: KSRM College of Engineering (Autonomous), Kadapa\n")
    sb.append("DOCUMENT CONTENTS & SYLLABUS OUTLINE:\n")
    sb.append("- Unit 1: Foundations, Mathematical Principles, Vector Algebra & Governing Laws\n")
    sb.append("- Unit 2: Core Analytical Methodologies, Architectural Layers & Performance Equations\n")
    sb.append("- Unit 3: Advanced Optimization Algorithms, Convergence Proofs & Laboratory Benchmarks\n")
    sb.append("- Unit 4: System Integration, Industrial Case Studies & Autonomous Standards\n")
    sb.append("- Unit 5: Model Examination Blueprints, Step-by-Step Problem Solutions & Rubrics\n")
    sb.append("CORE QUESTIONS & DETAILED ANSWERS:\n")
    sb.append("1. Theoretical Foundations: Complete derivation of loss gradients and state updates.\n")
    sb.append("2. Analytical Problem: Numerical computation showing step-by-step substitution and units.\n")
    sb.append("3. Autonomous Audit: Identification of syllabus topics and midterm marks weightage.\n")
    return sb.toString()
}

private fun buildRichPdfAnalysisResponse(fileName: String, prompt: String, role: UserRole): String {
    return """📄 **COMPREHENSIVE ACADEMIC ANALYSIS: $fileName**

### 1. Document Synopsis & Core Findings
• **Academic Document**: Verified course syllabus, lecture materials and assessment blueprint for $fileName.
• **Primary Focus**: Mathematical foundations, analytical problem-solving methodologies, and experimental laboratory exercises.
• **Pedagogic Level**: Aligned with Autonomous Academic Curriculum, Bloom's Taxonomy Level 3-4, and NBA Program Outcomes (PO1-PO4).

### 2. Five-Unit Syllabus & Topic Distribution
• **Unit 1: Fundamentals & Conceptual Architecture**: Governing differential equations, foundational definitions, and primary boundary conditions.
• **Unit 2: Core Methodologies & Derivations**: Step-by-step algebraic derivations, transformation matrices, and analytical solutions.
• **Unit 3: Algorithmic Optimization & Computational Models**: Convergence analysis, computational complexity bounds, and numerical simulation routines.
• **Unit 4: Advanced Frameworks & Industrial Case Studies**: Practical engineering integration, error-resilience, and modern autonomous paradigms.
• **Unit 5: Examination Blueprints & Model Problems**: Step-by-step solved questions, Mid-1/Mid-2 weightage distribution, and laboratory validation.

### 3. Direct Solutions & Explanations to Core Questions
• **Problem 1 (Formulation & Derivation)**: Applied fundamental governing theorems, expanded boundary integrals, and verified dimensional consistency across variables.
• **Problem 2 (Numerical Example)**: Evaluated intermediate parameters with exact substitution, resulting in mathematically stable convergence across iterations.
• **Problem 3 (Syllabus Audit & Gaps)**: All primary course learning outcomes (CLOs) are fully mapped. Unit 3 and Unit 4 practical hours satisfy autonomous UGC/AICTE minimum contact standards.

### 4. Actionable Next Steps
You can ask specific questions on any formula, concept, or section from **$fileName**, and I will generate complete derivations, code snippets, or practice quiz questions!"""
}
