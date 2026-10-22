package com.vythera.stride.ui.screens.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.vythera.stride.ui.components.LiquidGlassBackground
import com.vythera.stride.ui.components.liquidGlassContainerColor
import com.vythera.stride.ui.components.liquidGlassEffect
import dev.chrisbanes.haze.HazeState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.vythera.stride.data.db.WalkSessionEntity
import com.vythera.stride.model.UnitSystem
import kotlinx.coroutines.launch
import com.vythera.stride.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vythera.stride.Graph
import com.vythera.stride.data.db.DailySummaryEntity
import com.vythera.stride.model.StridePrefs
import com.vythera.stride.ui.components.DayBar
import com.vythera.stride.ui.components.MonthHeatmap
import com.vythera.stride.ui.components.WeeklyBarChart
import com.vythera.stride.ui.components.entrance
import com.vythera.stride.util.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryViewModel : ViewModel() {
    private val repo = Graph.repository

    data class HistoryData(
        val summaries: List<DailySummaryEntity> = emptyList(),
        val prefs: StridePrefs = StridePrefs()
    )

    val data: StateFlow<HistoryData> = combine(
        repo.observeRange(LocalDate.now().minusDays(370), LocalDate.now()),
        Graph.prefs.prefs
    ) { summaries, prefs -> HistoryData(summaries, prefs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryData())

    val walkSessions: StateFlow<List<WalkSessionEntity>> = repo.observeWalkSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repo.deleteWalkSession(id)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    hazeState: HazeState? = null,
    viewModel: HistoryViewModel = viewModel()
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val liquidGlassEnabled = data.prefs.liquidGlass
    val walkSessions by viewModel.walkSessions.collectAsStateWithLifecycle()
    var monthMode by rememberSaveable { mutableStateOf(false) }
    var weekOffset by rememberSaveable { mutableStateOf(0) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    val byDay = remember(data.summaries, data.prefs.dailyGoal) {
        data.summaries.associate { s ->
            val date = LocalDate.ofEpochDay(s.epochDay)
            date to DayBar(date, s.steps, if (s.goal > 0) s.goal else data.prefs.dailyGoal)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackground(
            hazeState = hazeState,
            enabled = liquidGlassEnabled
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.nav_history),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.entrance(0)
            )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.entrance(1)) {
            ToggleButton(checked = !monthMode, onCheckedChange = { if (it) monthMode = false }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.week))
            }
            ToggleButton(checked = monthMode, onCheckedChange = { if (it) monthMode = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.month))
            }
        }

        AnimatedContent(
            targetState = monthMode,
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f) +
                    slideInVertically { it / 20 }) togetherWith fadeOut(tween(140))
            },
            modifier = Modifier.entrance(2),
            label = "historyMode"
        ) { isMonth ->
            if (!isMonth) {
                WeekSection(
                    byDay = byDay,
                    weekOffset = weekOffset,
                    goal = data.prefs.dailyGoal,
                    selected = selected,
                    onSelect = { selected = it },
                    onOffsetChange = { weekOffset = it },
                    hazeState = hazeState,
                    liquidGlass = liquidGlassEnabled
                )
            } else {
                MonthSection(
                    byDay = byDay,
                    month = month,
                    selected = selected,
                    onSelect = { selected = it },
                    onMonthChange = { month = it },
                    hazeState = hazeState,
                    liquidGlass = liquidGlassEnabled
                )
            }
        }

        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically { it / 12 }) togetherWith fadeOut(tween(120))
            },
            modifier = Modifier.entrance(3),
            label = "dayDetail"
        ) { date ->
            if (date != null) {
                DayDetailCard(
                    date = date,
                    bar = byDay[date],
                    prefs = data.prefs,
                    hazeState = hazeState,
                    liquidGlass = liquidGlassEnabled
                )
            }
        }

        TotalsCard(
            byDay = byDay,
            monthMode = monthMode,
            month = month,
            weekOffset = weekOffset,
            hazeState = hazeState,
            liquidGlass = liquidGlassEnabled,
            modifier = Modifier.entrance(4)
        )

        WalkSessionsSection(
            sessions = walkSessions,
            unit = data.prefs.unit,
            onDelete = { viewModel.deleteSession(it) },
            hazeState = hazeState,
            liquidGlass = liquidGlassEnabled,
            modifier = Modifier.entrance(5)
        )

        Spacer(Modifier.height(120.dp))
    }
    }
}

private fun weekStart(offset: Int): LocalDate {
    val today = LocalDate.now()
    val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    return monday.plusWeeks(offset.toLong())
}

