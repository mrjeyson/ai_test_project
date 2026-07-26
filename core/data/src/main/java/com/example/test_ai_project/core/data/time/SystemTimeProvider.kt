package com.example.test_ai_project.core.data.time

import com.example.test_ai_project.core.domain.time.TimeProvider
import javax.inject.Inject

/**
 * The device clock.
 *
 * `currentTimeMillis` rather than `elapsedRealtime` deliberately: the timestamps it
 * produces are written to disk and compared across process restarts, which a
 * boot-relative clock cannot survive. The cost is that a user winding their clock forward
 * expires the cache early — a harmless outcome for something re-fetchable.
 */
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
