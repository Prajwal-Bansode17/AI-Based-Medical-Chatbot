package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun DashboardScreen(
    userName: String,
    onProfileClick: () -> Unit = {},
    onChatbotClick: () -> Unit = {},
    onSymptomsClick: () -> Unit = {},
    onMedicineClick: () -> Unit = {},
    onHealthTipsClick: () -> Unit = {}
) {

    // =========================================================
    // ANIMATION
    // =========================================================

    var showContent by remember {
        mutableStateOf(false)
    }


    LaunchedEffect(Unit) {

        showContent = true
    }


    // =========================================================
    // LOGIN SCREEN COLORS
    // =========================================================

    val backgroundTop =
        Color(0xFF080D18)

    val backgroundBottom =
        Color(0xFF0D1422)

    val primaryBlue =
        Color(0xFF4F7CFF)

    val darkBlue =
        Color(0xFFF8FAFC)

    val textDark =
        Color(0xFFF8FAFC)

    val textGray =
        Color(0xFF94A3B8)

    val lightBlue =
        Color(0xFF16233A)


    // =========================================================
    // MAIN BACKGROUND
    // =========================================================

    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                backgroundTop,
                                backgroundBottom
                            )
                    )
                )
    ) {


        // =====================================================
        // DECORATIVE CIRCLE - TOP RIGHT
        // =====================================================

        Box(

            modifier =
                Modifier
                    .size(190.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        color =
                            Color(0x224F7CFF),

                        shape =
                            CircleShape
                    )
        )


        // =====================================================
        // DECORATIVE CIRCLE - BOTTOM LEFT
        // =====================================================

        Box(

            modifier =
                Modifier
                    .size(140.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        color =
                            Color(0x184F7CFF),

                        shape =
                            CircleShape
                    )
        )


        // =====================================================
        // SCROLLABLE DASHBOARD
        // =====================================================

        LazyColumn(

            modifier =
                Modifier.fillMaxSize(),

            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 35.dp,
                    bottom = 25.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(0.dp)
        ) {


            // =================================================
            // HEADER
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(600)
                        ) +
                                slideInVertically(
                                    initialOffsetY = {
                                        -70
                                    },

                                    animationSpec =
                                        tween(600)
                                )
                ) {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        // =====================================
                        // GREETING
                        // =====================================

                        Column(

                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(

                                text =
                                    "Good day 👋",

                                color =
                                    textGray,

                                fontSize =
                                    13.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )


                            Text(

                                text =
                                    if (
                                        userName.isNotBlank()
                                    ) {
                                        "Hello, $userName"
                                    } else {
                                        "Hello there"
                                    },

                                color =
                                    darkBlue,

                                fontSize =
                                    26.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )


                            Text(

                                text =
                                    "How can I help you today?",

                                color =
                                    textGray,

                                fontSize =
                                    13.sp
                            )
                        }


                        // =====================================
                        // PROFILE BUTTON
                        // =====================================

                        Box(

                            modifier =
                                Modifier
                                    .size(54.dp)
                                    .shadow(
                                        elevation = 7.dp,
                                        shape = CircleShape
                                    )
                                    .background(
                                        color =
                                            Color(0xFF111A2A),

                                        shape =
                                            CircleShape
                                    )
                                    .clickable {
                                        onProfileClick()
                                    },

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Person,

                                contentDescription =
                                    "Profile",

                                tint =
                                    primaryBlue,

                                modifier =
                                    Modifier.size(29.dp)
                            )
                        }
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )
            }


            // =================================================
            // HEALTH STATUS CARD
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 650,
                                    delayMillis = 100
                                )
                        ) +
                                slideInVertically(
                                    initialOffsetY = {
                                        60
                                    },

                                    animationSpec =
                                        tween(
                                            durationMillis = 650,
                                            delayMillis = 100
                                        )
                                )
                ) {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(24.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFF111A2A)
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation =
                                    7.dp
                            )
                    ) {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(19.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {


                            // =================================
                            // HEART ICON
                            // =================================

                            Box(

                                modifier =
                                    Modifier
                                        .size(58.dp)
                                        .background(
                                            color =
                                                lightBlue,

                                            shape =
                                                RoundedCornerShape(
                                                    17.dp
                                                )
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.Favorite,

                                    contentDescription =
                                        "Health",

                                    tint =
                                        primaryBlue,

                                    modifier =
                                        Modifier.size(31.dp)
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.width(15.dp)
                            )


                            Column(

                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(

                                    text =
                                        "Your Health Assistant",

                                    color =
                                        textDark,

                                    fontSize =
                                        17.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )


                                Text(

                                    text =
                                        "Get quick health information and guidance.",

                                    color =
                                        textGray,

                                    fontSize =
                                        12.sp,

                                    lineHeight =
                                        17.sp
                                )
                            }
                        }
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )
            }


            // =================================================
            // AI ASSISTANT CARD
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 650,
                                    delayMillis = 180
                                )
                        ) +
                                slideInVertically(
                                    initialOffsetY = {
                                        70
                                    },

                                    animationSpec =
                                        tween(
                                            durationMillis = 650,
                                            delayMillis = 180
                                        )
                                )
                ) {

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChatbotClick()
                                },

                        shape =
                            RoundedCornerShape(25.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    primaryBlue
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation =
                                    10.dp
                            )
                    ) {

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {


                            // =================================
                            // AI ICON
                            // =================================

                            Box(

                                modifier =
                                    Modifier
                                        .size(64.dp)
                                        .background(
                                            color =
                                                Color.White,

                                            shape =
                                                RoundedCornerShape(
                                                    19.dp
                                                )
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.Face,

                                    contentDescription =
                                        "AI Medical Assistant",

                                    tint =
                                        primaryBlue,

                                    modifier =
                                        Modifier.size(36.dp)
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.width(16.dp)
                            )


                            Column(

                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(

                                    text =
                                        "AI Medical Assistant",

                                    color =
                                        Color.White,

                                    fontSize =
                                        19.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(5.dp)
                                )


                                Text(

                                    text =
                                        "Ask health-related questions",

                                    color =
                                        Color.White.copy(
                                            alpha = 0.82f
                                        ),

                                    fontSize =
                                        13.sp
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(9.dp)
                                )


                                Text(

                                    text =
                                        "Start conversation  →",

                                    color =
                                        Color.White,

                                    fontSize =
                                        12.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(27.dp)
                )
            }


            // =================================================
            // QUICK SERVICES HEADER
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 600,
                                    delayMillis = 250
                                )
                        )
                ) {

                    Column {

                        Text(

                            text =
                                "Quick Services",

                            color =
                                darkBlue,

                            fontSize =
                                21.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )


                        Text(

                            text =
                                "Everything you need in one place",

                            color =
                                textGray,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )
            }


            // =================================================
            // SERVICE ROW 1
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 600,
                                    delayMillis = 320
                                )
                        ) +
                                slideInVertically(
                                    initialOffsetY = {
                                        60
                                    },

                                    animationSpec =
                                        tween(
                                            durationMillis = 600,
                                            delayMillis = 320
                                        )
                                )
                ) {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {


                        DashboardServiceCard(

                            title =
                                "Symptoms",

                            subtitle =
                                "Check symptoms",

                            icon = {

                                Icon(

                                    imageVector =
                                        Icons.Default.Favorite,

                                    contentDescription =
                                        "Symptoms Checker",

                                    tint =
                                        primaryBlue,

                                    modifier =
                                        Modifier.size(29.dp)
                                )
                            },

                            modifier =
                                Modifier.weight(1f),

                            onClick =
                                onSymptomsClick
                        )


                        DashboardServiceCard(

                            title =
                                "Medicine",

                            subtitle =
                                "Medicine info",

                            icon = {

                                Icon(

                                    imageVector =
                                        Icons.Default.Info,

                                    contentDescription =
                                        "Medicine Information",

                                    tint =
                                        primaryBlue,

                                    modifier =
                                        Modifier.size(29.dp)
                                )
                            },

                            modifier =
                                Modifier.weight(1f),

                            onClick =
                                onMedicineClick
                        )
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )
            }


            // =================================================
            // SERVICE ROW 2
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 600,
                                    delayMillis = 400
                                )
                        ) +
                                slideInVertically(
                                    initialOffsetY = {
                                        60
                                    },

                                    animationSpec =
                                        tween(
                                            durationMillis = 600,
                                            delayMillis = 400
                                        )
                                )
                ) {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {


                        DashboardServiceCard(

                            title =
                                "Health Tips",

                            subtitle =
                                "Healthy lifestyle",

                            icon = {

                                Icon(

                                    imageVector =
                                        Icons.Default.Favorite,

                                    contentDescription =
                                        "Health Tips",

                                    tint =
                                        primaryBlue,

                                    modifier =
                                        Modifier.size(29.dp)
                                )
                            },

                            modifier =
                                Modifier.weight(1f),

                            onClick =
                                onHealthTipsClick
                        )


                        DashboardServiceCard(

                            title =
                                "AI Chat",

                            subtitle =
                                "Ask AI",

                            icon = {

                                Icon(

                                    imageVector =
                                        Icons.Default.Face,

                                    contentDescription =
                                        "AI Chat",

                                    tint =
                                        primaryBlue,

                                    modifier =
                                        Modifier.size(29.dp)
                                )
                            },

                            modifier =
                                Modifier.weight(1f),

                            onClick =
                                onChatbotClick
                        )
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(25.dp)
                )
            }


            // =================================================
            // DISCLAIMER CARD
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 700,
                                    delayMillis = 500
                                )
                        )
                ) {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFF111A2A).copy(
                                        alpha = 0.88f
                                    )
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                        ) {

                            Text(

                                text =
                                    "⚕ Health information",

                                color =
                                    primaryBlue,

                                fontSize =
                                    13.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )


                            Text(

                                text =
                                    "MedAssist AI provides general health information and is not a replacement for professional medical advice.",

                                color =
                                    textGray,

                                fontSize =
                                    11.sp,

                                lineHeight =
                                    16.sp
                            )
                        }
                    }
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )
            }


            // =================================================
            // FOOTER
            // =================================================

            item {

                AnimatedVisibility(

                    visible =
                        showContent,

                    enter =
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 700,
                                    delayMillis = 650
                                )
                        )
                ) {

                    Text(

                        text =
                            "MedAssist AI  •  Your intelligent health companion",

                        modifier =
                            Modifier.fillMaxWidth(),

                        color =
                            Color(0xFF64748B),

                        fontSize =
                            10.sp,

                        textAlign =
                            TextAlign.Center
                    )
                }
            }
        }
    }
}


// =============================================================
// SERVICE CARD
// =============================================================

@Composable
private fun DashboardServiceCard(

    title: String,

    subtitle: String,

    icon: @Composable () -> Unit,

    modifier: Modifier = Modifier,

    onClick: () -> Unit = {}
) {

    Card(

        modifier =
            modifier
                .height(148.dp)
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(21.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    6.dp
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(15.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            // =================================================
            // ICON CONTAINER
            // =================================================

            Box(

                modifier =
                    Modifier
                        .size(54.dp)
                        .background(
                            color =
                                Color(0xFF16233A),

                            shape =
                                RoundedCornerShape(17.dp)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                icon()
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            // =================================================
            // TITLE
            // =================================================

            Text(

                text =
                    title,

                color =
                    Color(0xFFF8FAFC),

                fontSize =
                    15.sp,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )


            // =================================================
            // SUBTITLE
            // =================================================

            Text(

                text =
                    subtitle,

                color =
                    Color(0xFF94A3B8),

                fontSize =
                    11.sp,

                textAlign =
                    TextAlign.Center
            )
        }
    }
}