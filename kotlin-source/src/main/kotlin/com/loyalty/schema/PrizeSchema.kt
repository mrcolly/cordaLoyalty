package com.loyalty.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object PrizeSchema


object PrizeSchemaV1 : MappedSchema(
        schemaFamily = PrizeSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPrize::class.java)) {
    @Entity
    @Table(name = "prize_states")
    class PersistentPrize(
            @Column(name = "Eni")
            var Eni: String,

            @Column(name="Partner")
            var Partner:String,

            @Column(name = "userId")
            var userId: String,

            @Column(name = "codeStateId")
            var codeStateId: String,

            @Column(name = "timestamp")
            var timestamp: Instant,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "","", Instant.now(), UUID.randomUUID())
    }
}