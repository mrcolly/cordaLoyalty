package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class UserPojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val loyaltyBalance: Int = 0,
        val lastOperation: String = "",
        val operationType: Char = '-',
        val deltaLoyalty: Int = 0
        )