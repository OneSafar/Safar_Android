package com.safarparmar.app.ui.premium

import androidx.lifecycle.ViewModelStore
import com.safarparmar.app.data.remote.dto.DhyanPricingDto
import com.safarparmar.app.data.remote.dto.VerifyPaymentResponseDto
import com.safarparmar.app.data.repository.PaymentRepository
import com.safarparmar.app.data.repository.PremiumRepository
import com.safarparmar.app.domain.model.PremiumStatus
import com.safarparmar.app.domain.model.UserProfile
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.ui.auth.MainDispatcherRule
import com.safarparmar.app.util.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumDhyanAccessTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val payments = mockk<PaymentRepository>()
    private val premium = mockk<PremiumRepository>()
    private val auth = mockk<AuthRepository>()
    private val store = ViewModelStore()

    @Before fun setup() {
        val legacy = PremiumStatus(isPremium = true, planType = "3month")
        every { premium.cachedStatus } returns flowOf(legacy)
        coEvery { premium.refreshStatus() } returns Result.success(legacy)
        coEvery { auth.getMe() } returns Resource.Success(UserProfile(id = "buyer", email = "buyer@example.com"))
        coEvery { payments.getDhyanPricing() } returns Result.success(DhyanPricingDto(accessState = "LEGACY_PREMIUM_DISCOUNT", couponEligible = true))
    }

    @After fun cleanup() { store.clear() }

    private fun create() = PremiumViewModel(payments, premium, auth).also { store.put("test", it) }

    @Test fun legacyPremiumDoesNotUnlockCourse() = runTest {
        val vm = create()
        advanceUntilIdle()
        assertEquals("DENIED", vm.dhyanLiveAccess.value)
        assertEquals("LEGACY_PREMIUM_DISCOUNT", vm.dhyanPricing.value.accessState)
    }

    @Test fun pricingFailureDoesNotFallBackToPremium() = runTest {
        coEvery { payments.getDhyanPricing() } returns Result.failure(Exception("offline"))
        val vm = create()
        advanceUntilIdle()
        assertEquals("ERROR", vm.dhyanLiveAccess.value)
    }

    @Test fun verifiedStandalonePurchaseRefreshesCourseAccess() = runTest {
        val vm = create()
        advanceUntilIdle()
        assertEquals("DENIED", vm.dhyanLiveAccess.value)
        every { payments.createOrder(49, "safar-30", null) } returns emptyFlow()
        vm.createDhyanOrder()
        advanceUntilIdle()
        coEvery { payments.getDhyanPricing() } returns Result.success(DhyanPricingDto(alreadyHasLive = true, accessState = "DHYAN_INCLUDED"))
        every { payments.verifyPayment("order", "payment", "signature") } returns flowOf(Result.success(VerifyPaymentResponseDto(success = true, message = null)))
        vm.verifyPayment("order", "payment", "signature")
        advanceUntilIdle()
        assertEquals("ALLOWED", vm.dhyanLiveAccess.value)
        assertEquals(PremiumUiState.DhyanPaymentSuccess, vm.uiState.value)
    }

    @Test fun returningFromCheckoutRefreshesExistingScreen() = runTest {
        val vm = create()
        advanceUntilIdle()
        coEvery { payments.getDhyanPricing() } returns Result.success(DhyanPricingDto(alreadyHasLive = true, accessState = "DHYAN_INCLUDED"))
        vm.refreshDhyanAccess()
        advanceUntilIdle()
        assertEquals("ALLOWED", vm.dhyanLiveAccess.value)
        coEvery { payments.getDhyanPricing() } returns Result.success(DhyanPricingDto(accessState = "LEGACY_PREMIUM_DISCOUNT"))
        vm.refreshDhyanAccess()
        advanceUntilIdle()
        assertEquals("DENIED", vm.dhyanLiveAccess.value)
    }
}
