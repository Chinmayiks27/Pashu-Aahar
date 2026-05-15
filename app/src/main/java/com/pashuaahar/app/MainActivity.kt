@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.pashuaahar.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.pashuaahar.app.LanguageManager.gs
import com.pashuaahar.app.ui.theme.PashuAaharTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import androidx.compose.ui.text.input.VisualTransformation

import java.util.*

fun hashPassword(password: String): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleDailyWorker(this)
        setContent {
            PashuAaharTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val db = AppDatabase.getDatabase(context)
                val preferenceManager = remember { PreferenceManager(context) }
                var currentUserPhone by remember { mutableStateOf(preferenceManager.getUserPhone()) }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavHost(
                        navController = navController,
                        startDestination = if (currentUserPhone != null) "main" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { phone ->
                                    preferenceManager.saveUserPhone(phone)
                                    currentUserPhone = phone
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                    // Restore all history from Firebase to local Room DB
                                    val appDb = AppDatabase.getDatabase(context)
                                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                        try { FirebaseManager.restoreFeedRecords(appDb, phone) } catch (_: Exception) {}
                                        try { FirebaseManager.restoreDailySavings(appDb, phone) } catch (_: Exception) {}
                                    }
                                },
                                onSignUpClick = { navController.navigate("signup") },
                                db = db
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                onRegisterSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                },
                                onLoginClick = { navController.navigate("login") },
                                db = db
                            )
                        }
                        composable("main") {
                            currentUserPhone?.let { phone ->
                                MainContainer(
                                    navController = navController,
                                    userPhone = phone,
                                    db = db,
                                    onLogout = {
                                        val phoneToDelete = currentUserPhone
                                        preferenceManager.clearUserPhone()
                                        currentUserPhone = null
                                        navController.navigate("login") {
                                            popUpTo("main") { inclusive = true }
                                        }
                                        // Clear local Room DB on logout so no stale data shown
                                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                            try {
                                                if (phoneToDelete != null) {
                                                    db.feedRecordDao().deleteAllForUser(phoneToDelete)
                                                    db.dailySavingEntryDao().deleteAllForUser(phoneToDelete)
                                                }
                                            } catch (_: Exception) {}
                                        }
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

@Composable
fun MainContainer(
    navController: androidx.navigation.NavController,
    userPhone: String,
    db: AppDatabase,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gs("app_title"), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            FirebaseManager.logout()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color(0xFF1B5E20))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE8F5E9),
                    titleContentColor = Color(0xFF1B5E20)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = gs("calc")) },
                    label = { Text(gs("calc")) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Paid, contentDescription = gs("prices")) },
                    label = { Text(gs("prices")) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = gs("history")) },
                    label = { Text(gs("history")) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Info, contentDescription = gs("tips")) },
                    label = { Text(gs("tips")) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = gs("ai_advisor")) },
                    label = { Text(gs("ai_advisor")) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> CattleProfileScreen(db, userPhone)
                1 -> IngredientPricesScreen(context = LocalContext.current)
                2 -> HistoryScreen(db, userPhone)
                3 -> VeterinaryTipsScreen()
                4 -> AiAdvisorScreen(db = db, userPhone = userPhone)
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onSignUpClick: () -> Unit, db: AppDatabase) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your registered email address.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            scope.launch {
                                val result = FirebaseManager.sendPasswordReset(resetEmail.trim())
                                withContext(Dispatchers.Main) {
                                    showForgotDialog = false
                                    Toast.makeText(
                                        context,
                                        if (result.isSuccess)
                                            "Reset link sent! Check your email inbox."
                                        else
                                            "Error: ${result.exceptionOrNull()?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Send Reset Link") }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pashu-Aahar", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(Modifier.height(8.dp))
        Text(
            if (LanguageManager.isKannada) "ಡೈರಿ ರೈತರಿಗಾಗಿ" else "For Dairy Farmers",
            color = Color.Gray, fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2E7D32)) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(gs("password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32)) }
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                scope.launch {
                    val firebaseResult = FirebaseManager.login(email.trim(), password)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        if (firebaseResult.isSuccess) {
                            onLoginSuccess(email.trim())
                        } else {
                            Toast.makeText(
                                context,
                                "Login failed: ${firebaseResult.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(gs("login"))
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showForgotDialog = true }) {
            Text(
                if (LanguageManager.isKannada) "ಪಾಸ್‌ವರ್ಡ್ ಮರೆತಿರಾ?" else "Forgot Password?",
                color = Color(0xFF2E7D32)
            )
        }
        TextButton(onClick = onSignUpClick) {
            Text(
                if (LanguageManager.isKannada) "ಖಾತೆ ಇಲ್ಲವೇ? ಸೈನ್ ಅಪ್" else "Don't have an account? Sign Up"
            )
        }
    }
}

@Composable
fun SignUpScreen(onRegisterSuccess: () -> Unit, onLoginClick: () -> Unit, db: AppDatabase) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }
    val passwordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
    val isPasswordStrong = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecial

    val strengthScore = listOf(hasMinLength, hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }
    val strengthLabel = when (strengthScore) {
        0, 1 -> "Very Weak"; 2 -> "Weak"; 3 -> "Fair"; 4 -> "Strong"; 5 -> "Very Strong"; else -> ""
    }
    val strengthColor = when (strengthScore) {
        0, 1 -> Color(0xFFD32F2F); 2 -> Color(0xFFFF5722); 3 -> Color(0xFFFFA000)
        4 -> Color(0xFF388E3C); 5 -> Color(0xFF1B5E20); else -> Color.Gray
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pashu-Aahar", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text(
            if (LanguageManager.isKannada) "ಹೊಸ ಖಾತೆ ತೆರೆಯಿರಿ" else "Create New Account",
            color = Color.Gray
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E7D32)) }
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2E7D32)) },
            supportingText = {
                if (email.isNotEmpty() && !email.contains("@"))
                    Text("Enter a valid email address", color = Color(0xFFD32F2F), fontSize = 11.sp)
            }
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(gs("password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32)) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = Color.Gray
                    )
                }
            }
        )

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier.weight(1f).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index < strengthScore) strengthColor else Color(0xFFE0E0E0))
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Password Strength:", fontSize = 11.sp, color = Color.Gray)
                Text(strengthLabel, fontSize = 11.sp, color = strengthColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PasswordRuleRow(hasMinLength, "At least 8 characters")
                    PasswordRuleRow(hasUppercase, "One uppercase letter (A-Z)")
                    PasswordRuleRow(hasLowercase, "One lowercase letter (a-z)")
                    PasswordRuleRow(hasDigit, "One number (0-9)")
                    PasswordRuleRow(hasSpecial, "One special character (!@#\$%^&*)")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32)) },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = Color.Gray
                    )
                }
            },
            supportingText = {
                if (confirmPassword.isNotEmpty() && !passwordsMatch)
                    Text("Passwords do not match", color = Color(0xFFD32F2F), fontSize = 11.sp)
                else if (passwordsMatch)
                    Text("Passwords match ✓", color = Color(0xFF2E7D32), fontSize = 11.sp)
            }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ->
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    !email.contains("@") ->
                        Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                    !isPasswordStrong ->
                        Toast.makeText(context, "Please meet all password requirements", Toast.LENGTH_LONG).show()
                    !passwordsMatch ->
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    else -> {
                        isLoading = true
                        // Navigate immediately after Firebase succeeds
                        // Do NOT wait for Room DB or profile save
                        scope.launch(Dispatchers.IO) {
                            val firebaseResult = FirebaseManager.register(email.trim(), password)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                if (firebaseResult.isSuccess) {
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    onRegisterSuccess()
                                    // Background: save profile and local DB after navigating
                                    scope.launch(Dispatchers.IO) {
                                        try { FirebaseManager.saveUserProfile(name, email.trim()) } catch (_: Exception) {}
                                        try { db.userDao().register(User(email.trim(), name, hashPassword(password))) } catch (_: Exception) {}
                                    }
                                } else {
                                    val msg = firebaseResult.exceptionOrNull()?.message ?: "Registration failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            else Text(gs("register"))
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onLoginClick) {
            Text(
                if (LanguageManager.isKannada) "ಲಾಗಿನ್ ಮಾಡಿ" else "Back to Login"
            )
        }
    }
}

