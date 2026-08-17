package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HealthTip(
    val title: String,
    val description: String
)

private val healthTips = listOf(

    HealthTip(
        title = "Stay Hydrated",
        description = "Drink adequate water throughout the day and pay attention to increased fluid needs during hot weather or physical activity."
    ),

    HealthTip(
        title = "Eat a Balanced Diet",
        description = "Include a variety of fruits, vegetables, whole grains, protein sources and other nutritious foods in your regular meals."
    ),

    HealthTip(
        title = "Stay Physically Active",
        description = "Regular physical activity can support cardiovascular health, strength, mobility and overall wellbeing."
    ),

    HealthTip(
        title = "Get Enough Sleep",
        description = "Maintain a regular sleep schedule and create a comfortable environment that supports good-quality sleep."
    ),

    HealthTip(
        title = "Maintain Personal Hygiene",
        description = "Regular handwashing, dental care and general personal hygiene can help reduce the spread of infections."
    ),

    HealthTip(
        title = "Manage Stress",
        description = "Consider healthy activities such as exercise, relaxation techniques, hobbies and spending time with supportive people."
    ),

    HealthTip(
        title = "Avoid Smoking",
        description = "Avoiding tobacco and exposure to tobacco smoke can reduce the risk of many serious health problems."
    ),

    HealthTip(
        title = "Regular Health Checkups",
        description = "Routine health checkups can help identify potential health concerns early. Follow recommendations from qualified healthcare professionals."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTipsScreen(
    onBackClick: () -> Unit
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Health Tips",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Healthy Habits",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Simple habits that can support general health and wellbeing.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            items(healthTips) { tip ->

                HealthTipCard(
                    tip = tip
                )
            }

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Text(
                        text = "⚠ Health tips are for general educational purposes and do not replace professional medical advice.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthTipCard(
    tip: HealthTip
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = tip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = tip.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}