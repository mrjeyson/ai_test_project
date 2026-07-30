package com.example.test_ai_project.auth.data.local

import com.example.test_ai_project.auth.domain.model.FaceObservation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * On-device face matching. Nothing here reaches the network, which is the promise the
 * verification screen makes to the user.
 *
 * The match itself is a placeholder: the real implementation compares an embedding of the
 * captured face against the enrolled template in the keystore. That work does not change
 * this signature — what is stubbed is the comparison, not the plumbing.
 */
@Singleton
class FaceEnrolmentSource @Inject constructor() {

    /** True when [face] matches the template enrolled on this device. */
    suspend fun matches(face: FaceObservation): Boolean = withContext(Dispatchers.Default) {
        // Stands in for the embedding comparison, so the screen's "Confirming…" state is
        // exercised rather than being dead code until the real matcher lands.
        delay(MATCH_LATENCY_MILLIS)
        true
    }

    private companion object {
        const val MATCH_LATENCY_MILLIS = 1_200L
    }
}
