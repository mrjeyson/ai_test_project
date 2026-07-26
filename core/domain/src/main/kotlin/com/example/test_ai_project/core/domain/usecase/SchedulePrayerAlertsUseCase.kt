package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.notification.PrayerAlarmScheduler
import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.domain.time.TimeProvider
import javax.inject.Inject

/**
 * Brings the device's pending prayer alerts back in step with the cache.
 *
 * The one rule, in one place: *the alerts are the prayers the cache still has ahead of
 * now*. Everything that can invalidate that calls this — a refresh that rewrote the days, a
 * reboot that cleared the alarms, an alert that just fired and rolled the window forward —
 * and none of them has to reason about which alarms to add or drop, because the answer is
 * always the whole set.
 *
 * Deliberately silent on failure being someone else's problem: it reads only from the
 * cache, so it works offline and has nothing to throw.
 */
class SchedulePrayerAlertsUseCase @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val prayerAlarmScheduler: PrayerAlarmScheduler,
    private val dateProvider: DateProvider,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke() {
        val schedule = prayerTimesRepository.schedule(dateProvider.today())

        // An empty list is a meaningful call, not a no-op to skip: with nothing cached
        // there is nothing legitimate to alert on, and any alarm still pending belongs to
        // a schedule that no longer exists.
        prayerAlarmScheduler.replaceAlerts(
            schedule
                ?.upcomingAfter(timeProvider.nowEpochMillis())
                .orEmpty()
                .take(PrayerAlarmScheduler.MAX_PENDING_ALERTS),
        )
    }
}
