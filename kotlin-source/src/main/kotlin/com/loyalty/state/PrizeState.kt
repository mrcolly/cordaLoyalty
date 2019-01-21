package com.loyalty.state

import com.loyalty.schema.PrizeSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

data class PrizeState(val Partner: Party,
                      val Eni: Party,
                      val userId: String,
                      val codeStateId: String,
                      val idPrize: String,
                      val timestamp: Instant,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(Partner, Eni)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PrizeSchemaV1 -> PrizeSchemaV1.PersistentPrize(
                    this.Eni.name.toString(),
                    this.Partner.name.toString(),
                    this.userId,
                    this.codeStateId,
                    this.idPrize,
                    this.timestamp,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PrizeSchemaV1)
}