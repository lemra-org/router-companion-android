package org.rm3l.router_companion.api.proxy

import org.rm3l.router_companion.resources.IPWhoisInfo

data class ProxyData @JvmOverloads constructor(val targetHost: String,
                     val requestMethod: RequestMethod? = null,
                     val requestHeaders: Map<String, List<String>>? = null,
                     val requestParams: Map<String, String>? = null,
                     val requestBody: String? = null)

/**
 * Java 5 enumeration of HTTP request methods
 */
enum class RequestMethod {
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
}

data class NetWhoisInfoProxyApiResponse(val host: String, val info: IPWhoisInfo?)