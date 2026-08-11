package com.example.test_ai_project.home.data.di

import com.example.test_ai_project.home.data.notification.AlarmManagerPrayerAlarmScheduler
import com.example.test_ai_project.home.data.service.DefaultLocationService
import com.example.test_ai_project.home.data.service.DefaultMovieService
import com.example.test_ai_project.home.data.service.DefaultPrayerService
import com.example.test_ai_project.home.data.service.DefaultWeatherService
import com.example.test_ai_project.home.data.service.SystemDateProvider
import com.example.test_ai_project.home.data.service.SystemTimeProvider
import com.example.test_ai_project.home.domain.service.DateProvider
import com.example.test_ai_project.home.domain.service.LocationService
import com.example.test_ai_project.home.domain.service.MovieService
import com.example.test_ai_project.home.domain.service.PrayerAlarmScheduler
import com.example.test_ai_project.home.domain.service.PrayerService
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.service.WeatherService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the home feature's service contracts to their implementations.
 *
 * This is the seam that lets `:feature:home:presentation` be compiled — and tested against
 * fakes — without ever seeing Room, Ktor or AlarmManager. `:app` puts this module on
 * the runtime classpath with `runtimeOnly`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class HomeDataModule {

    @Binds
    @Singleton
    internal abstract fun bindsLocationService(
        implementation: DefaultLocationService,
    ): LocationService

    @Binds
    @Singleton
    internal abstract fun bindsMovieService(implementation: DefaultMovieService): MovieService

    @Binds
    @Singleton
    internal abstract fun bindsPrayerService(implementation: DefaultPrayerService): PrayerService

    @Binds
    @Singleton
    internal abstract fun bindsWeatherService(
        implementation: DefaultWeatherService,
    ): WeatherService

    // The seam that keeps AlarmManager out of the domain layer: the rule "alert at every
    // upcoming prayer" is expressed and tested against this interface, and only the
    // binding below knows the platform is involved.
    @Binds
    @Singleton
    internal abstract fun bindsPrayerAlarmScheduler(
        implementation: AlarmManagerPrayerAlarmScheduler,
    ): PrayerAlarmScheduler

    // The two clocks the caches and the timetable are read against. Both are device state,
    // so the binding belongs in the data layer even though a ViewModel is what injects them
    // — the contracts above are all `:feature:home:presentation` ever sees.
    @Binds
    @Singleton
    internal abstract fun bindsDateProvider(implementation: SystemDateProvider): DateProvider

    @Binds
    @Singleton
    internal abstract fun bindsTimeProvider(implementation: SystemTimeProvider): TimeProvider
}

























