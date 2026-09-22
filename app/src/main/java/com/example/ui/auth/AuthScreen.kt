package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay

enum class AuthMode {
    SIGN_UP,
    SIGN_IN
}

@Composable
fun AuthScreen(
    onSignUpWithEmail: (name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSignInWithEmail: (email: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSignInWithGoogle: (email: String, name: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onStartGoogleOAuth: (() -> Unit)? = null,
    onContinueAsGuest: () -> Unit,
    onBack: (() -> Unit)? = null,
    initialEmail: String = "",
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthMode.SIGN_UP) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf(initialEmail) }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("auth_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Back Button (if dismissible) and Brand Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("auth_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Supabase Cloud Connected Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SunlitClay.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SunlitClay.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(SunlitClay)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Supabase Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal
                            )
                        }
                    }
                }
            }

            // App Logo & Title
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(SunlitClay)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Daymark logo",
                        tint = Charcoal,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (mode == AuthMode.SIGN_UP) "Create Your Journal" else "Welcome Back",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (mode == AuthMode.SIGN_UP)
                        "Sign up with your own Gmail or email to keep your memories personal and organized."
                    else
                        "Sign in to your personal visual journal.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Mode Toggle Tab: "Sign Up" vs "Sign In"
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = AshGrey.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Sign Up Tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (mode == AuthMode.SIGN_UP) Charcoal else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable {
                                    mode = AuthMode.SIGN_UP
                                    errorMessage = null
                                }
                                .testTag("tab_sign_up")
                        ) {
                            Text(
                                text = "Sign Up",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == AuthMode.SIGN_UP) Porcelain else Charcoal
                            )
                        }

                        // Sign In Tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (mode == AuthMode.SIGN_IN) Charcoal else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable {
                                    mode = AuthMode.SIGN_IN
                                    errorMessage = null
                                }
                                .testTag("tab_sign_in")
                        ) {
                            Text(
                                text = "Sign In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == AuthMode.SIGN_IN) Porcelain else Charcoal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // 1-Tap Google / Gmail Sign In Section
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Continue with Google / Gmail",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use your personal Gmail account to automatically identify your daily journal.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Sign In Button (Uses native Credential Manager or fallback)
                        OutlinedButton(
                            onClick = {
                                if (onStartGoogleOAuth != null) {
                                    onStartGoogleOAuth()
                                } else if (emailInput.isNotBlank() && emailInput.contains("@")) {
                                    val finalName = nameInput.trim().ifEmpty {
                                        emailInput.substringBefore("@").replace(".", " ")
                                            .replaceFirstChar { it.uppercase() }
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    onSignInWithGoogle(emailInput.trim(), finalName) { success, err ->
                                        isLoading = false
                                        if (!success) errorMessage = err
                                    }
                                } else {
                                    errorMessage = "Please enter your email address below or use Google Sign-in"
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Charcoal),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Charcoal
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_with_google_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Real official Google multi-colored G logo
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google Logo",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Divider: OR ENTER YOUR OWN EMAIL
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AshGrey.copy(alpha = 0.35f))
                    Text(
                        text = "OR USE ANY EMAIL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AshGrey.copy(alpha = 0.35f))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Form Fields: Name (if Sign Up), Email, Password
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Full Name (Only on Sign Up)
                    if (mode == AuthMode.SIGN_UP) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                errorMessage = null
                            },
                            label = { Text("Your Name") },
                            placeholder = { Text("e.g. Sara Jenkins") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SunlitClay
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Charcoal,
                                unfocusedBorderColor = AshGrey.copy(alpha = 0.5f),
                                focusedLabelColor = Charcoal
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )
                    }

                    // Email Address Input
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorMessage = null
                        },
                        label = { Text("Email / Gmail Address") },
                        placeholder = { Text("your.email@gmail.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = SunlitClay
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Charcoal,
                            unfocusedBorderColor = AshGrey.copy(alpha = 0.5f),
                            focusedLabelColor = Charcoal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SunlitClay
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = AshGrey
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Charcoal,
                            unfocusedBorderColor = AshGrey.copy(alpha = 0.5f),
                            focusedLabelColor = Charcoal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )
                }

                // Error Message
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE57373)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color(0xFFC62828),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Primary Action Button (Sign Up / Sign In) in Deep Charcoal
            item {
                Button(
                    onClick = {
                        val email = emailInput.trim()
                        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
                            errorMessage = "Please enter a valid Gmail or email address."
                            return@Button
                        }
                        if (passwordInput.isBlank() || passwordInput.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        if (mode == AuthMode.SIGN_UP) {
                            val name = nameInput.trim().ifEmpty {
                                email.substringBefore("@").replace(".", " ")
                                    .replaceFirstChar { it.uppercase() }
                            }
                            onSignUpWithEmail(name, email, passwordInput) { success, err ->
                                isLoading = false
                                if (!success) errorMessage = err
                            }
                        } else {
                            onSignInWithEmail(email, passwordInput) { success, err ->
                                isLoading = false
                                if (!success) errorMessage = err
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Charcoal,
                        contentColor = Porcelain
                    ),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_button")
                ) {
                    if (isLoading) {
                        Text("Processing...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (mode == AuthMode.SIGN_UP) "Create My Account" else "Sign In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Info Card: Supabase Cloud Connected Notice
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AshGrey.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SunlitClay,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cloud Sync Active: Live Supabase Auth and private cloud storage are connected. Sign in with Google or your email to sync and safeguard your daily memories.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Guest Option
            item {
                TextButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier.testTag("auth_guest_button")
                ) {
                    Text(
                        text = "Continue as Guest for now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
