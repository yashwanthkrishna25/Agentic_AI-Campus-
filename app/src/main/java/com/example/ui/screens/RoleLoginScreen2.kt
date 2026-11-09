//package com.example.ui.screens
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.testTag
//import androidx.compose.ui.platform.LocalContext
//import androidx.credentials.CredentialManager
//import androidx.credentials.CustomCredential
//import androidx.credentials.GetCredentialRequest
//import com.google.android.libraries.identity.googleid.GetGoogleIdOption
//import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import com.example.R
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.data.FirebaseRoleAuthManager
//import com.example.data.SessionManager
//import com.example.model.UserProfile
//import com.example.model.UserRole
//import com.example.ui.theme.*
//import kotlinx.coroutines.launch
//
///**
// * Unified Single Login & Sign Up Screen for AGENTRA AI.
// * Students, Faculty members, Admin, and Parents log in and sign up on this single unified interface.
// * Valid credentials persist session via SessionManager and sync role to Firestore user documents.
// */
//@Composable
//fun RoleLoginScreen(
//    onLoginSuccess: (UserProfile) -> Unit
//) {
//    val coroutineScope = rememberCoroutineScope()
//    var isSignUpTab by remember { mutableStateOf(false) }
//
//    fun handleSuccessfulLogin(profile: UserProfile) {
//        // The authenticated Firebase UID and trusted user record are resolved by the auth manager.
//        // Never synthesize a UID or write a role from the client during login.
//        onLoginSuccess(profile)
//    }
//
//    // Sign In states
//    var loginIdentifier by remember { mutableStateOf("") }
//    var loginPassword by remember { mutableStateOf("") }
//    var loginPasswordVisible by remember { mutableStateOf(false) }
//
//    // Sign Up states
//    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
//    var signUpName by remember { mutableStateOf("") }
//    var signUpId by remember { mutableStateOf("") }
//    var signUpEmail by remember { mutableStateOf("") }
//    var signUpPhone by remember { mutableStateOf("") }
//    var enteredOtp by remember { mutableStateOf("") }
//    var isOtpSent by remember { mutableStateOf(false) }
//    var isEmailOtpVerified by remember { mutableStateOf(false) }
//    var otpStatusMessage by remember { mutableStateOf<String?>(null) }
//    var signUpDepartment by remember { mutableStateOf("Computer Science & Engineering") }
//    var signUpPassword by remember { mutableStateOf("") }
//    var signUpPasswordVisible by remember { mutableStateOf(false) }
//
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//    var successMessage by remember { mutableStateOf<String?>(null) }
//    var isSubmitting by remember { mutableStateOf(false) }
//
//
//    val context = LocalContext.current
//    val credentialManager = remember { CredentialManager.create(context) }
//
//    fun startGoogleSignIn() {
//        if (isSubmitting) return
//        coroutineScope.launch {
//            isSubmitting = true
//            errorMessage = null
//            successMessage = null
//            try {
//                val googleOption = GetGoogleIdOption.Builder()
//                    .setFilterByAuthorizedAccounts(false)
//                    .setServerClientId(context.getString(R.string.default_web_client_id))
//                    .setAutoSelectEnabled(false)
//                    .build()
//
//                val request = GetCredentialRequest.Builder()
//                    .addCredentialOption(googleOption)
//                    .build()
//
//                val credentialResponse = credentialManager.getCredential(
//                    context = context,
//                    request = request
//                )
//                val credential = credentialResponse.credential
//
//                if (credential is CustomCredential &&
//                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
//                ) {
//                    val googleTokenCredential =
//                        GoogleIdTokenCredential.createFrom(credential.data)
//                    val result = FirebaseRoleAuthManager.signInWithGoogleIdToken(
//                        googleTokenCredential.idToken
//                    )
//                    result.onSuccess { profile ->
//                        handleSuccessfulLogin(profile)
//                    }.onFailure { error ->
//                        errorMessage = error.message ?: "Google sign-in failed."
//                    }
//                } else {
//                    errorMessage = "Google did not return a supported sign-in credential. Please try again."
//                }
//            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
//                errorMessage = when (e) {
//                    is androidx.credentials.exceptions.GetCredentialCancellationException ->
//                        "Google sign-in was cancelled."
//                    else -> e.message ?: "Could not open Google sign-in. Check your internet connection and Firebase setup."
//                }
//            } catch (e: Exception) {
//                errorMessage = e.message ?: "Google sign-in failed. Please try again."
//            } finally {
//                isSubmitting = false
//            }
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(
//                Brush.verticalGradient(
//                    colors = listOf(
//                        Color(0xFF030712),
//                        Color(0xFF0C1427),
//                        Color(0xFF060B18)
//                    )
//                )
//            )
//            .verticalScroll(rememberScrollState())
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Spacer(modifier = Modifier.height(18.dp))
//
//        // AGENTRA AI CAMPUS Stylized Animated Logo Crest
//        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "logo_anim")
//        val glowScale by infiniteTransition.animateFloat(
//            initialValue = 0.95f,
//            targetValue = 1.05f,
//            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
//                animation = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
//                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
//            ),
//            label = "scale"
//        )
//        val borderAlpha by infiniteTransition.animateFloat(
//            initialValue = 0.5f,
//            targetValue = 1f,
//            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
//                animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
//                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
//            ),
//            label = "border_alpha"
//        )
//
//        Box(
//            modifier = Modifier
//                .size(92.dp)
//                .graphicsLayer(scaleX = glowScale, scaleY = glowScale)
//                .clip(RoundedCornerShape(22.dp))
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(Color(0xFF0F1A30), Color(0xFF080E1E))
//                    )
//                )
//                .border(2.dp, CyberEmerald.copy(alpha = borderAlpha), RoundedCornerShape(22.dp)),
//            contentAlignment = Alignment.Center
//        ) {
//            Image(
//                painter = painterResource(id = R.drawable.agentra_logo),
//                contentDescription = "AGENTRA AI CAMPUS Logo",
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(4.dp)
//                    .clip(RoundedCornerShape(18.dp)),
//                contentScale = ContentScale.Crop
//            )
//        }
//
//        Spacer(modifier = Modifier.height(14.dp))
//
//        Text(
//            text = "AGENTRA AI CAMPUS",
//            style = MaterialTheme.typography.headlineMedium.copy(
//                fontWeight = FontWeight.Black,
//                letterSpacing = 2.sp,
//                color = Color.White
//            )
//        )
//
//        Text(
//            text = "Intelligent Agents. Smarter Academics.",
//            style = MaterialTheme.typography.bodySmall.copy(
//                color = CyberEmerald,
//                fontWeight = FontWeight.Bold,
//                fontSize = 12.sp,
//                letterSpacing = 0.5.sp
//            ),
//            textAlign = TextAlign.Center
//        )
//
//        Text(
//            text = "Autonomous Campus ERP & Multi-Role Governance",
//            style = MaterialTheme.typography.labelSmall.copy(
//                color = TextSecondary,
//                fontSize = 11.sp
//            )
//        )
//
//        Spacer(modifier = Modifier.height(14.dp))
//
//        // Live Firebase Cloud & Database Status Badge
//        Surface(
//            shape = RoundedCornerShape(20.dp),
//            color = Color(0xFF0F172A).copy(alpha = 0.85f),
//            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.6f))
//        ) {
//            Row(
//                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(8.dp)
//                        .clip(CircleShape)
//                        .background(CyberEmerald)
//                )
//                Spacer(modifier = Modifier.width(6.dp))
//                Text(
//                    text = "Firebase Auth & Firestore: Live Cloud Sync",
//                    color = CyberEmerald,
//                    fontSize = 11.sp,
//                    fontWeight = FontWeight.Bold,
//                    letterSpacing = 0.5.sp
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Tabs: Sign In / Create Account
//        Surface(
//            shape = RoundedCornerShape(14.dp),
//            color = Color(0xFF131D31),
//            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(4.dp)
//            ) {
//                TabButton(
//                    title = "Sign In",
//                    isSelected = !isSignUpTab,
//                    modifier = Modifier.weight(1f),
//                    onClick = {
//                        isSignUpTab = false
//                        errorMessage = null
//                    }
//                )
//                TabButton(
//                    title = "Create Account",
//                    isSelected = isSignUpTab,
//                    modifier = Modifier.weight(1f),
//                    onClick = {
//                        isSignUpTab = true
//                        errorMessage = null
//                    }
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(20.dp))
//
//        // Feedback messages
//        if (errorMessage != null) {
//            Surface(
//                color = Color(0xFF7F1D1D),
//                shape = RoundedCornerShape(10.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 14.dp)
//            ) {
//                Row(
//                    modifier = Modifier.padding(12.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5))
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = errorMessage!!,
//                        color = Color.White,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//            }
//        }
//
//        if (successMessage != null) {
//            Surface(
//                color = Color(0xFF065F46),
//                shape = RoundedCornerShape(10.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 14.dp)
//            ) {
//                Row(
//                    modifier = Modifier.padding(12.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7))
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = successMessage!!,
//                        color = Color.White,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//            }
//        }
//
//        if (!isSignUpTab) {
//            // ====================== SIGN IN FORM ======================
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(14.dp)
//            ) {
//                // Email or Institutional ID field
//                OutlinedTextField(
//                    value = loginIdentifier,
//                    onValueChange = { loginIdentifier = it },
//                    label = { Text("Campus Email") },
//                    placeholder = { Text("name@college.edu") },
//                    leadingIcon = {
//                        Icon(Icons.Default.Badge, contentDescription = null, tint = CyberEmerald)
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .testTag("login_identifier_input"),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = CyberEmerald,
//                        unfocusedBorderColor = Color(0xFF334155),
//                        focusedContainerColor = Color(0xFF0D1527),
//                        unfocusedContainerColor = Color(0xFF0D1527)
//                    ),
//                    singleLine = true
//                )
//
//                // Password field
//                OutlinedTextField(
//                    value = loginPassword,
//                    onValueChange = { loginPassword = it },
//                    label = { Text("Password") },
//                    placeholder = { Text("Enter your password") },
//                    leadingIcon = {
//                        Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricIndigo)
//                    },
//                    trailingIcon = {
//                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
//                            Icon(
//                                imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                                contentDescription = null,
//                                tint = TextMuted
//                            )
//                        }
//                    },
//                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .testTag("login_password_input"),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = ElectricIndigo,
//                        unfocusedBorderColor = Color(0xFF334155),
//                        focusedContainerColor = Color(0xFF0D1527),
//                        unfocusedContainerColor = Color(0xFF0D1527)
//                    ),
//                    singleLine = true
//                )
//
//                Text(
//                    text = "Use an institution-approved Firebase account. Demo role shortcuts are disabled for security.",
//                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
//                )
//
//                // Primary Login Button with Firebase Auth
//                Button(
//                    onClick = {
//                        if (loginIdentifier.isBlank()) {
//                            errorMessage = "Please enter your ID or Email."
//                            return@Button
//                        }
//                        if (loginPassword.isBlank()) {
//                            errorMessage = "Please enter your password."
//                            return@Button
//                        }
//                        isSubmitting = true
//                        errorMessage = null
//                        coroutineScope.launch {
//                            val result = FirebaseRoleAuthManager.signInWithFirebaseOrLocal(loginIdentifier, loginPassword)
//                            isSubmitting = false
//                            result.onSuccess { profile ->
//                                handleSuccessfulLogin(profile)
//                            }.onFailure { err ->
//                                errorMessage = err.message ?: "Authentication failed. Please check credentials."
//                            }
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(52.dp)
//                        .testTag("submit_login_button"),
//                    enabled = !isSubmitting,
//                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
//                    shape = RoundedCornerShape(12.dp)
//                ) {
//                    if (isSubmitting) {
//                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Connecting to Firebase...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
//                    } else {
//                        Icon(Icons.Default.Login, contentDescription = null)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Sign In to AGENTRA AI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
//                    }
//                }
//
//                // Google Sign In
//                OutlinedButton(
//                    onClick = { startGoogleSignIn() },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(50.dp)
//                        .testTag("google_login_button"),
//                    shape = RoundedCornerShape(12.dp),
//                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
//                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
//                ) {
//                    // Google 4-color dot icon
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(10.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFF4285F4))
//                        )
//                        Spacer(modifier = Modifier.width(3.dp))
//                        Box(
//                            modifier = Modifier
//                                .size(10.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFFEA4335))
//                        )
//                        Spacer(modifier = Modifier.width(3.dp))
//                        Box(
//                            modifier = Modifier
//                                .size(10.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFFFBBC05))
//                        )
//                        Spacer(modifier = Modifier.width(3.dp))
//                        Box(
//                            modifier = Modifier
//                                .size(10.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFF34A853))
//                        )
//                        Spacer(modifier = Modifier.width(10.dp))
//                        Text("Continue with Google Workspace", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
//                    }
//                }
//            }
//        } else {
//            // ====================== SIGN UP FORM ======================
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Text(
//                    text = "SELECT INSTITUTIONAL ROLE:",
//                    style = MaterialTheme.typography.labelSmall.copy(
//                        color = TextMuted,
//                        fontWeight = FontWeight.Bold,
//                        letterSpacing = 1.sp
//                    )
//                )
//
//                // 3 Role Selection Radios
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(6.dp)
//                ) {
//                    RoleSelectChip(
//                        role = UserRole.STUDENT,
//                        label = "Student",
//                        color = CyberEmerald,
//                        isSelected = selectedRole == UserRole.STUDENT,
//                        modifier = Modifier.weight(1f),
//                        onClick = { selectedRole = UserRole.STUDENT }
//                    )
//                    RoleSelectChip(
//                        role = UserRole.FACULTY,
//                        label = "Faculty",
//                        color = ElectricIndigo,
//                        isSelected = selectedRole == UserRole.FACULTY,
//                        modifier = Modifier.weight(1f),
//                        onClick = { selectedRole = UserRole.FACULTY }
//                    )
//                    RoleSelectChip(
//                        role = UserRole.HOD,
//                        label = "HOD",
//                        color = Color(0xFF8B5CF6),
//                        isSelected = selectedRole == UserRole.HOD,
//                        modifier = Modifier.weight(1f),
//                        onClick = { selectedRole = UserRole.HOD }
//                    )
//                }
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(6.dp)
//                ) {
//                    RoleSelectChip(
//                        role = UserRole.PRINCIPAL,
//                        label = "Principal",
//                        color = LuminousAmber,
//                        isSelected = selectedRole == UserRole.PRINCIPAL || selectedRole == UserRole.PRINCIPAL_ADMIN,
//                        modifier = Modifier.weight(1f),
//                        onClick = { selectedRole = UserRole.PRINCIPAL }
//                    )
//                    RoleSelectChip(
//                        role = UserRole.ADMIN,
//                        label = "Admin",
//                        color = Color(0xFFEF4444),
//                        isSelected = selectedRole == UserRole.ADMIN,
//                        modifier = Modifier.weight(1f),
//                        onClick = { selectedRole = UserRole.ADMIN }
//                    )
//                    RoleSelectChip(
//                        role = UserRole.PARENT,
//                        label = "Parent",
//                        color = Color(0xFF38BDF8),
//                        isSelected = selectedRole == UserRole.PARENT,
//                        modifier = Modifier.weight(1f),
//                        onClick = { selectedRole = UserRole.PARENT }
//                    )
//                }
//
//                // Name
//                OutlinedTextField(
//                    value = signUpName,
//                    onValueChange = { signUpName = it },
//                    label = { Text("Full Name") },
//                    placeholder = { Text("e.g. Sattu Penchala Prasad / Dr. Kishore") },
//                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberEmerald) },
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = CyberEmerald,
//                        unfocusedBorderColor = Color(0xFF334155),
//                        focusedContainerColor = Color(0xFF0D1527),
//                        unfocusedContainerColor = Color(0xFF0D1527)
//                    ),
//                    singleLine = true
//                )
//
//                // Roll No / Employee ID
//                OutlinedTextField(
//                    value = signUpId,
//                    onValueChange = { signUpId = it },
//                    label = { Text(if (selectedRole == UserRole.STUDENT) "Student Roll Number" else "Employee / Admin ID") },
//                    placeholder = { Text(if (selectedRole == UserRole.STUDENT) "e.g. 239Y1A0501" else "e.g. EMP-CSE-102") },
//                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ElectricIndigo) },
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = ElectricIndigo,
//                        unfocusedBorderColor = Color(0xFF334155),
//                        focusedContainerColor = Color(0xFF0D1527),
//                        unfocusedContainerColor = Color(0xFF0D1527)
//                    ),
//                    singleLine = true
//                )
//
//                // Email with OTP verification
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    OutlinedTextField(
//                        value = signUpEmail,
//                        onValueChange = {
//                            signUpEmail = it
//                            isEmailOtpVerified = false
//                            isOtpSent = false
//                        },
//                        label = { Text("Official Email Address") },
//                        placeholder = { Text("e.g. name@ksrmce.ac.in") },
//                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LuminousAmber) },
//                        trailingIcon = {
//                            if (isEmailOtpVerified) {
//                                Icon(Icons.Default.Verified, contentDescription = "Email Verified", tint = CyberEmerald)
//                            } else {
//                                TextButton(
//                                    onClick = {
//                                        if (com.example.data.OtpService.isValidEmail(signUpEmail)) {
//                                            coroutineScope.launch {
//                                                errorMessage = null
//                                                val result = com.example.data.OtpService.sendEmailOtp(signUpEmail)
//                                                result.onSuccess {
//                                                    isOtpSent = true
//                                                    isEmailOtpVerified = false
//                                                    otpStatusMessage = "Verification code sent. Check your inbox and spam folder."
//                                                }.onFailure { err ->
//                                                    isOtpSent = false
//                                                    otpStatusMessage = null
//                                                    errorMessage = err.message ?: "Could not send verification code."
//                                                }
//                                            }
//                                        } else {
//                                            errorMessage = "Please enter a valid email address first."
//                                        }
//                                    }
//                                ) {
//                                    Text(
//                                        text = if (isOtpSent) "Resend OTP" else "Send OTP",
//                                        color = CyberEmerald,
//                                        fontWeight = FontWeight.Bold,
//                                        fontSize = 11.5.sp
//                                    )
//                                }
//                            }
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(12.dp),
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedTextColor = Color.White,
//                            unfocusedTextColor = Color.White,
//                            focusedBorderColor = if (isEmailOtpVerified) CyberEmerald else LuminousAmber,
//                            unfocusedBorderColor = Color(0xFF334155),
//                            focusedContainerColor = Color(0xFF0D1527),
//                            unfocusedContainerColor = Color(0xFF0D1527)
//                        ),
//                        singleLine = true
//                    )
//
//                    // Email OTP Input Section when OTP is sent
//                    if (isOtpSent && !isEmailOtpVerified) {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(8.dp)
//                        ) {
//                            OutlinedTextField(
//                                value = enteredOtp,
//                                onValueChange = { if (it.length <= 6) enteredOtp = it },
//                                label = { Text("6-Digit Email OTP") },
//                                placeholder = { Text("Enter 6 digits") },
//                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = CyberEmerald) },
//                                modifier = Modifier.weight(1f),
//                                shape = RoundedCornerShape(10.dp),
//                                colors = OutlinedTextFieldDefaults.colors(
//                                    focusedTextColor = Color.White,
//                                    unfocusedTextColor = Color.White,
//                                    focusedBorderColor = CyberEmerald,
//                                    unfocusedBorderColor = Color(0xFF334155),
//                                    focusedContainerColor = Color(0xFF0D1527),
//                                    unfocusedContainerColor = Color(0xFF0D1527)
//                                ),
//                                singleLine = true
//                            )
//
//                            Button(
//                                onClick = {
//                                    coroutineScope.launch {
//                                        val result = com.example.data.OtpService.verifyEmailOtp(signUpEmail, enteredOtp)
//                                        result.onSuccess { verified ->
//                                            isEmailOtpVerified = verified
//                                            otpStatusMessage = if (verified) "Email verified successfully!" else null
//                                            errorMessage = if (verified) null else "Invalid or expired code. Please check your email and try again."
//                                        }.onFailure { err ->
//                                            errorMessage = err.message ?: "Could not verify the code."
//                                        }
//                                    }
//                                },
//                                shape = RoundedCornerShape(10.dp),
//                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
//                                modifier = Modifier.height(52.dp)
//                            ) {
//                                Text("Verify", fontWeight = FontWeight.Bold, color = Color.White)
//                            }
//                        }
//                    }
//
//                    if (otpStatusMessage != null) {
//                        Text(
//                            text = otpStatusMessage!!,
//                            color = if (isEmailOtpVerified) CyberEmerald else LuminousAmber,
//                            fontSize = 11.sp,
//                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
//                        )
//                    }
//                }
//
//                // Valid Phone Number
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    OutlinedTextField(
//                        value = signUpPhone,
//                        onValueChange = { signUpPhone = it.take(13) },
//                        label = { Text("10-Digit Mobile Number") },
//                        placeholder = { Text("+91 9876543210") },
//                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalBlue) },
//                        trailingIcon = {
//                            if (com.example.data.OtpService.isValidPhoneNumber(signUpPhone)) {
//                                Icon(Icons.Default.CheckCircle, contentDescription = "Valid Phone", tint = CyberEmerald)
//                            }
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(12.dp),
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedTextColor = Color.White,
//                            unfocusedTextColor = Color.White,
//                            focusedBorderColor = if (com.example.data.OtpService.isValidPhoneNumber(signUpPhone)) CyberEmerald else RoyalBlue,
//                            unfocusedBorderColor = Color(0xFF334155),
//                            focusedContainerColor = Color(0xFF0D1527),
//                            unfocusedContainerColor = Color(0xFF0D1527)
//                        ),
//                        singleLine = true
//                    )
//                    if (signUpPhone.isNotBlank() && !com.example.data.OtpService.isValidPhoneNumber(signUpPhone)) {
//                        Text(
//                            text = "Enter a valid 10-digit mobile number (e.g. 9876543210)",
//                            color = Color(0xFFF87171),
//                            fontSize = 10.5.sp,
//                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
//                        )
//                    }
//                }
//
//                // Department
//                OutlinedTextField(
//                    value = signUpDepartment,
//                    onValueChange = { signUpDepartment = it },
//                    label = { Text("Department / Branch") },
//                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = RoyalBlue) },
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = RoyalBlue,
//                        unfocusedBorderColor = Color(0xFF334155),
//                        focusedContainerColor = Color(0xFF0D1527),
//                        unfocusedContainerColor = Color(0xFF0D1527)
//                    ),
//                    singleLine = true
//                )
//
//                // Password
//                OutlinedTextField(
//                    value = signUpPassword,
//                    onValueChange = { signUpPassword = it },
//                    label = { Text("Create Password") },
//                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
//                    trailingIcon = {
//                        IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
//                            Icon(
//                                imageVector = if (signUpPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                                contentDescription = null,
//                                tint = TextMuted
//                            )
//                        }
//                    },
//                    visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = CyberEmerald,
//                        unfocusedBorderColor = Color(0xFF334155),
//                        focusedContainerColor = Color(0xFF0D1527),
//                        unfocusedContainerColor = Color(0xFF0D1527)
//                    ),
//                    singleLine = true
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                // Submit Sign Up Button with Firebase Auth
//                Button(
//                    onClick = {
//                        if (signUpName.isBlank()) {
//                            errorMessage = "Please enter your full name."
//                            return@Button
//                        }
//                        if (signUpPassword.length < 8) {
//                            errorMessage = "Create a password with at least 8 characters."
//                            return@Button
//                        }
//                        if (signUpId.isBlank()) {
//                            errorMessage = "Please enter your Roll Number or Employee ID."
//                            return@Button
//                        }
//                        if (!com.example.data.OtpService.isValidEmail(signUpEmail)) {
//                            errorMessage = "Please enter a valid email address."
//                            return@Button
//                        }
//                        if (!isEmailOtpVerified) {
//                            errorMessage = "Verify your email address before creating an account."
//                            return@Button
//                        }
//                        if (selectedRole != UserRole.STUDENT && selectedRole != UserRole.PARENT) {
//                            errorMessage = "Faculty, HOD, Principal, and Administrator accounts must be created or approved by an authorized administrator."
//                            return@Button
//                        }
//                        if (signUpPhone.isNotBlank() && !com.example.data.OtpService.isValidPhoneNumber(signUpPhone)) {
//                            errorMessage = "Please enter a valid 10-digit mobile number."
//                            return@Button
//                        }
//
//                        isSubmitting = true
//                        errorMessage = null
//                        coroutineScope.launch {
//                            val result = FirebaseRoleAuthManager.signUpWithFirebase(
//                                name = signUpName,
//                                email = signUpEmail,
//                                institutionalId = signUpId,
//                                phone = signUpPhone,
//                                role = selectedRole,
//                                department = signUpDepartment,
//                                password = signUpPassword
//                            )
//                            isSubmitting = false
//                            result.onSuccess { profile ->
//                                successMessage = "Account created & synchronized to Firestore! Welcome, ${profile.name}."
//                                handleSuccessfulLogin(profile)
//                            }.onFailure { err ->
//                                val message = err.message ?: "Registration failed."
//                                if (message.startsWith("Account created.")) {
//                                    successMessage = message
//                                    errorMessage = null
//                                } else {
//                                    errorMessage = message
//                                }
//                            }
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(52.dp)
//                        .testTag("submit_signup_button"),
//                    enabled = !isSubmitting,
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = when (selectedRole) {
//                            UserRole.STUDENT -> CyberEmeraldDark
//                            UserRole.FACULTY -> ElectricIndigo
//                            UserRole.HOD -> Color(0xFF8B5CF6)
//                            UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> Color(0xFFD97706)
//                            UserRole.ADMIN -> Color(0xFFDC2626)
//                            UserRole.PARENT -> Color(0xFF0284C7)
//                        }
//                    ),
//                    shape = RoundedCornerShape(12.dp)
//                ) {
//                    if (isSubmitting) {
//                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Creating Firebase Profile...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
//                    } else {
//                        Icon(Icons.Default.PersonAdd, contentDescription = null)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Create ${selectedRole.displayName} Account", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
//                    }
//                }
//
//                // Google Sign Up
//                OutlinedButton(
//                    onClick = { startGoogleSignIn() },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(50.dp),
//                    shape = RoundedCornerShape(12.dp),
//                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
//                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
//                ) {
//                    Text("Sign Up with Google Workspace", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(20.dp))
//    }
//
//}
//
//@Composable
//fun TabButton(
//    title: String,
//    isSelected: Boolean,
//    modifier: Modifier = Modifier,
//    onClick: () -> Unit
//) {
//    Surface(
//        modifier = modifier
//            .clip(RoundedCornerShape(10.dp))
//            .clickable(onClick = onClick),
//        color = if (isSelected) CyberEmeraldDark else Color.Transparent,
//        shape = RoundedCornerShape(10.dp)
//    ) {
//        Box(
//            modifier = Modifier.padding(vertical = 10.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = title,
//                color = if (isSelected) Color.White else TextSecondary,
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
//                fontSize = 14.sp
//            )
//        }
//    }
//}
//
//@Composable
//fun RoleSelectChip(
//    role: UserRole,
//    label: String,
//    color: Color,
//    isSelected: Boolean,
//    modifier: Modifier = Modifier,
//    onClick: () -> Unit
//) {
//    Surface(
//        modifier = modifier
//            .clip(RoundedCornerShape(10.dp))
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(10.dp),
//        color = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF0F172A),
//        border = androidx.compose.foundation.BorderStroke(
//            width = if (isSelected) 1.5.dp else 1.dp,
//            color = if (isSelected) color else Color(0xFF1E293B)
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(vertical = 10.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = when (role) {
//                    UserRole.STUDENT -> Icons.Default.School
//                    UserRole.FACULTY -> Icons.Default.SupervisorAccount
//                    UserRole.HOD -> Icons.Default.Engineering
//                    UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> Icons.Default.AdminPanelSettings
//                    UserRole.ADMIN -> Icons.Default.Domain
//                    UserRole.PARENT -> Icons.Default.FamilyRestroom
//                },
//                contentDescription = null,
//                tint = if (isSelected) color else TextMuted,
//                modifier = Modifier.size(20.dp)
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = label,
//                color = if (isSelected) Color.White else TextMuted,
//                fontSize = 12.sp,
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
//            )
//        }
//    }
//}
//
//@Composable
//fun DemoFillChip(
//    roleLabel: String,
//    color: Color,
//    modifier: Modifier = Modifier,
//    onClick: () -> Unit
//) {
//    Surface(
//        modifier = modifier
//            .clip(RoundedCornerShape(8.dp))
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(8.dp),
//        color = Color(0xFF131D31),
//        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
//    ) {
//        Row(
//            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(6.dp)
//                    .clip(CircleShape)
//                    .background(color)
//            )
//            Spacer(modifier = Modifier.width(5.dp))
//            Text(
//                text = roleLabel,
//                color = Color.White,
//                fontSize = 11.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//    }
//}
//
//@Composable
//fun GoogleAccountRow(
//    name: String,
//    email: String,
//    role: String,
//    onClick: () -> Unit
//) {
//    Surface(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(10.dp))
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(10.dp),
//        color = Color(0xFF1E293B)
//    ) {
//        Row(
//            modifier = Modifier.padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(36.dp)
//                    .clip(CircleShape)
//                    .background(CyberEmeraldDark),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = name.first().toString(),
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 15.sp
//                )
//            }
//            Spacer(modifier = Modifier.width(10.dp))
//            Column {
//                Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
//                Text(email, color = TextSecondary, fontSize = 11.sp)
//                Text(role, color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
//            }
//        }
//    }
//}







