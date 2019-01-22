package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class PrizePojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val Partner: String = "",
        val userId: String = "",
        val codeStateId: String = "",
        val externalId: String = ""
        )