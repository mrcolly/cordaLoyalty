package com.loyalty.contract

import com.loyalty.state.UserState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey
import java.time.Instant

class UserContract : Contract {
    companion object {
        @JvmStatic
        val USER_CONTRACT_ID = "com.loyalty.contract.UserContract"
    }


    override fun verify(tx: LedgerTransaction) {
        val commands = tx.commandsOfType<Commands>()
        for(command in commands) {
            val setOfSigners = command.signers.toSet()
            when (command.value) {
                is Commands.Create -> verifyCreate(tx, setOfSigners)
                is Commands.Update -> verifyUpdate(tx, setOfSigners)
                else -> throw IllegalArgumentException("Unrecognised command.")
            }
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {

        "user already exist" using (tx.inputsOfType<UserState>().isEmpty())
        "create must have userState output" using (!tx.outputsOfType<UserState>().isEmpty())
        val user = tx.outputsOfType<UserState>().single()
        "userId must be non-null" using (!user.linearId.externalId.isNullOrEmpty())
        "lastOperation must be filled" using (user.lastOperation.length > 0)
        "loyaltyBalamce must be greather than 0" using (user.loyaltyBalance >= 0)
        "operationType must be Prize (P) or Bill (B)" using (user.operationType == 'P' || user.operationType == 'B' || user.operationType == 'C')
        "All of the participants must be signers." using (signers.containsAll(user.participants.map { it.owningKey }))
    }

    private fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {

        "update must have userState input" using (!tx.inputsOfType<UserState>().isEmpty())
        "update must have userState output" using (!tx.outputsOfType<UserState>().isEmpty())
        val userIn = tx.inputsOfType<UserState>().single()
        val userOut = tx.outputsOfType<UserState>().single()
        "userIn and userOut must have the same linearId" using (userIn.linearId == userOut.linearId)
        "loyaltyBalance must change" using (userIn.loyaltyBalance != userOut.loyaltyBalance)

        //userIn
        "userId must be non-null" using (!userIn.linearId.externalId.isNullOrEmpty())
        "lastOperation must be filled" using (userIn.lastOperation.length > 0)
        "operationType must be Prize (P) or Bill (B)" using (userIn.operationType == 'P' || userIn.operationType == 'B' || userIn.operationType == 'C')
        "All of the participants must be signers." using (signers.containsAll(userIn.participants.map { it.owningKey }))

        //userOut
        "userId must be non-null" using (!userOut.linearId.externalId.isNullOrEmpty())
        "lastOperation must be filled" using (userOut.lastOperation.length > 0)
        "loyaltyBalamce must be greather than 0" using (userOut.loyaltyBalance >= 0)
        "operationType must be Prize (P) or Bill (B)" using (userOut.operationType == 'P' || userOut.operationType == 'B' || userOut.operationType == 'C')
        "All of the participants must be signers." using (signers.containsAll(userOut.participants.map { it.owningKey }))


    }


    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands, TypeOnlyCommandData()
        class Update : Commands, TypeOnlyCommandData()
    }

}