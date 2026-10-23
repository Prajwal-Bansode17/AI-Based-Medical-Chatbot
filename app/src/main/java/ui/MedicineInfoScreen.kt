package ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_based_medical_chatbot.LocalAppLanguageController
import com.example.ai_based_medical_chatbot.R
import com.example.ai_based_medical_chatbot.appText
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class Medicine(
    val name: String,
    val category: String,
    val uses: List<String>,
    val precautions: List<String>,
    val commonSideEffects: List<String>,
    val warning: String,
    val genericName: String = "",
    val strengthAndForm: String = "",
    val tags: List<String> = emptyList(),
    val shortDescription: String = "",
    val isFavorite: Boolean = false
)

private fun humanReadableCategory(
    drugClass: String,
    ingredients: String,
    uses: String,
    generic: String
): String {
    val combined = "$drugClass $ingredients $uses $generic".lowercase()
    return when {
        combined.contains("acetaminophen") || combined.contains("paracetamol") -> "Analgesic & Antipyretic"
        combined.contains("ibuprofen") || combined.contains("nsaid") -> "Pain Relief & Anti-inflammatory"
        combined.contains("amoxicillin") || combined.contains("antibiotic") || combined.contains("penicillin") -> "Antibiotic"
        combined.contains("cetirizine") || combined.contains("antihistamine") || combined.contains("allergy") -> "Allergy Relief & Antihistamine"
        combined.contains("omeprazole") || combined.contains("acid") || combined.contains("heartburn") || combined.contains("ppi") -> "Acidity & Heartburn Reducer"
        combined.contains("metformin") || combined.contains("diabetes") || combined.contains("blood sugar") -> "Diabetes Management"
        drugClass.contains("HUMAN", ignoreCase = true) -> "General Therapeutic Medicine"
        drugClass.isNotBlank() -> drugClass.trim()
        else -> "General Medicine"
    }
}

private fun extractStrengthAndForm(
    name: String,
    generic: String,
    ingredients: String,
    drugClass: String
): String {
    val combined = "$name $generic $ingredients".lowercase()

    when {
        name.equals("Dolo 650", ignoreCase = true) || name.contains("Dolo 650") -> return "Paracetamol 650 mg Tablet"
        name.equals("Crocin 500", ignoreCase = true) || name.contains("Crocin 500") -> return "Paracetamol 500 mg Tablet"
        name.equals("Calpol 650", ignoreCase = true) || name.contains("Calpol 650") -> return "Paracetamol 650 mg Tablet"
        name.equals("PCM 650", ignoreCase = true) -> return "Paracetamol 650 mg Tablet"
        name.equals("P-650", ignoreCase = true) -> return "Paracetamol 650 mg Tablet"
        name.contains("Paracetamol 125", ignoreCase = true) -> return "Suspension (Syrup)"
        name.contains("Paracetamol 500", ignoreCase = true) -> return "Tablet"
    }

    val strengthRegex = Regex("""(\d+(?:\.\d+)?\s*(?:mg|ml|mcg|g|%)(?:\s*/\s*\d+\s*(?:ml|mg))?)""", RegexOption.IGNORE_CASE)
    val strengthMatch = strengthRegex.find(name)
        ?: strengthRegex.find(ingredients)
        ?: strengthRegex.find(generic)
    val strength = strengthMatch?.value?.trim() ?: ""

    val form = when {
        combined.contains("syrup") || combined.contains("suspension") || combined.contains("oral solution") -> "Suspension (Syrup)"
        combined.contains("capsule") || combined.contains("cap") -> "Capsule"
        combined.contains("tablet") || combined.contains("tab") -> "Tablet"
        combined.contains("patch") -> "Patch"
        combined.contains("cream") || combined.contains("ointment") || combined.contains("gel") -> "Topical"
        combined.contains("injection") || combined.contains("injectable") -> "Injection"
        else -> "Tablet"
    }

    return if (strength.isNotBlank()) {
        val genName = generic.split(",").firstOrNull()?.trim() ?: ""
        if (genName.isNotBlank() && !name.contains(genName, ignoreCase = true)) {
            "$genName $strength $form"
        } else {
            "$strength $form"
        }
    } else {
        form
    }
}

private fun extractTags(
    name: String,
    generic: String,
    ingredients: String,
    drugClass: String,
    uses: String
): List<String> {
    val combined = "$name $generic $ingredients $drugClass $uses".lowercase()
    val tags = mutableListOf<String>()
    if (combined.contains("pain") || combined.contains("ache") || combined.contains("headache") || combined.contains("analgesic")) {
        tags.add("Pain relief")
    }
    if (combined.contains("fever") || combined.contains("antipyretic") || combined.contains("temperature")) {
        tags.add("Fever")
    }
    if (combined.contains("antibiotic") || combined.contains("bacterial") || combined.contains("infection")) {
        tags.add("Antibiotic")
    }
    if (combined.contains("allergy") || combined.contains("antihistamine") || combined.contains("sneezing") || combined.contains("cold")) {
        tags.add("Allergy relief")
    }
    if (combined.contains("acid") || combined.contains("heartburn") || combined.contains("gerd") || combined.contains("reflux")) {
        tags.add("Acidity relief")
    }
    if (combined.contains("diabetes") || combined.contains("blood sugar") || combined.contains("glucose")) {
        tags.add("Diabetes")
    }
    return tags.distinct().take(2).ifEmpty {
        listOf("General care")
    }
}

