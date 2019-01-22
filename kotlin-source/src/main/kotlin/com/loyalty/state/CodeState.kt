package com.loyalty.state

import com.loyalty.schema.CodeSchemaV1
import com.loyalty.schema.PrizeSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

data class CodeState(val Eni: Party,
                      val Partner: Party,
                      val points: Int,
                      val userId: String,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(Partner, Eni)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is CodeSchemaV1 -> CodeSchemaV1.PersistentCode(
                    this.Eni.name.toString(),
                    this.Partner.name.toString(),
                    this.points,
                    this.userId,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CodeSchemaV1)
}