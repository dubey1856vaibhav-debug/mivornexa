package com.example.bizflow.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.data.SessionManager
import com.example.bizflow.model.BusinessType
import com.example.bizflow.model.Customer
import com.example.bizflow.model.UserRole
import com.example.bizflow.ui.components.AnimatedAuthIllustration
import com.example.bizflow.ui.components.AuthFocusState
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthMode {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD,
    CUSTOMER_PORTAL_LOGIN
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    repository: BizFlowRepository,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onLoginSuccess: (isCustomerPortal: Boolean, customer: Customer?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customers by repository.customers.collectAsState()

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var focusState by remember { mutableStateOf(AuthFocusState.IDLE) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Session-persisted credentials
    val savedEmail = remember { SessionManager.getSavedEmail(context) }
    val savedRememberMe = remember { SessionManager.shouldRememberMe(context) }

    // Form inputs (remembered if rememberMe was previously set, or clean blank)
    var email by remember { mutableStateOf(if (savedRememberMe) savedEmail else "") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(savedRememberMe) }

    // Signup specific
    var fullName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var selectedBusinessType by remember { mutableStateOf(BusinessType.RETAIL_ECOMMERCE) }
    var agreedToTerms by remember { mutableStateOf(true) }

    // Customer Portal specific
    var customerEmailOrPhone by remember { mutableStateOf("") }

    // Dynamic Theme Palettes for AuthScreen
    val screenBg = if (isDarkTheme) Color(0xFF0F1219) else Color(0xFFF1F5F9)
    val cardBg = if (isDarkTheme) Color(0xFF181D29) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkTheme) Color(0xFF283245) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val tabContainerBg = if (isDarkTheme) Color(0xFF202737) else Color(0xFFF1F5F9)
    val inputBg = if (isDarkTheme) Color(0xFF202737) else Color(0xFFF8FAFC)
    val inputBorder = if (isDarkTheme) Color(0xFF333F56) else Color(0xFFCBD5E1)
    val googleBtnBg = if (isDarkTheme) Color(0xFF202737) else Color(0xFFFFFFFF)
    val googleBtnBorder = if (isDarkTheme) Color(0xFF333F56) else Color(0xFFCBD5E1)
    val dividerColor = if (isDarkTheme) Color(0xFF283245) else Color(0xFFE2E8F0)

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MivornexaPurple,
        unfocusedBorderColor = inputBorder,
        focusedContainerColor = inputBg,
        unfocusedContainerColor = inputBg,
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedLabelColor = MivornexaPurple,
        unfocusedLabelColor = textSecondary,
        cursorColor = MivornexaPurple
    )

    // Outer container with responsive background
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 12.dp else 6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .testTag("auth_card_container")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header: Branding + Dark Mode Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mivornexa",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MivornexaPurple
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0xFF3B1E78) else MivornexaPurpleContainer
                            ) {
                                Text(
                                    text = "BUSINESS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFD8B4FE) else MivornexaPurpleDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Theme Mode Toggle Button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = tabContainerBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, inputBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    val newTheme = !isDarkTheme
                                    onToggleDarkTheme(newTheme)
                                    SessionManager.setDarkThemePreference(context, newTheme)
                                }
                                .testTag("auth_dark_mode_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                    tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF6366F1),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDarkTheme) "Light" else "Dark",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                // 1. Playful Animated Character Illustration
                item {
                    AnimatedAuthIllustration(
                        focusState = focusState,
                        isSuccess = isSuccess
                    )
                }

                // 2. Clear Auth Mode Selector: Log In vs Sign Up
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabContainerBg)
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (authMode == AuthMode.LOGIN) MivornexaPurple else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                }
                                .testTag("tab_login")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Log In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (authMode == AuthMode.LOGIN) Color.White else textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (authMode == AuthMode.SIGNUP) MivornexaPurple else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    authMode = AuthMode.SIGNUP
                                    errorMessage = null
                                }
                                .testTag("tab_signup")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Sign Up",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (authMode == AuthMode.SIGNUP) Color.White else textSecondary
                                )
                            }
                        }
                    }
                }

                // Error Message Banner if invalid input
                if (errorMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0xFF451A1A) else Color(0xFFFEE2E2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                // 3. Dynamic Content Header
                item {
                    when (authMode) {
                        AuthMode.LOGIN -> {
                            Column {
                                Text(
                                    text = "Welcome back!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enter your credentials to access your company workspaces.",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        AuthMode.SIGNUP -> {
                            Column {
                                Text(
                                    text = "Create Mivornexa Account",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Start managing your operations across retail, services, or repair.",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        AuthMode.FORGOT_PASSWORD -> {
                            Column {
                                Text(
                                    text = "Reset Password",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enter your verified work email to receive password reset link.",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        AuthMode.CUSTOMER_PORTAL_LOGIN -> {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                ) {
                                    Text(
                                        text = "CLIENT & CUSTOMER PORTAL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Track Your Order & Invoices",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Client self-service access to order progress, milestones, and receipts.",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // 4. Form Fields
                if (authMode == AuthMode.LOGIN) {
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MivornexaPurple) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) focusState = AuthFocusState.EMAIL }
                                .testTag("login_email_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MivornexaPurple) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) focusState = AuthFocusState.PASSWORD }
                                .testTag("login_password_input")
                        )
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MivornexaPurple,
                                        uncheckedColor = textSecondary
                                    ),
                                    modifier = Modifier.testTag("remember_me_checkbox")
                                )
                                Text("Remember me", fontSize = 12.sp, color = textSecondary)
                            }
                            Text(
                                text = "Forgot password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MivornexaPurple,
                                modifier = Modifier
                                    .clickable { authMode = AuthMode.FORGOT_PASSWORD }
                                    .testTag("forgot_password_link")
                            )
                        }
                    }

                    // Primary Login Button
                    item {
                        Button(
                            onClick = {
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = "Please enter a valid work email address."
                                    return@Button
                                }
                                if (password.length < 4) {
                                    errorMessage = "Password must be at least 4 characters."
                                    return@Button
                                }
                                errorMessage = null
                                isAuthenticating = true
                                coroutineScope.launch {
                                    delay(600)
                                    val targetWs = repository.signInWithEmail(email)
                                    // Persist active session and remember me setting
                                    SessionManager.saveSession(
                                        context = context,
                                        email = email,
                                        rememberMe = rememberMe,
                                        workspaceId = targetWs.id,
                                        isCustomerPortal = false
                                    )
                                    isAuthenticating = false
                                    isSuccess = true
                                    focusState = AuthFocusState.AUTHENTICATED
                                    delay(400)
                                    onLoginSuccess(false, null)
                                }
                            },
                            enabled = !isAuthenticating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MivornexaPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_primary_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticating...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Log In to Mivornexa", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    // Google Sign-In Button
                    item {
                        OutlinedButton(
                            onClick = {
                                isAuthenticating = true
                                val activity = (context as? Activity)
                                    ?: ((context as? android.content.ContextWrapper)?.baseContext as? Activity)
                                val serverClientId = try {
                                    context.getString(R.string.default_web_client_id)
                                } catch (e: Exception) {
                                    null
                                }

                                if (activity != null && serverClientId != null) {
                                    val credentialManager = CredentialManager.create(context)
                                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
                                    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                                    coroutineScope.launch {
                                        try {
                                            val result = credentialManager.getCredential(activity, request)
                                            val credential = result.credential
                                            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                                                val signedInEmail = authResult.user?.email ?: "google.user@bizflow.io"
                                                val ws = repository.signInWithEmail(signedInEmail)
                                                SessionManager.saveSession(
                                                    context = context,
                                                    email = signedInEmail,
                                                    rememberMe = true,
                                                    workspaceId = ws.id,
                                                    isCustomerPortal = false
                                                )
                                                isAuthenticating = false
                                                isSuccess = true
                                                focusState = AuthFocusState.AUTHENTICATED
                                                delay(400)
                                                onLoginSuccess(false, null)
                                            } else {
                                                errorMessage = "Unexpected credential response"
                                                isAuthenticating = false
                                            }
                                        } catch (e: GetCredentialCancellationException) {
                                            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                                            isAuthenticating = false
                                        } catch (e: Exception) {
                                            Log.e("Auth", "Google Sign-In failed", e)
                                            errorMessage = e.localizedMessage ?: "Sign in failed"
                                            isAuthenticating = false
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        delay(400)
                                        val ws = repository.signInWithEmail("google.user@bizflow.io")
                                        SessionManager.saveSession(
                                            context = context,
                                            email = "google.user@bizflow.io",
                                            rememberMe = true,
                                            workspaceId = ws.id,
                                            isCustomerPortal = false
                                        )
                                        isAuthenticating = false
                                        isSuccess = true
                                        focusState = AuthFocusState.AUTHENTICATED
                                        delay(300)
                                        onLoginSuccess(false, null)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = googleBtnBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, googleBtnBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                tint = Color(0xFFEA4335),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Links to Sign-Up and Customer Portal
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row {
                                Text("New business? ", fontSize = 12.sp, color = textSecondary)
                                Text(
                                    text = "Create account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MivornexaPurple,
                                    modifier = Modifier
                                        .clickable {
                                            authMode = AuthMode.SIGNUP
                                            errorMessage = null
                                        }
                                        .testTag("switch_to_signup")
                                )
                            }

                            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 4.dp))

                            TextButton(
                                onClick = {
                                    authMode = AuthMode.CUSTOMER_PORTAL_LOGIN
                                    errorMessage = null
                                },
                                modifier = Modifier.testTag("switch_to_customer_portal")
                            ) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Client Portal (Track Order / Bills)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                } else if (authMode == AuthMode.SIGNUP) {
                    item {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Your Full Name *") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Work Email *") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (min 6 characters) *") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_password_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company / Business Name *") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_company_input")
                        )
                    }
                    item {
                        Text("Business Template (${BusinessType.values().size} Available)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(BusinessType.values()) { bType ->
                                FilterChip(
                                    selected = selectedBusinessType == bType,
                                    onClick = { selectedBusinessType = bType },
                                    label = { Text(bType.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MivornexaPurple,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                if (fullName.isBlank()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = "Please enter a valid work email address."
                                    return@Button
                                }
                                if (companyName.isBlank()) {
                                    errorMessage = "Please enter your company name."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@Button
                                }

                                errorMessage = null
                                isAuthenticating = true
                                coroutineScope.launch {
                                    delay(700)
                                    val newWs = repository.createCompanyAccount(fullName, email, companyName, selectedBusinessType)
                                    // Save registered company workspace and session
                                    SessionManager.saveSession(
                                        context = context,
                                        email = email,
                                        rememberMe = true,
                                        workspaceId = newWs.id,
                                        isCustomerPortal = false
                                    )
                                    isAuthenticating = false
                                    isSuccess = true
                                    focusState = AuthFocusState.AUTHENTICATED
                                    Toast.makeText(context, "Welcome! Workspace '$companyName' created.", Toast.LENGTH_SHORT).show()
                                    delay(400)
                                    onLoginSuccess(false, null)
                                }
                            },
                            enabled = !isAuthenticating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MivornexaPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("signup_submit_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Setting Up Company...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Create Account & Launch", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text("Already registered? ", fontSize = 12.sp, color = textSecondary)
                            Text(
                                text = "Sign in",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MivornexaPurple,
                                modifier = Modifier.clickable {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                }
                            )
                        }
                    }
                } else if (authMode == AuthMode.FORGOT_PASSWORD) {
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Registered Email Address") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Password reset instructions dispatched to $email", Toast.LENGTH_LONG).show()
                                authMode = AuthMode.LOGIN
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MivornexaPurple),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Send Reset Link", fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        TextButton(
                            onClick = { authMode = AuthMode.LOGIN },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Sign In", color = MivornexaPurple)
                        }
                    }
                } else if (authMode == AuthMode.CUSTOMER_PORTAL_LOGIN) {
                    item {
                        OutlinedTextField(
                            value = customerEmailOrPhone,
                            onValueChange = { customerEmailOrPhone = it },
                            label = { Text("Client Email or Phone Number *") },
                            placeholder = { Text("e.g. sarah.j@example.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("customer_portal_input")
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                val matchedCustomer = customers.find {
                                    it.email.equals(customerEmailOrPhone.trim(), true) ||
                                    it.phone.contains(customerEmailOrPhone.trim())
                                } ?: customers.firstOrNull()

                                isAuthenticating = true
                                coroutineScope.launch {
                                    delay(600)
                                    SessionManager.saveSession(
                                        context = context,
                                        email = customerEmailOrPhone,
                                        rememberMe = true,
                                        workspaceId = repository.currentWorkspace.value.id,
                                        isCustomerPortal = true,
                                        customerId = matchedCustomer?.id
                                    )
                                    isAuthenticating = false
                                    isSuccess = true
                                    delay(400)
                                    onLoginSuccess(true, matchedCustomer)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("enter_customer_portal_button")
                        ) {
                            Text("Enter Client Portal", fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        TextButton(
                            onClick = { authMode = AuthMode.LOGIN },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return to Staff / Owner Login", color = textSecondary)
                        }
                    }
                }
            }
        }
    }
}
