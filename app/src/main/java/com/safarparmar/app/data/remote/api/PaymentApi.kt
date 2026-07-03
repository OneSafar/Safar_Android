package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.CreateOrderRequestDto
import com.safarparmar.app.data.remote.dto.CreateOrderResponseDto
import com.safarparmar.app.data.remote.dto.ExtendPlanRequestDto
import com.safarparmar.app.data.remote.dto.VerifyPaymentRequestDto
import com.safarparmar.app.data.remote.dto.VerifyPaymentResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentApi {
    @POST("payments/create-order")
    suspend fun createOrder(@Body request: CreateOrderRequestDto): Response<com.safarparmar.app.data.remote.dto.CreateOrderResponseWrapper>

    @POST("extend-plan")
    suspend fun extendPlan(@Body request: ExtendPlanRequestDto): Response<com.safarparmar.app.data.remote.dto.CreateOrderResponseWrapper>

    @POST("payments/verify")
    suspend fun verifyPayment(@Body request: VerifyPaymentRequestDto): Response<VerifyPaymentResponseDto>
}
