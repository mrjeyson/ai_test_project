package com.example.test_ai_project.home.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.test_ai_project.home.domain.service.PrayerService
import com.example.test_ai_project.home.domain.model.Prayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A prayer time has arrived: announce it, then set the next alarms.
 *
 * The second half is what keeps the feature alive without the app. Only a bounded window of
 * alarms is ever pending, so each one that fires has to roll the window forward — which
 * makes the chain self-perpetuating for as long as the cache has days in it, with no
 * background service and no server pushing anything.
 */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notifier: PrayerNotifier

    @Inject
    lateinit var prayerService: PrayerService

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PRAYER_ALERT) return

        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.toPrayerOrNull()
        if (prayer == null) {
            // A pending alarm from a build whose enum has since changed, most likely.
            // Rescheduling below still runs, so the chain repairs itself.
            Log.w(TAG, "Prayer alert with no recognisable prayer: ${intent.extras}")
        } else {
            // Posted before the suspending work starts, not after. Notifying is the part
            // the user is waiting on, and it must not sit behind a database read.
            notifier.notify(prayer)
        }

        // `goAsync` buys this receiver time past the return of `onReceive`, which is the
        // only way to touch a database from one at all. The window is short — around ten
        // seconds — and reading the cache to reset alarms fits inside it comfortably.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // IO confinement lives in the repository this reaches, so there is nothing
                // to confine here.
                prayerService.scheduleAlerts()
            } catch (failure: Exception) {
                // A receiver that throws takes the process down. The next refresh — or the
                // next boot — rebuilds the alarms, so failing quietly here costs one
                // rescheduling pass rather than a crash the user sees.
                Log.e(TAG, "Could not reschedule prayer alerts", failure)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun String.toPrayerOrNull(): Prayer? =
        Prayer.entries.firstOrNull { it.name == this }

    companion object {
        /**
         * Explicit rather than relying on the component name alone.
         *
         * The receiver is not exported, so this is not a filter — it is a guard against a
         * stale `PendingIntent` from an older build waking this class with a payload it
         * would misread.
         */
        const val ACTION_PRAYER_ALERT =
            "com.example.test_ai_project.home.data.action.PRAYER_ALERT"

        const val EXTRA_PRAYER = "com.example.test_ai_project.home.data.extra.PRAYER"

        private const val TAG = "PrayerAlarmReceiver"
    }
}
