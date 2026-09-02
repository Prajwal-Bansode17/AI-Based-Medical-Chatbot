package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {

    // =========================================================
    // MEDASSIST COLORS
    // =========================================================

    val backgroundTop = Color(0xFFF7FCFF)
    val backgroundBottom = Color(0xFFE5F5FA)

    val primaryBlue = Color(0xFF1976D2)
    val primaryDark = Color(0xFF123A56)
    val teal = Color(0xFF009688)

    val textPrimary = Color(0xFF172B4D)
    val textSecondary = Color(0xFF71818C)

    val surface = Color.White
    val surfaceVariant = Color(0xFFEAF6FA)

    val borderColor = Color(0xFFDCE8EE)

    val logoutBackground = Color(0xFFFFF3F2)
    val logoutBorder = Color(0xFFFFD9D5)
    val logoutText = Color(0xFFD32F2F)


    // =========================================================
    // PASSWORD UI STATE
    // =========================================================

    var passwordVisible by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // USER VALUES
    // =========================================================

    val displayName =
        userName.trim().ifBlank {
            "MedAssist User"
        }

    val displayEmail =
        userEmail.trim().ifBlank {
            "Email not available"
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
    ) {

        // =====================================================
        // TOP RIGHT DECORATION
        // =====================================================

        Box(
            modifier = Modifier
                .size(210.dp)
                .align(Alignment.TopEnd)
                .background(
                    primaryBlue.copy(alpha = 0.06f),
                    CircleShape
                )
        )


        // =====================================================
        // BOTTOM LEFT DECORATION
        // =====================================================

        Box(
            modifier = Modifier
                .size(155.dp)
                .align(Alignment.BottomStart)
                .background(
                    teal.copy(alpha = 0.055f),
                    CircleShape
                )
        )


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
        ) {


            // =================================================
            // HEADER
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBackClick
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription =
                            "Back",

                        tint =
                            primaryDark,

                        modifier =
                            Modifier.size(25.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    text = "My Profile",

                    color =
                        primaryDark,

                    fontSize =
                        22.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )


            // =================================================
            // PROFILE HEADER CARD
            // =================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(27.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            5.dp
                    )
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    // -----------------------------------------
                    // AVATAR
                    // -----------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .size(92.dp)
                                .shadow(
                                    elevation = 9.dp,
                                    shape = CircleShape
                                )
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
                                Icons.Default.Person,

                            contentDescription =
                                "Profile",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(48.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(13.dp)
                    )


                    // -----------------------------------------
                    // NAME
                    // -----------------------------------------

                    Text(

                        text =
                            displayName,

                        color =
                            textPrimary,

                        fontSize =
                            22.sp,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )


                    // -----------------------------------------
                    // EMAIL
                    // -----------------------------------------

                    Text(

                        text =
                            displayEmail,

                        color =
                            textSecondary,

                        fontSize =
                            12.sp,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    Spacer(
                        modifier =
                            Modifier.height(13.dp)
                    )


                    // -----------------------------------------
                    // SECURE BADGE
                    // -----------------------------------------

                    Row(

                        modifier =
                            Modifier
                                .background(
                                    surfaceVariant,
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 7.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Lock,

                            contentDescription =
                                "Secure",

                            tint =
                                primaryBlue,

                            modifier =
                                Modifier.size(14.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(

                            text =
                                "Secure account",

                            color =
                                primaryBlue,

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )


            // =================================================
            // ACCOUNT SECTION
            // =================================================

            Text(

                text =
                    "Account",

                color =
                    textPrimary,

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(22.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            3.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {


                    // -----------------------------------------
                    // PROFILE
                    // -----------------------------------------

                    ProfileInfoRow(

                        icon =
                            Icons.Default.Person,

                        title =
                            "Profile",

                        subtitle =
                            "Your MEDASSIST account",

                        iconColor =
                            primaryBlue
                    )


                    ProfileDivider()


                    // -----------------------------------------
                    // EMAIL
                    // -----------------------------------------

                    ProfileInfoRow(

                        icon =
                            Icons.Default.Email,

                        title =
                            "Email",

                        subtitle =
                            displayEmail,

                        iconColor =
                            teal
                    )


                    ProfileDivider()


                    // -----------------------------------------
                    // PASSWORD
                    // -----------------------------------------

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 8.dp,
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        // -------------------------------------
                        // PASSWORD ICON
                        // -------------------------------------

                        Box(

                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .background(
                                        Color(0xFFEAF6FA),
                                        RoundedCornerShape(13.dp)
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Lock,

                                contentDescription =
                                    "Password",

                                tint =
                                    primaryBlue,

                                modifier =
                                    Modifier.size(21.dp)
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )


                        // -------------------------------------
                        // PASSWORD TEXT
                        // -------------------------------------

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(

                                text =
                                    "Password",

                                color =
                                    textPrimary,

                                fontSize =
                                    13.sp,

                                fontWeight =
                                    FontWeight.SemiBold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )


                            Text(

                                text =
                                    if (passwordVisible) {
                                        "Password protected"
                                    } else {
                                        "••••••••"
                                    },

                                color =
                                    textSecondary,

                                fontSize =
                                    13.sp,

                                fontWeight =
                                    if (passwordVisible) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    }
                            )
                        }


                        // -------------------------------------
                        // HIDE / UNHIDE BUTTON
                        // -------------------------------------

                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            }
                        ) {
                            Text(
                                text = if (passwordVisible) "◉" else "◌",
                                color = primaryBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            // =================================================
            // MEDASSIST AI SECTION
            // =================================================

            Text(

                text =
                    "MEDASSIST AI",

                color =
                    textPrimary,

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(22.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            surface
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            3.dp
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(17.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    // -----------------------------------------
                    // BRAND ICON
                    // -----------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .size(50.dp)
                                .background(
                                    surfaceVariant,
                                    RoundedCornerShape(16.dp)
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
                                primaryBlue,

                            modifier =
                                Modifier.size(27.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(13.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                "Your intelligent health companion",

                            color =
                                textPrimary,

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(

                            text =
                                "Get general health information and guidance with MEDASSIST AI.",

                            color =
                                textSecondary,

                            fontSize =
                                11.sp,

                            lineHeight =
                                16.sp
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )


            // =================================================
            // SIGN OUT
            // =================================================

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onLogoutClick()
                        }
                        .border(
                            width = 1.dp,
                            color = logoutBorder,
                            shape =
                                RoundedCornerShape(20.dp)
                        ),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            logoutBackground
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(17.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    // -----------------------------------------
                    // LOGOUT ICON
                    // -----------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .size(46.dp)
                                .background(
                                    Color(0xFFFFE3E0),
                                    RoundedCornerShape(14.dp)
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(

                            text =
                                "↪",

                            color =
                                logoutText,

                            fontSize =
                                26.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(13.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                "Sign Out",

                            color =
                                logoutText,

                            fontSize =
                                15.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(

                            text =
                                "Sign out safely from this device",

                            color =
                                textSecondary,

                            fontSize =
                                11.sp
                        )
                    }


                    Text(

                        text =
                            "›",

                        color =
                            logoutText,

                        fontSize =
                            27.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // =================================================
            // FOOTER
            // =================================================

            Text(

                text =
                    "MEDASSIST AI  •  Secure • Simple • Intelligent",

                modifier =
                    Modifier.fillMaxWidth(),

                color =
                    textSecondary,

                fontSize =
                    9.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }
}


// =============================================================
// PROFILE INFO ROW
// =============================================================

@Composable
private fun ProfileInfoRow(

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    subtitle: String,

    iconColor: Color
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(

            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFEAF6FA),
                        RoundedCornerShape(13.dp)
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    title,

                tint =
                    iconColor,

                modifier =
                    Modifier.size(21.dp)
            )
        }


        Spacer(
            modifier =
                Modifier.width(12.dp)
        )


        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(

                text =
                    title,

                color =
                    Color(0xFF172B4D),

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )


            Text(

                text =
                    subtitle,

                color =
                    Color(0xFF71818C),

                fontSize =
                    11.sp,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================
// DIVIDER
// =============================================================

@Composable
private fun ProfileDivider() {

    Box(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
                .height(1.dp)
                .background(
                    Color(0xFFE8EEF2)
                )
    )
}