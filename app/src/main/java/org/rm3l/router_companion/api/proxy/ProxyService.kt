package org.rm3l.router_companion.api.proxy

import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.resources.IPWhoisInfo
import org.rm3l.router_companion.utils.retrofit.Retry
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Endpoints exposed by the proxy server located at http://tools.rm3l.org:5180
 */
interface ProxyService {

    @Retry
    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("proxy")
    fun proxy(@Body proxyData: ProxyData): Call<IPWhoisInfo>

    @Retry
    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("proxy/networkGeoLocation")
    fun bulkNetworkGeoLocation(@Body ipsOrHostsToResolve: List<String>): Call<List<NetWhoisInfoProxyApiResponse>>
}