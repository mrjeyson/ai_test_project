package com.example.test_ai_project.core.data.repository

import com.example.test_ai_project.core.common.dispatcher.AppDispatcher
import com.example.test_ai_project.core.common.dispatcher.Dispatcher
import com.example.test_ai_project.core.domain.repository.FaceVerificationRepository
import com.example.test_ai_project.core.model.FaceObservation
import com.example.test_ai_project.core.model.FaceVerificationOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
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
class FaceVerificationRepositoryImpl @Inject constructor(
    @param:Dispatcher(AppDispatcher.Default) private val defaultDispatcher: CoroutineDispatcher,
) : FaceVerificationRepository {

    override suspend fun verify(face: FaceObservation): FaceVerificationOutcome =
        withContext(defaultDispatcher) {
            // Stands in for the embedding comparison, so the screen's "Confirming…" state
            // is exercised rather than being dead code until the real matcher lands.
            delay(MATCH_LATENCY_MILLIS)
            FaceVerificationOutcome.Verified
        }

    private companion object {
        const val MATCH_LATENCY_MILLIS = 1_200L
    }
}
