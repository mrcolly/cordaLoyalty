package com.loyalty.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.IOUContract
import com.example.state.IOUState
import com.loyalty.Pojo.CouponPojo
import com.loyalty.contract.CouponContract
import com.loyalty.contract.UserContract
import com.loyalty.flow.BillFlow.getUser
import com.loyalty.state.CouponState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*


object CouponFlow {
    @InitiatingFlow
    @StartableByRPC
    class Creator(val Eni: Party,
                  val partner: Party,
                  val couponPojo : CouponPojo) : FlowLogic<SignedTransaction>() {

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
                throw FlowException("cannot start couponCreator flow from this node")
            }

            val user = getUser(couponPojo.userId, serviceHub)

            if(user == null) throw FlowException("no user " + couponPojo.userId + " found")

            val couponState = CouponState(Eni,
                    partner,
                    couponPojo.points,
                    couponPojo.userId,
                    UniqueIdentifier(id = UUID.randomUUID(), externalId = couponPojo.externalId))

            var updateUser = user.state.data
            updateUser.operationType = 'C'
            updateUser.lastOperation = couponState.linearId.externalId!!
            updateUser.deltaLoyalty = -couponState.points
            updateUser.loyaltyBalance += updateUser.deltaLoyalty


            val txCommand = Command(CouponContract.Commands.Create(), couponState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(couponState, CouponContract.COUPON_CONTRACT_ID)
                    .addCommand(txCommand)
                    .addInputState(user)
                    .addOutputState(updateUser, UserContract.USER_CONTRACT_ID)
                    .addCommand(UserContract.Commands.Update(), couponState.participants.map { it.owningKey })

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
            // Send the state to the counterparty, and receive it back with their signature.
            val partnerFlow = initiateFlow(partner)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(partnerFlow), GATHERING_SIGS.childProgressTracker()))

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
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}
