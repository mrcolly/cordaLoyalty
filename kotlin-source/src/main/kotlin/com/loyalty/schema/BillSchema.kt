package com.loyalty.schema

import com.example.schema.IOUSchema
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BillSchema


object BillSchemaV1 : MappedSchema(
        schemaFamily = BillSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBill::class.java)) {
    @Entity
    @Table(name = "bill_states")
    class PersistentBill(
            @Column(name = "Eni")
            var Eni: String,

            @Column(name = "userId")
            var userId: String,

            @Column(name = "amount")
            var amount: Double,

            @Column(name = "emissionDate")
            var emissionDate: Instant,

            @Column(name = "earnedPoints")
            var earnedPoints: Int,

            @Column(name = "couponStateId")
            var couponStateId: String,

            @Column(name = "type")
            var type: Char,

            @Column(name = "expirationDate")
            var expirationDate: Instant,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 0.0,Instant.now(),0,"", '-', Instant.now(), UUID.randomUUID())
    }
}