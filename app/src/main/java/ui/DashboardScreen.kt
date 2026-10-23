package ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.MedicineReminder
import com.example.ai_based_medical_chatbot.MedicineReminderRepository
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.AppBackground
import com.example.ai_based_medical_chatbot.ui.theme.AppSurface
import com.example.ai_based_medical_chatbot.ui.theme.BorderSubtle
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.StatusError
import com.example.ai_based_medical_chatbot.ui.theme.StatusSuccess
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextLight
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// HEALTHCARE VECTOR ICONS (Consistent 24x24 scale)
// ============================================================

val HomeVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "HomeVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 10f)
            lineTo(12f, 3f)
            lineTo(21f, 10f)
            lineTo(21f, 20f)
            curveTo(21f, 20.6f, 20.6f, 21f, 20f, 21f)
            lineTo(15f, 21f)
            lineTo(15f, 14f)
            lineTo(9f, 14f)
            lineTo(9f, 21f)
            lineTo(4f, 21f)
            curveTo(3.4f, 21f, 3f, 20.6f, 3f, 20f)
            close()
        }
    }.build()
}

val ScaleVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ScaleVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 21f)
            lineTo(18f, 21f)
            moveTo(12f, 21f)
            lineTo(12f, 5f)
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(4f, 7f)
            lineTo(2f, 13f)
            lineTo(8f, 13f)
            close()
            moveTo(20f, 7f)
            lineTo(16f, 13f)
            lineTo(22f, 13f)
            close()
            moveTo(12f, 4f)
            curveTo(12.55f, 4f, 13f, 3.55f, 13f, 3f)
            curveTo(13f, 2.45f, 12.55f, 2f, 12f, 2f)
            curveTo(11.45f, 2f, 11f, 2.45f, 11f, 3f)
            curveTo(11f, 3.55f, 11.45f, 4f, 12f, 4f)
            close()
        }
    }.build()
}

val StethoscopeVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StethoscopeVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DarkTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 3f)
            lineTo(6f, 8f)
            curveTo(6f, 11.31f, 8.69f, 14f, 12f, 14f)
            curveTo(15.31f, 14f, 18f, 11.31f, 18f, 8f)
            lineTo(18f, 3f)
            moveTo(12f, 14f)
            lineTo(12f, 17f)
            curveTo(12f, 19.21f, 13.79f, 21f, 16f, 21f)
            curveTo(18.21f, 21f, 20f, 19.21f, 20f, 17f)
            lineTo(20f, 16f)
            moveTo(19f, 16f)
            lineTo(21f, 16f)
        }
    }.build()
}

val PillVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PillVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4.5f, 10.5f)
            lineTo(10.5f, 4.5f)
            curveTo(12.5f, 2.5f, 15.5f, 2.5f, 17.5f, 4.5f)
            curveTo(19.5f, 6.5f, 19.5f, 9.5f, 17.5f, 11.5f)
            lineTo(11.5f, 17.5f)
            curveTo(9.5f, 19.5f, 6.5f, 19.5f, 4.5f, 17.5f)
            curveTo(2.5f, 15.5f, 2.5f, 12.5f, 4.5f, 10.5f)
            close()
            moveTo(8.5f, 6.5f)
            lineTo(15.5f, 13.5f)
        }
    }.build()
}

val HeartPulseVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "HeartPulseVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DarkTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19.5f, 12.5f)
            curveTo(21.5f, 10f, 21f, 6.5f, 18.5f, 4.5f)
            curveTo(16f, 2.5f, 13.5f, 3.5f, 12f, 5.5f)
            curveTo(10.5f, 3.5f, 8f, 2.5f, 5.5f, 4.5f)
            curveTo(3f, 6.5f, 2.5f, 10f, 4.5f, 12.5f)
            lineTo(12f, 20.5f)
            lineTo(19.5f, 12.5f)
            close()
            moveTo(3f, 12f)
            lineTo(7f, 12f)
            lineTo(9f, 9f)
            lineTo(12f, 15f)
            lineTo(14f, 11f)
            lineTo(16f, 13f)
            lineTo(21f, 13f)
        }
    }.build()
}

val DocumentScannerVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DocumentScannerVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 7f)
            lineTo(3f, 4f)
            lineTo(6f, 4f)
            moveTo(18f, 4f)
            lineTo(21f, 4f)
            lineTo(21f, 7f)
            moveTo(3f, 17f)
            lineTo(3f, 20f)
            lineTo(6f, 20f)
            moveTo(18f, 20f)
            lineTo(21f, 20f)
            lineTo(21f, 17f)
            moveTo(7f, 9f)
            lineTo(17f, 9f)
            moveTo(7f, 12f)
            lineTo(17f, 12f)
            moveTo(7f, 15f)
            lineTo(13f, 15f)
        }
    }.build()
}

val ClockVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ClockVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            close()
            moveTo(12f, 6f)
            lineTo(12f, 12f)
            lineTo(16f, 14f)
        }
    }.build()
}

val ChatVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChatVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21f, 11.5f)
            curveTo(21f, 16.19f, 16.97f, 20f, 12f, 20f)
            curveTo(10.42f, 20f, 8.93f, 19.62f, 7.64f, 18.96f)
            lineTo(3f, 20f)
            lineTo(4.28f, 15.86f)
            curveTo(3.48f, 14.58f, 3f, 13.09f, 3f, 11.5f)
            curveTo(3f, 6.81f, 7.03f, 3f, 12f, 3f)
            curveTo(16.97f, 3f, 21f, 6.81f, 21f, 11.5f)
            close()
            moveTo(8f, 11.5f)
            lineTo(8.01f, 11.5f)
            moveTo(12f, 11.5f)
            lineTo(12.01f, 11.5f)
            moveTo(16f, 11.5f)
            lineTo(16.01f, 11.5f)
        }
    }.build()
}

val UserCardVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "UserCardVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(DeepTeal),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 4f)
            lineTo(20f, 4f)
            curveTo(21.1f, 4f, 22f, 4.9f, 22f, 6f)
            lineTo(22f, 18f)
            curveTo(22f, 19.1f, 21.1f, 20f, 20f, 20f)
            lineTo(4f, 20f)
            curveTo(2.9f, 20f, 2f, 19.1f, 2f, 18f)
            lineTo(2f, 6f)
            curveTo(2f, 4.9f, 2.9f, 4f, 4f, 4f)
            close()
            moveTo(12f, 11f)
            curveTo(13.1f, 11f, 14f, 10.1f, 14f, 9f)
            curveTo(14f, 7.9f, 13.1f, 7f, 12f, 7f)
            curveTo(10.9f, 7f, 10f, 7.9f, 10f, 9f)
            curveTo(10f, 10.1f, 10.9f, 11f, 12f, 11f)
            close()
            moveTo(7f, 16f)
            curveTo(7f, 14.34f, 9.24f, 13f, 12f, 13f)
            curveTo(14.76f, 13f, 17f, 14.34f, 17f, 16f)
        }
    }.build()
}

// ============================================================
// REDESIGNED PREMIUM DASHBOARD SCREEN
// ============================================================

