package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.AuthRepository
import com.example.test_ai_project.core.model.AuthOutcome
import com.example.test_ai_project.core.model.VaultCredentials
import javax.inject.Inject

/** Checks already-validated credentials against the vault on this device. */
class AuthenticateLocallyUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(credentials: VaultCredentials): AuthOutcome =
        authRepository.authenticate(credentials)
}
