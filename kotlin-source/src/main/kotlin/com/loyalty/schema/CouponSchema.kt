package com.loyalty.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CouponSchema


object CouponSchemaV1 : MappedSchema(
        schemaFamily = CouponSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentCoupon::class.java)) {
    @Entity
    @Table(name = "coupon_states")
    class PersistentCoupon(
            @Column(name = "Eni")
            var Eni: String,

            @Column(name="points")
            var points: Int,

            @Column(name="userId")
            var userId: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", 0,"", UUID.randomUUID())
    }
}