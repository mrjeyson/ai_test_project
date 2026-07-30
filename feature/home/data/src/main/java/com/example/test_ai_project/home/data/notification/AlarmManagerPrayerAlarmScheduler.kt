package com.example.test_ai_project.home.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.test_ai_project.home.domain.service.PrayerAlarmScheduler
import com.example.test_ai_project.home.domain.model.PrayerTime
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The platform alarm clock, driving one notification per prayer.
 *
 * [AlarmManager] rather than `WorkManager`: work requests are batched and deferred by
 * design, and "roughly around Asr" is not a prayer time. Alarms are also the only
 * mechanism that survives the app being swapped out entirely — nothing of ours is running
 * when one fires, which is precisely what makes the alerts work with no network and no
 * process.
 *
 * The only class in the app that knows an alarm is how this is done. Everything above it
 * talks to [PrayerAlarmScheduler].
 */
@Singleton
class AlarmManagerPrayerAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PrayerAlarmScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    override suspend fun replaceAlerts(times: List<PrayerTime>) = withContext(Dispatchers.IO) {
        val manager = alarmManager ?: return@withContext

        // Cancel the whole slot range first, not just the slots about to be reused. A
        // schedule that shrank — the cache pruned down to one day, say — would otherwise
        // leave the tail of the previous set pending, and those alarms would still fire.
        repeat(PrayerAlarmScheduler.MAX_PENDING_ALERTS) { slot ->
            val pending = pendingIntent(slot, PendingIntent.FLAG_NO_CREATE) ?: return@repeat
            manager.cancel(pending)
            // Drops the system's record of it too, so the FLAG_NO_CREATE probe above
            // reports this slot as genuinely empty next time round.
            pending.cancel()
        }

        times.take(PrayerAlarmScheduler.MAX_PENDING_ALERTS).forEachIndexed { slot, time ->
            manager.setAlert(slot = slot, time = time)
        }
    }

    /**
     * Sets one alert, exactly if the system allows it and approximately if it does not.
     *
     * From Android 12, exact alarms need `SCHEDULE_EXACT_ALARM`, which the user can revoke
     * from Settings at any time. The fallback is not a formality: an inexact alarm may
     * arrive minutes late, but a caught exception that scheduled nothing would mean the
     * alert never arrives at all — and the on-screen schedule stays correct either way.
     *
     * `...AndWhileIdle` in both branches is what gets past Doze, which is the state a
     * phone is reliably in at Fajr.
     */
    private fun AlarmManager.setAlert(slot: Int, time: PrayerTime) {
        val intent = pendingIntent(slot, PendingIntent.FLAG_UPDATE_CURRENT, time) ?: return
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()

        try {
            if (canBeExact) {
                setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.startEpochMillis, intent)
            } else {
                setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.startEpochMillis, intent)
            }
        } catch (denied: SecurityException) {
            // The permission can be revoked between the check above and the call below.
            // An inexact alarm is still worth setting; losing the alert is not.
            Log.w(TAG, "Exact alarm denied for ${time.prayer}; falling back to inexact", denied)
            setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.startEpochMillis, intent)
        }
    }

    /**
     * The alarm's callback, addressed by [slot].
     *
     * Slots, rather than an id derived from the date and prayer, are what make cancelling
     * total. `PendingIntent` equality ignores extras and compares the request code and the
     * intent's filter fields, so a fixed range of request codes is a set the scheduler can
     * always enumerate and clear — whereas hashed ids can only be cancelled if you still
     * remember every one you issued.
     *
     * Returns null with [PendingIntent.FLAG_NO_CREATE] when that slot holds no alarm, which
     * is how the cancel loop skips empty slots without creating them.
     *
     * [time] is carried as an extra so the receiver knows which prayer woke it. That the
     * extras play no part in matching is what makes this work in both directions: the
     * cancel path can address a slot without knowing what is in it, and
     * [PendingIntent.FLAG_UPDATE_CURRENT] rewrites the payload of a slot being reused.
     * `FLAG_IMMUTABLE` does not prevent that — it stops the *recipient* rewriting the
     * intent, not the app that owns it.
     */
    private fun pendingIntent(slot: Int, flags: Int, time: PrayerTime? = null): PendingIntent? {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALERT)
            .apply {
                // The enum's name, not its ordinal: a reordered enum would silently
                // remap every pending alarm, and a name that no longer exists is at
                // least a failure the receiver can see.
                time?.let { putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, it.prayer.name) }
            }

        return PendingIntent.getBroadcast(
            context,
            slot,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "PrayerAlarmScheduler"
    }
}
