package com.loyalty.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CodeSchema


object CodeSchemaV1 : MappedSchema(
        schemaFamily = CodeSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentCode::class.java)) {
    @Entity
    @Table(name = "code_states")
    class PersistentCode(
            @Column(name = "Eni")
            var Eni: String,

            @Column(name="Partner")
            var Partner: String,

            @Column(name="points")
            var points: Int,

            @Column(name="userId")
            var userId: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 0,"", UUID.randomUUID())
    }
}