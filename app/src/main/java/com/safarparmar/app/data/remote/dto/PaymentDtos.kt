package com.safarparmar.app.data.remote.dto

data class CreateOrderRequestDto(
    val amount: Int,
    val courseId: String
)

data class CreateOrderResponseWrapper(
    val success: Boolean,
    val order: CreateOrderResponseDto,
    val keyId: String?,
    val message: String?
)

data class CreateOrderResponseDto(
    val id: String,
    val amount: Int,
    val currency: String,
    val receipt: String?,
    val status: String?
)

data class VerifyPaymentRequestDto(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String
)

data class VerifyPaymentResponseDto(
    val success: Boolean,
    val message: String?
)
