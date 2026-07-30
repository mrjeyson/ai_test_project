package com.example.test_ai_project.home.presentation.home.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.test_ai_project.home.presentation.R
import com.example.test_ai_project.home.presentation.map.navigation.MapRouteKey
import com.example.test_ai_project.home.presentation.movies.navigation.MoviesRouteKey
import com.example.test_ai_project.home.presentation.prayertimes.navigation.PrayerTimesRouteKey
import com.example.test_ai_project.home.presentation.weather.navigation.WeatherRouteKey
import com.example.test_ai_project.resource.R as ResR

/**
 * The four top-level destinations of the home shell, in the order the design puts them
 * in the navigation bar.
 *
 * The enum is the single source of truth: [HomeNavHost] registers one destination per
 * entry and the bar renders one item per entry, so adding or reordering a tab is a change
 * to this list and nothing else.
 *
 * [routeKey] holds the route *instance* rather than its class, because a tap needs the
 * object to navigate with while selection matching needs the class — deriving the class
 * from the object is free, the reverse is not.
 */
enum class HomeTab(
    val routeKey: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Movies(
        routeKey = MoviesRouteKey,
        labelRes = ResR.string.home_tab_movies,
        iconRes = R.drawable.ic_tab_movies,
    ),
    Map(
        routeKey = MapRouteKey,
        labelRes = ResR.string.home_tab_map,
        iconRes = R.drawable.ic_tab_map,
    ),
    PrayerTimes(
        routeKey = PrayerTimesRouteKey,
        labelRes = ResR.string.home_tab_prayer,
        iconRes = R.drawable.ic_tab_prayer,
    ),
    Weather(
        routeKey = WeatherRouteKey,
        labelRes = ResR.string.home_tab_weather,
        iconRes = R.drawable.ic_tab_weather,
    ),
    ;

    companion object {
        /** The tab the shell opens on, and the one `back` unwinds to. */
        val Start: HomeTab = Movies
    }
}
