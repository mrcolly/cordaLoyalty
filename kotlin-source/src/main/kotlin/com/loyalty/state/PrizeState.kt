package com.loyalty.state

import com.loyalty.flow.CouponFlow
import com.loyalty.schema.PrizeSchemaV1
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant

data class PrizeState(val Eni: Party,
                      val Partner: Party,
                      val userId: String,
                      val couponStateId: String,
                      val costPoints: Int,
                     override val linearId: UniqueIdentifier = UniqueIdentifier(),
                     val couponScheduledTime: Instant = Instant.now()):
        LinearState, QueryableState, SchedulableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(Partner, Eni)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create(CouponFlow.Consumer::class.java, thisStateRef), couponScheduledTime)
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PrizeSchemaV1 -> PrizeSchemaV1.PersistentPrize(
                    this.Eni.name.toString(),
                    this.Partner.name.toString(),
                    this.userId,
                    this.couponStateId,
                    this.costPoints,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PrizeSchemaV1)
}