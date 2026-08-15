package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF071A33),
            Color(0xFF0A2D4F),
            Color(0xFF064E63)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Forgot Password?",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter your email to reset your password",
                color = Color(0xFFB9EAF2),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Email")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    // Reset password functionality will be added later
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Send Reset Link",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = {
                    onBackToLogin()
                }
            ) {
                Text(
                    text = "Back to Login",
                    color = Color(0xFF7DE8F0)
                )
            }
        }
    }
}