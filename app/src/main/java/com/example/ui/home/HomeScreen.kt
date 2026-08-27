package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.DoseStatus
import com.example.model.MealInstruction
import com.example.model.TodayDoseItem
import com.example.ui.components.AdMobBanner
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.MedicineTypeIcon
import com.example.ui.components.PermissionBanner
import com.example.ui.components.SnoozeBottomSheet
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MedSuccessGreen
import com.example.ui.theme.MedWarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddMedicine: () -> Unit,
    onNavigateToMedicineDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val doseItems by viewModel.doseItems.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    var snoozeTargetItem by remember { mutableStateOf<TodayDoseItem?>(null) }

    val filteredItems = remember(doseItems, selectedFilter) {
        when (selectedFilter) {
            DoseFilter.ALL -> doseItems
            DoseFilter.UPCOMING -> doseItems.filter { it.effectiveStatus == DoseStatus.UPCOMING }
            DoseFilter.TAKEN -> doseItems.filter { it.effectiveStatus == DoseStatus.TAKEN }
            DoseFilter.MISSED -> doseItems.filter { it.effectiveStatus == DoseStatus.MISSED }
            DoseFilter.SKIPPED -> doseItems.filter { it.effectiveStatus == DoseStatus.SKIPPED }
        }
    }

    val totalCount = doseItems.size
    val takenCount = doseItems.count { it.effectiveStatus == DoseStatus.TAKEN }
    val adherencePercent = if (totalCount > 0) (takenCount * 100) / totalCount else 0

    val isSelectedToday = remember(selectedDate) {
        val today = Calendar.getInstance()
        selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    Scaffold(
        modifier = modifier.testTag("home_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSelectedToday) "Today's Schedule" else formatHeaderDate(selectedDate),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(selectedDate.time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (!isSelectedToday) {
                        IconButton(
                            onClick = { viewModel.goToToday() },
                            modifier = Modifier.testTag("go_to_today_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Go to Today",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddMedicine,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_medicine_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medicine"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Permission Warning Banner if permissions missing
            item {
                PermissionBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 2. Horizontal Date Strip
            item {
                DateStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 3. Adherence Summary Card
            if (doseItems.isNotEmpty()) {
                item {
                    AdherenceScoreCard(
                        takenCount = takenCount,
                        totalCount = totalCount,
                        percent = adherencePercent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // 4. Filter Chips
            item {
                FilterChipRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) },
                    counts = mapOf(
                        DoseFilter.ALL to doseItems.size,
                        DoseFilter.UPCOMING to doseItems.count { it.effectiveStatus == DoseStatus.UPCOMING },
                        DoseFilter.TAKEN to takenCount,
                        DoseFilter.MISSED to doseItems.count { it.effectiveStatus == DoseStatus.MISSED },
                        DoseFilter.SKIPPED to doseItems.count { it.effectiveStatus == DoseStatus.SKIPPED }
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 5. Categorized Dose Items or Empty State
            if (filteredItems.isEmpty()) {
                item {
                    EmptyDosesCard(
                        filter = selectedFilter,
                        isSelectedToday = isSelectedToday,
                        onAddClick = onNavigateToAddMedicine,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            } else {
                // Group doses by time periods: Morning, Afternoon, Evening, Night
                val periods = listOf(
                    TimePeriod.MORNING to filteredItems.filter { it.reminderTime.hour in 4..11 },
                    TimePeriod.AFTERNOON to filteredItems.filter { it.reminderTime.hour in 12..16 },
                    TimePeriod.EVENING to filteredItems.filter { it.reminderTime.hour in 17..20 },
                    TimePeriod.NIGHT to filteredItems.filter { it.reminderTime.hour >= 21 || it.reminderTime.hour < 4 }
                )

                periods.forEach { (period, items) ->
                    if (items.isNotEmpty()) {
                        item(key = "header_${period.name}") {
                            PeriodHeader(
                                period = period,
                                count = items.size,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(
                            items = items,
                            key = { it.idKey }
                        ) { doseItem ->
                            DoseItemCard(
                                doseItem = doseItem,
                                is24Hour = userSettings.is24HourFormat,
                                onTakeNow = { viewModel.markDoseTaken(doseItem) },
                                onSnoozeClick = { snoozeTargetItem = doseItem },
                                onSkip = { viewModel.markDoseSkipped(doseItem) },
                                onUndo = { viewModel.undoDoseAction(doseItem) },
                                onClick = { onNavigateToMedicineDetails(doseItem.medicine.id) },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("dose_item_${doseItem.medicine.id}")
                            )
                        }
                    }
                }
            }

            // 6. Medical Disclaimer Card at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
                MedicalDisclaimerCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 7. AdMob Banner
            item {
                AdMobBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    // Snooze Bottom Sheet Modal
    snoozeTargetItem?.let { item ->
        SnoozeBottomSheet(
            medicineName = item.medicine.name,
            onSnoozeSelected = { mins ->
                viewModel.snoozeDose(item, mins)
                snoozeTargetItem = null
            },
            onDismiss = { snoozeTargetItem = null }
        )
    }
}

enum class TimePeriod(val title: String, val icon: ImageVector) {
    MORNING("Morning", Icons.Default.WbSunny),
    AFTERNOON("Afternoon", Icons.Default.WbSunny),
    EVENING("Evening", Icons.Default.WbTwilight),
    NIGHT("Night", Icons.Default.Nightlight)
}

@Composable
private fun PeriodHeader(
    period: TimePeriod,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = period.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = period.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DoseItemCard(
    doseItem: TodayDoseItem,
    is24Hour: Boolean,
    onTakeNow: () -> Unit,
    onSnoozeClick: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = doseItem.effectiveStatus
    val medicine = doseItem.medicine
    val reminderTime = doseItem.reminderTime

    val cardBorderColor by animateColorAsState(
        targetValue = when (status) {
            DoseStatus.TAKEN -> MedSuccessGreen.copy(alpha = 0.4f)
            DoseStatus.MISSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            DoseStatus.SNOOZED -> MedWarningAmber.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (cardBorderColor != Color.Transparent) {
            androidx.compose.foundation.BorderStroke(1.5.dp, cardBorderColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Medicine Type Icon Badge
                MedicineTypeIcon(
                    medicineType = medicine.medicineType,
                    colorHex = medicine.colorHex,
                    size = 48.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        StatusBadge(status = status)
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = medicine.fullDosage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reminderTime.format(is24Hour),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (medicine.mealInstruction != MealInstruction.NONE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•  ${medicine.mealInstruction.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons
            Spacer(modifier = Modifier.height(14.dp))

            when (status) {
                DoseStatus.UPCOMING, DoseStatus.MISSED, DoseStatus.SNOOZED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Take Now Button
                        Button(
                            onClick = onTakeNow,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp)
                                .testTag("take_now_btn_${medicine.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MedSuccessGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Take Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Snooze Button
                        OutlinedButton(
                            onClick = onSnoozeClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("snooze_btn_${medicine.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = MedWarningAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Snooze",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Skip Button
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(42.dp)
                                .testTag("skip_btn_${medicine.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Skip",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                DoseStatus.TAKEN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✓ Completed" + if (doseItem.log?.getFormattedActionTime()?.isNotEmpty() == true) " at ${doseItem.log.getFormattedActionTime()}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MedSuccessGreen
                        )

                        OutlinedButton(
                            onClick = onUndo,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("undo_btn_${medicine.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateLeft,
                                contentDescription = "Undo",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 11.sp)
                        }
                    }
                }

                DoseStatus.SKIPPED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "↷ Skipped this dose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = onUndo,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("undo_btn_${medicine.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateLeft,
                                contentDescription = "Undo",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdherenceScoreCard(
    takenCount: Int,
    totalCount: Int,
    percent: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                CircularProgressIndicator(
                    progress = { if (totalCount > 0) takenCount.toFloat() / totalCount else 0f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = if (percent == 100) "All done for today! 🎉" else "Daily Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$takenCount of $totalCount doses taken",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DateStrip(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Generate list of days from -4 to +10 days
    val days = remember {
        val list = mutableListOf<Calendar>()
        for (i in -4..10) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }
            list.add(cal)
        }
        list
    }

    LaunchedEffect(Unit) {
        // Scroll to center around today (index 4)
        listState.scrollToItem(2)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { date ->
            val isSelected = date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)

            val today = Calendar.getInstance()
            val isToday = date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date.time)
            val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(date.time)

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDateSelected(date) }
                    .testTag("date_chip_$dayNum"),
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selectedFilter: DoseFilter,
    onFilterSelected: (DoseFilter) -> Unit,
    counts: Map<DoseFilter, Int>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DoseFilter.entries) { filter ->
            val count = counts[filter] ?: 0
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text("${filter.displayName} ($count)", fontSize = 12.sp)
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun EmptyDosesCard(
    filter: DoseFilter,
    isSelectedToday: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.med_banner_1787386555101),
                contentDescription = "Empty Medicines",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (filter == DoseFilter.ALL) {
                    if (isSelectedToday) "No medicines scheduled for today" else "No medicines on this day"
                } else {
                    "No ${filter.displayName.lowercase()} doses"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Keep your health on track by setting up your daily prescriptions and supplements.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_add_medicine_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Medicine")
            }
        }
    }
}

private fun formatHeaderDate(calendar: Calendar): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(calendar.time)
}
