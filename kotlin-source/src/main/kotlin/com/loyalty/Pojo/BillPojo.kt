package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class BillPojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val userId: String = "",
        val amount: Double = 0.0,
        val emissionDate: Instant = Instant.now().minusSeconds(200),
        val earnedPoints: Int = 0,
        val couponStateId: String = ""
)