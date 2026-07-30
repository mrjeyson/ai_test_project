package com.example.test_ai_project.auth.domain.service

import com.example.test_ai_project.auth.domain.model.CredentialsValidation
import com.example.test_ai_project.auth.domain.model.LoginResult
import com.example.test_ai_project.auth.domain.model.VaultCredentials

/**
 * Everything the login feature can do.
 *
 * A service, not a set of use-case classes: the two operations below are one cohesive
 * responsibility — turning what the user typed into an authenticated session — and
 * splitting them into `ValidateCredentialsUseCase` and `AuthenticateLocallyUseCase` buys
 * an extra class per verb without making either independently useful.
 *
 * The contract says nothing about *how* authentication happens — keystore, secure element,
 * or a stub — which is what lets the login screen be finished and tested before that
 * choice is made.
 */
interface AuthService {

    /**
     * Checks a filled-in form without touching the vault.
     *
     * Synchronous and pure, so the screen can call it on every submit without a coroutine
     * and a test can exercise every rule without a dispatcher.
     *
     * @param dateOfBirthDigits the eight digits `ddMMyyyy`, with no separators — the UI
     *   inserts the slashes visually, so the domain never has to strip them.
     */
    fun validate(dateOfBirthDigits: String, passportNumber: String): CredentialsValidation

    /** Checks already-validated credentials against the vault on this device. */
    suspend fun authenticate(credentials: VaultCredentials): LoginResult
}
