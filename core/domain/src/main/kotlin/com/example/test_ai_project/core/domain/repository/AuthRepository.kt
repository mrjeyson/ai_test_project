package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.AuthOutcome
import com.example.test_ai_project.core.model.VaultCredentials

/**
 * Verifies credentials against the credential enrolled on this device.
 *
 * The contract says nothing about *how* — keystore, secure element, or a stub — which is
 * what lets the login feature be finished and tested before that choice is made.
 */
interface AuthRepository {
    suspend fun authenticate(credentials: VaultCredentials): AuthOutcome
}
