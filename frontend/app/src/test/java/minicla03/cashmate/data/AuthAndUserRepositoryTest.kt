package minicla03.cashmate.data

import kotlinx.coroutines.test.runTest
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.BusinessErrorReason
import minicla03.cashmate.core.network.InMemoryTokenStorage
import minicla03.cashmate.core.network.SessionExpiryNotifier
import minicla03.cashmate.data.remote.datasource.FakeAuthRemoteDataSource
import minicla03.cashmate.data.remote.datasource.FakeRemoteConfig
import minicla03.cashmate.data.remote.datasource.FakeRemoteStore
import minicla03.cashmate.data.remote.datasource.FakeUserRemoteDataSource
import minicla03.cashmate.data.remote.datasource.UserRemoteDataSource
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.mapper.NetworkErrorMapper
import minicla03.cashmate.data.repository.AuthRepositoryImpl
import minicla03.cashmate.data.repository.AuthSessionStateHolder
import minicla03.cashmate.data.repository.InMemoryUserPreferencesRepository
import minicla03.cashmate.data.repository.SafeRemoteCall
import minicla03.cashmate.data.repository.UserRepositoryImpl
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.AppThemeMode
import minicla03.cashmate.domain.model.AuthenticationState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency

class AuthAndUserRepositoryTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC)
    private val errorMapper = NetworkErrorMapper()

    private class Harness(
        val config: FakeRemoteConfig = FakeRemoteConfig().apply { simulatedDelayMs = 0L },
        val clock: Clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC)
    ) {
        val store = FakeRemoteStore(clock, config)
        val tokenStorage = InMemoryTokenStorage()
        val prefsRepo = InMemoryUserPreferencesRepository()
        val notifier = SessionExpiryNotifier()
        val sessionHolder = AuthSessionStateHolder(tokenStorage, prefsRepo)
        val safeRemoteCall = SafeRemoteCall(NetworkErrorMapper())
        val authRemote = FakeAuthRemoteDataSource(store)
        val userRemote = FakeUserRemoteDataSource(store)
        val authRepo = AuthRepositoryImpl(
            authRemoteDataSource = authRemote,
            userRemoteDataSource = userRemote,
            tokenStorage = tokenStorage,
            sessionStateHolder = sessionHolder,
            userPreferencesRepository = prefsRepo,
            sessionExpiryNotifier = notifier,
            safeRemoteCall = safeRemoteCall
        )
        val userRepo = UserRepositoryImpl(
            userRemoteDataSource = userRemote,
            sessionStateHolder = sessionHolder,
            userPreferencesRepository = prefsRepo,
            safeRemoteCall = safeRemoteCall
        )
    }

    @Test
    fun `restoreSession sets unauthenticated when no tokens exist`() = runTest {
        val h = Harness()
        h.authRepo.restoreSession()
        assertEquals(AuthenticationState.Unauthenticated, h.authRepo.authenticationState.value)
    }

    @Test
    fun `login register verify reset and logout update session and tokens`() = runTest {
        val h = Harness()
        h.authRepo.restoreSession()

        // Invalid credentials
        val badLogin = h.authRepo.login(FakeRemoteStore.SEEDED_USER_EMAIL, "wrong-password")
        assertEquals(AppResult.Error(AppError.Auth.InvalidCredentials), badLogin)
        assertEquals(AuthenticationState.Unauthenticated, h.authRepo.authenticationState.value)

        // Register new user -> requires email verification
        val registered = h.authRepo.register("Alice", "alice@example.com", "Password123!")
        assertTrue(registered is AppResult.Success)
        assertTrue((registered as AppResult.Success).data.requiresEmailVerification)

        // Login before verification fails with EmailNotVerified
        val unverifiedLogin = h.authRepo.login("alice@example.com", "Password123!")
        assertEquals(AppResult.Error(AppError.Auth.EmailNotVerified), unverifiedLogin)

        // Verify with invalid token fails
        val badVerify = h.authRepo.verifyEmail("alice@example.com", "wrong")
        assertEquals(AppResult.Error(AppError.Auth.InvalidVerificationToken), badVerify)

        // Verify with valid code succeeds
        val goodVerify = h.authRepo.verifyEmail("alice@example.com", FakeRemoteStore.DEMO_VERIFICATION_CODE)
        assertTrue(goodVerify is AppResult.Success)
        assertTrue((goodVerify as AppResult.Success).data.verified)

        // Login succeeds and stores tokens
        val loggedIn = h.authRepo.login("alice@example.com", "Password123!")
        assertTrue(loggedIn is AppResult.Success)
        assertTrue(h.authRepo.authenticationState.value is AuthenticationState.Authenticated)
        assertNotNull(h.tokenStorage.getAccessToken())
        assertNotNull(h.tokenStorage.getRefreshToken())

        // Restore session while active stays Authenticated
        h.authRepo.restoreSession()
        assertTrue(h.authRepo.authenticationState.value is AuthenticationState.Authenticated)

        // Logout clears tokens and transitions to Unauthenticated even if network fails
        h.config.simulatedErrorProvider = { UnknownHostException("offline") }
        h.authRepo.logout()
        h.config.simulatedErrorProvider = null
        assertNull(h.tokenStorage.getAccessToken())
        assertNull(h.tokenStorage.getRefreshToken())
        assertEquals(AuthenticationState.Unauthenticated, h.authRepo.authenticationState.value)

        // Forgot & reset password flow
        assertTrue(h.authRepo.requestPasswordReset("alice@example.com") is AppResult.Success)
        assertTrue(
            h.authRepo.resetPassword("alice@example.com", FakeRemoteStore.DEMO_RESET_CODE, "NewPass123!")
                is AppResult.Success
        )
        assertTrue(h.authRepo.login("alice@example.com", "NewPass123!") is AppResult.Success)
    }

    @Test
    fun `restoreSession refreshes expired access token and clears session when refresh fails`() = runTest {
        val h = Harness()
        // Login first so FakeRemoteStore has a valid refresh token
        assertTrue(
            h.authRepo.login(FakeRemoteStore.SEEDED_USER_EMAIL, FakeRemoteStore.SEEDED_USER_PASSWORD)
                is AppResult.Success
        )
        val validRefresh = h.tokenStorage.getRefreshToken()!!

        // Simulate 401 on first getCurrentUser, then 200 after refresh
        var firstMeCall = true
        val customUserRemote = object : UserRemoteDataSource by h.userRemote {
            override suspend fun getCurrentUser(): UserDto {
                if (firstMeCall) {
                    firstMeCall = false
                    throw HttpException(
                        Response.error<Unit>(401, "".toResponseBody("application/json".toMediaType()))
                    )
                }
                return h.userRemote.getCurrentUser()
            }
        }
        val repo = AuthRepositoryImpl(
            authRemoteDataSource = h.authRemote,
            userRemoteDataSource = customUserRemote,
            tokenStorage = h.tokenStorage,
            sessionStateHolder = h.sessionHolder,
            userPreferencesRepository = h.prefsRepo,
            sessionExpiryNotifier = h.notifier,
            safeRemoteCall = h.safeRemoteCall
        )
        h.tokenStorage.saveTokens("expired-token", validRefresh)

        repo.restoreSession()
        assertTrue(repo.authenticationState.value is AuthenticationState.Authenticated)
        assertTrue(h.tokenStorage.getRefreshToken() != validRefresh)

        // Now force 401 with invalid refresh token -> session cleared & Unauthenticated
        firstMeCall = true
        h.tokenStorage.saveTokens("expired-token", "revoked-refresh-token")
        repo.restoreSession()
        assertEquals(AuthenticationState.Unauthenticated, repo.authenticationState.value)
        assertNull(h.tokenStorage.getAccessToken())
        assertNull(h.tokenStorage.getRefreshToken())
    }

    @Test
    fun `user repository manages profile preferences consents export and account deletion`() = runTest {
        val h = Harness()
        h.authRepo.login(FakeRemoteStore.SEEDED_USER_EMAIL, FakeRemoteStore.SEEDED_USER_PASSWORD)

        // Profile update updates authenticated user state
        val updatedUser = h.userRepo.updateProfile("Claudio", "cla@example.com")
        assertTrue(updatedUser is AppResult.Success)
        assertEquals(
            "Claudio",
            (h.authRepo.authenticationState.value as AuthenticationState.Authenticated).user.name
        )

        // Preferences update syncs local UserPreferencesRepository
        val prefsUpdated = h.userRepo.updatePreferences(
            AppPreferences(AppThemeMode.DARK, Currency.getInstance("USD"))
        )
        assertTrue(prefsUpdated is AppResult.Success)
        assertEquals(AppThemeMode.DARK, h.prefsRepo.preferences.value.themeMode)
        assertEquals(Currency.getInstance("USD"), h.prefsRepo.preferences.value.currency)

        // Privacy & consents
        val privacy = h.userRepo.getPrivacyInfo()
        assertTrue(privacy is AppResult.Success)
        val consents = (privacy as AppResult.Success).data.consents
        val requiredConsent = consents.first { it.required }
        val optionalConsent = consents.first { !it.required }

        val revokeRequired = h.userRepo.updateConsent(requiredConsent.id, false)
        assertEquals(
            AppResult.Error(AppError.BusinessRule(BusinessErrorReason.REQUIRED_CONSENT_CANNOT_BE_REVOKED)),
            revokeRequired
        )

        val toggleOptional = h.userRepo.updateConsent(optionalConsent.id, true)
        assertTrue(toggleOptional is AppResult.Success)
        assertTrue((toggleOptional as AppResult.Success).data.first { it.id == optionalConsent.id }.granted)

        // Export user data
        val export = h.userRepo.exportUserData()
        assertTrue(export is AppResult.Success)
        val exportData = (export as AppResult.Success).data
        assertTrue(exportData.contentBytes.isNotEmpty())
        assertTrue(String(exportData.contentBytes).contains("cla@example.com"))

        // Delete account clears session & resets state to Unauthenticated
        val deleted = h.userRepo.deleteAccount("DELETE", FakeRemoteStore.SEEDED_USER_PASSWORD)
        assertTrue(deleted is AppResult.Success)
        assertNull(h.tokenStorage.getAccessToken())
        assertEquals(AuthenticationState.Unauthenticated, h.authRepo.authenticationState.value)
    }

    @Test
    fun `error mapper covers 429 and auth error codes without breaking existing mappings`() {
        assertEquals(
            AppError.Network.TooManyRequests,
            errorMapper.map(HttpException(Response.error<Unit>(429, "".toResponseBody("application/json".toMediaType()))))
        )
    }
}

