package com.example.test_ai_project.core.domain.notification

import com.example.test_ai_project.core.model.PrayerTime

/**
 * Arranges for the device to raise an alert at each prayer time.
 *
 * Declared in the domain layer, with no mention of alarms, notifications or intents, so the
 * rule "the alerts are whatever the cache says is still ahead" can be expressed and tested
 * without the platform. The implementation in `:core:data` is the only thing that knows an
 * `AlarmManager` is involved.
 *
 * The alerts fire on-device: once set, they need no network, no server and no running
 * process, which is what makes them work in exactly the conditions the cache exists for.
 */
interface PrayerAlarmScheduler {

    /**
     * Replaces every scheduled alert with one per entry in [times].
     *
     * Replace rather than add, and that is the entire contract. Alerts are derived state —
     * a pure function of the cached schedule and the current time — so the only way to keep
     * them correct is to re-derive the whole set each time. Adding would leave alerts for a
     * day the user has since left behind, firing at the wrong local time.
     *
     * An empty list therefore means "cancel everything", and is the correct call when the
     * cache has run out.
     */
    suspend fun replaceAlerts(times: List<PrayerTime>)

    companion object {
        /**
         * How many alerts are ever pending at once.
         *
         * Ten covers the two-day window the schedule spans, which is as far ahead as the
         * cache can see. Anything past that would be an alarm the app has no times for.
         */
        const val MAX_PENDING_ALERTS = 10
    }
}
