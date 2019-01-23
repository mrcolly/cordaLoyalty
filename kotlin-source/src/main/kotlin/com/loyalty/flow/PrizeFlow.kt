package com.loyalty.flow

import co.paralleluniverse.fibers.Suspendable
import com.loyalty.Pojo.BillPojo
import com.loyalty.Pojo.PrizePojo
import com.loyalty.contract.BillContract
import com.loyalty.contract.PrizeContract
import com.loyalty.contract.UserContract
import com.loyalty.state.BillState
import com.loyalty.state.CodeState
import com.loyalty.state.PrizeState
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
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap
import java.util.*


object PrizeFlow {
    @InitiatingFlow
    @StartableByRPC
    class Creator(val Eni: Party,
                  val prizePojo: PrizePojo) : FlowLogic<SignedTransaction>() {

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
            if(myLegalIdentity.name.organisation == "Eni"){
                throw FlowException("cannot start prize flow from this node")
            }

            val eniSession = initiateFlow(Eni)

            val prizeState = PrizeState(Eni,
                    myLegalIdentity,
                    prizePojo.userId,
                    prizePojo.codeStateId,
                    prizePojo.costPoints,
                    UniqueIdentifier(id = UUID.randomUUID(), externalId = prizePojo.externalId)
            )

            val txCommand = Command(PrizeContract.Commands.Create(), prizeState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(prizeState, PrizeContract.PRIZE_CONTRACT_ID)
                    .addCommand(txCommand)

            eniSession.send(prizePojo)

            val packet1: UntrustworthyData<StateAndRef<CodeState>> = eniSession.receive<StateAndRef<CodeState>>()
            val codeStateInput: StateAndRef<CodeState> = packet1.unwrap { data ->
                data
            }

            txBuilder.addInputState(codeStateInput)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS

            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(eniSession), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Creator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {


            val packet1: UntrustworthyData<PrizePojo> = otherPartyFlow.receive<PrizePojo>()
            val prizePojo: PrizePojo = packet1.unwrap { data ->
                data
            }

            val codeStateInput = BillFlow.getCodeState(prizePojo.codeStateId, serviceHub)

            if(codeStateInput!=null) otherPartyFlow.send(codeStateInput) else throw FlowException("cannot find code "+ prizePojo.codeStateId)

            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                }
            }

            return subFlow(signTransactionFlow)
        }
    }

    @Throws(FlowException::class)
    fun getCodeState(codeStateId: String, serviceHub: ServiceHub): StateAndRef<CodeState>? {

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
    fun getUser(userStateId: String, serviceHub: ServiceHub): StateAndRef<UserState>? {

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