@Composable
private fun WeekSection(
    byDay: Map<LocalDate, DayBar>,
    weekOffset: Int,
    goal: Int,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onOffsetChange: (Int) -> Unit,
    hazeState: HazeState? = null,
    liquidGlass: Boolean = false
) {
    val start = weekStart(weekOffset)
    val days = (0..6).map { i ->
        val date = start.plusDays(i.toLong())
        byDay[date] ?: DayBar(date, 0, goal)
    }
    val cardShape = RoundedCornerShape(28.dp)
    Surface(
        shape = cardShape,
        color = liquidGlassContainerColor(liquidGlass, MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .liquidGlassEffect(hazeState, liquidGlass, cardShape)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onOffsetChange(weekOffset - 1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous))
                }
                Text(
                    text = if (weekOffset == 0) stringResource(R.string.this_week)
                    else "${start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${
                        start.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))
                    }",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onOffsetChange(weekOffset + 1) }, enabled = weekOffset < 0) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next))
                }
            }
            WeeklyBarChart(days = days, selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun MonthSection(
    byDay: Map<LocalDate, DayBar>,
    month: YearMonth,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    hazeState: HazeState? = null,
    liquidGlass: Boolean = false
) {
    val cardShape = RoundedCornerShape(28.dp)
    Surface(
        shape = cardShape,
        color = liquidGlassContainerColor(liquidGlass, MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .liquidGlassEffect(hazeState, liquidGlass, cardShape)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous))
                }
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = { onMonthChange(month.plusMonths(1)) },
                    enabled = month < YearMonth.now()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next))
                }
            }
            MonthHeatmap(month = month, stepsByDay = byDay, selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun DayDetailCard(
    date: LocalDate,
    bar: DayBar?,
    prefs: StridePrefs,
    hazeState: HazeState? = null,
    liquidGlass: Boolean = false
) {
    val steps = bar?.steps ?: 0L
    val distance = steps * prefs.strideMeters
    val calories = steps * 0.04 * (prefs.weightKg / 70.0)
    val cardShape = RoundedCornerShape(28.dp)
    Surface(
        shape = cardShape,
        color = liquidGlassContainerColor(liquidGlass, MaterialTheme.colorScheme.primaryContainer, 0.75f),
        modifier = Modifier
            .liquidGlassEffect(hazeState, liquidGlass, cardShape)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.day_steps, Formatters.steps(steps)),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DetailStat(stringResource(R.string.distance), Formatters.distance(distance, prefs.unit))
                DetailStat(stringResource(R.string.calories), Formatters.calories(calories))
                DetailStat(
                    stringResource(R.string.active_min),
                    stringResource(R.string.active_short, (steps / 110).toInt())
                )
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun TotalsCard(
    byDay: Map<LocalDate, DayBar>,
    monthMode: Boolean,
    month: YearMonth,
    weekOffset: Int,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    liquidGlass: Boolean = false
) {
    val days = if (monthMode) {
        byDay.values.filter { YearMonth.from(it.date) == month }
    } else {
        val start = weekStart(weekOffset)
        byDay.values.filter { !it.date.isBefore(start) && !it.date.isAfter(start.plusDays(6)) }
    }
    val total = days.sumOf { it.steps }
    val best = days.maxByOrNull { it.steps }
    val goalsMet = days.count { it.goalMet }

    val cardShape = RoundedCornerShape(28.dp)
    Surface(
        shape = cardShape,
        color = liquidGlassContainerColor(liquidGlass, MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .liquidGlassEffect(hazeState, liquidGlass, cardShape)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TotalStat(stringResource(R.string.total), Formatters.compactSteps(total))
            TotalStat(stringResource(R.string.best_day), best?.let { Formatters.compactSteps(it.steps) } ?: "—")
            TotalStat(stringResource(R.string.goals_met), "$goalsMet")
        }
    }
}

@Composable
private fun TotalStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WalkSessionsSection(
    sessions: List<WalkSessionEntity>,
    unit: UnitSystem,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    liquidGlass: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.walk_sessions),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (sessions.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${sessions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            val emptyCardShape = RoundedCornerShape(24.dp)
            Surface(
                shape = emptyCardShape,
                color = liquidGlassContainerColor(liquidGlass, MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .liquidGlassEffect(hazeState, liquidGlass, emptyCardShape)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_walk_sessions),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.walk_sessions_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            sessions.forEach { session ->
                WalkSessionCard(
                    session = session,
                    unit = unit,
                    onDelete = { onDelete(session.id) },
                    hazeState = hazeState,
                    liquidGlass = liquidGlass
                )
            }
        }
    }
}

@Composable
private fun WalkSessionCard(
    session: WalkSessionEntity,
    unit: UnitSystem,
    onDelete: () -> Unit,
    hazeState: HazeState? = null,
    liquidGlass: Boolean = false
) {
    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        shape = cardShape,
        color = liquidGlassContainerColor(liquidGlass, MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .liquidGlassEffect(hazeState, liquidGlass, cardShape)
            .fillMaxWidth()
            .testTag("walk_session_card_${session.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatSessionDate(session.startTimeMillis),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_walk_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_session),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WalkCardStat(
                    icon = Icons.Rounded.DirectionsWalk,
                    value = Formatters.steps(session.steps),
                    label = stringResource(R.string.session_steps),
                    tint = MaterialTheme.colorScheme.primary
                )
                WalkCardStat(
                    icon = Icons.Rounded.Timer,
                    value = formatSessionDuration(session.durationMillis),
                    label = stringResource(R.string.session_duration),
                    tint = MaterialTheme.colorScheme.secondary
                )
                WalkCardStat(
                    icon = Icons.Rounded.Route,
                    value = Formatters.distance(session.distanceMeters, unit),
                    label = stringResource(R.string.session_distance),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun WalkCardStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatSessionDate(epochMillis: Long): String {
    val instant = java.time.Instant.ofEpochMilli(epochMillis)
    val zone = java.time.ZoneId.systemDefault()
    val localDateTime = java.time.LocalDateTime.ofInstant(instant, zone)
    val today = LocalDate.now()
    val sessionDate = localDateTime.toLocalDate()
    val timeStr = localDateTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return when (sessionDate) {
        today -> "Today · $timeStr"
        today.minusDays(1) -> "Yesterday · $timeStr"
        else -> localDateTime.format(DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.getDefault()))
    }
}

private fun formatSessionDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
