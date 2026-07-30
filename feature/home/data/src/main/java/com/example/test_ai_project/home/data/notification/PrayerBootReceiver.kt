package com.example.test_ai_project.home.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.test_ai_project.home.domain.service.PrayerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rebuilds the prayer alarms after the device restarts or the app is replaced.
 *
 * Not optional, and easy to miss: the system discards every pending alarm on reboot. With
 * no receiver here, a phone restarted overnight would raise no alert at Fajr, and none
 * afterwards either — the self-perpetuating chain in [PrayerAlarmReceiver] only continues
 * while one of its links keeps firing, and a reboot breaks every link at once.
 *
 * It reads from the cache and nothing else, so it works on a device that boots with no
 * signal.
 */
@AndroidEntryPoint
class PrayerBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prayerService: PrayerService

    override fun onReceive(context: Context, intent: Intent) {
        // MY_PACKAGE_REPLACED is here for the same reason as BOOT_COMPLETED: an app update
        // also clears pending alarms, and an update installed overnight would otherwise be
        // indistinguishable from the feature quietly breaking.
        if (intent.action !in HandledActions) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                prayerService.scheduleAlerts()
            } catch (failure: Exception) {
                // Boot is a hostile moment — the database may still be locked behind
                // direct-boot encryption. Crashing the receiver would be visible to the
                // user; the next app launch reschedules anyway.
                Log.e(TAG, "Could not restore prayer alerts after boot", failure)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PrayerBootReceiver"

        val HandledActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
