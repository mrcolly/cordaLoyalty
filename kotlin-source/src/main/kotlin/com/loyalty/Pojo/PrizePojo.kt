package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class PrizePojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val Partner: String = "",
        val userId: String = "",
        val codeStateId: String = "",
        val externalId: String = "",
        val timestamp: Instant = Instant.now()
        )