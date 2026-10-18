package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.SageGreen
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.CoralAccent
import com.example.ai_based_medical_chatbot.ui.theme.AppBackground
import com.example.ai_based_medical_chatbot.ui.theme.AppSurface
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenBorder
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSoft
import com.example.ai_based_medical_chatbot.ui.theme.LightGreenSurface
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTeal
import com.example.ai_based_medical_chatbot.ui.theme.PrimaryTealDark
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.appText

data class Medicine(
    val name: String,
    val category: String,
    val uses: List<String>,
    val precautions: List<String>,
    val commonSideEffects: List<String>,
    val warning: String,
    val genericName: String = ""
)

private fun MedicineRecord.toMedicine(): Medicine {

    val useList = uses
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)

    val precautionList = precautions
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)

    val sideEffectList = sideEffects
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)

    return Medicine(
        name = name.ifBlank {
            generic.ifBlank {
                brand.ifBlank {
                    "Unknown Medicine"
                }
            }
        },
        category = drugClass.ifBlank {
            "Medicine"
        },
        uses = useList.ifEmpty {
            listOf(
                "Uses information is not available in this record."
            )
        },
        precautions = precautionList.ifEmpty {
            listOf(
                "Check the official product label or consult a qualified healthcare professional."
            )
        },
        commonSideEffects = sideEffectList.ifEmpty {
            listOf(
                "Side-effect information is not available in this record."
            )
        },
        warning = warnings.ifBlank {
            "This information is educational and does not replace professional medical advice."
        },
        genericName = generic
    )
}

private suspend fun loadMedicineResults(
    context: android.content.Context,
    query: String
): List<MedicineRecord> {

    return withContext(Dispatchers.IO) {

        if (query.isBlank()) {

            MedicineRepository
                .getMedicines(context)
                .take(20)

        } else {

            MedicineRepository.searchMedicines(
                context = context,
                query = query,
                limit = 20
            )
        }
    }
}

