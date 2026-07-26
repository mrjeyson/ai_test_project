package com.example.test_ai_project.feature.home.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.test_ai_project.feature.home.R
import com.example.test_ai_project.feature.home.map.MapRouteKey
import com.example.test_ai_project.feature.home.movies.MoviesRouteKey
import com.example.test_ai_project.feature.home.prayertimes.PrayerTimesRouteKey
import com.example.test_ai_project.feature.home.weather.WeatherRouteKey

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
        labelRes = R.string.home_tab_movies,
        iconRes = R.drawable.ic_tab_movies,
    ),
    Map(
        routeKey = MapRouteKey,
        labelRes = R.string.home_tab_map,
        iconRes = R.drawable.ic_tab_map,
    ),
    PrayerTimes(
        routeKey = PrayerTimesRouteKey,
        labelRes = R.string.home_tab_prayer,
        iconRes = R.drawable.ic_tab_prayer,
    ),
    Weather(
        routeKey = WeatherRouteKey,
        labelRes = R.string.home_tab_weather,
        iconRes = R.drawable.ic_tab_weather,
    ),
    ;

    companion object {
        /** The tab the shell opens on, and the one `back` unwinds to. */
        val Start: HomeTab = Movies
    }
}
