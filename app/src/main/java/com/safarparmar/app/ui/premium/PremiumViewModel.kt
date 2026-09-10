package com.safarparmar.app.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.repository.PaymentRepository
import com.safarparmar.app.data.repository.PremiumRepository
import com.safarparmar.app.domain.model.PremiumStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PremiumUiState {
    object Idle : PremiumUiState()
    object Loading : PremiumUiState()
    data class OrderCreated(val order: com.safarparmar.app.data.remote.dto.CreateOrderResponseDto, val planType: String, val keyId: String?) : PremiumUiState()
    data class PaymentSuccess(val status: PremiumStatus, val isRestore: Boolean = false) : PremiumUiState()
    object DhyanPaymentSuccess : PremiumUiState()
    object NoActivePlan : PremiumUiState()
    data class Error(val message: String) : PremiumUiState()
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val premiumRepository: PremiumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PremiumUiState>(PremiumUiState.Idle)
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _premiumStatus = MutableStateFlow(PremiumStatus())
    val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()

    private val _dhyanPricing = MutableStateFlow(com.safarparmar.app.data.remote.dto.DhyanPricingDto())
    val dhyanPricing: StateFlow<com.safarparmar.app.data.remote.dto.DhyanPricingDto> = _dhyanPricing.asStateFlow()
    private var pendingCourseId: String? = null

    init {
        viewModelScope.launch {
            premiumRepository.cachedStatus.collect { status ->
                _premiumStatus.value = status
            }
        }
        refreshPremiumStatus(showLoading = false)
        viewModelScope.launch {
            paymentRepository.getDhyanPricing()
                .onSuccess { _dhyanPricing.value = it }
                .onFailure { _dhyanPricing.value = com.safarparmar.app.data.remote.dto.DhyanPricingDto(accessState = "ERROR") }
        }
        viewModelScope.launch {
            PaymentEventBus.paymentEvents.collect { event ->
                when (event) {
                    is PaymentEvent.Success -> {
                        val data = event.paymentData
                        if (
                            data?.orderId?.isNotBlank() == true &&
                            data.paymentId?.isNotBlank() == true &&
                            data.signature?.isNotBlank() == true
                        ) {
                            verifyPayment(
                                orderId = data.orderId,
                                paymentId = data.paymentId,
                                signature = data.signature
                            )
                        } else {
                            refreshPremiumStatus(
                                showLoading = true,
                                fallbackError = "Payment returned from PhonePe/Razorpay, but verification data was incomplete. Please tap Restore Safar Premium in a moment."
                            )
                        }
                    }
                    is PaymentEvent.Error -> {
                        _uiState.value = PremiumUiState.Error(event.description ?: "Payment failed")
                    }
                }
            }
        }
    }

    fun createOrder(duration: Int) {
        pendingCourseId = if (duration == 6) "study-planner-pro-6month" else "study-planner-pro-3month"
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            paymentRepository.extendPlan(duration).collect { result ->
                result.fold(
                    onSuccess = { orderResponse ->
                        _uiState.value = PremiumUiState.OrderCreated(orderResponse.order, "${duration}month", orderResponse.keyId)
                    },
                    onFailure = { error ->
                        _uiState.value = PremiumUiState.Error(error.message ?: "Failed to create order")
                    }
                )
            }
        }
    }

    fun createDhyanOrder() {
        pendingCourseId = "safar-30"
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            paymentRepository.createOrder(49, "safar-30").collect { result ->
                result.fold(
                    onSuccess = { response -> _uiState.value = PremiumUiState.OrderCreated(response.order, "dhyan", response.keyId) },
                    onFailure = { error -> _uiState.value = PremiumUiState.Error(error.message ?: "Failed to create Dhyan order") },
                )
            }
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            paymentRepository.verifyPayment(orderId, paymentId, signature).collect { result ->
                result.fold(
                    onSuccess = { verification ->
                        if (pendingCourseId == "safar-30") {
                            pendingCourseId = null
                            _uiState.value = PremiumUiState.DhyanPaymentSuccess
                            return@fold
                        }
                        val embeddedStatus = premiumRepository.cacheVerifiedStatus(verification.premium)
                        val statusResult = if (embeddedStatus?.hasAnyPaidAccess == true) {
                            Result.success(embeddedStatus)
                        } else {
                            premiumRepository.refreshStatus()
                        }
                        statusResult.fold(
                            onSuccess = { status ->
                                if (status.hasAnyPaidAccess) {
                                    _premiumStatus.value = status
                                    _uiState.value = PremiumUiState.PaymentSuccess(status)
                                } else {
                                    _uiState.value = PremiumUiState.Error("Payment verified, but paid access is not active yet. Please use Restore Safar Premium in a moment.")
                                }
                            },
                            onFailure = { error ->
                                _uiState.value = PremiumUiState.Error(error.message ?: "Payment verified, but Safar Premium status could not be restored")
                            },
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = PremiumUiState.Error(error.message ?: "Payment verification failed")
                    }
                )
            }
        }
    }

    fun refreshPremiumStatus(
        showLoading: Boolean = true,
        fallbackError: String = "No active Safar Premium plan found.",
        isRestore: Boolean = false,
    ) {
        if (showLoading) _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            premiumRepository.refreshStatus().fold(
                onSuccess = { status ->
                    _premiumStatus.value = status
                    if (showLoading) {
                        _uiState.value = if (status.hasAnyPaidAccess) {
                            PremiumUiState.PaymentSuccess(status, isRestore = isRestore)
                        } else {
                            if (isRestore) PremiumUiState.NoActivePlan else PremiumUiState.Error(fallbackError)
                        }
                    }
                },
                onFailure = { error ->
                    if (showLoading) {
                        _uiState.value = PremiumUiState.Error(error.message ?: "Could not restore Safar Premium status")
                    }
                },
            )
        }
    }

    fun startFreeTrial() {
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            premiumRepository.startTrial().fold(
                onSuccess = { status ->
                    _premiumStatus.value = status
                    _uiState.value = if (status.hasAnyPaidAccess) {
                        PremiumUiState.PaymentSuccess(status)
                    } else {
                        PremiumUiState.Error("The trial was started, but premium access is not active yet. Please tap Restore Safar Premium in a moment.")
                    }
                },
                onFailure = { error ->
                    _uiState.value = PremiumUiState.Error(error.message ?: "Could not start the 7-day free trial")
                },
            )
        }
    }

    fun resetState() {
        _uiState.value = PremiumUiState.Idle
    }
    
    fun notifyPaymentFailed(errorDescription: String) {
        _uiState.value = PremiumUiState.Error("Payment Failed: $errorDescription")
    }
}
