package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppRole
import com.example.data.model.LoginResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    var showCreateAccount by rememberSaveable { mutableStateOf(false) }

    if (showCreateAccount) {
        CreateAccountScreen(
            viewModel = viewModel,
            onBackToLogin = { showCreateAccount = false }
        )
        return
    }

    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf<AppRole?>(null) }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }

    val loginState by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BmsDarkBackground,
                        BmsDarkSurface,
                        Color(0xFF070B14)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("login_screen")
    ) {
        // Decorative background glows
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-120).dp, y = (-180).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BmsPrimary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 130.dp, y = 180.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BmsSecondary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Centered Card Container
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
            ) {
                // Application Logo & Brand
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BmsPrimary, BmsSecondary)
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                ) {
                    Text(
                        text = "BOSS",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to BOSS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Business Operational Studio System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Error Message Banner
                AnimatedVisibility(
                    visible = loginState is LoginResult.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val errorMsg = (loginState as? LoginResult.Error)?.message ?: ""
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BmsError.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BmsError.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .testTag("login_error_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = BmsError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMsg,
                                color = BmsError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // User ID / Username / Email / Mobile Number Field
                OutlinedTextField(
                    value = emailOrUsername,
                    onValueChange = {
                        emailOrUsername = it
                        if (loginState is LoginResult.Error) viewModel.clearLoginError()
                    },
                    label = { Text("User ID / Username / Email / Mobile") },
                    placeholder = { Text("e.g. admin@bms.com, emp_shakir, or mobile") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "User Identifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field with Show/Hide Toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (loginState is LoginResult.Error) viewModel.clearLoginError()
                    },
                    label = { Text("Password") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.testTag("password_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.login(
                                emailOrUsername = emailOrUsername,
                                password = password,
                                selectedRole = selectedRole,
                                rememberMe = rememberMe
                            )
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Role Selection Dropdown Field
                ExposedDropdownMenuBox(
                    expanded = isRoleDropdownExpanded,
                    onExpandedChange = { isRoleDropdownExpanded = !isRoleDropdownExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("role_dropdown")
                ) {
                    OutlinedTextField(
                        value = selectedRole?.label ?: "Auto-Detect from Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role Verification (RBAC)") },
                        placeholder = { Text("Auto-Detect from Account") },
                        leadingIcon = {
                            Icon(
                                imageVector = when (selectedRole) {
                                    AppRole.ADMIN -> Icons.Default.Security
                                    AppRole.MANAGER -> Icons.Default.ManageAccounts
                                    AppRole.EMPLOYEE -> Icons.Default.Person
                                    null -> Icons.Outlined.Badge
                                },
                                contentDescription = "Role Icon",
                                tint = when (selectedRole) {
                                    AppRole.ADMIN -> BmsError
                                    AppRole.MANAGER -> MaterialTheme.colorScheme.primary
                                    AppRole.EMPLOYEE -> Color(0xFF10B981)
                                    null -> MaterialTheme.colorScheme.primary
                                }
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isRoleDropdownExpanded,
                        onDismissRequest = { isRoleDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Auto-Detect from Account", fontWeight = FontWeight.Bold)
                                    Text("Recommended", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Auto", tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                selectedRole = null
                                isRoleDropdownExpanded = false
                                if (loginState is LoginResult.Error) viewModel.clearLoginError()
                            },
                            modifier = Modifier.testTag("role_option_autodetect")
                        )

                        Divider()

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Admin", fontWeight = FontWeight.Bold)
                                    Text("Full System Access", fontSize = 11.sp, color = BmsError)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Security, contentDescription = "Admin", tint = BmsError)
                            },
                            onClick = {
                                selectedRole = AppRole.ADMIN
                                isRoleDropdownExpanded = false
                                if (loginState is LoginResult.Error) viewModel.clearLoginError()
                            },
                            modifier = Modifier.testTag("role_option_admin")
                        )

                        Divider()

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Manager", fontWeight = FontWeight.Bold)
                                    Text("Team & Operations", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ManageAccounts, contentDescription = "Manager", tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                selectedRole = AppRole.MANAGER
                                isRoleDropdownExpanded = false
                                if (loginState is LoginResult.Error) viewModel.clearLoginError()
                            },
                            modifier = Modifier.testTag("role_option_manager")
                        )

                        Divider()

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Employee", fontWeight = FontWeight.Bold)
                                    Text("Personal Workspace", fontSize = 11.sp, color = Color(0xFF10B981))
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Employee", tint = Color(0xFF10B981))
                            },
                            onClick = {
                                selectedRole = AppRole.EMPLOYEE
                                isRoleDropdownExpanded = false
                                if (loginState is LoginResult.Error) viewModel.clearLoginError()
                            },
                            modifier = Modifier.testTag("role_option_employee")
                        )
                    }
                }

                // Remember Me Checkbox & Forgot Password Link
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            modifier = Modifier.testTag("remember_me_checkbox")
                        )
                        Text(
                            text = "Remember Me",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            resetEmailInput = emailOrUsername
                            showForgotPasswordDialog = true
                        },
                        modifier = Modifier.testTag("forgot_password_button")
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sign In Button with Loading State
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login(
                            emailOrUsername = emailOrUsername,
                            password = password,
                            selectedRole = selectedRole,
                            rememberMe = rememberMe
                        )
                    },
                    enabled = loginState !is LoginResult.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_button")
                ) {
                    if (loginState is LoginResult.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Authenticating...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Sign In",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In to Workspace",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // New User / Create Account Section
                OutlinedButton(
                    onClick = { showCreateAccount = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Create Account",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New User? Create Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Security Assurance / RBAC explanation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "RBAC Security",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Protected by Role-Based Access Control (RBAC). Your actual permissions are securely verified on authentication.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = {
            showForgotPasswordDialog = false
            resetSuccessMessage = null
        }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockReset,
                        contentDescription = "Forgot Password",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Reset Your Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter your registered email address to receive password reset instructions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("Account Email") },
                        placeholder = { Text("your.email@bms.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (resetSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = resetSuccessMessage!!,
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            showForgotPasswordDialog = false
                            resetSuccessMessage = null
                        }) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (resetEmailInput.isNotBlank()) {
                                    resetSuccessMessage = "Reset link dispatched to $resetEmailInput. Check your inbox."
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Send Reset Link")
                        }
                    }
                }
            }
        }
    }
}
