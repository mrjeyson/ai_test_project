package com.example.test_ai_project.home.domain.service

import com.example.test_ai_project.home.domain.model.CalendarDate

/**
 * Today, according to the device.
 *
 * An interface rather than a call to the system clock, because "has the prayer timetable
 * rolled over to tomorrow?" is otherwise untestable without waiting for midnight.
 */
interface DateProvider {
    fun today(): CalendarDate
}
