package com.example.plugin

import com.example.api.ExampleApi
import com.loyalty.api.CodeApi
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class ExamplePlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis = listOf(Function(::ExampleApi), Function(::CodeApi))

}
