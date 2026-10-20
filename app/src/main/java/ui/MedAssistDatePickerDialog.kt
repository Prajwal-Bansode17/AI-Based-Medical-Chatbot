package ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai_based_medical_chatbot.ui.theme.DarkTeal
import com.example.ai_based_medical_chatbot.ui.theme.DeepTeal
import com.example.ai_based_medical_chatbot.ui.theme.Ivory
import com.example.ai_based_medical_chatbot.ui.theme.Mint
import com.example.ai_based_medical_chatbot.ui.theme.PureWhite
import com.example.ai_based_medical_chatbot.ui.theme.SoftMint
import com.example.ai_based_medical_chatbot.ui.theme.TextDark
import com.example.ai_based_medical_chatbot.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CalendarViewMode {
    DAYS,
    MONTHS,
    YEARS
}

/**
 * Custom Compose-based DatePickerDialog providing reliable, user-friendly
 * Year, Month, and Day selection without native Android DatePicker fragmentation.
 *
 * Features:
 * - Direct Year selection: Tapping Year header opens Year grid auto-scrolled to current selection.
 * - Direct Month selection: Tapping Month header opens 12-Month grid for instant selection.
 * - Future dates strictly disabled and non-clickable.
 * - Fluid transitions back to Days view upon picking Month or Year.
 * - Conforms to MedAssist AI DeepTeal, SageGreen, Mint, and Ivory theme.
 */
