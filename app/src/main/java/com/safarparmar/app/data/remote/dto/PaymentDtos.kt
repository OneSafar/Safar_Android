package com.safarparmar.app.data.remote.dto

data class CreateOrderRequestDto(
    val amount: Int,
    val courseId: String,
    val couponCode: String? = null,
)

data class DhyanPricingDto(
    val productId: String = "safar-30",
    val durationMonths: Int = 6,
    val standardPrice: Int = 49,
    val premiumPrice: Int = 29,
    val couponEligible: Boolean = false,
    val alreadyHasLive: Boolean = false,
    val accessState: String = "LOADING",
    val availablePlanIds: List<String> = emptyList(),
    val dhyanStartsAt: String? = null,
    val dhyanExpiresAt: String? = null,
)

data class ExtendPlanRequestDto(
    val duration: Int
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
    val message: String?,
    val premium: PremiumStatusResponse? = null,
)
