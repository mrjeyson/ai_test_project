package com.example.test_ai_project.core.data.time

import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.model.CalendarDate
import java.util.Calendar
import javax.inject.Inject

/**
 * Today in the device's own time zone — which is the right answer for a date of birth,
 * where the user's local calendar is what they are reading off their passport.
 */
class SystemDateProvider @Inject constructor() : DateProvider {

    override fun today(): CalendarDate {
        val now = Calendar.getInstance()
        return CalendarDate(
            year = now.get(Calendar.YEAR),
            // Calendar months are zero-based; CalendarDate months are not.
            month = now.get(Calendar.MONTH) + 1,
            day = now.get(Calendar.DAY_OF_MONTH),
        )
    }
}
