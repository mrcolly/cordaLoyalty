package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class PrizePojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val Partner: String = "",
        val userId: String = "",
        val couponStateId: String = "",
        val costPoints: Int = 0,
        val externalId: String = ""
        )