// Helper composable for each password rule row
@Composable
fun PasswordRuleRow(passed: Boolean, rule: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (passed) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = rule,
            fontSize = 12.sp,
            color = if (passed) Color(0xFF2E7D32) else Color.Gray
        )
    }
}

@Composable
fun CattleProfileScreen(db: AppDatabase, userPhone: String) {
    var currentStep by remember { mutableIntStateOf(1) }
    var cowName by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("Desi") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var currentYield by remember { mutableStateOf("") }
    var targetYield by remember { mutableStateOf("") }
    var resultRecipe by remember { mutableStateOf<Map<String, Double>?>(null) }
    var savings by remember { mutableStateOf(0.0) }

    // Feature states
    var selectedIngredients by remember { mutableStateOf(setOf("Maize", "Cottonseed Cake", "Wheat Bran")) }
    var marketBagPrice by remember { mutableStateOf("1200") }
    var nutritionMatrix by remember { mutableStateOf<NutritionMatrix?>(null) }


    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(gs("calc"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            StepCircle(1, currentStep)
            StepLine()
            StepCircle(2, currentStep)
            StepLine()
            StepCircle(3, currentStep)
        }
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (currentStep) {
                1 -> Column {
                    Text(if(LanguageManager.isKannada) "ಹಂತ 1: ಹಸುವಿನ ವಿವರ" else "Step 1: Cow Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))


                    OutlinedTextField(
                        value = cowName,
                        onValueChange = { cowName = it },
                        label = { Text(if (LanguageManager.isKannada) "ಹಸುವಿನ ಹೆಸರು" else "Cow Name") },
                        placeholder = {
                            Text(if (LanguageManager.isKannada) "ಉದಾ: ಲಕ್ಷ್ಮಿ" else "e.g. Lakshmi")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("🐄", fontSize = 18.sp) },
                        supportingText = {
                            Text(
                                if (LanguageManager.isKannada) "ಗುರುತಿಸಲು"
                                else "helps identify your cow"
                            )
                        }
                    )

                    Text(if(LanguageManager.isKannada) "ತಳಿ ಆಯ್ಕೆಮಾಡಿ" else "Select Breed")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Desi", "Jersey", "HF").forEach { b ->
                            FilterChip(selected = breed == b, onClick = { breed = b }, label = { Text(b) })
                        }
                    }
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text(if(LanguageManager.isKannada) "ವಯಸ್ಸು (ವರ್ಷ)" else "Age (Years)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text(if(LanguageManager.isKannada) "ತೂಕ (ಕೆಜಿ)" else "Weight (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { if(age.isNotEmpty() && weight.isNotEmpty()) currentStep = 2 else Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text(if(LanguageManager.isKannada) "ಮುಂದೆ" else "Next") }
                }
                2 -> Column {
                    Text(if(LanguageManager.isKannada) "ಹಂತ 2: ಹಾಲು ಇಳುವರಿ ಗುರಿ" else "Step 2: Yield Target", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = currentYield, onValueChange = { currentYield = it }, label = { Text(if(LanguageManager.isKannada) "ಪ್ರಸ್ತುತ ಇಳುವರಿ (ಲೀಟರ್)" else "Current Yield (L)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = targetYield, onValueChange = { targetYield = it }, label = { Text(if(LanguageManager.isKannada) "ಗುರಿ ಇಳುವರಿ (ಲೀಟರ್)" else "Target Yield (L)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    OutlinedTextField(
                        value = marketBagPrice,
                        onValueChange = { marketBagPrice = it },
                        label = { Text(LanguageManager.gs("market_price")) },
                        placeholder = { Text("e.g. 1200") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = {
                            Text(
                                if (LanguageManager.isKannada) "50 ಕೆಜಿ ಚೀಲದ ಬೆಲೆ (₹)"
                                else "Price of 50 kg market feed bag (₹)"
                            )
                        }
                    )
                    Text(
                        "💡 Grain prices can be updated in the Prices tab",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    val allIngredients = listOf("Maize", "Barley", "Millet", "Cottonseed Cake", "Soy Meal", "Mustard Cake", "Wheat Bran", "Rice Bran")
                    Text(
                        text = if (LanguageManager.isKannada) "ಲಭ್ಯವಿರುವ ಪದಾರ್ಥಗಳನ್ನು ಆರಿಸಿ" else "Select Available Ingredients",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allIngredients.forEach { ingredient ->
                            val label = LanguageManager.gs(ingredient)
                            FilterChip(
                                selected = ingredient in selectedIngredients,
                                onClick = {
                                    selectedIngredients = if (ingredient in selectedIngredients) {
                                        selectedIngredients - ingredient
                                    } else {
                                        selectedIngredients + ingredient
                                    }
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (ingredient in selectedIngredients)
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                    if (selectedIngredients.isEmpty()) {
                        Text(
                            text = if (LanguageManager.isKannada) "ಕನಿಷ್ಠ ಒಂದು ಪದಾರ್ಥವನ್ನು ಆರಿಸಿ" else "Please select at least one ingredient",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { currentStep = 1 }, modifier = Modifier.weight(1f)) { Text(if(LanguageManager.isKannada) "ಹಿಂದೆ" else "Back") }
                        Button(onClick = {
                            val w = weight.toDoubleOrNull() ?: 400.0
                            val ty = targetYield.toDoubleOrNull() ?: 0.0
                            val cy = currentYield.toDoubleOrNull() ?: 0.0
                            val a = age.toDoubleOrNull() ?: 5.0
                            val error = NutritionEngine.validateTargetYield(breed, ty)
                            if (error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            } else if (selectedIngredients.isEmpty()) {
                                Toast.makeText(context, "Select at least one ingredient", Toast.LENGTH_SHORT).show()
                            } else {
                                val breedMultiplier = when(breed) {
                                    "HF" -> 1.2
                                    "Jersey" -> 1.1
                                    else -> 1.0
                                }
                                val profile = CattleProfile(breed, a, w, cy)
                                val recipe = NutritionEngine.calculateRecipe(profile, ty, selectedIngredients.toList())
                                resultRecipe = recipe

                                val bagPrice = marketBagPrice.toDoubleOrNull() ?: 1200.0
                                savings = NutritionEngine.calculateSavings(
                                    concKg        = (1.5 + ty * 0.4) * breedMultiplier,
                                    marketBagPrice = bagPrice,
                                    targetYield   = ty,
                                    currentYield  = cy,
                                    availableIngredients = selectedIngredients.toList(),
                                    context = context
                                )

                                nutritionMatrix = NutritionEngine.calculateMatrix(profile, ty)

                                currentStep = 3
                                scope.launch(Dispatchers.IO) {
                                    val record = FeedRecord(
                                        userPhone = userPhone,
                                        date = System.currentTimeMillis(),
                                        cowName = cowName,
                                        breed = breed,
                                        age = a,
                                        weight = w,
                                        currentYield = cy,
                                        targetYield = ty,
                                        marketBagPrice = bagPrice,
                                        dailySavings = savings
                                    )
                                    // Insert to Room and get real generated ID
                                    val generatedId = db.feedRecordDao().insertRecord(record)
                                    // Sync to Firebase using real ID - prevents overwrite bug
                                    FirebaseManager.syncFeedRecord(record.copy(id = generatedId.toInt()))
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, if(LanguageManager.isKannada) "ಇತಿಹಾಸದಲ್ಲಿ ಉಳಿಸಲಾಗಿದೆ!" else "Saved to history!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }, modifier = Modifier.weight(1f)) { Text(if(LanguageManager.isKannada) "ಲೆಕ್ಕ ಹಾಕಿ" else "Calculate") }
                    }
                }
                3 -> Column {
                    Text(if(LanguageManager.isKannada) "ಹಂತ 3: ಶಿಫಾರಸು ಮಾಡಿದ ಆಹಾರ" else "Step 3: Recommended Recipe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                    Spacer(Modifier.height(16.dp))

                    nutritionMatrix?.let { matrix ->
                        NutritionMatrixCard(matrix, targetYield.toDoubleOrNull() ?: 0.0)
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1F8E9),
                            contentColor = Color(0xFF1B5E20)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            resultRecipe?.forEach { (item, amount) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Task 9: Icon update
                                        Icon(imageVector = getFodderIcon(item), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(item, color = Color(0xFF1B5E20))
                                    }
                                    Text("${String.format("%.2f", amount)} kg", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                }
                            }
                            Divider(Modifier.padding(vertical = 8.dp), color = Color(0xFFC8E6C9))

                            val breedMultiplier = when(breed) {
                                "HF" -> 1.2
                                "Jersey" -> 1.1
                                else -> 1.0
                            }
                            val tyVal = targetYield.toDoubleOrNull() ?: 0.0
                            val cyVal = currentYield.toDoubleOrNull() ?: 0.0
                            val concKg = (1.5 + tyVal * 0.4) * breedMultiplier
                            val bagPrice = marketBagPrice.toDoubleOrNull() ?: 1200.0
                            val marketCostDay = concKg * (bagPrice / 50.0)
                            val hmCostDay = NutritionEngine.calculateHomemadeCost(
                                concKg,
                                tyVal,
                                cyVal,
                                selectedIngredients.toList(),
                                context = context
                            )
                            val saving = maxOf(marketCostDay - hmCostDay, 0.0)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Market Feed"); Text("₹${"%.2f".format(marketCostDay)}/day", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Home-made Feed"); Text("₹${"%.2f".format(hmCostDay)}/day", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                            Divider(Modifier.padding(vertical = 6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Daily Saving", fontWeight = FontWeight.Bold)
                                Text("₹${"%.2f".format(saving)}", color = Color(0xFF2E7D32), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monthly Saving (×30)")
                                Text("₹${"%.2f".format(saving * 30)}", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Yearly Saving (×365)")
                                Text("₹${"%.0f".format(saving * 365)}", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { currentStep = 1; cowName = ""; age = ""; weight = ""; currentYield = ""; targetYield = "" }, modifier = Modifier.fillMaxWidth()) { Text(if(LanguageManager.isKannada) "ಮತ್ತೆ ಪ್ರಾರಂಭಿಸಿ" else "Start New Calculation") }
                }
            }
        }
    }
}

@Composable fun StepCircle(step: Int, currentStep: Int) {
    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (step <= currentStep) Color(0xFF2E7D32) else Color.LightGray), contentAlignment = Alignment.Center) {
        Text(step.toString(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}
@Composable fun StepLine() { Box(Modifier.width(40.dp).height(2.dp).background(Color.LightGray)) }

@Composable
fun HistoryScreen(db: AppDatabase, userPhone: String) {
    val history by db.feedRecordDao().getHistory(userPhone).collectAsState(initial = emptyList())
    val accumulated by db.dailySavingEntryDao().getTotalAccumulated(userPhone).collectAsState(initial = 0.0)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedRecord by remember { mutableStateOf<FeedRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Cost Savings History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (LanguageManager.isKannada) "ಯಾವುದೇ ದಾಖಲೆಗಳಿಲ್ಲ" else "No records found.")
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {

                // ── Green "Total Money Saved" banner ──
                val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayTotal = history
                    .filter { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)) == todayString }
                    .sumOf { it.dailySavings }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total Money Saved So Far", color = Color.White, fontSize = 14.sp)
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", accumulated ?: 0.0)}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Today's saving: ₹${"%.2f".format(todayTotal)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }

                // ── Info text ──
                Text(
                    "💡 Total savings grow automatically each day as you add cow calculations.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // ── Individual history cards ──
                history.forEach { record ->
                    HistoryCard(
                        record,
                        onClick = { selectedRecord = record },
                        onDelete = {
                            scope.launch {
                                db.feedRecordDao().deleteRecord(record)
                                try { FirebaseManager.deleteFeedRecord(record) } catch (_: Exception) {}
                            }
                        }
                    )
                }

            }
        }

        // ── Detail dialog ──
        selectedRecord?.let { rec ->
            AlertDialog(
                onDismissRequest = { selectedRecord = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Calculation Detail", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Cow Profile", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        if (rec.cowName.isNotBlank()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cow Name", color = Color.Gray, fontSize = 13.sp)
                                Text("🐄 ${rec.cowName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Breed", color = Color.Gray, fontSize = 13.sp)
                            Text(rec.breed, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Age", color = Color.Gray, fontSize = 13.sp)
                            Text(
                                if (rec.age > 0) "${rec.age.toInt()} years" else "Not recorded",
                                fontSize = 13.sp,
                                color = if (rec.age > 0) Color.Unspecified else Color.Gray
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Weight", color = Color.Gray, fontSize = 13.sp)
                            Text("${rec.weight.toInt()} kg", fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Yield", color = Color.Gray, fontSize = 13.sp)
                            Text("${rec.currentYield}L → ${rec.targetYield}L/day", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("Savings Breakdown", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Market price", color = Color.Gray, fontSize = 13.sp)
                            Text("₹${rec.marketBagPrice.toInt()}/50kg", color = Color.Red, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Daily saving", color = Color.Gray, fontSize = 13.sp)
                            Text("₹${"%.2f".format(rec.dailySavings)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monthly saving", color = Color.Gray, fontSize = 13.sp)
                            Text("₹${"%.2f".format(rec.dailySavings * 30)}", color = Color(0xFF2E7D32), fontSize = 14.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Yearly saving", color = Color.Gray, fontSize = 13.sp)
                            Text("₹${"%.0f".format(rec.dailySavings * 365)}", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("Farmer Impact", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        val yearlySaving = rec.dailySavings * 365
                        val impactMsg = when {
                            yearlySaving >= 10000 -> "Excellent! This saving can fund a new calf purchase in 2 years."
                            yearlySaving >= 5000 -> "Great! Yearly saving covers school fees or farm equipment."
                            yearlySaving >= 2000 -> "Good start! Saving adds up to cover monthly household expenses."
                            else -> "Every rupee saved helps. Increase yield target to save more."
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                            Text(impactMsg, modifier = Modifier.padding(10.dp), fontSize = 13.sp, color = Color(0xFF1B5E20), lineHeight = 20.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedRecord = null }) {
                        Text("Close", color = Color(0xFF2E7D32))
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryCard(record: FeedRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F8E9),
            contentColor = Color(0xFF1B5E20)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(sdf.format(Date(record.date)), fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🐄 ", fontSize = 16.sp)
                Text(
                    if (record.cowName.isNotBlank()) record.cowName
                    else record.breed,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    fontSize = 15.sp
                )
                if (record.cowName.isNotBlank()) {
                    Text(
                        " (${record.breed})",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
            Text(
                "Yield: ${record.currentYield}L → ${record.targetYield}L  •  ₹${"%.2f".format(record.dailySavings)} Saved",
                fontSize = 13.sp,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
fun HistoricalSavingsChart(records: List<FeedRecord>) {
    val cumulativeSavings = records.sumOf { it.dailySavings }
    Column {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Total Money Saved So Far", color = Color.White, fontSize = 14.sp)
                Text(
                    "₹${String.format(Locale.getDefault(), "%.2f", cumulativeSavings)}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiAdvisorScreen(db: AppDatabase, userPhone: String) {
    val history by db.feedRecordDao().getHistory(userPhone).collectAsState(initial = emptyList())
    val lastRecord = history.firstOrNull()

    var report by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hasAnalyzed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var chatInput by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<Pair<String, String>>() }
    var isChatLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🤖", fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (LanguageManager.isKannada) "AI ಸಲಹೆಗಾರ" else "AI Farm Advisor",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    if (LanguageManager.isKannada) "ನಿಮ್ಮ ಹಸುವಿನ ವಿಶ್ಲೇಷಣೆ" else "Powered by Google Gemini",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (lastRecord == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (LanguageManager.isKannada)
                            "ಮೊದಲು ಕ್ಯಾಲ್ಕುಲೇಟರ್‌ನಲ್ಲಿ ಒಂದು ಲೆಕ್ಕ ಮಾಡಿ"
                        else
                            "No calculation found.\nPlease use the Calculator tab first,\nthen come back for AI analysis.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color(0xFF795548)
                    )
                }
            }
        } else {
            // Last calculation summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (LanguageManager.isKannada) "ಕೊನೆಯ ಲೆಕ್ಕ" else "Analyzing your last calculation",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    val cowLabel = if (lastRecord.cowName.isNotBlank())
                        "🐄 ${lastRecord.cowName} (${lastRecord.breed})"
                    else "🐄 ${lastRecord.breed}"
                    Text(cowLabel, fontSize = 14.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Text(
                        "Weight: ${lastRecord.weight.toInt()} kg  •  Age: ${if (lastRecord.age > 0) "${lastRecord.age.toInt()} yrs" else "N/A"}",
                        fontSize = 12.sp, color = Color.Gray
                    )
                    Text(
                        "Yield: ${lastRecord.currentYield}L → ${lastRecord.targetYield}L/day",
                        fontSize = 12.sp, color = Color(0xFF2E7D32)
                    )
                    Text(
                        "Daily saving: ₹${"%.2f".format(lastRecord.dailySavings)}",
                        fontSize = 12.sp, color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Analyze button
            Button(
                onClick = {
                    isLoading = true
                    hasAnalyzed = true
                    scope.launch {
                        report = GeminiAdvisor.getAdvice(
                            breed = lastRecord.breed,
                            age = lastRecord.age,
                            weight = lastRecord.weight,
                            targetYield = lastRecord.targetYield,
                            recipe = mapOf(
                                "Current Yield" to lastRecord.currentYield,
                                "Target Yield" to lastRecord.targetYield,
                                "Daily Saving" to lastRecord.dailySavings
                            ),
                            language = if (LanguageManager.isKannada) "Kannada" else "English"
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (LanguageManager.isKannada) "ವಿಶ್ಲೇಷಿಸಲಾಗುತ್ತಿದೆ..." else "Analyzing with Gemini AI...",
                        color = Color.White
                    )
                } else {
                    Text(
                        if (hasAnalyzed && report.isNotEmpty())
                            (if (LanguageManager.isKannada) "ಮತ್ತೆ ವಿಶ್ಲೇಷಿಸಿ" else "Re-Analyze")
                        else
                            (if (LanguageManager.isKannada) "AI ವಿಶ್ಲೇಷಣೆ ಪ್ರಾರಂಭಿಸಿ" else "▶  Analyse My Cow's Feed"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Report card — full height, no restriction
            if (hasAnalyzed) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0))
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()  // ← expands to show ALL content
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📋", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (LanguageManager.isKannada) "AI ವರದಿ" else "AI Analysis Report",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1),
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF1565C0))
                            }
                        } else {
                            // Show full report — no height limit
                            Text(
                                text = report,
                                fontSize = 14.sp,
                                color = Color(0xFF0D47A1),
                                lineHeight = 22.sp,
                                softWrap = true
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    if (LanguageManager.isKannada)
                        "* AI ಸಲಹೆಯು ಸಾಮಾನ್ಯ ಮಾಹಿತಿಗಾಗಿ ಮಾತ್ರ. ತಜ್ಞ ಪಶುವೈದ್ಯರನ್ನು ಸಂಪರ್ಕಿಸಿ."
                    else
                        "* AI advice is for general guidance only. Always consult a qualified veterinarian for medical decisions.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }

            // Chatbot Section
            Spacer(Modifier.height(20.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(Modifier.height(12.dp))

            Text("💬 Ask a Question", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
            Text("Ask anything about your cow's feed, health, or milk.", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            chatHistory.forEach { (role, message) ->
                val isUser = role == "user"
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 280.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Color(0xFF2E7D32) else Color(0xFFE3F2FD)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            message,
                            Modifier.padding(10.dp),
                            color = if (isUser) Color.White else Color(0xFF0D47A1),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            if (isChatLoading) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Text("🤔 Thinking...", Modifier.padding(10.dp), color = Color(0xFF0D47A1), fontSize = 13.sp)
                }
            }

            if (chatHistory.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                listOf(
                    "How to increase milk yield?",
                    "Best time to feed?",
                    "Signs of calcium deficiency?",
                    "How much water needed?"
                ).forEach { suggestion ->
                    OutlinedButton(
                        onClick = {
                            chatHistory.add("user" to suggestion)
                            isChatLoading = true
                            scope.launch {
                                val reply = GeminiAdvisor.chat(
                                    suggestion,
                                    lastRecord.breed,
                                    if (LanguageManager.isKannada) "Kannada" else "English"
                                )
                                chatHistory.add("ai" to reply)
                                isChatLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Text(suggestion, color = Color(0xFF2E7D32), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type your question...") },
                    maxLines = 2
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank() && !isChatLoading) {
                            val q = chatInput.trim()
                            chatInput = ""
                            chatHistory.add("user" to q)
                            isChatLoading = true
                            scope.launch {
                                val reply = GeminiAdvisor.chat(
                                    q,
                                    lastRecord.breed,
                                    if (LanguageManager.isKannada) "Kannada" else "English"
                                )
                                chatHistory.add("ai" to reply)
                                isChatLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .background(Color(0xFF2E7D32), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun VeterinaryTipsScreen() {
    val tips = listOf(
        VetTip("🐄",
            "Balanced Diet", "ಸಮತೋಲಿತ ಆಹಾರ",
            "Provide a proper mix of green fodder (10–15 kg) and dry fodder (4–6 kg) with concentrate daily to maximize milk yield. Never feed wilted or mouldy fodder.",
            "ಹಾಲಿನ ಇಳುವರಿ ಹೆಚ್ಚಿಸಲು ಹಸಿರು ಮೇವು (10–15 ಕೆಜಿ) ಮತ್ತು ಒಣ ಮೇವು (4–6 ಕೆಜಿ) ಮಿಶ್ರಣ ನೀಡಿ.",

        ),
        VetTip("💧",
            "Clean Water", "ಶುದ್ಧ ನೀರು",
            "Cows producing 10L of milk need 50–60L of clean water daily. Provide water 3–4 times a day, especially after milking and feeding.",
            "10 ಲೀ ಹಾಲು ಕೊಡುವ ಹಸುಗಳಿಗೆ ದಿನಕ್ಕೆ 50–60 ಲೀ ನೀರು ಬೇಕಾಗುತ್ತದೆ.",

        ),
        VetTip("🧹",
            "Shed Hygiene", "ಹಟ್ಟಿ ಸ್ವಚ್ಛತೆ",
            "Clean the shed twice daily. Disinfect with lime powder weekly. Poor hygiene causes massitis which reduces yield by up to 25%.",
            "ದಿನಕ್ಕೆ ಎರಡು ಬಾರಿ ಹಟ್ಟಿ ಸ್ವಚ್ಛಗೊಳಿಸಿ. ವಾರಕ್ಕೊಮ್ಮೆ ಸುಣ್ಣ ಹಾಕಿ.",

        ),
        VetTip("💉",
            "Vaccination Schedule", "ಲಸಿಕೆ ವೇಳಾಪಟ್ಟಿ",
            "FMD vaccine every 6 months. HS + BQ combined annually before monsoon. Brucellosis for heifers (4–8 months) once. Always record dates.",
            "FMD ಲಸಿಕೆ ಪ್ರತಿ 6 ತಿಂಗಳಿಗೊಮ್ಮೆ. HS + BQ ಮಳೆಗಾಲಕ್ಕೆ ಮುಂಚೆ ವಾರ್ಷಿಕ.",

        ),
        VetTip("🌾",
            "Fodder Storage", "ಮೇವು ಸಂಗ್ರಹಣೆ",
            "Store dry fodder in a raised, ventilated room to prevent fungal growth. Silage (fermented green fodder) lasts 6 months and retains 90% nutrition.",
            "ಒಣ ಮೇವನ್ನು ಗಾಳಿ ಇರುವ ಮೇಲ್ಮನೆಯಲ್ಲಿ ಸಂಗ್ರಹಿಸಿ. ಸೈಲೇಜ್ 6 ತಿಂಗಳು ಬಾಳಿಕೆ ಬರುತ್ತದೆ.",

        )
    )

    var playingUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(gs("tips"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(Modifier.height(16.dp))

        tips.forEach { tip ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF1B5E20)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tip.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(if (LanguageManager.isKannada) tip.titleKn else tip.titleEn, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (LanguageManager.isKannada) tip.descKn else tip.descEn, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF2E7D32))

                    if (tip.videoUrl != null) {
                        Spacer(Modifier.height(12.dp))
                        if (playingUrl == tip.videoUrl) {
                            VetVideoPlayer(url = tip.videoUrl)
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = { playingUrl = null }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (LanguageManager.isKannada) "ವಿಡಿಯೋ ಮುಚ್ಚಿ" else "Close Video")
                            }
                        } else {
                            Button(onClick = { playingUrl = tip.videoUrl }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (LanguageManager.isKannada) "ವಿಡಿಯೋ ನೋಡಿ" else "Watch Video")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VetVideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(url) { onDispose { exoPlayer.release() } }
    AndroidView(
        factory = { androidx.media3.ui.PlayerView(it).apply { player = exoPlayer; useController = true } },
        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun NutritionMatrixCard(matrix: NutritionMatrix, targetYield: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF0D47A1)),
        border = BorderStroke(1.dp, Color(0xFF1565C0))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Science, null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(gs("scientific_matrix"), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(if (LanguageManager.isKannada) "ಗುರಿ: ${targetYield.toInt()} ಲೀ/ದಿನ" else "Target: ${targetYield.toInt()} L/day", fontSize = 12.sp, color = Color(0xFF1565C0))
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFBBDEFB))
            Spacer(Modifier.height(8.dp))
            MatrixRow(label = if (LanguageManager.isKannada) "ಒಣ ಪದಾರ್ಥ (DM)" else "Dry Matter (DM)", value = "${"%.2f".format(matrix.dryMatter)} kg/day", hint = if (LanguageManager.isKannada) "ಒಟ್ಟು ಆಹಾರ ತೂಕ" else "Total feed weight needed")
            MatrixRow(label = if (LanguageManager.isKannada) "ಕಚ್ಚಾ ಪ್ರೋಟೀನ್ (CP)" else "Crude Protein (CP)", value = "${"%.0f".format(matrix.crudeProtein * 1000)} g/day", hint = if (LanguageManager.isKannada) "ಮಾಂಸಖಂಡ ಮತ್ತು ಹಾಲು" else "For muscle & milk production")
            MatrixRow(label = if (LanguageManager.isKannada) "ಒಟ್ಟು ಶಕ್ತಿ (TDN)" else "Total Energy (TDN)", value = "${"%.2f".format(matrix.tdn)} kg/day", hint = if (LanguageManager.isKannada) "ಶಕ್ತಿ ಅಗತ್ಯ" else "Energy requirement")
        }
    }
}

@Composable
fun MatrixRow(label: String, value: String, hint: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D47A1))
            Text(hint, fontSize = 11.sp, color = Color(0xFF546E7A))
        }
        Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
    }
    HorizontalDivider(color = Color(0xFFE3F2FD), thickness = 0.5.dp)
}

@Composable
fun IngredientPricesScreen(context: Context) {
    var prices by remember { mutableStateOf(IngredientPriceManager.getAllPrices(context)) }
    val ingredients = listOf("Maize", "Barley", "Millet", "Cottonseed Cake", "Soy Meal", "Mustard Cake", "Wheat Bran", "Rice Bran")
    val emojis = mapOf("Maize" to "🌽", "Barley" to "🌾", "Millet" to "🌾", "Cottonseed Cake" to "🌻", "Soy Meal" to "🫘", "Mustard Cake" to "🌾", "Wheat Bran" to "🌾", "Rice Bran" to "🍚")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(gs("prices"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.dp, Color(0xFF2E7D32))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "These prices are used in your cost comparison. Update them to match your local market rates for accurate savings.",
                    fontSize = 12.sp,
                    color = Color(0xFF1B5E20),
                    lineHeight = 18.sp
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(ingredients) { ingredient ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emojis[ingredient] ?: "🌾", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(gs(ingredient), modifier = Modifier.weight(1f), fontSize = 16.sp)
                    OutlinedTextField(
                        value = prices[ingredient]?.toString() ?: "",
                        onValueChange = { newValue ->
                            val dValue = newValue.toDoubleOrNull() ?: 0.0
                            prices = prices + (ingredient to dValue)
                        },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("₹/kg") },
                        singleLine = true
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                prices.forEach { (name, price) -> IngredientPriceManager.setPrice(name, price, context) }
                Toast.makeText(context, if(LanguageManager.isKannada) "ಬೆಲೆಗಳನ್ನು ನವೀಕರಿಸಲಾಗಿದೆ!" else "Prices updated!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(if(LanguageManager.isKannada) "ಬೆಲೆಗಳನ್ನು ಉಳಿಸಿ" else "Save Prices")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                IngredientPriceManager.resetToDefaults(context)
                prices = IngredientPriceManager.getAllPrices(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if(LanguageManager.isKannada) "ಪೂರ್ವನಿಯೋಜಿತಕ್ಕೆ ಮರುಹೊಂದಿಸಿ" else "Reset to Defaults")
        }
    }
}

private fun getFodderIcon(itemName: String): ImageVector {
    return when {
        itemName.contains("Maize", ignoreCase = true) || itemName.contains("Barley", ignoreCase = true) || itemName.contains("Millet", ignoreCase = true) -> Icons.Default.Grass
        itemName.contains("Cake", ignoreCase = true) || itemName.contains("Meal", ignoreCase = true) -> Icons.Default.EnergySavingsLeaf
        itemName.contains("Bran", ignoreCase = true) -> Icons.Default.Grain
        itemName.contains("Green Fodder", ignoreCase = true) -> Icons.Default.Forest
        itemName.contains("Dry Fodder", ignoreCase = true) -> Icons.Default.Agriculture
        itemName.contains("Mineral", ignoreCase = true) || itemName.contains("Salt", ignoreCase = true) -> Icons.Default.Science
        else -> Icons.Default.Grass
    }
}

data class VetTip(val emoji: String, val titleEn: String, val titleKn: String, val descEn: String, val descKn: String, val videoUrl: String? = null)