@Composable
fun DashboardScreen(
    userName: String,
    onProfileClick: () -> Unit = {},
    onChatbotClick: () -> Unit = {},
    onSymptomSubmit: (String) -> Unit = {},
    onBMIClick: () -> Unit = {},
    onSymptomsClick: () -> Unit = {},
    onMedicineClick: () -> Unit = {},
    onHealthTipsClick: () -> Unit = {},
    onPrescriptionClick: () -> Unit = {},
    onMedicineReminderClick: () -> Unit = {},
    onHealthProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedLanguage = LocalAppLanguageController.current.selectedLanguage
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun t(key: String): String = appText(key, selectedLanguage)

    // User & Preferences state
    var userGender by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }

    // Live saved data states (refreshed automatically on screen resume)
    var savedReminders by remember { mutableStateOf<List<MedicineReminder>>(emptyList()) }
    var savedPrescriptions by remember { mutableStateOf<JSONArray>(JSONArray()) }
    var savedBmi by remember { mutableFloatStateOf(0f) }
    var savedBmiStatus by remember { mutableStateOf("") }

    // AI Symptom Input Card State
    var symptomText by remember { mutableStateOf("") }
    var symptomError by remember { mutableStateOf("") }
    var isSubmittingSymptom by remember { mutableStateOf(false) }

    // Quick Actions grid expansion (Show all vs compact)
    var showAllQuickActions by remember { mutableStateOf(false) }

    // Profile menu popup state
    var showProfileMenu by remember { mutableStateOf(false) }

    fun refreshDashboardData() {
        val currentBmiData = BMIRepository.getBMIData(context)
        userGender = currentBmiData.gender
        savedBmi = currentBmiData.bmi
        savedBmiStatus = if (currentBmiData.bmi > 0f) currentBmiData.bmiStatus else ""

        savedReminders = MedicineReminderRepository.getAll(context)
        savedPrescriptions = PrescriptionRepository.getSavedPrescriptions(context)
    }

    LaunchedEffect(Unit) {
        showContent = true
        refreshDashboardData()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshDashboardData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val displayName = userName.trim().ifBlank { "User" }

    // Formatted current dynamic date (e.g., "Monday, 18 Oct")
    val currentDateFormatted = remember {
        val formatter = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
        formatter.format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Soft ambient decorative background gradients
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .background(Mint.copy(alpha = 0.22f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .background(SoftMint.copy(alpha = 0.35f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // =========================================================
                // A. PREMIUM HEADER SECTION
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(300)) + slideInVertically(
                            initialOffsetY = { -15 },
                            animationSpec = tween(300)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hello, $displayName",
                                    color = TextDark,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(SageGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentDateFormatted,
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Profile action avatar with aligned dropdown options
                            Box {
                                Surface(
                                    onClick = { showProfileMenu = true },
                                    shape = CircleShape,
                                    color = PureWhite,
                                    border = BorderStroke(1.5.dp, Mint),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        DashboardGenderAvatar(gender = userGender)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showProfileMenu,
                                    onDismissRequest = { showProfileMenu = false },
                                    modifier = Modifier
                                        .background(PureWhite)
                                        .border(1.dp, Mint.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(t("profile"), color = TextDark, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = DeepTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showProfileMenu = false
                                            onProfileClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(t("dashboard_health_profile"), color = TextDark) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = UserCardVectorIcon,
                                                contentDescription = null,
                                                tint = DeepTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showProfileMenu = false
                                            onHealthProfileClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(t("sign_out"), color = StatusError, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = null,
                                                tint = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showProfileMenu = false
                                            onLogoutClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // B. AI SYMPTOM INPUT CARD (Hero Triage Action)
                // =========================================================
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(350, delayMillis = 50)) + slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = tween(350, delayMillis = 50)
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 3.dp,
                                    shape = RoundedCornerShape(22.dp),
                                    ambientColor = DarkTeal.copy(alpha = 0.06f),
                                    spotColor = DeepTeal.copy(alpha = 0.10f)
                                ),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, Mint.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(SoftMint, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = StethoscopeVectorIcon,
                                            contentDescription = null,
                                            tint = DeepTeal,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = t("dashboard_bothering_title"),
                                            color = TextDark,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.2).sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Describe your symptoms for instant AI medical triage",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Multiline symptom input container
                                OutlinedTextField(
                                    value = symptomText,
                                    onValueChange = {
                                        symptomText = it
                                        if (symptomError.isNotBlank()) symptomError = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp),
                                    placeholder = {
                                        Text(
                                            text = t("dashboard_symptom_placeholder"),
                                            color = TextLight,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = TextDark,
                                        fontSize = 14.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepTeal,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedContainerColor = Ivory.copy(alpha = 0.4f),
                                        unfocusedContainerColor = Ivory.copy(alpha = 0.35f),
                                        cursorColor = DeepTeal
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            val query = symptomText.trim()
                                            if (query.isBlank()) {
                                                symptomError = t("dashboard_symptom_empty_error")
                                            } else {
                                                keyboardController?.hide()
                                                isSubmittingSymptom = true
                                                onSymptomSubmit(query)
                                            }
                                        }
                                    )
                                )

                                if (symptomError.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = symptomError,
                                        color = StatusError,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bottom action row inside symptom card
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftMint.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(SageGreen, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Verified Medical AI",
                                                color = DeepTeal,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val query = symptomText.trim()
                                            if (query.isBlank()) {
                                                symptomError = t("dashboard_symptom_empty_error")
                                            } else {
                                                keyboardController?.hide()
                                                isSubmittingSymptom = true
                                                onSymptomSubmit(query)
                                            }
                                        },
                                        enabled = !isSubmittingSymptom,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DeepTeal,
                                            contentColor = PureWhite
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        if (isSubmittingSymptom) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = PureWhite,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = t("dashboard_submit_symptom"),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // C. QUICK ACTIONS SECTION (2-Column Grid matching reference)
                // =========================================================
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400, delayMillis = 100))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = t("quick_actions"),
                                    color = TextDark,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = t("choose_need"),
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            TextButton(
                                onClick = { showAllQuickActions = !showAllQuickActions }
                            ) {
                                Text(
                                    text = if (showAllQuickActions) t("dashboard_view_less") else t("dashboard_view_all"),
                                    color = DeepTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Row 1: Symptoms Checker & BMI Checker
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumQuickActionCard(
                            title = t("dashboard_symptom_checker"),
                            subtitle = t("dashboard_symptom_checker_sub"),
                            icon = StethoscopeVectorIcon,
                            iconContainerColor = SoftMint,
                            iconTint = DarkTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onSymptomsClick
                        )

                        PremiumQuickActionCard(
                            title = t("dashboard_bmi_checker"),
                            subtitle = t("dashboard_bmi_checker_sub"),
                            icon = ScaleVectorIcon,
                            iconContainerColor = Mint,
                            iconTint = DeepTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onBMIClick
                        )
                    }
                }

                // Row 2: Medicine Information & Medicine Reminder
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumQuickActionCard(
                            title = t("dashboard_medicine_info"),
                            subtitle = t("dashboard_medicine_info_sub"),
                            icon = PillVectorIcon,
                            iconContainerColor = Mint.copy(alpha = 0.7f),
                            iconTint = DeepTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onMedicineClick
                        )

                        PremiumQuickActionCard(
                            title = t("dashboard_medicine_reminder"),
                            subtitle = t("dashboard_medicine_reminder_sub"),
                            icon = ClockVectorIcon,
                            iconContainerColor = SoftMint,
                            iconTint = DarkTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onMedicineReminderClick
                        )
                    }
                }

                // Row 3: Prescription Scanner & Health Tips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumQuickActionCard(
                            title = t("dashboard_prescription_scanner"),
                            subtitle = t("dashboard_prescription_scanner_sub"),
                            icon = DocumentScannerVectorIcon,
                            iconContainerColor = SoftMint,
                            iconTint = DeepTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onPrescriptionClick
                        )

                        PremiumQuickActionCard(
                            title = t("dashboard_health_tips"),
                            subtitle = t("dashboard_health_tips_sub"),
                            icon = HeartPulseVectorIcon,
                            iconContainerColor = Mint,
                            iconTint = DarkTeal,
                            modifier = Modifier.weight(1f),
                            onClick = onHealthTipsClick
                        )
                    }
                }

                // Row 4 (Expandable via "View all"): Health Profile & Chat with MedAssist AI
                if (showAllQuickActions) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumQuickActionCard(
                                title = t("dashboard_health_profile"),
                                subtitle = t("dashboard_health_profile_sub"),
                                icon = UserCardVectorIcon,
                                iconContainerColor = SoftMint,
                                iconTint = DarkTeal,
                                modifier = Modifier.weight(1f),
                                onClick = onHealthProfileClick
                            )

                            PremiumQuickActionCard(
                                title = t("dashboard_chat_ai"),
                                subtitle = t("dashboard_chat_ai_sub"),
                                icon = ChatVectorIcon,
                                iconContainerColor = Mint,
                                iconTint = DeepTeal,
                                modifier = Modifier.weight(1f),
                                onClick = onChatbotClick
                            )
                        }
                    }
                }

                // =========================================================
                // D. TODAY'S HEALTH OVERVIEW (Real Saved Data Only)
                // =========================================================
                item {
                    val activeReminders = savedReminders.filter { it.enabled }
                    val nextReminder = activeReminders.firstOrNull() ?: savedReminders.firstOrNull()
                    val hasBmi = savedBmi > 0f
                    val hasPrescriptions = savedPrescriptions.length() > 0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("dashboard_todays_overview"),
                                color = TextDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Medicine Reminder Real Status Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMedicineReminderClick() },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Mint)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(SoftMint, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ClockVectorIcon,
                                            contentDescription = null,
                                            tint = DeepTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t("dashboard_medicine_reminder"),
                                            color = TextDark,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (nextReminder != null) {
                                                "${t("dashboard_next_dose")}: ${nextReminder.medicineName}${if (nextReminder.strength.isNotBlank()) " (${nextReminder.strength})" else ""}"
                                            } else {
                                                t("dashboard_no_upcoming_reminders")
                                            },
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftMint
                                    ) {
                                        Text(
                                            text = if (nextReminder != null) nextReminder.time else "Schedule",
                                            color = DeepTeal,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact 2-column data row for BMI & Prescriptions (when available)
                        if (hasBmi || hasPrescriptions) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (hasBmi) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onBMIClick() },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                                        border = BorderStroke(1.dp, Mint),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = t("dashboard_latest_bmi"),
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                    imageVector = ScaleVectorIcon,
                                                    contentDescription = null,
                                                    tint = DeepTeal,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.1f", savedBmi),
                                                color = DeepTeal,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = savedBmiStatus.ifBlank { "Recorded" },
                                                color = TextDark,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (hasPrescriptions) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onPrescriptionClick() },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                                        border = BorderStroke(1.dp, Mint),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Prescriptions",
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                    imageVector = DocumentScannerVectorIcon,
                                                    contentDescription = null,
                                                    tint = DeepTeal,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${savedPrescriptions.length()} Saved",
                                                color = DarkTeal,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Optical records",
                                                color = TextDark,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // MEDICAL DISCLAIMER & FOOTER
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(SoftMint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Health disclaimer",
                                    tint = DeepTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("health_information"),
                                    color = DeepTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = t("health_disclaimer"),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // =========================================================
            // E. BOTTOM NAVIGATION (Rounded, Destination-Aware Bar)
            // =========================================================
            DashboardBottomNavigation(
                currentDestination = "home",
                onHomeClick = { /* Already on home */ },
                onChatbotClick = onChatbotClick,
                onRemindersClick = onMedicineReminderClick,
                onProfileClick = onProfileClick,
                t = ::t
            )
        }
    }
}

