package com.loyalty.api


import com.loyalty.Pojo.BillPojo
import com.loyalty.flow.BillFlow
import com.loyalty.state.BillState
import com.loyalty.state.UserState
import com.snam.POJO.ResponsePojo
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow

@Path("user")
class UserApi(private val rpcOps: CordaRPCOps) {

    companion object {
        private val logger: Logger = loggerFor<CouponApi>()
    }


    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllProposalsByParams(@DefaultValue("1") @QueryParam("page") page: Int,
                                @DefaultValue("") @QueryParam("id") externalId: String,
                                @DefaultValue("unconsumed") @QueryParam("status") status: String): Response {

        try {
            var myPage = page

            if (myPage < 1) {
                myPage = 1
            }

            var myStatus = Vault.StateStatus.UNCONSUMED

            when (status) {
                "consumed" -> myStatus = Vault.StateStatus.CONSUMED
                "all" -> myStatus = Vault.StateStatus.ALL
            }

            val results = builder {

                var criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(myStatus)


                if (externalId.length > 0) {
                    val customCriteria = QueryCriteria.LinearStateQueryCriteria(externalId = listOf(externalId), status = myStatus)
                    criteria = criteria.and(customCriteria)
                }

                val results = rpcOps.vaultQueryBy<UserState>(
                        criteria,
                        PageSpecification(myPage, DEFAULT_PAGE_SIZE),
                        Sort(setOf(Sort.SortColumn(SortAttribute.Standard(Sort.VaultStateAttribute.RECORDED_TIME), Sort.Direction.DESC)))
                )

                val statesAndMetadata = results.states.zip(results.statesMetadata)

                return Response.ok(statesAndMetadata).build()
            }
        } catch (ex: Exception) {
            val msg = ex.message
            logger.error(ex.message, ex)
            val resp = ResponsePojo("ERROR", msg!!)
            return Response.status(BAD_REQUEST).entity(resp).build()
        }
    }


}
