package com.example.test_ai_project.core.data.repository

import com.example.test_ai_project.core.common.dispatcher.AppDispatcher
import com.example.test_ai_project.core.common.dispatcher.Dispatcher
import com.example.test_ai_project.core.domain.repository.AuthRepository
import com.example.test_ai_project.core.model.AuthOutcome
import com.example.test_ai_project.core.model.VaultCredentials
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * On-device authentication. Nothing here leaves the process, which is the promise the
 * login screen makes to the user.
 *
 * The comparison itself is a placeholder: the real implementation derives a key from the
 * credentials and compares it against the enrolment held in the Android keystore. That
 * work does not change this signature, so the feature above it is complete either way —
 * what is stubbed is the check, not the plumbing.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun authenticate(credentials: VaultCredentials): AuthOutcome =
        withContext(ioDispatcher) {
            // Stands in for the keystore round trip, so the caller's loading state is
            // exercised rather than being dead code until the real check lands.
            delay(VERIFICATION_LATENCY_MILLIS)
            AuthOutcome.Success
        }

    private companion object {
        const val VERIFICATION_LATENCY_MILLIS = 700L
    }
}
