package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

    val primary = Color(0xFF4F7CFF)
    val darkBlue = Color(0xFFF8FAFC)
    val gray = Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF080D18),
                        Color(0xFF0D1422)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(Modifier.height(18.dp))

            // HEADER
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
                        contentDescription = "Back",
                        tint = darkBlue
                    )
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = "My Profile",
                    color = darkBlue,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(25.dp))

            // PROFILE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111A2A)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape
                            )
                            .background(
                                primary,
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
                            tint = Color.White,
                            modifier =
                                Modifier.size(55.dp)
                        )
                    }

                    Spacer(Modifier.height(17.dp))

                    Text(
                        text =
                            if (userName.isBlank())
                                "MedAssist User"
                            else
                                userName,
                        color = darkBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text =
                            if (userEmail.isBlank())
                                "Email not available"
                            else
                                userEmail,
                        color = gray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ACCOUNT INFORMATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(23.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111A2A)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 5.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Account Information",
                        color = darkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(18.dp))

                    ProfileInfoRow(
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = primary
                            )
                        },
                        title = "Full Name",
                        value =
                            if (userName.isBlank())
                                "MedAssist User"
                            else
                                userName
                    )

                    Spacer(Modifier.height(16.dp))

                    ProfileInfoRow(
                        icon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = primary
                            )
                        },
                        title = "Email Address",
                        value =
                            if (userEmail.isBlank())
                                "Not available"
                            else
                                userEmail
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // LOGOUT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLogoutClick()
                    },
                shape = RoundedCornerShape(23.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111A2A)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 5.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Color(0xFFFFEEEE),
                                RoundedCornerShape(15.dp)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "↪",
                            color = Color(0xFFD95C5C),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Sign Out",
                            color = Color(0xFFB74747),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text =
                                "Safely sign out of this account",
                            color = gray,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "›",
                        color = Color(0xFFD95C5C),
                        fontSize = 30.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = primary,
                    modifier = Modifier.size(15.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text =
                        "Your account information is handled securely.",
                    color = gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}


@Composable
private fun ProfileInfoRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(45.dp)
                .background(
                    Color(0xFF16233A),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment =
                Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.width(13.dp))

        Column {

            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = value,
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}