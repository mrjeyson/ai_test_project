package com.example.test_ai_project.home.presentation.prayertimes.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
import com.example.test_ai_project.home.domain.model.Prayer
import com.example.test_ai_project.resource.component.AppLoadingState
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.VaultCharcoal
import com.example.test_ai_project.resource.theme.VaultHairline
import com.example.test_ai_project.resource.theme.VaultMistDeep
import com.example.test_ai_project.resource.theme.VaultStone
import com.example.test_ai_project.resource.theme.VaultTeal
import com.example.test_ai_project.resource.theme.VaultTealLight
import com.example.test_ai_project.home.presentation.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.test_ai_project.home.presentation.prayertimes.contract.NextPrayer
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerEntry
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerStatus
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesEvent
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesState
import com.example.test_ai_project.home.presentation.prayertimes.viewmodel.PrayerTimesViewModel
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppLoadingState

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel, or
 * the permission API.
 */
@Composable
internal fun PrayerTimesScreen(
    modifier: Modifier = Modifier,
    viewModel: PrayerTimesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remainingMillis by viewModel.remainingMillis.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        // The grants are not inspected. Whatever the user decided, the next step is the
        // same — try to build a timetable — and the ViewModel explains why if it cannot.
        onResult = { viewModel.onEvent(PrayerTimesEvent.PermissionsResolved) },
    )

    /**
     * Whether this screen has already had its one shot at the system dialogs.
     *
     * `rememberSaveable`, so switching tabs and coming back does not re-prompt. After a
     * refusal the page offers an Allow button instead, which is the only way a second
     * dialog can appear.
     */
    var hasRequestedPermissions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasRequestedPermissions) return@LaunchedEffect
        hasRequestedPermissions = true

        val missing = context.missingPermissions()
        if (missing.isEmpty()) {
            viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        } else {
            permissionLauncher.launch(missing)
        }
    }

    PrayerTimesScreen(
        uiState = uiState,
        remainingMillis = remainingMillis,
        onRetry = { viewModel.onEvent(PrayerTimesEvent.RetryRequested) },
        onChangeLocation = { viewModel.onEvent(PrayerTimesEvent.LocationChangeRequested) },
        onRequestPermission = { permissionLauncher.launch(RequiredPermissions) },
        onDismissMessage = { viewModel.onEvent(PrayerTimesEvent.MessageDismissed) },
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
internal fun PrayerTimesScreen(
    uiState: PrayerTimesState,
    remainingMillis: Long?,
    onRetry: () -> Unit,
    onChangeLocation: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // A strip, not a spinner over the page: the cached timetable stays readable and
        // scrollable while a refetch runs, which is the entire point of caching it.
        if (uiState.isLoading && uiState.isCached) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.messageRes != null) {
            MessageBanner(
                messageRes = uiState.messageRes,
                // A refused permission is fixed by a grant, not by trying the same call
                // again — so the button offers the thing that would actually work.
                actionRes = if (uiState.isPermissionRequestable) {
                    ResR.string.prayer_allow
                } else {
                    ResR.string.prayer_retry
                },
                onAction = if (uiState.isPermissionRequestable) onRequestPermission else onRetry,
                onDismiss = onDismissMessage,
            )
        }

        when {
            uiState.isInitialLoad -> AppLoadingState()

            uiState.isEmpty -> EmptyState(onRetry = onRetry)

            else -> PrayerTimesContent(
                uiState = uiState,
                remainingMillis = remainingMillis,
                onChangeLocation = onChangeLocation,
            )
        }
    }
}

