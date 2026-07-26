package com.example.test_ai_project.core.data.di

import com.example.test_ai_project.core.data.notification.AlarmManagerPrayerAlarmScheduler
import com.example.test_ai_project.core.data.repository.AuthRepositoryImpl
import com.example.test_ai_project.core.data.repository.FaceVerificationRepositoryImpl
import com.example.test_ai_project.core.data.repository.ItemRepositoryImpl
import com.example.test_ai_project.core.data.repository.LocationRepositoryImpl
import com.example.test_ai_project.core.data.repository.MapCameraRepositoryImpl
import com.example.test_ai_project.core.data.repository.MovieRepositoryImpl
import com.example.test_ai_project.core.data.repository.PrayerTimesRepositoryImpl
import com.example.test_ai_project.core.data.repository.WeatherRepositoryImpl
import com.example.test_ai_project.core.data.time.SystemDateProvider
import com.example.test_ai_project.core.data.time.SystemTimeProvider
import com.example.test_ai_project.core.domain.notification.PrayerAlarmScheduler
import com.example.test_ai_project.core.domain.repository.AuthRepository
import com.example.test_ai_project.core.domain.repository.FaceVerificationRepository
import com.example.test_ai_project.core.domain.repository.ItemRepository
import com.example.test_ai_project.core.domain.repository.LocationRepository
import com.example.test_ai_project.core.domain.repository.MapCameraRepository
import com.example.test_ai_project.core.domain.repository.MovieRepository
import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.domain.repository.WeatherRepository
import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.domain.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds domain interfaces to their data-layer implementations.
 *
 * This is the seam that lets a feature be tested against a fake repository, and
 * lets Room/Retrofit be replaced without any consumer changing.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    internal abstract fun bindsItemRepository(
        implementation: ItemRepositoryImpl,
    ): ItemRepository

    @Binds
    internal abstract fun bindsMovieRepository(
        implementation: MovieRepositoryImpl,
    ): MovieRepository

    @Binds
    internal abstract fun bindsLocationRepository(
        implementation: LocationRepositoryImpl,
    ): LocationRepository

    @Binds
    internal abstract fun bindsMapCameraRepository(
        implementation: MapCameraRepositoryImpl,
    ): MapCameraRepository

    @Binds
    internal abstract fun bindsPrayerTimesRepository(
        implementation: PrayerTimesRepositoryImpl,
    ): PrayerTimesRepository

    @Binds
    internal abstract fun bindsWeatherRepository(
        implementation: WeatherRepositoryImpl,
    ): WeatherRepository

    // The seam that keeps AlarmManager out of the domain layer: the rule "alert at every
    // upcoming prayer" is expressed and tested against this interface, and only the
    // binding below knows the platform is involved.
    @Binds
    internal abstract fun bindsPrayerAlarmScheduler(
        implementation: AlarmManagerPrayerAlarmScheduler,
    ): PrayerAlarmScheduler

    @Binds
    internal abstract fun bindsAuthRepository(
        implementation: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    internal abstract fun bindsFaceVerificationRepository(
        implementation: FaceVerificationRepositoryImpl,
    ): FaceVerificationRepository

    @Binds
    internal abstract fun bindsDateProvider(
        implementation: SystemDateProvider,
    ): DateProvider

    @Binds
    internal abstract fun bindsTimeProvider(
        implementation: SystemTimeProvider,
    ): TimeProvider
}
