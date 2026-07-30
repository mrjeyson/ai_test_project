package com.example.test_ai_project.auth.data.di

import com.example.test_ai_project.auth.data.service.DefaultAuthService
import com.example.test_ai_project.auth.data.service.DefaultFaceVerificationService
import com.example.test_ai_project.auth.data.service.SystemDateProvider
import com.example.test_ai_project.auth.domain.service.AuthService
import com.example.test_ai_project.auth.domain.service.DateProvider
import com.example.test_ai_project.auth.domain.service.FaceVerificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the auth feature's service contracts to their implementations.
 *
 * Both halves of authentication — the credential check and the face match — are bound here
 * because they are one feature with one entry and one exit: the user is either through the
 * gate or not. `:app` puts this module on the runtime classpath with `runtimeOnly`, so
 * `:feature:auth:presentation` compiles without ever seeing it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {

    @Binds
    @Singleton
    internal abstract fun bindsAuthService(implementation: DefaultAuthService): AuthService

    @Binds
    @Singleton
    internal abstract fun bindsFaceVerificationService(
        implementation: DefaultFaceVerificationService,
    ): FaceVerificationService

    // The device calendar, which only the data layer is allowed to read. Bound here rather
    // than shared with the other features: all this one asks is whether a date of birth has
    // happened yet.
    @Binds
    @Singleton
    internal abstract fun bindsDateProvider(implementation: SystemDateProvider): DateProvider
}
