package com.safarparmar.app.data.repository

import com.safarparmar.app.data.remote.api.PaymentApi
import com.safarparmar.app.data.remote.dto.CreateOrderRequestDto
import com.safarparmar.app.data.remote.dto.VerifyPaymentRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PaymentRepository @Inject constructor(
    private val api: PaymentApi
) {
    fun createOrder(amount: Int, courseId: String): Flow<Result<com.safarparmar.app.data.remote.dto.CreateOrderResponseWrapper>> = flow {
        try {
            val response = api.createOrder(CreateOrderRequestDto(amount, courseId))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception(response.message() ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String): Flow<Result<Boolean>> = flow {
        try {
            val response = api.verifyPayment(
                VerifyPaymentRequestDto(
                    razorpay_order_id = orderId,
                    razorpay_payment_id = paymentId,
                    razorpay_signature = signature
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception(response.body()?.message ?: response.message() ?: "Verification failed")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
