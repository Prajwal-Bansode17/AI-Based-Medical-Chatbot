package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {

    // =========================================================
    // ANIMATION STATE
    // =========================================================

    var showContent by remember {
        mutableStateOf(false)
    }


    val logoScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.65f,

        animationSpec = tween(
            durationMillis = 750,
            easing = FastOutSlowInEasing
        ),

        label = "logoScale"
    )


    // =========================================================
    // SPLASH TIMER
    // =========================================================

    LaunchedEffect(Unit) {

        showContent = true

        delay(2500)

        onSplashFinished()
    }


    // =========================================================
    // LOGIN SCREEN THEME COLORS
    // =========================================================

    val backgroundTop =
        Color(0xFFEAF8FC)

    val backgroundBottom =
        Color(0xFFD8F0F6)

    val primaryBlue =
        Color(0xFF087EA4)

    val darkBlue =
        Color(0xFF123A56)

    val textGray =
        Color(0xFF71818C)


    // =========================================================
    // BACKGROUND
    // =========================================================

    Box(

        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundTop,
                        backgroundBottom
                    )
                )
            ),

        contentAlignment = Alignment.Center
    ) {


        // =====================================================
        // DECORATIVE TOP RIGHT CIRCLE
        // =====================================================

        Box(

            modifier = Modifier
                .size(230.dp)
                .align(Alignment.TopEnd)
                .background(
                    color = Color(0x22087EA4),
                    shape = CircleShape
                )
        )


        // =====================================================
        // DECORATIVE BOTTOM LEFT CIRCLE
        // =====================================================

        Box(

            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomStart)
                .background(
                    color = Color(0x18087EA4),
                    shape = CircleShape
                )
        )


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            // =================================================
            // LOGO ANIMATION
            // =================================================

            AnimatedVisibility(

                visible = showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(700)
                    ) +
                            scaleIn(
                                initialScale = 0.65f,

                                animationSpec =
                                    tween(
                                        durationMillis = 700,
                                        easing = FastOutSlowInEasing
                                    )
                            )
            ) {


                // =============================================
                // LOGO CARD
                // =============================================

                Box(

                    modifier = Modifier
                        .size(112.dp)
                        .scale(logoScale)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFDDECEF),
                            shape = RoundedCornerShape(32.dp)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {


                    // =========================================
                    // BLUE CIRCLE
                    // =========================================

                    Box(

                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                color = primaryBlue,
                                shape = CircleShape
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {


                        Icon(

                            imageVector =
                                Icons.Default.Favorite,

                            contentDescription =
                                "MedAssist AI",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(40.dp)
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =================================================
            // APP NAME
            // =================================================

            AnimatedVisibility(

                visible = showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 700,
                                delayMillis = 250
                            )
                    )
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(

                        text =
                            "MedAssist AI",

                        color =
                            darkBlue,

                        fontSize =
                            31.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )


                    Text(

                        text =
                            "Your intelligent health companion",

                        color =
                            textGray,

                        fontSize =
                            14.sp,

                        textAlign =
                            TextAlign.Center
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(42.dp)
            )


            // =================================================
            // LOADING BAR
            // =================================================

            AnimatedVisibility(

                visible = showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 700,
                                delayMillis = 500
                            )
                    )
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    LinearProgressIndicator(

                        modifier =
                            Modifier.size(
                                width = 110.dp,
                                height = 4.dp
                            ),

                        color =
                            primaryBlue,

                        trackColor =
                            Color.White.copy(
                                alpha = 0.8f
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )


                    Text(

                        text =
                            "Preparing your health assistant...",

                        color =
                            textGray,

                        fontSize =
                            11.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(38.dp)
            )


            // =================================================
            // SECURITY TEXT
            // =================================================

            AnimatedVisibility(

                visible = showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 700,
                                delayMillis = 650
                            )
                    )
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Lock,

                        contentDescription =
                            "Secure",

                        tint =
                            primaryBlue,

                        modifier =
                            Modifier.size(17.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )


                    Text(

                        text =
                            "Secure • Simple • Intelligent",

                        color =
                            textGray,

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