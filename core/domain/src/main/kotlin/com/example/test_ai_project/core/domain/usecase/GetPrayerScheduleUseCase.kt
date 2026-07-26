package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.PrayerSchedule
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Observes the cached prayer schedule for [today] and the day after.
 *
 * Takes the date rather than reading a clock, for the same reason
 * [GetMoviePageUseCase] takes a page number: the returned flow is bound to the argument for
 * as long as it is collected, and a screen left open past midnight has to *switch* to a new
 * one. Baking the date in here would mean a page that quietly kept counting down to
 * yesterday's prayers.
 */
class GetPrayerScheduleUseCase @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
) {
    operator fun invoke(today: CalendarDate): Flow<PrayerSchedule?> =
        prayerTimesRepository.observeSchedule(today)
}
