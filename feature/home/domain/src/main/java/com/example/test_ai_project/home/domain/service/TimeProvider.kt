package com.example.test_ai_project.home.domain.service

/**
 * Wall-clock time, in milliseconds since the epoch.
 *
 * Separate from [DateProvider], which answers "what is today's date in the user's
 * calendar". This one answers "how long ago did that happen" for cache expiry — a
 * different question, with different correctness rules (no time zone, no locale) and a
 * different reason to be injectable: cache-expiry logic is untestable against a real clock
 * without sleeping.
 */
interface TimeProvider {
    fun nowEpochMillis(): Long
}
