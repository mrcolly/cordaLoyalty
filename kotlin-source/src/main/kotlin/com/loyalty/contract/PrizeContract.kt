package com.loyalty.contract

import com.loyalty.state.PrizeState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey
import java.time.Instant

class PrizeContract : Contract {
    companion object {
        @JvmStatic
        val PRIZE_CONTRACT_ID = "com.example.loyalty.contract.PrizeContract"
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
        "create must have one Prize output" using (!tx.outputsOfType<PrizeState>().isEmpty())
        val prize = tx.outputsOfType<PrizeState>().single()
        "userId must be non-null" using (prize.userId.length > 0)
        "All of the participants must be signers." using (signers.containsAll(prize.participants.map { it.owningKey }))
        "Partecipants must differ" using (prize.Eni.name != prize.Partner.name)
    }


    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands, TypeOnlyCommandData()
    }

}