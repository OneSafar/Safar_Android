package com.safarparmar.app.data.repository

import com.safarparmar.app.data.remote.api.PaymentApi
import com.safarparmar.app.data.remote.dto.CreateOrderRequestDto
import com.safarparmar.app.data.remote.dto.ExtendPlanRequestDto
import com.safarparmar.app.data.remote.dto.VerifyPaymentRequestDto
import com.safarparmar.app.data.remote.dto.VerifyPaymentResponseDto
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
                val errorBody = response.errorBody()?.string()
                emit(Result.failure(Exception(errorBody ?: response.message() ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun extendPlan(duration: Int): Flow<Result<com.safarparmar.app.data.remote.dto.CreateOrderResponseWrapper>> = flow {
        try {
            val response = api.extendPlan(ExtendPlanRequestDto(duration = duration))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Result.failure(Exception(errorBody ?: response.message() ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String): Flow<Result<VerifyPaymentResponseDto>> = flow {
        try {
            val response = api.verifyPayment(
                VerifyPaymentRequestDto(
                    razorpay_order_id = orderId,
                    razorpay_payment_id = paymentId,
                    razorpay_signature = signature
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                emit(Result.success(body))
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Result.failure(Exception(body?.message ?: errorBody ?: response.message() ?: "Verification failed")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
