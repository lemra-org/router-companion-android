package org.rm3l.router_companion.lookup

import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.api.proxy.ProxyData
import org.rm3l.router_companion.resources.IPWhoisInfo
import org.rm3l.router_companion.utils.retrofit.Retry
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface IPGeoLookupService {

    @Retry
    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("proxy")
//    @GET("{ipOrHost}.json")
//    fun lookupIP(@Path("ipOrHost") ipOrHost: String): Call<IPWhoisInfo>
    fun lookupIP(@Body proxyData: ProxyData): Call<IPWhoisInfo>
}
