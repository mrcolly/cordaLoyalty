package com.loyalty.state

import com.loyalty.schema.UserSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

data class UserState(val Eni: Party,
                     val loyaltyBalance: Int,
                     val lastOperation: String,
                     val operationType: Char,
                     val deltaLoyalty: Int,
                     val timestamp: Instant,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(Eni)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is UserSchemaV1 -> UserSchemaV1.PersistentUser(
                    this.Eni.name.toString(),
                    this.loyaltyBalance,
                    this.lastOperation,
                    this.operationType,
                    this.deltaLoyalty,
                    this.timestamp,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UserSchemaV1)
}