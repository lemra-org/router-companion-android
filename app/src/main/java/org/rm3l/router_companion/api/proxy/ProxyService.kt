package org.rm3l.router_companion.api.proxy

import com.google.gson.JsonElement
import org.rm3l.router_companion.utils.retrofit.Retry
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Endpoints exposed by the proxy server located at https://reverse-proxy.services.rm3l.org (formerly, http://tools.rm3l.org:5180)
 */
interface ProxyService {

    /**
     * See JsonElement.ext.kt for type-safe parsing if needed
     */
    @Retry
    @Headers("Content-Type: application/json")
    @POST("proxy")
    fun proxy(@Body proxyData: ProxyData): Call<JsonElement>

    @Retry
    @Headers("Content-Type: application/json")
    @POST("proxy/networkGeoLocation")
    fun bulkNetworkGeoLocation(@Body ipsOrHostsToResolve: List<String>): Call<List<NetWhoisInfoProxyApiResponse>>
}