@Composable
fun MedicineInfoScreen(
    onBackClick: () -> Unit,
    onMedicineClick: (Medicine) -> Unit
) {

    val context = LocalContext.current

    val appLanguageController = LocalAppLanguageController.current
    val selectedLanguage = appLanguageController.selectedLanguage
    val keyboardController = LocalSoftwareKeyboardController.current

    fun t(key: String): String = appText(key, selectedLanguage)


    var searchQuery by remember {
        mutableStateOf("")
    }

    var searchResults by remember {
        mutableStateOf<List<MedicineRecord>>(emptyList())
    }

    var isSearching by remember {
        mutableStateOf(true)
    }

    /*
     * Search is delayed by 300 ms so that the database
     * is not searched for every character typed.
     *
     * The actual medicine search runs on Dispatchers.IO,
     * keeping the Compose UI thread responsive.
     */
    LaunchedEffect(searchQuery) {

        isSearching = true

        delay(300)

        val results = loadMedicineResults(
            context = context,
            query = searchQuery
        )

        searchResults = results

        isSearching = false
    }

    val filteredMedicines = remember(searchResults) {
        searchResults.map {
            it.toMedicine()
        }
    }

    val primary = DeepTeal
    val darkBlue = DarkTeal
    val gray = TextMuted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Ivory,
                        SoftMint
                    )
                )
            )
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),

            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = 25.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            // =====================================================
            // HEADER
            // =====================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(PureWhite, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PureWhite,
                            contentColor = DarkTeal
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkTeal
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Column {

                        Text(
                            text =
                                "Medicine Information",

                            color =
                                DarkTeal,

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                t("medicine_info_subtitle"),

                            color =
                                TextMuted,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }

            // =====================================================
            // SEARCH
            // =====================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                PureWhite
                        ),

                    border = BorderStroke(1.dp, Mint),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                ) {

                    OutlinedTextField(

                        value =
                            searchQuery,

                        onValueChange = { value ->
                            searchQuery = value
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(6.dp),

                        singleLine = true,

                        keyboardOptions =
                            KeyboardOptions(
                                imeAction =
                                    ImeAction.Search
                            ),

                        keyboardActions =
                            KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),

                        placeholder = {

                            Text(
                                text =
                                    t("medicine_search_placeholder"),
                                color = TextMuted
                            )
                        },

                        leadingIcon = {

                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DeepTeal
                            )
                        },

                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedTextColor =
                                    TextDark,

                                unfocusedTextColor =
                                    TextDark,

                                focusedPlaceholderColor =
                                    TextMuted,

                                unfocusedPlaceholderColor =
                                    TextMuted,

                                focusedBorderColor =
                                    DeepTeal,

                                unfocusedBorderColor =
                                    Mint,

                                cursorColor =
                                    DeepTeal,

                                focusedContainerColor =
                                    PureWhite,

                                unfocusedContainerColor =
                                    SoftMint.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // =====================================================
            // INTRO CARD
            // =====================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(20.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                DarkTeal
                        ),

                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.4f)),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(18.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(54.dp)
                                    .background(
                                        PureWhite.copy(
                                            alpha = 0.16f
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Image(
                                painter =
                                    painterResource(
                                        id =
                                            R.drawable.medassist_logo
                                    ),

                                contentDescription =
                                    "MEDASSIST AI",

                                modifier =
                                    Modifier.size(40.dp),

                                contentScale =
                                    ContentScale.Fit
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                text =
                                    t("medicine_universal_search"),

                                color =
                                    PureWhite,

                                fontSize =
                                    18.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    t("medicine_search_database"),

                                color =
                                    PureWhite.copy(
                                        alpha = 0.85f
                                    ),

                                fontSize =
                                    12.sp,

                                lineHeight =
                                    17.sp
                            )
                        }
                    }
                }
            }

            // =====================================================
            // SECTION TITLE
            // =====================================================

            item {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                ) {

                    Text(
                        text =
                            if (searchQuery.isBlank()) {
                                t("medicine_available")
                            } else {
                                t("medicine_search_results")
                            },

                        color =
                            DarkTeal,

                        fontSize =
                            19.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    val resultText =
                        if (isSearching) {
                            t("medicine_searching")
                        } else if (searchQuery.isBlank()) {
                            t("medicine_showing").replace(
                                "{count}",
                                filteredMedicines.size.toString()
                            )
                        } else {
                            t(
                                if (filteredMedicines.size == 1)
                                    "medicine_one_result"
                                else
                                    "medicine_many_results"
                            ).replace(
                                "{count}",
                                filteredMedicines.size.toString()
                            )
                        }

                    Text(
                        text = resultText,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // =====================================================
            // SEARCHING
            // =====================================================

            if (isSearching) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PureWhite
                            ),

                        border = BorderStroke(1.dp, Mint)
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(25.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    t("medicine_searching_short"),

                                color =
                                    DeepTeal,

                                fontSize =
                                    15.sp,

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    t("medicine_finding"),

                                color =
                                    TextMuted,

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }

            } else if (filteredMedicines.isEmpty()) {

                // =================================================
                // NO RESULTS
                // =================================================

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PureWhite
                            ),

                        border = BorderStroke(1.dp, Mint)
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(25.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "⌕",

                                color =
                                    DeepTeal,

                                fontSize =
                                    35.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    t("medicine_none"),

                                color =
                                    DarkTeal,

                                fontSize =
                                    16.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    t("medicine_try_another"),

                                color =
                                    TextMuted,

                                fontSize =
                                    12.sp
                            )
                        }
                    }
                }

            } else {

                // =================================================
                // RESULTS
                // =================================================

                items(
                    items = filteredMedicines
                ) { medicine ->

                    MedicineCard(
                        medicine = medicine,

                        onClick = {
                            onMedicineClick(medicine)
                        }
                    )
                }
            }

            // =====================================================
            // DISCLAIMER
            // =====================================================

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                SoftMint
                        ),

                    border = BorderStroke(1.dp, Mint)
                ) {

                    Text(
                        text =
                            t("medicine_disclaimer"),

                        modifier =
                            Modifier.padding(14.dp),

                        color =
                            DarkTeal,

                        fontSize =
                            11.sp,

                        lineHeight =
                            17.sp
                    )
                }
            }
        }
    }
}

// =============================================================
// MEDICINE CARD
// =============================================================

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    PureWhite
            ),

        border = BorderStroke(1.dp, Mint),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .background(
                            SoftMint,
                            RoundedCornerShape(14.dp)
                        )
                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(14.dp)),

                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = PillVectorIcon,
                    contentDescription = "Medicine",
                    tint = DeepTeal,
                    modifier = Modifier.size(22.dp)
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
                        medicine.name,

                    color =
                        DarkTeal,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines = 2
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        medicine.category,

                    color =
                        TextMuted,

                    fontSize =
                        12.sp,

                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}