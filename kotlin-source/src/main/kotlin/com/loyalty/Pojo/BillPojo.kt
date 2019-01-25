package com.loyalty.Pojo

import net.corda.core.serialization.CordaSerializable
import java.time.Duration
import java.time.Instant

@CordaSerializable
data class BillPojo(
        val Eni: String = "O=Eni,L=Milan,C=IT",
        val userId: String = "",
        val amount: Double = 0.0,
        val emissionDate: Instant = Instant.now().minusSeconds(200),
        val earnedPoints: Int = if((getHoursDifference(emissionDate, Instant.now())) < 72) (amount * 0.2).toInt() else (amount * 0.1).toInt(),
        val couponStateId: String = "",
        val type: Char = '-',
        val expirationDate: Instant = emissionDate.plusSeconds(108000)
        )


private fun getHoursDifference(before: Instant, after: Instant): Long{
    return Duration.between(before, after).seconds / 3600
}