@Composable
private fun PrayerTimesContent(
    uiState: PrayerTimesState,
    remainingMillis: Long?,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatTime = rememberClockFormatter(zoneId = uiState.zoneId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        OfflineStatusStrip(
            lastUpdatedLabel = uiState.lastUpdatedEpochMillis?.let(formatTime),
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            NextPrayerCard(
                next = uiState.next,
                remainingMillis = remainingMillis,
                formatTime = formatTime,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = ResR.string.prayer_daily_schedule),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.entries.forEach { entry ->
                PrayerRow(entry = entry, formatTime = formatTime)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            LocationRow(
                label = uiState.locationLabel,
                isLocating = uiState.isLoading,
                onChangeLocation = onChangeLocation,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * The strip under the app bar: what the page is showing, and how old it is.
 *
 * Both halves say the same thing from different directions — the times are on disk, and
 * here is when they were put there — which together let the user judge whether an offline
 * page is still one they should trust.
 */
@Composable
private fun OfflineStatusStrip(
    lastUpdatedLabel: String?,
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
                            .background(color = VaultTeal, shape = CircleShape),
                    )
                    Text(
                        text = stringResource(id = ResR.string.prayer_cached_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultStone,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                if (lastUpdatedLabel != null) {
                    Text(
                        text = stringResource(id = ResR.string.prayer_last_update, lastUpdatedLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultStone,
                    )
                }
            }

            HorizontalDivider(color = VaultHairline)
        }
    }
}

/**
 * The countdown card.
 *
 * [remainingMillis] arrives separately from the rest of the state so that a tick recomposes
 * this and nothing else — every other composable on the page takes only `uiState`, which
 * does not change from one second to the next, so Compose skips them.
 */
@Composable
private fun NextPrayerCard(
    next: NextPrayer?,
    remainingMillis: Long?,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp)
                // Merged so a screen reader announces the card as one sentence rather than
                // reading "02:14" as an orphaned number.
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (next == null) {
                // Everything today has passed and tomorrow was never cached. Saying so is
                // more honest than a countdown to nothing.
                Text(
                    text = stringResource(id = ResR.string.prayer_all_complete),
                    style = MaterialTheme.typography.bodyLarge,
                    color = VaultStone,
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            val locale = currentLocale()

            Text(
                text = stringResource(
                    id = ResR.string.prayer_next_label,
                    stringResource(id = next.prayer.labelRes).uppercase(locale),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = VaultStone,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatCountdown(remainingMillis, locale),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = ResR.string.prayer_remaining),
                    style = MaterialTheme.typography.bodyLarge,
                    color = VaultStone,
                    modifier = Modifier.padding(start = 8.dp, bottom = 5.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock_small),
                    contentDescription = null,
                    tint = VaultTeal,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(
                        id = if (next.isTomorrow) {
                            ResR.string.prayer_starts_at_tomorrow
                        } else {
                            ResR.string.prayer_starts_at
                        },
                        formatTime(next.startEpochMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTeal,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/**
 * One prayer in the daily schedule.
 *
 * The next prayer is inverted rather than merely accented, because it is the one thing on
 * the page a user opens the app to find. A tinted border would be a hint; a dark card in a
 * column of white ones is unmissable at arm's length.
 */
@Composable
private fun PrayerRow(
    entry: PrayerEntry,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val isNext = entry.status == PrayerStatus.Next

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isNext) VaultCharcoal else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        // On the dark card a mist-coloured disc would be a bright hole, so
                        // the circle becomes a barely-there lift instead.
                        color = if (isNext) Color.White.copy(alpha = 0.08f) else VaultMistDeep,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = entry.prayer.iconRes),
                    contentDescription = null,
                    tint = if (isNext) VaultTealLight else VaultStone,
                    modifier = Modifier.size(19.dp),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = stringResource(id = entry.prayer.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNext) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = entry.prayer.captionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNext) VaultTealLight else VaultStone,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(entry.startEpochMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNext) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                StatusChip(status = entry.status)
            }
        }
    }
}

/**
 * The COMPLETED / UPCOMING tag.
 *
 * [PrayerStatus.Later] renders nothing at all, which is the design working correctly rather
 * than a gap: labelling the whole evening "UPCOMING" would say nothing the ordering does
 * not already say, and would compete with the one row that matters.
 */
@Composable
private fun StatusChip(status: PrayerStatus, modifier: Modifier = Modifier) {
    val labelRes = when (status) {
        PrayerStatus.Completed -> ResR.string.prayer_status_completed
        PrayerStatus.Next -> ResR.string.prayer_status_upcoming
        PrayerStatus.Later -> return
    }

    Surface(
        modifier = modifier.padding(top = 4.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (status == PrayerStatus.Next) VaultTealLight else VaultMistDeep,
    ) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = if (status == PrayerStatus.Next) VaultCharcoal else VaultStone,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Where these times were computed for, and the control to re-detect it. */
@Composable
private fun LocationRow(
    label: String?,
    isLocating: Boolean,
    onChangeLocation: () -> Unit,
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
            TextButton(onClick = onChangeLocation, enabled = !isLocating) {
                Text(
                    text = stringResource(
                        id = if (isLocating) ResR.string.prayer_locating else ResR.string.prayer_change_location,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLocating) VaultStone else VaultTeal,
                )
            }
        }
    }
}

/**
 * Advisory, not blocking. The fetch failed but the cache did not, so this sits above the
 * timetable rather than replacing it.
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
            Text(text = stringResource(id = ResR.string.prayer_dismiss))
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
            text = stringResource(id = ResR.string.prayer_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = ResR.string.prayer_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = VaultStone,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(id = ResR.string.prayer_retry))
        }
    }
}

/**
 * Formats an instant as a clock time, in the zone the times belong to.
 *
 * Two things are deliberately deferred to the device. Whether to write "3:42 PM" or "15:42"
 * is a system setting, and overriding it to match a design would be wrong on any phone set
 * the other way. The zone, in contrast, is the *location's* — a schedule cached in London
 * and read after landing elsewhere still describes London's day, and formatting it against
 * the new local zone would silently shift every row.
 *
 * The id is safe to hand to [TimeZone.getTimeZone], which answers GMT for anything it does
 * not know: the data layer stores the id of the zone it actually resolved, never the raw
 * string the provider sent.
 */
@Composable
private fun rememberClockFormatter(zoneId: String?): (Long) -> String {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = currentLocale()

    return remember(zoneId, is24Hour, locale) {
        val formatter = SimpleDateFormat(
            if (is24Hour) "H:mm" else "h:mm a",
            locale,
        ).apply {
            zoneId?.let { timeZone = TimeZone.getTimeZone(it) }
        }
        // An anonymous function, not a lambda literal: a `{ ... }` here would be parsed as
        // a trailing lambda passed to `apply` on the line above.
        fun(epochMillis: Long): String = formatter.format(Date(epochMillis))
    }
}

/**
 * The locale, read so that a change to it recomposes what was formatted with it.
 *
 * `Locale.getDefault()` would be the obvious call and is the wrong one inside a composable:
 * it reads no observable state, so text formatted from it keeps the old locale until
 * something unrelated happens to recompose. [LocalConfiguration] is observable, and its
 * locale list is the same one the platform hands the rest of the app.
 *
 * `LocalLocale`, which the lint check for this suggests, does not exist in the Compose
 * version this project builds against.
 */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * The countdown, as `HH:MM` — or `MM:SS` inside the final hour.
 *
 * The switch is what stops the display looking frozen. Held at `HH:MM` throughout, the
 * digits would sit unchanged for a minute at a time in exactly the stretch a user is
 * watching them most closely.
 */
private fun formatCountdown(remainingMillis: Long?, locale: Locale): String {
    // Negative is reachable: the tick that crosses a prayer time lands a moment after it.
    val totalSeconds = ((remainingMillis ?: 0L) / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        String.format(locale, "%02d:%02d", hours, minutes)
    } else {
        String.format(locale, "%02d:%02d", minutes, seconds)
    }
}

@get:StringRes
private val Prayer.labelRes: Int
    get() = when (this) {
        Prayer.Fajr -> ResR.string.prayer_fajr
        Prayer.Dhuhr -> ResR.string.prayer_dhuhr
        Prayer.Asr -> ResR.string.prayer_asr
        Prayer.Maghrib -> ResR.string.prayer_maghrib
        Prayer.Isha -> ResR.string.prayer_isha
    }

@get:StringRes
private val Prayer.captionRes: Int
    get() = when (this) {
        Prayer.Fajr -> ResR.string.prayer_fajr_caption
        Prayer.Dhuhr -> ResR.string.prayer_dhuhr_caption
        Prayer.Asr -> ResR.string.prayer_asr_caption
        Prayer.Maghrib -> ResR.string.prayer_maghrib_caption
        Prayer.Isha -> ResR.string.prayer_isha_caption
    }

/** Sun through the daylight prayers, crescent once the sun is down. */
@get:DrawableRes
private val Prayer.iconRes: Int
    get() = when (this) {
        Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr -> R.drawable.ic_prayer_sun
        Prayer.Maghrib, Prayer.Isha -> R.drawable.ic_prayer_moon
    }

private fun Context.missingPermissions(): Array<String> = RequiredPermissions
    .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
    .toTypedArray()

/**
 * Coarse location and, on API 33+, notifications.
 *
 * Fine location is deliberately *not* asked for, and this is the one screen in the app that
 * can say so honestly: the repository treats a cached day as valid within ten kilometres,
 * so metre-level precision would buy nothing and cost the user the stronger grant. A device
 * that already granted fine location for the map satisfies the coarse check anyway.
 */
private val RequiredPermissions: Array<String> = buildList {
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

// Previews are pinned to a fixed instant — 26 July 2026, 13:28 in London — because every
// state on this page is a function of the clock, and a preview that read the real one would
// look different every time it was opened.

@Preview(showBackground = true, backgroundColor = 0xFFEFF4F3, heightDp = 780)
@Composable
private fun PrayerTimesScreenPreview() {
    AppTheme {
        PrayerTimesScreen(
            uiState = previewUiState(),
            remainingMillis = PreviewAsr - PreviewNow,
            onRetry = {},
            onChangeLocation = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF4F3, heightDp = 780)
@Composable
private fun PrayerTimesScreenOfflinePreview() {
    AppTheme {
        PrayerTimesScreen(
            uiState = previewUiState().copy(
                messageRes = ResR.string.prayer_error_unreachable,
                isLoading = true,
            ),
            remainingMillis = PreviewAsr - PreviewNow,
            onRetry = {},
            onChangeLocation = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF4F3)
@Composable
private fun PrayerTimesScreenEmptyPreview() {
    AppTheme {
        PrayerTimesScreen(
            uiState = PrayerTimesState(),
            remainingMillis = null,
            onRetry = {},
            onChangeLocation = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

private const val PreviewFajr = 1_785_035_520_000L
private const val PreviewDhuhr = 1_785_065_280_000L
private const val PreviewAsr = 1_785_076_920_000L
private const val PreviewMaghrib = 1_785_088_440_000L
private const val PreviewIsha = 1_785_093_720_000L
private const val PreviewNow = 1_785_068_880_000L
private const val PreviewFetchedAt = 1_785_066_300_000L

private fun previewUiState() = PrayerTimesState(
    entries = listOf(
        PrayerEntry(Prayer.Fajr, PreviewFajr, PrayerStatus.Completed),
        PrayerEntry(Prayer.Dhuhr, PreviewDhuhr, PrayerStatus.Completed),
        PrayerEntry(Prayer.Asr, PreviewAsr, PrayerStatus.Next),
        PrayerEntry(Prayer.Maghrib, PreviewMaghrib, PrayerStatus.Later),
        PrayerEntry(Prayer.Isha, PreviewIsha, PrayerStatus.Later),
    ),
    next = NextPrayer(Prayer.Asr, PreviewAsr, isTomorrow = false),
    locationLabel = "London, United Kingdom",
    zoneId = "Europe/London",
    lastUpdatedEpochMillis = PreviewFetchedAt,
)
