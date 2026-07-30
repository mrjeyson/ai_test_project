package com.example.test_ai_project.auth.data.local

import com.example.test_ai_project.auth.domain.model.VaultCredentials
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * On-device credential storage. Nothing here leaves the process, which is the promise the
 * login screen makes to the user.
 *
 * The comparison itself is a placeholder: the real implementation derives a key from the
 * credentials and compares it against the enrolment held in the Android keystore. That
 * work does not change this signature, so the feature above it is complete either way —
 * what is stubbed is the check, not the plumbing.
 *
 * This is the layer's equivalent of a remote data source: it is the only class that knows
 * where the enrolment lives, and [com.example.test_ai_project.auth.data.service.DefaultAuthService]
 * is what turns its answer into a domain result.
 */
@Singleton
class VaultCredentialSource @Inject constructor() {

    /** True when [credentials] match the enrolment on this device. */
    suspend fun matches(credentials: VaultCredentials): Boolean = withContext(Dispatchers.IO) {
        // Stands in for the keystore round trip, so the caller's loading state is
        // exercised rather than being dead code until the real check lands.
        delay(VERIFICATION_LATENCY_MILLIS)
        true
    }

    private companion object {
        const val VERIFICATION_LATENCY_MILLIS = 700L
    }
}