@Composable
fun MedAssistDatePickerDialog(
    initialDate: Date?,
    onDateSelected: (day: Int, month: Int, year: Int) -> Unit,
    onDismiss: () -> Unit,
    t: (String) -> String,
    title: String? = null,
    allowFutureDates: Boolean = false
) {
    val today = remember { Calendar.getInstance() }
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    val maxYear = if (allowFutureDates) todayYear + 20 else todayYear

    val baseCalendar = remember {
        Calendar.getInstance().apply {
            if (initialDate != null && (allowFutureDates || !initialDate.after(today.time))) {
                time = initialDate
            } else {
                // Default to 20 years ago for adult birth date default if unset and future not allowed
                if (allowFutureDates) {
                    time = today.time
                } else {
                    set(Calendar.YEAR, todayYear - 20)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
    }

    var selectedYear by remember { mutableIntStateOf(baseCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(baseCalendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(baseCalendar.get(Calendar.DAY_OF_MONTH)) }

    var viewMode by remember { mutableStateOf(CalendarViewMode.DAYS) }

    val monthNames = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }

    val monthShortNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val yearList = remember(maxYear) { (1900..maxYear).toList() }
    val yearGridState = rememberLazyGridState()

    // Auto-scroll to selected year when switching to YEARS mode
    LaunchedEffect(viewMode) {
        if (viewMode == CalendarViewMode.YEARS) {
            val index = yearList.indexOf(selectedYear)
            if (index >= 0) {
                // Scroll with offset so it lands nicely in the middle
                yearGridState.scrollToItem((index - 3).coerceAtLeast(0))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(26.dp), spotColor = DeepTeal.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.5.dp, Mint)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row: Title & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title ?: t("bmi_select_date"),
                            color = DarkTeal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(SoftMint, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = t("btn_cancel"),
                                tint = DarkTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Date Display Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftMint, RoundedCornerShape(14.dp))
                            .border(BorderStroke(1.dp, Mint), RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp, horizontal = 14.dp)
                    ) {
                        val displayFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
                        val displayCal = Calendar.getInstance().apply {
                            set(selectedYear, selectedMonth, selectedDay)
                        }
                        Text(
                            text = displayFormat.format(displayCal.time),
                            color = DeepTeal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Month and Year Switcher Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Month button (disabled if in YEAR/MONTH view or at 1900 Jan)
                        IconButton(
                            onClick = {
                                if (selectedMonth > 0) {
                                    selectedMonth--
                                } else if (selectedYear > 1900) {
                                    selectedYear--
                                    selectedMonth = 11
                                }
                                val maxDays = getDaysInMonth(selectedYear, selectedMonth)
                                if (selectedDay > maxDays) selectedDay = maxDays
                            },
                            enabled = viewMode == CalendarViewMode.DAYS && !(selectedYear == 1900 && selectedMonth == 0),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = if (viewMode == CalendarViewMode.DAYS) DarkTeal else TextMuted.copy(alpha = 0.4f)
                            )
                        }

                        // Month & Year Selector Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month selector pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (viewMode == CalendarViewMode.MONTHS) DeepTeal else Ivory)
                                    .border(BorderStroke(1.dp, Mint), RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewMode = if (viewMode == CalendarViewMode.MONTHS) CalendarViewMode.DAYS else CalendarViewMode.MONTHS
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${monthNames[selectedMonth]} ▾",
                                    color = if (viewMode == CalendarViewMode.MONTHS) PureWhite else DarkTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Year selector pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (viewMode == CalendarViewMode.YEARS) DeepTeal else Ivory)
                                    .border(BorderStroke(1.dp, Mint), RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewMode = if (viewMode == CalendarViewMode.YEARS) CalendarViewMode.DAYS else CalendarViewMode.YEARS
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$selectedYear ▾",
                                    color = if (viewMode == CalendarViewMode.YEARS) PureWhite else DarkTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Next Month button (disabled if in future and not allowed)
                        val isNextDisabled = viewMode != CalendarViewMode.DAYS ||
                                (!allowFutureDates && (
                                    (selectedYear > todayYear) ||
                                    (selectedYear == todayYear && selectedMonth >= todayMonth)
                                )) ||
                                (allowFutureDates && selectedYear >= maxYear && selectedMonth >= 11)

                        IconButton(
                            onClick = {
                                if (selectedMonth < 11) {
                                    selectedMonth++
                                } else if (selectedYear < maxYear) {
                                    selectedYear++
                                    selectedMonth = 0
                                }
                                val maxDays = getDaysInMonth(selectedYear, selectedMonth)
                                if (selectedDay > maxDays) selectedDay = maxDays
                            },
                            enabled = !isNextDisabled,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = if (!isNextDisabled) DarkTeal else TextMuted.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DYNAMIC BODY: DAYS, MONTHS, OR YEARS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        when (viewMode) {
                            CalendarViewMode.DAYS -> {
                                DaysGridView(
                                    year = selectedYear,
                                    month = selectedMonth,
                                    selectedDay = selectedDay,
                                    todayYear = todayYear,
                                    todayMonth = todayMonth,
                                    todayDay = todayDay,
                                    allowFutureDates = allowFutureDates,
                                    onDayClick = { day ->
                                        selectedDay = day
                                    }
                                )
                            }

                            CalendarViewMode.MONTHS -> {
                                MonthsGridView(
                                    selectedMonth = selectedMonth,
                                    isCurrentYear = selectedYear == todayYear,
                                    todayMonth = todayMonth,
                                    monthShortNames = monthShortNames,
                                    allowFutureDates = allowFutureDates,
                                    onMonthClick = { month ->
                                        selectedMonth = month
                                        val maxDays = getDaysInMonth(selectedYear, selectedMonth)
                                        if (selectedDay > maxDays) selectedDay = maxDays
                                        viewMode = CalendarViewMode.DAYS
                                    }
                                )
                            }

                            CalendarViewMode.YEARS -> {
                                YearsGridView(
                                    yearList = yearList,
                                    selectedYear = selectedYear,
                                    gridState = yearGridState,
                                    onYearClick = { year ->
                                        selectedYear = year
                                        // If switching year makes current selected month in the future, clamp it
                                        if (!allowFutureDates && selectedYear == todayYear && selectedMonth > todayMonth) {
                                            selectedMonth = todayMonth
                                        }
                                        val maxDays = getDaysInMonth(selectedYear, selectedMonth)
                                        if (selectedDay > maxDays) selectedDay = maxDays
                                        viewMode = CalendarViewMode.DAYS
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, Mint),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                        ) {
                            Text(
                                text = t("btn_cancel"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                onDateSelected(selectedDay, selectedMonth + 1, selectedYear)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepTeal,
                                contentColor = PureWhite
                            )
                        ) {
                            Text(
                                text = t("btn_confirm"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysGridView(
    year: Int,
    month: Int,
    selectedDay: Int,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    allowFutureDates: Boolean = false,
    onDayClick: (Int) -> Unit
) {
    val daysInMonth = getDaysInMonth(year, month)
    val firstDayOfWeek = getFirstDayOfWeek(year, month) // 1 = Sunday, 7 = Saturday

    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.fillMaxSize()) {
        // Weekday initials
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days Grid (up to 6 rows x 7 cols = 42 cells)
        val offset = firstDayOfWeek - 1
        val totalCells = ((offset + daysInMonth + 6) / 7) * 7

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(totalCells) { cellIndex ->
                val day = cellIndex - offset + 1
                if (day in 1..daysInMonth) {
                    val isFuture = !allowFutureDates && (
                            (year > todayYear) ||
                            (year == todayYear && month > todayMonth) ||
                            (year == todayYear && month == todayMonth && day > todayDay)
                    )

                    val isSelected = day == selectedDay

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> DeepTeal
                                    else -> PureWhite
                                }
                            )
                            .clickable(enabled = !isFuture) {
                                onDayClick(day)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            color = when {
                                isSelected -> PureWhite
                                isFuture -> TextMuted.copy(alpha = 0.3f)
                                else -> DarkTeal
                            },
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthsGridView(
    selectedMonth: Int,
    isCurrentYear: Boolean,
    todayMonth: Int,
    monthShortNames: List<String>,
    allowFutureDates: Boolean = false,
    onMonthClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(12) { monthIndex ->
            val isSelected = monthIndex == selectedMonth
            val isFuture = !allowFutureDates && isCurrentYear && monthIndex > todayMonth

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> DeepTeal
                            else -> SoftMint
                        }
                    )
                    .border(
                        BorderStroke(1.dp, if (isSelected) DeepTeal else Mint),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isFuture) {
                        onMonthClick(monthIndex)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monthShortNames[monthIndex],
                    color = when {
                        isSelected -> PureWhite
                        isFuture -> TextMuted.copy(alpha = 0.35f)
                        else -> DarkTeal
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun YearsGridView(
    yearList: List<Int>,
    selectedYear: Int,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onYearClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(yearList) { year ->
            val isSelected = year == selectedYear

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) DeepTeal else Ivory
                    )
                    .border(
                        BorderStroke(1.dp, if (isSelected) DeepTeal else Mint),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onYearClick(year)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = year.toString(),
                    color = if (isSelected) PureWhite else DarkTeal,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return cal.get(Calendar.DAY_OF_WEEK)
}
