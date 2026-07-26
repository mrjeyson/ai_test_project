package com.example.test_ai_project.feature.home.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.core.domain.exception.MovieServiceNotConfiguredException
import com.example.test_ai_project.core.domain.usecase.GetMoviePageUseCase
import com.example.test_ai_project.core.domain.usecase.RefreshMoviePageUseCase
import com.example.test_ai_project.core.model.MoviePage
import com.example.test_ai_project.feature.home.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MoviesViewModel @Inject constructor(
    getMoviePage: GetMoviePageUseCase,
    private val refreshMoviePage: RefreshMoviePageUseCase,
) : ViewModel() {

    private val selectedPage = MutableStateFlow(MoviePage.FIRST_PAGE)
    private val isLoading = MutableStateFlow(false)
    private val messageRes = MutableStateFlow<Int?>(null)

    /**
     * Reads come from the cache, never from the fetch.
     *
     * [flatMapLatest] is what makes a page change instant: selecting page 4 switches the
     * database query immediately and shows page 4 if it is already cached, rather than
     * waiting on the network to decide what to display.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MoviesUiState> = combine(
        selectedPage.flatMapLatest { page -> getMoviePage(page) },
        isLoading,
        messageRes,
    ) { moviePage, loading, message ->
        MoviesUiState(
            page = moviePage.page,
            totalPages = moviePage.totalPages,
            movies = moviePage.movies,
            isLoading = loading,
            messageRes = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MoviesUiState(),
    )

    private var loadJob: Job? = null

    init {
        // Not forced: on a warm start the cache is usually still fresh, and this becomes a
        // no-op rather than a redundant round trip.
        load(MoviePage.FIRST_PAGE, force = false)
    }

    fun selectPage(page: Int) {
        if (page == selectedPage.value) return
        selectedPage.value = page
        load(page, force = false)
    }

    /** Explicit user action, so it bypasses the freshness check. */
    fun refresh() = load(selectedPage.value, force = true)

    /** Retry after a failure — same page, and forced, since the cache is what failed us. */
    fun retry() = load(selectedPage.value, force = true)

    fun dismissMessage() {
        messageRes.value = null
    }

    private fun load(page: Int, force: Boolean) {
        // Cancelling matters: a user tapping through the page control faster than the
        // network answers would otherwise leave earlier fetches racing to clear the
        // loading flag and the banner out from under the newest one.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading.value = true
            runCatching { refreshMoviePage(page = page, force = force) }
                .onSuccess { messageRes.value = null }
                .onFailure { messageRes.value = it.toMessageRes() }
            isLoading.value = false
        }
    }

    private fun Throwable.toMessageRes(): Int = when (this) {
        // Checked before IOException, which it is a subtype of. A build with no API key
        // will never succeed no matter how good the signal is, so telling the user to
        // check their connection would send them after the wrong problem entirely.
        is MovieServiceNotConfiguredException -> R.string.movies_error_not_configured
        is IOException -> R.string.movies_error_unreachable
        else -> R.string.movies_error_generic
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