private fun extractShortDescription(
    name: String,
    uses: String,
    category: String
): String {
    when {
        name.contains("Dolo", ignoreCase = true) || name.contains("Crocin", ignoreCase = true) -> return "Pain relief and fever reducer"
        name.contains("Calpol", ignoreCase = true) -> return "Pain and fever relief (Child & Adult)"
        name.contains("Paracetamol", ignoreCase = true) -> return "Pain relief and fever reducer"
        name.contains("Ibuprofen", ignoreCase = true) -> return "Relief of pain and inflammation"
        name.contains("Amoxicillin", ignoreCase = true) -> return "Bacterial infections treatment"
        name.contains("Cetirizine", ignoreCase = true) -> return "Relief from allergy and cold symptoms"
        name.contains("Omeprazole", ignoreCase = true) -> return "Heartburn and acidity relief"
        name.contains("Metformin", ignoreCase = true) -> return "Blood sugar control in diabetes"
    }
    val lines = uses.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    return lines.firstOrNull() ?: category
}

fun MedicineRecord.toMedicine(): Medicine {
    val cleanName = name.ifBlank { generic.ifBlank { brand.ifBlank { "Unknown Medicine" } } }.trim()
    val cleanGeneric = generic.trim()

    val displayCategory = humanReadableCategory(drugClass, ingredients, uses, cleanGeneric)
    val strengthForm = extractStrengthAndForm(cleanName, cleanGeneric, ingredients, drugClass)
    val tagsList = extractTags(cleanName, cleanGeneric, ingredients, drugClass, uses)
    val shortDesc = extractShortDescription(cleanName, uses, displayCategory)

    val useList = uses
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)
        .ifEmpty {
            listOf("Follow physician prescription", "For therapeutic symptom management as directed")
        }

    val precautionList = precautions
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)
        .ifEmpty {
            listOf("Keep away from children", "Store in a cool dry place", "Consult physician if pregnant or nursing")
        }

    val sideEffectList = sideEffects
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)
        .ifEmpty {
            listOf("Usually well tolerated", "Consult doctor if symptoms persist or unexpected reactions occur")
        }

    return Medicine(
        name = cleanName,
        category = displayCategory,
        uses = useList,
        precautions = precautionList,
        commonSideEffects = sideEffectList,
        warning = warnings.ifBlank {
            "Follow standard medical guidelines and consult your doctor."
        },
        genericName = cleanGeneric,
        strengthAndForm = strengthForm,
        tags = tagsList,
        shortDescription = shortDesc
    )
}

enum class MedicineSortOption {
    RELEVANCE,
    NAME_ASC,
    NAME_DESC
}

private suspend fun loadMedicineResults(
    context: android.content.Context,
    query: String,
    isBrowsingAll: Boolean,
    catalogueLimit: Int
): List<MedicineRecord> {
    return withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            if (isBrowsingAll) {
                MedicineRepository.getMedicines(context, limit = catalogueLimit)
            } else {
                MedicineRepository.getMedicines(context, limit = 20)
            }
        } else {
            MedicineRepository.searchMedicines(
                context = context,
                query = query,
                limit = 50
            )
        }
    }
}

data class PopularMedicineItem(
    val name: String,
    val category: String,
    val searchTerm: String,
    val subtitle: String = when (name) {
        "Paracetamol" -> "Pain & Fever"
        "Ibuprofen" -> "Pain Relief"
        "Amoxicillin" -> "Antibiotic"
        "Cetirizine" -> "Allergy Relief"
        "Omeprazole" -> "Acidity Relief"
        "Metformin" -> "Diabetes"
        else -> category
    }
)

val popularMedicinesList = listOf(
    PopularMedicineItem("Paracetamol", "Analgesic", "paracetamol", "Pain & Fever"),
    PopularMedicineItem("Ibuprofen", "Pain Relief", "ibuprofen", "Pain Relief"),
    PopularMedicineItem("Amoxicillin", "Antibiotic", "amoxicillin", "Antibiotic"),
    PopularMedicineItem("Cetirizine", "Antihistamine", "cetirizine", "Allergy Relief"),
    PopularMedicineItem("Omeprazole", "Acid Reducer", "omeprazole", "Acidity Relief"),
    PopularMedicineItem("Metformin", "Diabetes", "metformin", "Diabetes")
)

val ShieldCheckVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ShieldCheckVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M12 2L4 5v6.09c0 5.05 3.41 9.76 8 10.91 4.59-1.15 8-5.86 8-10.91V5l-8-3zm-1.06 13.54L7.4 12.01l1.41-1.41 2.13 2.12 5.66-5.65 1.41 1.41-7.07 7.06z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
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
    val focusRequester = remember { FocusRequester() }

    fun t(key: String): String = appText(key, selectedLanguage)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isBrowsingAll by rememberSaveable { mutableStateOf(false) }
    var currentSort by rememberSaveable { mutableStateOf(MedicineSortOption.RELEVANCE) }
    var catalogueLimit by rememberSaveable { mutableStateOf(40) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var searchResults by remember { mutableStateOf<List<MedicineRecord>>(emptyList()) }
    var isSearching by remember { mutableStateOf(true) }

    // Speech recognition launchers
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                searchQuery = spoken
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val localeTag = when (selectedLanguage) {
                "मराठी" -> "mr-IN"
                "हिंदी" -> "hi-IN"
                else -> "en-IN"
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PROMPT, t("medicine_search_placeholder"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, t("medicine_voice_search_unavailable"), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, t("medicine_mic_permission_required"), Toast.LENGTH_SHORT).show()
        }
    }

    fun launchVoiceSearch() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val localeTag = when (selectedLanguage) {
                "मराठी" -> "mr-IN"
                "हिंदी" -> "hi-IN"
                else -> "en-IN"
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PROMPT, t("medicine_search_placeholder"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, t("medicine_voice_search_unavailable"), Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(searchQuery, isBrowsingAll, catalogueLimit) {
        isSearching = true
        delay(200)
        val results = loadMedicineResults(
            context = context,
            query = searchQuery,
            isBrowsingAll = isBrowsingAll,
            catalogueLimit = catalogueLimit
        )
        searchResults = results
        isSearching = false
    }

    val filteredMedicines = remember(searchResults) {
        searchResults.map { it.toMedicine() }
    }

    val sortedMedicines = remember(filteredMedicines, currentSort) {
        when (currentSort) {
            MedicineSortOption.RELEVANCE -> filteredMedicines
            MedicineSortOption.NAME_ASC -> filteredMedicines.sortedBy { it.name.lowercase() }
            MedicineSortOption.NAME_DESC -> filteredMedicines.sortedByDescending { it.name.lowercase() }
        }
    }

    val relatedMedicines = remember(searchQuery, searchResults) {
        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            MedicineRepository.getRelatedMedicines(searchQuery, searchResults).map { it.toMedicine() }
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Ivory,
                        SoftMint.copy(alpha = 0.5f),
                        Ivory
                    )
                )
            )
    ) {
        MedicineBackgroundDecorations()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 16.dp,
                end = 18.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================
            // A. HEADER (Pinned top item)
            // =========================================================
            item(key = "header_item") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isBrowsingAll && searchQuery.isBlank()) {
                                isBrowsingAll = false
                            } else if (searchQuery.isNotBlank()) {
                                searchQuery = ""
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
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
                            tint = DarkTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = t("medicine_info"),
                            color = DarkTeal,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t("medicine_info_subtitle"),
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // =========================================================
            // B. HERO CARD (Smoothly animates in/out without removing item slot)
            // =========================================================
            item(key = "hero_container_item") {
                AnimatedVisibility(
                    visible = searchQuery.isBlank() && !isBrowsingAll,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            SoftMint.copy(alpha = 0.75f),
                                            Mint.copy(alpha = 0.30f),
                                            PureWhite
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t("medicine_hero_title"),
                                        color = DarkTeal,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = t("medicine_hero_subtitle"),
                                        color = TextDark.copy(alpha = 0.75f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                MedicineBottleHeroIllustration()
                            }
                        }
                    }
                }
            }

            // =========================================================
            // C. SEARCH BAR (Stable keyed slot, retains focus and cursor)
            // =========================================================
            item(key = "search_bar_item") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        placeholder = {
                            Text(
                                text = t("medicine_search_placeholder"),
                                color = TextMuted,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DeepTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            keyboardController?.show()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                IconButton(
                                    onClick = { launchVoiceSearch() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = MicIcon,
                                        contentDescription = "Voice input",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = DeepTeal,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                }
            }

            // =========================================================
            // D. OVERVIEW / HOME SECTIONS (When search is blank & not browsing all)
            // =========================================================
            if (searchQuery.isBlank() && !isBrowsingAll) {
                // Universal Medicine Search Card
                item(key = "universal_search_card_item") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isBrowsingAll = true
                                keyboardController?.show()
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkTeal),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF065259),
                                            Color(0xFF006D77),
                                            Color(0xFF0A7B85)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 15.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            PureWhite.copy(alpha = 0.16f),
                                            RoundedCornerShape(13.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.medassist_logo),
                                        contentDescription = "MEDASSIST AI",
                                        modifier = Modifier.size(32.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Spacer(modifier = Modifier.width(13.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t("medicine_universal_search"),
                                        color = PureWhite,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = t("medicine_search_database"),
                                        color = PureWhite.copy(alpha = 0.88f),
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF7B944), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Browse",
                                        tint = DarkTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Popular Medicines Header
                item(key = "popular_medicines_header_item") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("medicine_popular"),
                            color = DarkTeal,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { isBrowsingAll = true }) {
                            Text(
                                text = t("medicine_view_all"),
                                color = DeepTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3-Column Grid
                popularMedicinesList.chunked(3).forEachIndexed { rowIndex, rowItems ->
                    item(key = "popular_grid_row_$rowIndex") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (medicineItem in rowItems) {
                                PopularMedicineGridCard(
                                    item = medicineItem,
                                    onClick = {
                                        searchQuery = medicineItem.name
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Available Medicines Header
                item(key = "available_medicines_header_item") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = t("medicine_available"),
                            color = DarkTeal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t("medicine_showing_popular"),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // =========================================================
                // SCREEN 2 / CATALOGUE HEADER + SORT
                // =========================================================
                item(key = "results_header_item") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val headerTitle = if (searchQuery.isNotBlank()) {
                                t("medicine_search_results")
                            } else {
                                t("medicine_complete_catalogue")
                            }
                            Text(
                                text = headerTitle,
                                color = DarkTeal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val resultCountText = if (isSearching) {
                                t("medicine_searching")
                            } else if (sortedMedicines.size == 1) {
                                t("medicine_one_result")
                            } else {
                                t("medicine_matching_count").replace("{count}", sortedMedicines.size.toString())
                            }
                            Text(
                                text = resultCountText,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isBrowsingAll && searchQuery.isBlank()) {
                                Row(
                                    modifier = Modifier
                                        .background(SoftMint.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Mint), RoundedCornerShape(12.dp))
                                        .clickable { isBrowsingAll = false }
                                        .padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = t("medicine_back_to_overview"),
                                        color = DeepTeal,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Sort Button (Pill shape with DropdownMenu)
                            Box {
                                Row(
                                    modifier = Modifier
                                        .background(PureWhite, RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Mint.copy(alpha = 0.7f)), RoundedCornerShape(12.dp))
                                        .clickable { sortMenuExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sortText = when (currentSort) {
                                        MedicineSortOption.RELEVANCE -> t("medicine_sort")
                                        MedicineSortOption.NAME_ASC -> "${t("medicine_sort")}: A–Z"
                                        MedicineSortOption.NAME_DESC -> "${t("medicine_sort")}: Z–A"
                                    }
                                    Text(
                                        text = sortText,
                                        color = DarkTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Sort",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                    modifier = Modifier.background(PureWhite)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = t("medicine_sort_relevance"),
                                                color = if (currentSort == MedicineSortOption.RELEVANCE) DeepTeal else TextDark,
                                                fontWeight = if (currentSort == MedicineSortOption.RELEVANCE) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            currentSort = MedicineSortOption.RELEVANCE
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = t("medicine_sort_az"),
                                                color = if (currentSort == MedicineSortOption.NAME_ASC) DeepTeal else TextDark,
                                                fontWeight = if (currentSort == MedicineSortOption.NAME_ASC) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            currentSort = MedicineSortOption.NAME_ASC
                                            sortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = t("medicine_sort_za"),
                                                color = if (currentSort == MedicineSortOption.NAME_DESC) DeepTeal else TextDark,
                                                fontWeight = if (currentSort == MedicineSortOption.NAME_DESC) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            currentSort = MedicineSortOption.NAME_DESC
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // E. RESULT / AVAILABLE CARDS
            // =========================================================
            if (isSearching) {
                item(key = "skeleton_loading_item") {
                    MedicineLoadingSkeleton()
                }
            } else if (sortedMedicines.isEmpty()) {
                item(key = "empty_state_item") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(SoftMint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PillVectorIcon,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = t("medicine_none"),
                                color = DarkTeal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t("medicine_try_another"),
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = sortedMedicines,
                    key = { it.name + "_" + it.strengthAndForm }
                ) { medicine ->
                    MedicineResultCard(
                        medicine = medicine,
                        selectedLanguage = selectedLanguage,
                        isSearchResult = searchQuery.isNotBlank() || isBrowsingAll,
                        onClick = { onMedicineClick(medicine) }
                    )
                }
            }

            // Pagination button when viewing all catalogue
            if (isBrowsingAll && searchQuery.isBlank() && sortedMedicines.isNotEmpty() && !isSearching) {
                item(key = "load_more_catalogue_item") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { catalogueLimit += 40 },
                            modifier = Modifier
                                .background(PureWhite, RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Mint), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = t("medicine_load_more"),
                                color = DeepTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =========================================================
            // F. RELATED MEDICINES (Screen 2: when available)
            // =========================================================
            if (searchQuery.isNotBlank() && relatedMedicines.isNotEmpty()) {
                item(key = "related_header_item") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t("medicine_related"),
                                color = DarkTeal,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = t("medicine_related_sub"),
                                color = TextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                        TextButton(onClick = { isBrowsingAll = true }) {
                            Text(
                                text = t("medicine_see_all"),
                                color = DeepTeal,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(
                    items = relatedMedicines,
                    key = { "rel_" + it.name + "_" + it.strengthAndForm }
                ) { relMed ->
                    MedicineResultCard(
                        medicine = relMed,
                        selectedLanguage = selectedLanguage,
                        isSearchResult = true,
                        onClick = { onMedicineClick(relMed) }
                    )
                }
            }

            // =========================================================
            // G. EDUCATIONAL DISCLAIMER
            // =========================================================
            item(key = "disclaimer_item") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftMint.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(PureWhite, CircleShape)
                                .border(BorderStroke(1.dp, Mint), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ShieldCheckVectorIcon,
                                contentDescription = "Medical Shield",
                                tint = DeepTeal,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("symptoms_disclaimer_title"),
                                color = DeepTeal,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = t("medicine_disclaimer"),
                                color = TextDark.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// =============================================================
// POPULAR MEDICINE GRID CARD (3-Column with Press Animation)
// =============================================================

@Composable
private fun PopularMedicineGridCard(
    item: PopularMedicineItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(),
        label = "pressScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(SoftMint.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PopularMedicineIllustration(item = item)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                color = DarkTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.subtitle,
                color = TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================
// MEDICINE RESULT CARD (Matches Screen 1 & Screen 2)
// =============================================================

@Composable
private fun MedicineResultCard(
    medicine: Medicine,
    selectedLanguage: String = "English",
    isSearchResult: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Packaging Illustration
            ProductPackagingIllustration(
                medicineName = medicine.name,
                form = medicine.strengthAndForm,
                modifier = Modifier.size(width = 54.dp, height = 44.dp)
            )

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    color = DarkTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val formulation = if (medicine.strengthAndForm.isNotBlank()) {
                    medicine.strengthAndForm
                } else if (medicine.genericName.isNotBlank()) {
                    medicine.genericName
                } else {
                    medicine.category
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formulation,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSearchResult && medicine.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (tag in medicine.tags.take(2)) {
                            Box(
                                modifier = Modifier
                                    .background(SoftMint, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = localizeMedicineTag(tag, selectedLanguage),
                                    color = DeepTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else if (!isSearchResult && medicine.shortDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = medicine.shortDescription,
                        color = TextDark.copy(alpha = 0.70f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SoftMint.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =============================================================
// PRODUCT PACKAGING ILLUSTRATION (Compose Canvas)
// =============================================================

@Composable
fun ProductPackagingIllustration(
    medicineName: String,
    form: String = "",
    modifier: Modifier = Modifier.size(54.dp, 44.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val isSyrup = form.contains("Syrup", ignoreCase = true) ||
                form.contains("Suspension", ignoreCase = true) ||
                medicineName.contains("Suspension", ignoreCase = true) ||
                medicineName.contains("Syrup", ignoreCase = true)

        if (isSyrup) {
            val botW = w * 0.44f
            val botH = h * 0.78f
            val botLeft = w * 0.28f
            val botTop = h * 0.18f

            drawOval(
                color = Color.Black.copy(alpha = 0.12f),
                topLeft = Offset(botLeft - 2.dp.toPx(), botTop + botH - 2.dp.toPx()),
                size = Size(botW + 4.dp.toPx(), 6.dp.toPx())
            )

            drawRoundRect(
                color = Color(0xFF78350F),
                topLeft = Offset(botLeft, botTop),
                size = Size(botW, botH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawLine(
                color = Color(0xFFD97706).copy(alpha = 0.6f),
                start = Offset(botLeft + 3.dp.toPx(), botTop + 6.dp.toPx()),
                end = Offset(botLeft + 3.dp.toPx(), botTop + botH - 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            drawRoundRect(
                color = PureWhite,
                topLeft = Offset(botLeft + 2.dp.toPx(), botTop + botH * 0.28f),
                size = Size(botW - 4.dp.toPx(), botH * 0.50f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            val crossCenterX = botLeft + botW / 2
            val crossCenterY = botTop + botH * 0.52f
            drawLine(
                color = Color(0xFFD32F2F),
                start = Offset(crossCenterX - 4.dp.toPx(), crossCenterY),
                end = Offset(crossCenterX + 4.dp.toPx(), crossCenterY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0xFFD32F2F),
                start = Offset(crossCenterX, crossCenterY - 4.dp.toPx()),
                end = Offset(crossCenterX, crossCenterY + 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            drawRoundRect(
                color = Color(0xFF92400E),
                topLeft = Offset(botLeft + botW * 0.28f, botTop - 5.dp.toPx()),
                size = Size(botW * 0.44f, 6.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawRoundRect(
                color = PureWhite,
                topLeft = Offset(botLeft + botW * 0.22f, botTop - 9.dp.toPx()),
                size = Size(botW * 0.56f, 6.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        } else {
            val boxW = w * 0.52f
            val boxH = h * 0.76f
            val boxLeft = w * 0.06f
            val boxTop = h * 0.12f

            val (boxBaseColor, boxAccentColor) = when {
                medicineName.contains("Crocin", ignoreCase = true) ->
                    Pair(Color(0xFF1E40AF), Color(0xFF3B82F6))
                medicineName.contains("Dolo", ignoreCase = true) ->
                    Pair(Color(0xFFF0F9FF), Color(0xFFEF4444))
                medicineName.contains("Calpol", ignoreCase = true) ->
                    Pair(Color(0xFFFFF7ED), Color(0xFFEA580C))
                medicineName.contains("Amoxicillin", ignoreCase = true) ->
                    Pair(Color(0xFFFEF2F2), Color(0xFFDC2626))
                medicineName.contains("Ibuprofen", ignoreCase = true) ->
                    Pair(Color(0xFFFFFBEB), Color(0xFFF59E0B))
                medicineName.contains("Cetirizine", ignoreCase = true) ->
                    Pair(Color(0xFFF0FDF4), Color(0xFF10B981))
                else ->
                    Pair(Color(0xFFF8FAFC), DeepTeal)
            }

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.08f),
                topLeft = Offset(boxLeft + 2.dp.toPx(), boxTop + 2.dp.toPx()),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            drawRoundRect(
                color = boxBaseColor,
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            drawRoundRect(
                color = boxAccentColor.copy(alpha = 0.4f),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            drawRoundRect(
                color = boxAccentColor,
                topLeft = Offset(boxLeft + 3.dp.toPx(), boxTop + 4.dp.toPx()),
                size = Size(boxW - 6.dp.toPx(), boxH * 0.32f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawLine(
                color = TextMuted.copy(alpha = 0.6f),
                start = Offset(boxLeft + 5.dp.toPx(), boxTop + boxH * 0.54f),
                end = Offset(boxLeft + boxW - 5.dp.toPx(), boxTop + boxH * 0.54f),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = TextMuted.copy(alpha = 0.4f),
                start = Offset(boxLeft + 5.dp.toPx(), boxTop + boxH * 0.70f),
                end = Offset(boxLeft + boxW * 0.65f, boxTop + boxH * 0.70f),
                strokeWidth = 1.dp.toPx()
            )

            // Overlapping Silver Blister Foil
            val blistW = w * 0.48f
            val blistH = h * 0.66f
            val blistLeft = w * 0.46f
            val blistTop = h * 0.24f

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.10f),
                topLeft = Offset(blistLeft + 1.dp.toPx(), blistTop + 1.dp.toPx()),
                size = Size(blistW, blistH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
                ),
                topLeft = Offset(blistLeft, blistTop),
                size = Size(blistW, blistH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF94A3B8),
                topLeft = Offset(blistLeft, blistTop),
                size = Size(blistW, blistH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            val pillRadius = 3.dp.toPx()
            val colSpacing = blistW / 3.5f
            val rowSpacing = blistH / 2.8f
            for (r in 0..1) {
                for (c in 0..2) {
                    val px = blistLeft + 6.dp.toPx() + c * colSpacing
                    val py = blistTop + 6.dp.toPx() + r * rowSpacing
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = pillRadius,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF64748B).copy(alpha = 0.5f),
                        radius = pillRadius,
                        center = Offset(px, py),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }
            }
        }
    }
}

// =============================================================
// POPULAR MEDICINE CUSTOM PILL ILLUSTRATIONS
// =============================================================

@Composable
private fun PopularMedicineIllustration(
    item: PopularMedicineItem,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(36.dp)) {
        val w = size.width
        val h = size.height

        when (item.name) {
            "Paracetamol" -> {
                val tabRadius = w * 0.30f
                val centerX = w * 0.50f

                drawCircle(
                    color = DarkTeal.copy(alpha = 0.10f),
                    radius = tabRadius,
                    center = Offset(centerX, h * 0.62f)
                )
                drawCircle(
                    color = Color(0xFFE2EBE8),
                    radius = tabRadius,
                    center = Offset(centerX, h * 0.58f)
                )
                drawCircle(
                    color = PureWhite,
                    radius = tabRadius,
                    center = Offset(centerX, h * 0.44f)
                )
                drawCircle(
                    color = Mint,
                    radius = tabRadius,
                    center = Offset(centerX, h * 0.44f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawLine(
                    color = Color(0xFFBACBC7),
                    start = Offset(centerX - tabRadius * 0.75f, h * 0.44f),
                    end = Offset(centerX + tabRadius * 0.75f, h * 0.44f),
                    strokeWidth = 1.3.dp.toPx()
                )
            }
            "Ibuprofen" -> {
                rotate(degrees = -30f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    val capW = w * 0.70f
                    val capH = h * 0.34f
                    val left = (w - capW) / 2
                    val top = (h - capH) / 2
                    val rad = CornerRadius(capH / 2, capH / 2)

                    drawRoundRect(
                        color = Color(0xFFFA6400),
                        topLeft = Offset(left, top),
                        size = Size(capW / 2 + 1f, capH),
                        cornerRadius = rad
                    )
                    drawRoundRect(
                        color = PureWhite,
                        topLeft = Offset(left + capW / 2, top),
                        size = Size(capW / 2, capH),
                        cornerRadius = rad
                    )
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(left + capW / 2, top),
                        end = Offset(left + capW / 2, top + capH),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawRoundRect(
                        color = Color(0xFFD65500).copy(alpha = 0.5f),
                        topLeft = Offset(left, top),
                        size = Size(capW, capH),
                        cornerRadius = rad,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
            "Amoxicillin" -> {
                rotate(degrees = 30f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    val capW = w * 0.70f
                    val capH = h * 0.34f
                    val left = (w - capW) / 2
                    val top = (h - capH) / 2
                    val rad = CornerRadius(capH / 2, capH / 2)

                    drawRoundRect(
                        color = Color(0xFFD32F2F),
                        topLeft = Offset(left, top),
                        size = Size(capW / 2 + 1f, capH),
                        cornerRadius = rad
                    )
                    drawRoundRect(
                        color = PureWhite,
                        topLeft = Offset(left + capW / 2, top),
                        size = Size(capW / 2, capH),
                        cornerRadius = rad
                    )
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(left + capW / 2, top),
                        end = Offset(left + capW / 2, top + capH),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawRoundRect(
                        color = Color(0xFFB71C1C).copy(alpha = 0.5f),
                        topLeft = Offset(left, top),
                        size = Size(capW, capH),
                        cornerRadius = rad,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
            "Cetirizine" -> {
                val ovalW = w * 0.74f
                val ovalH = h * 0.40f
                val left = (w - ovalW) / 2
                val top = (h - ovalH) / 2
                val rad = CornerRadius(ovalH / 2, ovalH / 2)

                drawRoundRect(
                    color = PureWhite,
                    topLeft = Offset(left, top),
                    size = Size(ovalW, ovalH),
                    cornerRadius = rad
                )
                drawRoundRect(
                    color = Mint,
                    topLeft = Offset(left, top),
                    size = Size(ovalW, ovalH),
                    cornerRadius = rad,
                    style = Stroke(width = 1.3.dp.toPx())
                )
                drawLine(
                    color = Color(0xFFBACBC7),
                    start = Offset(w * 0.50f, top + 2.dp.toPx()),
                    end = Offset(w * 0.50f, top + ovalH - 2.dp.toPx()),
                    strokeWidth = 1.3.dp.toPx()
                )
            }
            "Omeprazole" -> {
                rotate(degrees = -35f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    val capW = w * 0.70f
                    val capH = h * 0.34f
                    val left = (w - capW) / 2
                    val top = (h - capH) / 2
                    val rad = CornerRadius(capH / 2, capH / 2)

                    drawRoundRect(
                        color = Color(0xFF1976D2),
                        topLeft = Offset(left, top),
                        size = Size(capW / 2 + 1f, capH),
                        cornerRadius = rad
                    )
                    drawRoundRect(
                        color = PureWhite,
                        topLeft = Offset(left + capW / 2, top),
                        size = Size(capW / 2, capH),
                        cornerRadius = rad
                    )
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(left + capW / 2, top),
                        end = Offset(left + capW / 2, top + capH),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawRoundRect(
                        color = Color(0xFF0D47A1).copy(alpha = 0.5f),
                        topLeft = Offset(left, top),
                        size = Size(capW, capH),
                        cornerRadius = rad,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
            "Metformin" -> {
                val tabRadius = w * 0.32f
                val centerX = w * 0.50f
                val centerY = h * 0.50f

                drawCircle(
                    color = Color(0xFFDFA07E),
                    radius = tabRadius,
                    center = Offset(centerX, centerY + 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFF6B392),
                    radius = tabRadius,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color(0xFFE59C76),
                    radius = tabRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawLine(
                    color = Color(0xFFCC7F5A),
                    start = Offset(centerX - tabRadius * 0.75f, centerY),
                    end = Offset(centerX + tabRadius * 0.75f, centerY),
                    strokeWidth = 1.3.dp.toPx()
                )
            }
            else -> {
                drawCircle(
                    color = SoftMint,
                    radius = w * 0.32f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
        }
    }
}

// =============================================================
// ANIMATED LOADING SKELETON
// =============================================================

@Composable
private fun MedicineLoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = alpha)),
                border = BorderStroke(1.dp, Mint.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp, 44.dp)
                            .background(SoftMint.copy(alpha = alpha), RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .background(Mint.copy(alpha = alpha), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.80f)
                                .height(11.dp)
                                .background(SoftMint.copy(alpha = alpha), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

// =============================================================
// HERO MEDICINE BOTTLE WITH SPILLING CAPSULES
// =============================================================

@Composable
private fun MedicineBottleHeroIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 92.dp, height = 82.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = SoftMint.copy(alpha = 0.65f),
            radius = w * 0.40f,
            center = Offset(w * 0.42f, h * 0.50f)
        )
        drawOval(
            color = DarkTeal.copy(alpha = 0.14f),
            topLeft = Offset(w * 0.12f, h * 0.84f),
            size = Size(w * 0.46f, h * 0.11f)
        )

        val bodyLeft = w * 0.14f
        val bodyTop = h * 0.28f
        val bodyWidth = w * 0.44f
        val bodyHeight = h * 0.56f
        val cornerRad = CornerRadius(10.dp.toPx(), 10.dp.toPx())

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(PureWhite, SoftMint.copy(alpha = 0.85f))
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRad
        )
        drawRoundRect(
            color = Mint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRad,
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawRoundRect(
            color = PureWhite,
            topLeft = Offset(w * 0.24f, h * 0.20f),
            size = Size(w * 0.24f, h * 0.10f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = Mint,
            topLeft = Offset(w * 0.24f, h * 0.20f),
            size = Size(w * 0.24f, h * 0.10f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawRoundRect(
            color = DeepTeal,
            topLeft = Offset(w * 0.20f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.10f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        drawLine(
            color = Mint.copy(alpha = 0.6f),
            start = Offset(w * 0.23f, h * 0.15f),
            end = Offset(w * 0.49f, h * 0.15f),
            strokeWidth = 1.dp.toPx()
        )

        val labelLeft = w * 0.18f
        val labelTop = h * 0.40f
        val labelWidth = w * 0.36f
        val labelHeight = h * 0.34f
        drawRoundRect(
            color = PureWhite,
            topLeft = Offset(labelLeft, labelTop),
            size = Size(labelWidth, labelHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = Mint.copy(alpha = 0.6f),
            topLeft = Offset(labelLeft, labelTop),
            size = Size(labelWidth, labelHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        val crossCenterX = labelLeft + labelWidth / 2
        val crossCenterY = labelTop + labelHeight * 0.42f
        val crossArm = 6.dp.toPx()
        val crossThick = 2.5.dp.toPx()
        drawRoundRect(
            color = DeepTeal,
            topLeft = Offset(crossCenterX - crossArm, crossCenterY - crossThick / 2),
            size = Size(crossArm * 2, crossThick),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        drawRoundRect(
            color = DeepTeal,
            topLeft = Offset(crossCenterX - crossThick / 2, crossCenterY - crossArm),
            size = Size(crossThick, crossArm * 2),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        drawLine(
            color = Mint,
            start = Offset(labelLeft + 4.dp.toPx(), labelTop + labelHeight * 0.78f),
            end = Offset(labelLeft + labelWidth - 4.dp.toPx(), labelTop + labelHeight * 0.78f),
            strokeWidth = 1.5.dp.toPx()
        )

        // Spilling Capsule 1
        rotate(degrees = 28f, pivot = Offset(w * 0.74f, h * 0.42f)) {
            val capWidth = w * 0.28f
            val capHeight = h * 0.16f
            val capLeft = w * 0.60f
            val capTop = h * 0.34f
            val capRad = CornerRadius(capHeight / 2, capHeight / 2)

            drawRoundRect(
                color = DeepTeal,
                topLeft = Offset(capLeft, capTop),
                size = Size(capWidth / 2, capHeight),
                cornerRadius = capRad
            )
            drawRoundRect(
                color = Mint,
                topLeft = Offset(capLeft + capWidth / 2, capTop),
                size = Size(capWidth / 2, capHeight),
                cornerRadius = capRad
            )
            drawLine(
                color = PureWhite,
                start = Offset(capLeft + capWidth / 2, capTop),
                end = Offset(capLeft + capWidth / 2, capTop + capHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawRoundRect(
                color = DarkTeal.copy(alpha = 0.3f),
                topLeft = Offset(capLeft, capTop),
                size = Size(capWidth, capHeight),
                cornerRadius = capRad,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Spilling Capsule 2
        rotate(degrees = -32f, pivot = Offset(w * 0.70f, h * 0.72f)) {
            val capWidth = w * 0.24f
            val capHeight = h * 0.14f
            val capLeft = w * 0.58f
            val capTop = h * 0.65f
            val capRad = CornerRadius(capHeight / 2, capHeight / 2)

            drawRoundRect(
                color = SoftMint,
                topLeft = Offset(capLeft, capTop),
                size = Size(capWidth / 2, capHeight),
                cornerRadius = capRad
            )
            drawRoundRect(
                color = PureWhite,
                topLeft = Offset(capLeft + capWidth / 2, capTop),
                size = Size(capWidth / 2, capHeight),
                cornerRadius = capRad
            )
            drawLine(
                color = Mint,
                start = Offset(capLeft + capWidth / 2, capTop),
                end = Offset(capLeft + capWidth / 2, capTop + capHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawRoundRect(
                color = Mint,
                topLeft = Offset(capLeft, capTop),
                size = Size(capWidth, capHeight),
                cornerRadius = capRad,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

// =============================================================
// SUBTLE FLOATING BACKGROUND DECORATIONS
// =============================================================

@Composable
private fun MedicineBackgroundDecorations(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_float")
    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_1"
    )
    val floatOffset2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = SoftMint.copy(alpha = 0.28f),
            radius = w * 0.35f,
            center = Offset(w * 0.85f, h * 0.08f + floatOffset1)
        )

        rotate(degrees = 35f, pivot = Offset(w * 0.12f, h * 0.22f + floatOffset2)) {
            val pillW = 54.dp.toPx()
            val pillH = 22.dp.toPx()
            drawRoundRect(
                color = Mint.copy(alpha = 0.15f),
                topLeft = Offset(w * 0.12f - pillW / 2, h * 0.22f - pillH / 2 + floatOffset2),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH / 2, pillH / 2),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        val node1 = Offset(w * 0.88f, h * 0.40f + floatOffset1)
        val node2 = Offset(w * 0.94f, h * 0.44f + floatOffset1)
        val node3 = Offset(w * 0.82f, h * 0.45f + floatOffset1)
        drawLine(color = Mint.copy(alpha = 0.18f), start = node1, end = node2, strokeWidth = 1.2.dp.toPx())
        drawLine(color = Mint.copy(alpha = 0.18f), start = node1, end = node3, strokeWidth = 1.2.dp.toPx())
        drawCircle(color = DeepTeal.copy(alpha = 0.10f), radius = 4.dp.toPx(), center = node1)
        drawCircle(color = SoftMint.copy(alpha = 0.25f), radius = 3.5.dp.toPx(), center = node2)
        drawCircle(color = SoftMint.copy(alpha = 0.25f), radius = 3.5.dp.toPx(), center = node3)

        drawCircle(
            color = SoftMint.copy(alpha = 0.22f),
            radius = w * 0.30f,
            center = Offset(w * 0.10f, h * 0.82f + floatOffset2)
        )

        rotate(degrees = -25f, pivot = Offset(w * 0.85f, h * 0.74f + floatOffset1)) {
            val pillW = 48.dp.toPx()
            val pillH = 18.dp.toPx()
            drawRoundRect(
                color = DeepTeal.copy(alpha = 0.08f),
                topLeft = Offset(w * 0.85f - pillW / 2, h * 0.74f - pillH / 2 + floatOffset1),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH / 2, pillH / 2),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}
