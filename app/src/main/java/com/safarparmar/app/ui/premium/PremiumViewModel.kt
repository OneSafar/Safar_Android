package com.safarparmar.app.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.repository.PaymentRepository
import com.safarparmar.app.data.remote.dto.CreateOrderResponseDto
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
    object PaymentSuccess : PremiumUiState()
    data class Error(val message: String) : PremiumUiState()
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PremiumUiState>(PremiumUiState.Idle)
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            PaymentEventBus.paymentEvents.collect { event ->
                when (event) {
                    is PaymentEvent.Success -> {
                        val data = event.paymentData
                        if (data != null) {
                            verifyPayment(
                                orderId = data.orderId,
                                paymentId = data.paymentId,
                                signature = data.signature
                            )
                        } else {
                            _uiState.value = PremiumUiState.Error("Missing payment data")
                        }
                    }
                    is PaymentEvent.Error -> {
                        _uiState.value = PremiumUiState.Error(event.description ?: "Payment failed")
                    }
                }
            }
        }
    }

    fun createOrder(amount: Int, courseId: String) {
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            paymentRepository.createOrder(amount, courseId).collect { result ->
                result.fold(
                    onSuccess = { orderResponse ->
                        _uiState.value = PremiumUiState.OrderCreated(orderResponse.order, courseId, orderResponse.keyId)
                    },
                    onFailure = { error ->
                        _uiState.value = PremiumUiState.Error(error.message ?: "Failed to create order")
                    }
                )
            }
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            paymentRepository.verifyPayment(orderId, paymentId, signature).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = PremiumUiState.PaymentSuccess
                    },
                    onFailure = { error ->
                        _uiState.value = PremiumUiState.Error(error.message ?: "Payment verification failed")
                    }
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = PremiumUiState.Idle
    }
    
    fun notifyPaymentFailed(errorDescription: String) {
        _uiState.value = PremiumUiState.Error("Payment Failed: $errorDescription")
    }
}
