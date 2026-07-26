package com.example.test_ai_project.feature.home.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.CurrentWeather
import com.example.test_ai_project.core.model.WeatherCondition
import com.example.test_ai_project.core.ui.component.LoadingState
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.core.ui.theme.VaultCharcoal
import com.example.test_ai_project.core.ui.theme.VaultHairline
import com.example.test_ai_project.core.ui.theme.VaultMistDeep
import com.example.test_ai_project.core.ui.theme.VaultStone
import com.example.test_ai_project.core.ui.theme.VaultTeal
import com.example.test_ai_project.feature.home.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel, or the
 * permission API.
 */
@Composable
internal fun WeatherRoute(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        // The grants are not inspected. Whatever the user decided, the next step is the same —
        // try to build a reading — and the ViewModel explains why if it cannot.
        onResult = { viewModel.onPermissionsResolved() },
    )

    /**
     * Whether this screen has already had its one shot at the system dialogs.
     *
     * `rememberSaveable`, so switching tabs and coming back does not re-prompt. After a refusal
     * the page offers an Allow button instead, which is the only way a second dialog can appear.
     */
    var hasRequestedPermissions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasRequestedPermissions) return@LaunchedEffect
        hasRequestedPermissions = true

        val missing = context.missingPermissions()
        if (missing.isEmpty()) {
            viewModel.onPermissionsResolved()
        } else {
            permissionLauncher.launch(missing)
        }
    }

    WeatherScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRequestPermission = { permissionLauncher.launch(RequiredPermissions) },
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
internal fun WeatherScreen(
    uiState: WeatherUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // A strip, not a spinner over the page: the cached reading stays readable and scrollable
        // while a refetch runs, which is the entire point of caching it.
        if (uiState.isLoading && uiState.isCached) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.messageRes != null) {
            MessageBanner(
                messageRes = uiState.messageRes,
                // A refused permission is fixed by a grant, not by trying the same call again —
                // so the button offers the thing that would actually work.
                actionRes = if (uiState.isPermissionRequestable) {
                    R.string.weather_allow
                } else {
                    R.string.weather_retry
                },
                onAction = if (uiState.isPermissionRequestable) onRequestPermission else onRetry,
                onDismiss = onDismissMessage,
            )
        }

        when {
            uiState.isInitialLoad -> LoadingState()

            uiState.isEmpty -> EmptyState(onRetry = onRetry)

            else -> WeatherContent(uiState = uiState, onRefresh = onRefresh)
        }
    }
}

