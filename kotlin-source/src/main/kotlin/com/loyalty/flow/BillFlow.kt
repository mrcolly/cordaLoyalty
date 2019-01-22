package com.loyalty.flow

import co.paralleluniverse.fibers.Suspendable
import com.loyalty.Pojo.BillPojo
import com.loyalty.contract.BillContract
import com.loyalty.contract.UserContract
import com.loyalty.state.BillState
import com.loyalty.state.CodeState
import com.loyalty.state.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*


object BillFlow {
    @InitiatingFlow
    @StartableByRPC
    class Creator(val Eni: Party,
                  val billPojo: BillPojo) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()


        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val myLegalIdentity = serviceHub.myInfo.legalIdentities.first()

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            if(myLegalIdentity.name.organisation != "Eni"){
                throw FlowException("cannot start bill flow from this node")
            }

            val codeStateInput = getCodeState(billPojo.codeStateId, serviceHub)
            val updateUser = getUpdateUser(billPojo.userId, serviceHub)


            val billState = BillState(Eni,
                    billPojo.userId,
                    billPojo.amount,
                    billPojo.emissionDate,
                    billPojo.earnedPoints,
                    billPojo.codeStateId)

            val txCommand = Command(BillContract.Commands.Create(), billState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(billState, BillContract.BILL_CONTRACT_ID)
                    .addCommand(txCommand)

            if(codeStateInput != null) txBuilder.addInputState(codeStateInput)

            if(updateUser != null){
                var userState = updateUser.state.data
                val oldBalance = userState.loyaltyBalance

                if(codeStateInput == null){
                    userState.loyaltyBalance = userState.loyaltyBalance + billState.earnedPoints
                    userState.deltaLoyalty = oldBalance - userState.loyaltyBalance
                }else{
                    userState.deltaLoyalty = 0
                }
                userState.lastOperation = billState.linearId.id.toString()

                txBuilder
                        .addInputState(updateUser)
                        .addOutputState(userState, UserContract.USER_CONTRACT_ID)
                        .addCommand(UserContract.Commands.Update(), billState.participants.map { it.owningKey })
            }else{
                val userState = UserState(Eni,
                        billState.earnedPoints,
                        billState.linearId.id.toString(),
                        'B',
                        if(codeStateInput == null) billState.earnedPoints else 0,
                        UniqueIdentifier(id = UUID.randomUUID(), externalId = billPojo.userId))

                txBuilder
                        .addOutputState(userState, UserContract.USER_CONTRACT_ID)
                        .addCommand(UserContract.Commands.Create(), billState.participants.map { it.owningKey })
            }

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(signedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Creator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                }
            }

            return subFlow(signTransactionFlow)
        }
    }

    @Throws(FlowException::class)
    private fun getCodeState(codeStateId: String, serviceHub: ServiceHub): StateAndRef<CodeState>? {

        if(codeStateId.length<1) return null

        var criteria : QueryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val customCriteria = QueryCriteria.LinearStateQueryCriteria( uuid = listOf(UUID.fromString(codeStateId)))
        criteria = criteria.and(customCriteria)

        val codeStates = serviceHub.vaultService.queryBy<CodeState>(
                criteria,
                PageSpecification(1, MAX_PAGE_SIZE),
                Sort(setOf(Sort.SortColumn(SortAttribute.Standard(Sort.VaultStateAttribute.RECORDED_TIME), Sort.Direction.DESC)))
        ).states

        if(codeStates.size > 1 || codeStates.size == 0) throw FlowException("no code state with UUID" +codeStateId+ "found")

        return codeStates.get(0)
    }

    @Throws(FlowException::class)
    private fun getUpdateUser(userStateId: String, serviceHub: ServiceHub): StateAndRef<UserState>? {

        var criteria : QueryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val customCriteria = QueryCriteria.LinearStateQueryCriteria( externalId = listOf(userStateId))
        criteria = criteria.and(customCriteria)

        val userStates = serviceHub.vaultService.queryBy<UserState>(
                criteria,
                PageSpecification(1, MAX_PAGE_SIZE),
                Sort(setOf(Sort.SortColumn(SortAttribute.Standard(Sort.VaultStateAttribute.RECORDED_TIME), Sort.Direction.DESC)))
        ).states

        if(userStates.size > 1 || userStates.size == 0) return null

        return userStates.get(0)
    }
}
