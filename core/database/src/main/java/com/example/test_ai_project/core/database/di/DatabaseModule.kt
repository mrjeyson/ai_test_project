package com.example.test_ai_project.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.test_ai_project.core.database.AppDatabase
import com.example.test_ai_project.core.database.dao.ItemDao
import com.example.test_ai_project.core.database.dao.LocationDao
import com.example.test_ai_project.core.database.dao.MovieDao
import com.example.test_ai_project.core.database.dao.PrayerTimesDao
import com.example.test_ai_project.core.database.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME,
    ).build()

    // Exposed separately so consumers depend on the DAO, not the whole database.
    @Provides
    fun providesItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    fun providesMovieDao(database: AppDatabase): MovieDao = database.movieDao()

    @Provides
    fun providesLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    fun providesPrayerTimesDao(database: AppDatabase): PrayerTimesDao = database.prayerTimesDao()

    @Provides
    fun providesWeatherDao(database: AppDatabase): WeatherDao = database.weatherDao()

    private const val DATABASE_NAME = "test_ai_project.db"
}
