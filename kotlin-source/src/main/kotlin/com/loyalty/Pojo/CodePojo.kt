package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class CodePojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val Partner: String = "",
        val points: Int = 0,
        val userId: String = "",
        val externalId: String = ""
)
