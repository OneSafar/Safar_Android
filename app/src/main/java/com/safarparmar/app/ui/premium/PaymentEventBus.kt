package com.safarparmar.app.ui.premium

import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PaymentEventBus {
    private val _paymentEvents = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 1)
    val paymentEvents = _paymentEvents.asSharedFlow()

    fun postSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        _paymentEvents.tryEmit(PaymentEvent.Success(razorpayPaymentId, paymentData))
    }

    fun postError(code: Int, description: String?, paymentData: PaymentData?) {
        _paymentEvents.tryEmit(PaymentEvent.Error(code, description, paymentData))
    }
}

sealed class PaymentEvent {
    data class Success(val razorpayPaymentId: String?, val paymentData: PaymentData?) : PaymentEvent()
    data class Error(val code: Int, val description: String?, val paymentData: PaymentData?) : PaymentEvent()
}
