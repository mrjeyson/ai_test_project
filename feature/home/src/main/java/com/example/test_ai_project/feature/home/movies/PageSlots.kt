package com.example.test_ai_project.feature.home.movies

/** One position in the page control: either a tappable page, or an elided run of them. */
internal sealed interface PageSlot {
    data class Number(val page: Int) : PageSlot
    data object Gap : PageSlot
}

/**
 * Lays out a page control that has to fit on a phone while indexing up to 500 pages.
 *
 * Built by collecting the pages that must be reachable — the first, the last, and a
 * [radius] either side of [current] — and then inserting a gap wherever the sorted result
 * skips a number. Expressing it as "which pages matter, then where are the holes" keeps it
 * correct at the edges, where the usual index-arithmetic version tends to produce a "…"
 * hiding a single page, or a window that runs off the end.
 *
 * A pure function, and `internal` rather than private, so the edge cases are unit-testable
 * without a composable.
 */
internal fun pageSlots(current: Int, total: Int, radius: Int = DEFAULT_RADIUS): List<PageSlot> {
    if (total < 1) return emptyList()

    val pages = buildSet {
        add(1)
        add(total)
        for (page in (current - radius)..(current + radius)) {
            if (page in 1..total) add(page)
        }
    }.sorted()

    return buildList {
        var previous: Int? = null
        for (page in pages) {
            // A gap of exactly one page is not worth eliding — "1 … 3" is the same width
            // as "1 2 3" and tells the user less.
            if (previous != null && page - previous == 2) add(PageSlot.Number(previous + 1))
            else if (previous != null && page - previous > 2) add(PageSlot.Gap)
            add(PageSlot.Number(page))
            previous = page
        }
    }
}

private const val DEFAULT_RADIUS = 1
