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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
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
    // MEDASSIST AI COLORS
    // =========================================================

    val backgroundTop =
        Color(0xFFF7FCFF)

    val backgroundBottom =
        Color(0xFFE2F4F9)

    val primaryBlue =
        Color(0xFF1976D2)

    val primaryBlueDark =
        Color(0xFF123A56)

    val teal =
        Color(0xFF009688)

    val textGray =
        Color(0xFF71818C)

    val borderColor =
        Color(0xFFDDECEF)


    // =========================================================
    // ANIMATION STATE
    // =========================================================

    var showContent by remember {
        mutableStateOf(false)
    }


    val logoScale by animateFloatAsState(

        targetValue =
            if (showContent) {
                1f
            } else {
                0.72f
            },

        animationSpec =
            tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            ),

        label = "Splash Logo Scale"
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
    // ROOT
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
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {


        // =====================================================
        // TOP RIGHT DECORATIVE CIRCLE
        // =====================================================

        Box(
            modifier = Modifier
                .size(235.dp)
                .align(Alignment.TopEnd)
                .background(
                    color =
                        primaryBlue.copy(
                            alpha = 0.065f
                        ),
                    shape = CircleShape
                )
        )


        // =====================================================
        // BOTTOM LEFT DECORATIVE CIRCLE
        // =====================================================

        Box(
            modifier = Modifier
                .size(175.dp)
                .align(Alignment.BottomStart)
                .background(
                    color =
                        teal.copy(
                            alpha = 0.055f
                        ),
                    shape = CircleShape
                )
        )


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(

            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            // =================================================
            // MEDASSIST LOGO
            // =================================================

            AnimatedVisibility(

                visible = showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(650)
                    ) +
                            scaleIn(
                                initialScale = 0.72f,

                                animationSpec =
                                    tween(
                                        durationMillis = 700,
                                        easing =
                                            FastOutSlowInEasing
                                    )
                            )
            ) {

                Box(

                    modifier = Modifier
                        .size(112.dp)
                        .scale(logoScale)
                        .shadow(
                            elevation = 16.dp,
                            shape =
                                RoundedCornerShape(30.dp)
                        )
                        .background(
                            Color.White,
                            RoundedCornerShape(30.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape =
                                RoundedCornerShape(30.dp)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {


                    // -----------------------------------------
                    // Brand Circle
                    // -----------------------------------------

                    Box(

                        modifier = Modifier
                            .size(70.dp)
                            .background(

                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryBlue,
                                        teal
                                    )
                                ),

                                CircleShape
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Favorite,

                            contentDescription =
                                "MEDASSIST AI",

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
                    Modifier.height(25.dp)
            )


            // =================================================
            // APP NAME
            // =================================================

            AnimatedVisibility(

                visible =
                    showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 650,
                                delayMillis = 150
                            )
                    )
            ) {

                Text(

                    text =
                        "MEDASSIST AI",

                    color =
                        primaryBlueDark,

                    fontSize =
                        31.sp,

                    fontWeight =
                        FontWeight.Bold,

                    letterSpacing =
                        0.3.sp,

                    textAlign =
                        TextAlign.Center
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // =================================================
            // TAGLINE
            // =================================================

            AnimatedVisibility(

                visible =
                    showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 650,
                                delayMillis = 250
                            )
                    )
            ) {

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


            Spacer(
                modifier =
                    Modifier.height(42.dp)
            )


            // =================================================
            // LOADING SECTION
            // =================================================

            AnimatedVisibility(

                visible =
                    showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 650,
                                delayMillis = 400
                            )
                    )
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    // -----------------------------------------
                    // Progress Bar
                    // -----------------------------------------

                    LinearProgressIndicator(

                        modifier =
                            Modifier.size(
                                width = 120.dp,
                                height = 4.dp
                            ),

                        color =
                            primaryBlue,

                        trackColor =
                            Color.White.copy(
                                alpha = 0.85f
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
                            11.sp,

                        textAlign =
                            TextAlign.Center
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(40.dp)
            )


            // =================================================
            // SECURITY BADGE
            // =================================================

            AnimatedVisibility(

                visible =
                    showContent,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 650,
                                delayMillis = 550
                            )
                    )
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    Box(

                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                Color.White.copy(
                                    alpha = 0.9f
                                ),
                                CircleShape
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Lock,

                            contentDescription =
                                "Secure",

                            tint =
                                primaryBlue,

                            modifier =
                                Modifier.size(16.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
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