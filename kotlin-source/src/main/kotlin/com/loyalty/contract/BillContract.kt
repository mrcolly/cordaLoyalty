package com.loyalty.contract

import com.loyalty.state.BillState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey
import java.time.Instant

class BillContract : Contract {
    companion object {
        @JvmStatic
        val BILL_CONTRACT_ID = "com.example.loyalty.contract.BillContract"
    }


    override fun verify(tx: LedgerTransaction) {
        val commands = tx.commandsOfType<Commands>()
        for(command in commands) {
            val setOfSigners = command.signers.toSet()
            when (command.value) {
                is Commands.Create -> verifyCreate(tx, setOfSigners)
                else -> throw IllegalArgumentException("Unrecognised command.")
            }
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val bill = tx.outputsOfType<BillState>().single()
        "userId must be non-null" using (bill.userId.length > 0)
        "amount must be greather than 0" using (bill.amount > 0)
        "emissiondate must be in the past" using (bill.emissionDate < Instant.now())
        "earnedPoints must be greather than 0" using (bill.earnedPoints >= 0)
        "All of the participants must be signers." using (signers.containsAll(bill.participants.map { it.owningKey }))
    }


    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }

}