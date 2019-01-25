package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class CouponPojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val points: Int = 0,
        val userId: String = "",
        val externalId: String = generateId()
)

fun generateId(): String{
    return "C"+ System.currentTimeMillis()
}