@Composable
private fun WeatherContent(
    uiState: WeatherUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatTime = rememberClockFormatter(zoneOffsetSeconds = uiState.zoneOffsetSeconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        OfflineStatusStrip(
            lastUpdatedLabel = uiState.lastUpdatedEpochMillis?.let(formatTime),
            isStale = uiState.isStale,
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Non-null whenever this composable is reached: `isEmpty` and `isInitialLoad` cover
            // every state where it is not, and both are handled a frame up.
            uiState.current?.let { current ->
                CurrentConditionsCard(place = uiState.place, current = current)

                if (uiState.hourly.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HourlyForecastCard(columns = uiState.hourly, formatTime = formatTime)
                }

                Spacer(modifier = Modifier.height(12.dp))
                DetailsCard(current = current)
            }

            if (uiState.daily.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                DailyForecastCard(rows = uiState.daily)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LocationRow(
                label = uiState.place,
                isRefreshing = uiState.isLoading,
                onRefresh = onRefresh,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * The strip under the app bar: what the page is showing, and how old it is.
 *
 * The age line does more work here than on the prayer page. A cached timetable is still correct
 * a week later, so its timestamp is reassurance; a cached temperature is only the last one known,
 * so past [WeatherUiState.isStale] this stops reporting a time and starts warning about one.
 */
@Composable
private fun OfflineStatusStrip(
    lastUpdatedLabel: String?,
    isStale: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                // Amber would be the obvious choice for stale and is the wrong
                                // one: the dot reports that the data is on disk, which is just as
                                // true when it is old. The wording beside it carries the age.
                                color = if (isStale) VaultStone else VaultTeal,
                                shape = CircleShape,
                            ),
                    )
                    Text(
                        text = stringResource(id = R.string.weather_cached_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultStone,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                if (lastUpdatedLabel != null) {
                    Text(
                        text = stringResource(
                            id = if (isStale) {
                                R.string.weather_stale
                            } else {
                                R.string.weather_last_update
                            },
                            lastUpdatedLabel,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultStone,
                    )
                }
            }

            HorizontalDivider(color = VaultHairline)
        }
    }
}

/** The headline block: where, how warm, what it looks like, and the day's range. */
@Composable
private fun CurrentConditionsCard(
    place: String?,
    current: CurrentWeather,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    // Merged so a screen reader announces the place and the reading as one
                    // sentence rather than reading "-2°" as an orphaned number.
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = stringResource(id = R.string.weather_current_location),
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultStone,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = place.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_place),
                            contentDescription = null,
                            tint = VaultStone,
                            modifier = Modifier.padding(start = 6.dp).size(13.dp),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(
                            text = current.temperatureCelsius.asDegrees(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = current.conditionCaption(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = VaultStone,
                            modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = current.condition.iconRes(current.isNight)),
                    // Decorative: the caption beside it already names the condition in the
                    // provider's own words, and repeating it would double every announcement.
                    contentDescription = null,
                    tint = VaultStone.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 8.dp).size(64.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TemperatureRangeRow(current = current)
        }
    }
}

/** High, low and feels-like, as three equal cells on a recessed strip. */
@Composable
private fun TemperatureRangeRow(
    current: CurrentWeather,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(vertical = 12.dp)) {
            RangeCell(
                labelRes = R.string.weather_high,
                value = current.highCelsius,
                modifier = Modifier.weight(1f),
            )
            RangeCell(
                labelRes = R.string.weather_low,
                value = current.lowCelsius,
                modifier = Modifier.weight(1f),
            )
            RangeCell(
                labelRes = R.string.weather_feels_like,
                value = current.feelsLikeCelsius,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RangeCell(
    @StringRes labelRes: Int,
    value: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = VaultStone,
        )
        Text(
            text = value.asDegrees(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * The short-range strip.
 *
 * Horizontally scrollable rather than sized to fit: the window is 24 hours in three-hour steps,
 * which is eight columns plus "now", and squeezing nine columns into a phone width would leave
 * each one too narrow for a two-digit negative temperature.
 */
@Composable
private fun HourlyForecastCard(
    columns: List<HourlyColumn>,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            SectionHeading(
                iconRes = R.drawable.ic_clock_small,
                titleRes = R.string.weather_hourly_title,
                trailingRes = R.string.weather_hourly_window,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                columns.forEach { column ->
                    HourlyColumnItem(column = column, formatTime = formatTime)
                }
            }
        }
    }
}

@Composable
private fun HourlyColumnItem(
    column: HourlyColumn,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(56.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (column.isNow) {
                stringResource(id = R.string.weather_hourly_now)
            } else {
                formatTime(column.startEpochMillis)
            },
            style = MaterialTheme.typography.labelSmall,
            // The leading column is where the user is standing, so it is the one anchor the rest
            // of the strip is read against.
            fontWeight = if (column.isNow) FontWeight.Bold else FontWeight.Normal,
            color = if (column.isNow) VaultTeal else VaultStone,
        )
        Icon(
            painter = painterResource(id = column.condition.iconRes(column.isNight)),
            contentDescription = stringResource(id = column.condition.labelRes),
            tint = VaultTeal,
            modifier = Modifier.padding(vertical = 8.dp).size(20.dp),
        )
        Text(
            text = column.temperatureCelsius.asDegrees(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Wind, humidity and visibility — the three readings the design calls out. */
@Composable
private fun DetailsCard(
    current: CurrentWeather,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.weather_details_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            DetailRow(
                iconRes = R.drawable.ic_wind,
                labelRes = R.string.weather_wind,
                // Converted here rather than in the model: metres per second is what the provider
                // reports and what the cache stores, and km/h is a presentation choice that
                // belongs with the string it is formatted into.
                value = stringResource(
                    id = R.string.weather_wind_value,
                    (current.windMetresPerSecond * KMH_PER_METRE_PER_SECOND).roundToInt(),
                ),
                modifier = Modifier.padding(top = 12.dp),
            )
            DetailRow(
                iconRes = R.drawable.ic_humidity,
                labelRes = R.string.weather_humidity,
                value = stringResource(
                    id = R.string.weather_humidity_value,
                    current.humidityPercent,
                ),
                modifier = Modifier.padding(top = 8.dp),
            )
            DetailRow(
                iconRes = R.drawable.ic_visibility,
                labelRes = R.string.weather_visibility,
                // An em dash, not "0 km": the provider omits this reading rather than sending
                // zero, and "0 km" would claim the user cannot see their own hand.
                value = current.visibilityMetres?.let { metres ->
                    stringResource(
                        id = R.string.weather_visibility_value,
                        metres / METRES_PER_KILOMETRE,
                    )
                } ?: stringResource(id = R.string.weather_value_unknown),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailRow(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = VaultStone,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** The multi-day list, each row placing its range on a track shared with the others. */
@Composable
private fun DailyForecastCard(
    rows: List<DailyRow>,
    modifier: Modifier = Modifier,
) {
    val formatWeekday = rememberWeekdayFormatter()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeading(
                iconRes = R.drawable.ic_calendar,
                titleRes = R.string.weather_daily_title,
                trailingRes = null,
            )

            rows.forEach { row ->
                DailyForecastRowItem(
                    row = row,
                    formatWeekday = formatWeekday,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyForecastRowItem(
    row: DailyRow,
    formatWeekday: (CalendarDate) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (row.isToday) {
                stringResource(id = R.string.weather_daily_today)
            } else {
                formatWeekday(row.date)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (row.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (row.isToday) MaterialTheme.colorScheme.onSurface else VaultStone,
            modifier = Modifier.width(52.dp),
        )

        Icon(
            // Always the daytime glyph: a row that stands for a whole day has no single answer to
            // "is the sun up", and the condition it shows is sampled at midday anyway.
            painter = painterResource(id = row.condition.iconRes(isNight = false)),
            contentDescription = stringResource(id = row.condition.labelRes),
            tint = VaultTeal,
            modifier = Modifier.size(18.dp),
        )

        TemperatureBar(
            startFraction = row.barStartFraction,
            endFraction = row.barEndFraction,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        Text(
            text = row.highCelsius.asDegrees(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
        Text(
            text = row.lowCelsius.asDegrees(),
            style = MaterialTheme.typography.bodyMedium,
            color = VaultStone,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}

/**
 * One day's range on the week's scale.
 *
 * Drawn rather than laid out. The alternative — a `Row` of weighted spacers around a filled `Box`
 * — cannot express a zero-width segment, because a `weight` of zero collapses the sibling layout,
 * and a flat day is a perfectly ordinary forecast. Here the segment is clamped to the track's own
 * height instead, so a day with no spread renders as a dot.
 */
@Composable
private fun TemperatureBar(
    startFraction: Float,
    endFraction: Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = VaultMistDeep
    val fillColor = VaultCharcoal

    Canvas(modifier = modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)

        val left = size.width * startFraction.coerceIn(0f, 1f)
        val right = size.width * endFraction.coerceIn(0f, 1f)
        val segmentWidth = (right - left).coerceAtLeast(size.height)

        drawRoundRect(
            color = fillColor,
            // Clamped so that widening a zero-length segment to the minimum cannot push it off
            // the right-hand end on the warmest day of the list.
            topLeft = Offset(x = left.coerceAtMost(size.width - segmentWidth), y = 0f),
            size = Size(width = segmentWidth, height = size.height),
            cornerRadius = radius,
        )
    }
}

/** A card heading: small icon, title, and an optional right-aligned caption. */
@Composable
private fun SectionHeading(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes trailingRes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = VaultStone,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        if (trailingRes != null) {
            Text(
                text = stringResource(id = trailingRes),
                style = MaterialTheme.typography.labelMedium,
                color = VaultTeal,
            )
        }
    }
}

/** Where these readings were taken, and the control to re-detect it. */
@Composable
private fun LocationRow(
    label: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_place),
                contentDescription = null,
                tint = VaultStone,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = label.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
            TextButton(onClick = onRefresh, enabled = !isRefreshing) {
                Text(
                    text = stringResource(
                        id = if (isRefreshing) {
                            R.string.weather_refreshing
                        } else {
                            R.string.weather_refresh
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRefreshing) VaultStone else VaultTeal,
                )
            }
        }
    }
}

/**
 * Advisory, not blocking. The fetch failed but the cache did not, so this sits above the readings
 * rather than replacing them.
 */
@Composable
private fun MessageBanner(
    @StringRes messageRes: Int,
    @StringRes actionRes: Int,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        TextButton(onClick = onAction) {
            Text(text = stringResource(id = actionRes))
        }
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(id = R.string.weather_dismiss))
        }
    }
}

/** Nothing has ever been cached — a first run that has not reached the network yet. */
@Composable
private fun EmptyState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.weather_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.weather_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = VaultStone,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(id = R.string.weather_retry))
        }
    }
}

/**
 * Formats an instant as a clock time, in the zone the readings belong to.
 *
 * Whether to write "3 PM" or "15:00" is a device setting, and overriding it to match a design
 * would be wrong on any phone set the other way.
 *
 * The zone is built from a raw offset rather than looked up by name, because an offset is all the
 * provider gives — see [com.example.test_ai_project.core.model.WeatherSnapshot.zoneOffsetSeconds].
 * A [SimpleTimeZone] with no DST rules is the honest representation of that: it cannot know about
 * a transition, and guessing a named zone from the offset would be wrong for half the year in half
 * the world.
 */
@Composable
private fun rememberClockFormatter(zoneOffsetSeconds: Int?): (Long) -> String {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = currentLocale()

    return remember(zoneOffsetSeconds, is24Hour, locale) {
        // "h a" rather than "h:mm a" for the 12-hour case: the forecast steps land on the hour, so
        // the minutes are always ":00" and would cost the strip a third of its column width to say
        // nothing.
        val formatter = SimpleDateFormat(if (is24Hour) "H:mm" else "h a", locale).apply {
            zoneOffsetSeconds?.let {
                timeZone = SimpleTimeZone(it * MILLIS_PER_SECOND, OpenWeatherZoneId)
            }
        }
        // An anonymous function, not a lambda literal: a `{ ... }` here would be parsed as a
        // trailing lambda passed to `apply` on the line above.
        fun(epochMillis: Long): String = formatter.format(Date(epochMillis))
    }
}

/**
 * Formats a calendar date as a short weekday name — "Mon".
 *
 * UTC on both sides, deliberately: a [CalendarDate] has no time and no place, so it is built into a
 * `Calendar` and read back in the same fixed zone. Involving a real one would only introduce a
 * midnight boundary for the weekday to be wrong across.
 */
@Composable
private fun rememberWeekdayFormatter(): (CalendarDate) -> String {
    val locale = currentLocale()

    return remember(locale) {
        val utc = TimeZone.getTimeZone("UTC")
        val formatter = SimpleDateFormat("EEE", locale).apply { timeZone = utc }

        fun(date: CalendarDate): String {
            val calendar = Calendar.getInstance(utc).apply {
                // Without this the calendar keeps the current time of day, which cannot change the
                // weekday but does make the result depend on when it was built.
                clear()
                set(date.year, date.month - 1, date.day)
            }
            return formatter.format(calendar.time)
        }
    }
}

/**
 * The locale, read so that a change to it recomposes what was formatted with it.
 *
 * `Locale.getDefault()` would be the obvious call and is the wrong one inside a composable: it
 * reads no observable state, so text formatted from it keeps the old locale until something
 * unrelated happens to recompose. See the same helper on the prayer screen for the full argument.
 */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * A temperature as whole degrees.
 *
 * Rounded here rather than in the ViewModel: the cache holds what the provider sent, and how many
 * digits of it are worth showing is a question about the design, not about the data.
 */
@Composable
private fun Double.asDegrees(): String =
    stringResource(id = R.string.weather_temperature, roundToInt())

/**
 * The condition caption under the temperature.
 *
 * Prefers the provider's own prose — "broken clouds" is more use than "Cloudy" — and falls back to
 * the group name when it sent none. Capitalised with the display locale rather than the default
 * one, so a Turkish device does not get a dotless capital in an English string.
 */
@Composable
private fun CurrentWeather.conditionCaption(): String {
    val locale = currentLocale()
    if (description.isBlank()) return stringResource(id = condition.labelRes)
    return description.replaceFirstChar { it.titlecase(locale) }
}

/** Sun and moon split only for a clear sky — a cloud looks the same at midnight. */
@DrawableRes
private fun WeatherCondition.iconRes(isNight: Boolean): Int = when (this) {
    WeatherCondition.Clear -> {
        if (isNight) R.drawable.ic_weather_clear_night else R.drawable.ic_weather_clear
    }

    WeatherCondition.Clouds -> R.drawable.ic_weather_clouds
    WeatherCondition.Rain, WeatherCondition.Drizzle -> R.drawable.ic_weather_rain
    WeatherCondition.Thunderstorm -> R.drawable.ic_weather_storm
    WeatherCondition.Snow -> R.drawable.ic_weather_snow
    WeatherCondition.Mist -> R.drawable.ic_weather_mist
    // A cloud is the least wrong thing to draw for a group this build does not know.
    WeatherCondition.Unknown -> R.drawable.ic_weather_clouds
}

@get:StringRes
private val WeatherCondition.labelRes: Int
    get() = when (this) {
        WeatherCondition.Clear -> R.string.weather_condition_clear
        WeatherCondition.Clouds -> R.string.weather_condition_clouds
        WeatherCondition.Rain -> R.string.weather_condition_rain
        WeatherCondition.Drizzle -> R.string.weather_condition_drizzle
        WeatherCondition.Thunderstorm -> R.string.weather_condition_thunderstorm
        WeatherCondition.Snow -> R.string.weather_condition_snow
        WeatherCondition.Mist -> R.string.weather_condition_mist
        WeatherCondition.Unknown -> R.string.weather_condition_unknown
    }

private fun Context.missingPermissions(): Array<String> = RequiredPermissions
    .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
    .toTypedArray()

/**
 * Coarse location only.
 *
 * Fine location would buy nothing: the repository treats a cached snapshot as valid within five
 * kilometres, and the provider's own model is coarser than that. Notifications are not asked for
 * either — unlike the prayer tab, nothing here fires an alert. A device that already granted fine
 * location for the map satisfies the coarse check anyway.
 */
private val RequiredPermissions: Array<String> =
    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)

private const val MILLIS_PER_SECOND = 1_000
private const val METRES_PER_KILOMETRE = 1_000
private const val KMH_PER_METRE_PER_SECOND = 3.6

/** Names the synthetic zone in a stack trace. Never parsed, and not a real zone id. */
private const val OpenWeatherZoneId = "OpenWeather"

// Previews are pinned to a fixed instant — 26 July 2026, 13:28 UTC — because the staleness banner
// and the hourly window are both functions of the clock, and a preview that read the real one
// would look different every time it was opened.

@Preview(showBackground = true, backgroundColor = 0xFFEFF4F3, heightDp = 1120)
@Composable
private fun WeatherScreenPreview() {
    AppTheme {
        WeatherScreen(
            uiState = previewUiState(),
            onRetry = {},
            onRefresh = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF4F3, heightDp = 1120)
@Composable
private fun WeatherScreenOfflinePreview() {
    AppTheme {
        WeatherScreen(
            uiState = previewUiState().copy(
                messageRes = R.string.weather_error_unreachable,
                isStale = true,
                isLoading = true,
            ),
            onRetry = {},
            onRefresh = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF4F3)
@Composable
private fun WeatherScreenEmptyPreview() {
    AppTheme {
        WeatherScreen(
            uiState = WeatherUiState(),
            onRetry = {},
            onRefresh = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

private const val PreviewNow = 1_785_072_480_000L
private const val PreviewStepMillis = 3 * 60 * 60 * 1_000L

private fun previewUiState(): WeatherUiState {
    val hourlyConditions = listOf(
        WeatherCondition.Clouds,
        WeatherCondition.Snow,
        WeatherCondition.Snow,
        WeatherCondition.Clouds,
        WeatherCondition.Clear,
    )

    return WeatherUiState(
        place = "Reykjavík, IS",
        // Iceland keeps UTC all year, which is also the one offset a preview cannot get wrong.
        zoneOffsetSeconds = 0,
        current = CurrentWeather(
            temperatureCelsius = -2.0,
            feelsLikeCelsius = -8.0,
            highCelsius = 1.0,
            lowCelsius = -5.0,
            condition = WeatherCondition.Clouds,
            description = "broken clouds",
            isNight = false,
            windMetresPerSecond = 3.9,
            humidityPercent = 78,
            visibilityMetres = 12_000,
        ),
        hourly = hourlyConditions.mapIndexed { index, condition ->
            HourlyColumn(
                startEpochMillis = PreviewNow + index * PreviewStepMillis,
                temperatureCelsius = -2.0 - index,
                condition = condition,
                isNight = false,
                isNow = index == 0,
            )
        },
        daily = listOf(
            DailyRow(CalendarDate(2026, 7, 26), 1.0, -6.0, WeatherCondition.Snow, true, 0f, 0.58f),
            DailyRow(CalendarDate(2026, 7, 27), 2.0, -3.0, WeatherCondition.Clouds, false, 0.25f, 0.67f),
            DailyRow(CalendarDate(2026, 7, 28), 4.0, 0.0, WeatherCondition.Clear, false, 0.5f, 0.83f),
            DailyRow(CalendarDate(2026, 7, 29), 6.0, 2.0, WeatherCondition.Clear, false, 0.67f, 1f),
        ),
        lastUpdatedEpochMillis = PreviewNow - 4 * 60 * 1_000L,
    )
}
