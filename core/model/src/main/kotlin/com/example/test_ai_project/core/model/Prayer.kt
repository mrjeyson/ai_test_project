package com.example.test_ai_project.core.model

/**
 * The five obligatory daily prayers, in the order they occur.
 *
 * An enum rather than a string, because the set is closed and fixed for all time: a day
 * has exactly these five, never four and never six. Every layer leans on that — the cache
 * stores five columns, the schedule sorts by `ordinal`, and the UI maps each entry to an
 * icon and a label without a `when` that needs an `else`.
 *
 * Sunrise is deliberately absent. It bounds the end of Fajr rather than being a prayer of
 * its own, and including it would make "the five daily prayers" a list of six.
 */
enum class Prayer {
    Fajr,
    Dhuhr,
    Asr,
    Maghrib,
    Isha,
}
