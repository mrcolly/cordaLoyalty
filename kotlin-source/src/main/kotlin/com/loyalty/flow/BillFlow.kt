package com.loyalty.flow

import co.paralleluniverse.fibers.Suspendable
import com.loyalty.Pojo.BillPojo
import com.loyalty.contract.BillContract
import com.loyalty.contract.CouponContract
import com.loyalty.contract.UserContract
import com.loyalty.state.BillState
import com.loyalty.state.CouponState
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
                  val billPojo: BillPojo) : FlowLogic<BillState>() {

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
        override fun call(): BillState {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val myLegalIdentity = serviceHub.myInfo.legalIdentities.first()

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            if(myLegalIdentity.name.organisation != "Eni"){
                throw FlowException("cannot start bill flow from this node")
            }

            val couponStateInput = getCouponState(billPojo.couponStateId, serviceHub)
            val updateUser = getUser(billPojo.userId, serviceHub)


            val billState = BillState(Eni,
                    billPojo.userId,
                    billPojo.amount,
                    billPojo.emissionDate,
                    billPojo.earnedPoints,
                    billPojo.couponStateId,
                    billPojo.type,
                    billPojo.expirationDate,
                    UniqueIdentifier(id = UUID.randomUUID(), externalId = billPojo.externalId))

            if(couponStateInput != null) {
                billState.earnedPoints = 0
                billState.amount -= couponStateInput.state.data.points
            }

            val txCommand = Command(BillContract.Commands.Create(), billState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(billState, BillContract.BILL_CONTRACT_ID)
                    .addCommand(txCommand)

            if(couponStateInput != null) {
                txBuilder.addInputState(couponStateInput)
                       // .addCommand(CouponContract.Commands.Consume(), couponStateInput.state.data.participants.map { it.owningKey })
            }

            if(updateUser != null){
                var userState = updateUser.state.data

                if(couponStateInput != null && couponStateInput.state.data.userId != userState.linearId.externalId){
                    throw FlowException(userState.linearId.externalId+" cannot spend this coupon")
                }

                val oldBalance = userState.loyaltyBalance

                userState.loyaltyBalance = userState.loyaltyBalance + billState.earnedPoints
                userState.deltaLoyalty = userState.loyaltyBalance - oldBalance
                userState.operationType = 'B'
                userState.lastOperation = billState.linearId.id.toString()

                txBuilder
                        .addInputState(updateUser)
                        .addOutputState(userState, UserContract.USER_CONTRACT_ID)
                        .addCommand(UserContract.Commands.Update(), billState.participants.map { it.owningKey })
            }else{
                val userState = UserState(Eni,
                        1000 + billState.earnedPoints, //bonus first bill
                        billState.linearId.id.toString(),
                        'B',
                        1000 + billState.earnedPoints,
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
            subFlow(FinalityFlow(signedTx, FINALISING_TRANSACTION.childProgressTracker()))
            return billState
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
    fun getCouponState(couponStateId: String, serviceHub: ServiceHub): StateAndRef<CouponState>? {

        if(couponStateId.length<1) return null

        var criteria : QueryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val customCriteria = QueryCriteria.LinearStateQueryCriteria( externalId = listOf(couponStateId))
        criteria = criteria.and(customCriteria)

        val couponStates = serviceHub.vaultService.queryBy<CouponState>(
                criteria,
                PageSpecification(1, MAX_PAGE_SIZE),
                Sort(setOf(Sort.SortColumn(SortAttribute.Standard(Sort.VaultStateAttribute.RECORDED_TIME), Sort.Direction.DESC)))
        ).states

        if(couponStates.size > 1 || couponStates.size == 0) throw FlowException("no coupon state with UUID" +couponStateId+ "found")

        return couponStates.get(0)
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