// =============================================================
// E. ROUNDED BOTTOM NAVIGATION BAR COMPONENT
// =============================================================

@Composable
private fun DashboardBottomNavigation(
    currentDestination: String,
    onHomeClick: () -> Unit,
    onChatbotClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onProfileClick: () -> Unit,
    t: (String) -> String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = PureWhite,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = HomeVectorIcon,
                label = t("dashboard_nav_home"),
                isSelected = currentDestination == "home",
                onClick = onHomeClick,
                modifier = Modifier.weight(1f)
            )

            BottomNavItem(
                icon = ChatVectorIcon,
                label = t("dashboard_nav_chat"),
                isSelected = currentDestination == "chat",
                onClick = onChatbotClick,
                modifier = Modifier.weight(1f)
            )

            BottomNavItem(
                icon = ClockVectorIcon,
                label = t("dashboard_nav_reminders"),
                isSelected = currentDestination == "reminders",
                onClick = onRemindersClick,
                modifier = Modifier.weight(1f)
            )

            BottomNavItem(
                icon = UserCardVectorIcon,
                label = t("dashboard_nav_profile"),
                isSelected = currentDestination == "profile",
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedIconTint by animateColorAsState(
        targetValue = if (isSelected) DeepTeal else TextMuted,
        animationSpec = tween(180),
        label = "nav_tint"
    )
    val animatedIndicatorBg by animateColorAsState(
        targetValue = if (isSelected) SoftMint else Color.Transparent,
        animationSpec = tween(180),
        label = "nav_indicator"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill indicator around icon when selected
        Box(
            modifier = Modifier
                .height(28.dp)
                .widthIn(min = 46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(animatedIndicatorBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedIconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = if (isSelected) DeepTeal else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================
// C. REUSABLE PREMIUM 2-COLUMN ACTION CARD (Matching Reference 2)
// =============================================================

@Composable
private fun PremiumQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(116.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Mint.copy(alpha = 0.4f)),
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Vector icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconContainerColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Small arrow indicator in top-right
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(SoftMint.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextDark,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =============================================================
// GENDER AVATAR
// =============================================================

@Composable
private fun DashboardGenderAvatar(gender: String) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Profile",
        tint = DeepTeal,
        modifier = Modifier.size(22.dp)
    )
}
