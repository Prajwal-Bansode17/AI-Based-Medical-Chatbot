package ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    // COLORS
    // =========================================================

    val backgroundColor =
        Color(0xFF071A33)

    val cardColor =
        Color(0xFF123653)

    val accentColor =
        Color(0xFF087EA4)

    val lightAccentColor =
        Color(0xFF7DE8F0)

    val primaryText =
        Color.White

    val secondaryText =
        Color(0xFFB9EAF2)


    // =========================================================
    // MAIN SCREEN
    // =========================================================

    Column(

        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)

    ) {


        // =====================================================
        // HEADER
        // =====================================================

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            IconButton(

                onClick =
                    onBackClick

            ) {

                Icon(

                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,

                    contentDescription =
                        "Back",

                    tint =
                        primaryText
                )
            }


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            Text(

                text =
                    "My Profile",

                color =
                    primaryText,

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }


        // =====================================================
        // PROFILE CONTENT
        // =====================================================

        Column(

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally

        ) {


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // =================================================
            // PROFILE AVATAR
            // =================================================

            Box(

                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color =
                            accentColor,

                        shape =
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
                        Modifier.size(50.dp)
                )
            }


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // =================================================
            // USER NAME
            // =================================================

            Text(

                text =
                    if (userName.isBlank()) {
                        "MedAssist User"
                    } else {
                        userName
                    },

                color =
                    primaryText,

                fontSize =
                    23.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )


            // =================================================
            // USER EMAIL
            // =================================================

            Text(

                text =
                    if (userEmail.isBlank()) {
                        "Email not available"
                    } else {
                        userEmail
                    },

                color =
                    secondaryText,

                fontSize =
                    13.sp
            )


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =================================================
            // ACCOUNT INFORMATION CARD
            // =================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            cardColor
                    )

            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)

                ) {

                    Text(

                        text =
                            "Account Information",

                        color =
                            primaryText,

                        fontSize =
                            17.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )


                    // -----------------------------------------
                    // EMAIL
                    // -----------------------------------------

                    Row(

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(42.dp)
                                    .background(
                                        Color(0xFF0D5068),
                                        RoundedCornerShape(12.dp)
                                    ),

                            contentAlignment =
                                Alignment.Center

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Email,

                                contentDescription =
                                    "Email",

                                tint =
                                    lightAccentColor,

                                modifier =
                                    Modifier.size(21.dp)
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )


                        Column {

                            Text(

                                text =
                                    "Email",

                                color =
                                    secondaryText,

                                fontSize =
                                    12.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )


                            Text(

                                text =
                                    if (userEmail.isBlank()) {
                                        "Not available"
                                    } else {
                                        userEmail
                                    },

                                color =
                                    primaryText,

                                fontSize =
                                    14.sp,

                                fontWeight =
                                    FontWeight.Medium
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // =================================================
            // SIGN OUT CARD
            // =================================================

            Card(

                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {

                        onLogoutClick()
                    },

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF182D3F)
                    )

            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {


                    // =========================================
                    // LOGOUT ICON
                    // =========================================

                    Box(

                        modifier =
                            Modifier
                                .size(46.dp)
                                .background(
                                    Color(0xFF5A2630),
                                    RoundedCornerShape(14.dp)
                                ),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        // No Icons.Default.Logout
                        // This avoids the unresolved reference.

                        Text(

                            text =
                                "↪",

                            color =
                                Color(0xFFFF8A80),

                            fontSize =
                                27.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(14.dp)
                    )


                    // =========================================
                    // SIGN OUT TEXT
                    // =========================================

                    Column(

                        modifier =
                            Modifier.weight(1f)

                    ) {

                        Text(

                            text =
                                "Sign Out",

                            color =
                                Color(0xFFFFB4AB),

                            fontSize =
                                16.sp,

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
                                secondaryText,

                            fontSize =
                                12.sp
                        )
                    }


                    // =========================================
                    // ARROW
                    // =========================================

                    Text(

                        text =
                            "›",

                        color =
                            Color(0xFFFF8A80),

                        fontSize =
                            28.sp,

                        fontWeight =
                            FontWeight.Light
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // =================================================
            // SECURITY MESSAGE
            // =================================================

            Text(

                text =
                    "Your account information is handled securely.",

                color =
                    Color(0xFF7FA8B8),

                fontSize =
                    11.sp
            )
        }
    }
}