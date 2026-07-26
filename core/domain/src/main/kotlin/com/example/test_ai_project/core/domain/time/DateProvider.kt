package com.example.test_ai_project.core.domain.time

import com.example.test_ai_project.core.model.CalendarDate

/**
 * Today, according to the device.
 *
 * An interface rather than a call to the system clock, because "is this date of birth in
 * the future?" is otherwise untestable without waiting for tomorrow.
 */
interface DateProvider {
    fun today(): CalendarDate
}
