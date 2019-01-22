package com.loyalty.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object UserSchema


object UserSchemaV1 : MappedSchema(
        schemaFamily = UserSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentUser::class.java)) {
    @Entity
    @Table(name = "user_states")
    class PersistentUser(
            @Column(name = "Eni")
            var Eni: String,

            @Column(name = "loyaltyBalance")
            var loyaltyBalance: Int,

            @Column(name = "lastOperation")
            var lastOperation: String,

            @Column(name = "operationType")
            var operationType: Char,

            @Column(name = "deltaLoyalty")
            var deltaLoyalty: Int,

            @Column(name = "linear_id")
            var linearId: UUID

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", 0, "", '-',0, UUID.randomUUID())
    }
}