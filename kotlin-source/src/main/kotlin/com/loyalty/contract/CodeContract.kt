package com.loyalty.contract

import com.loyalty.state.CodeState
import com.loyalty.state.PrizeState
import com.loyalty.state.UserState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.node.services.Vault
import java.security.PublicKey
import java.time.Instant

class CodeContract : Contract {
    companion object {
        @JvmStatic
        val CODE_CONTRACT_ID = "com.loyalty.contract.CodeContract"
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

        "create must have one Code output" using (!tx.outputsOfType<CodeState>().isEmpty())
        val code = tx.outputsOfType<CodeState>().single()
        "points must be grather than 0" using (code.points > 0)
        "All of the participants must be signers." using (signers.containsAll(code.participants.map { it.owningKey }))
        "Partecipants must differ" using (code.Eni.name != code.Partner.name)
        "create must have one User input" using (!tx.inputsOfType<UserState>().isEmpty())
        val user = tx.outputsOfType<UserState>().single()
        "user must have balance for the code" using (code.points <= user.loyaltyBalance)
    }


    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands, TypeOnlyCommandData()
    }

}