package com.loyalty.state

import com.loyalty.schema.BillSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

data class BillState(val Eni: Party,
                    val userId: String,
                    val amount: Double,
                    val emissionDate: Instant,
                    val earnedPoints: Int,
                    val couponStateId: String,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(Eni)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BillSchemaV1 -> BillSchemaV1.PersistentBill(
                    this.Eni.name.toString(),
                    this.userId,
                    this.amount,
                    this.emissionDate,
                    this.earnedPoints,
                    this.couponStateId,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BillSchemaV1)
}