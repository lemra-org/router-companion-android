package org.rm3l.router_companion.lookup

import org.rm3l.router_companion.resources.MACOUIVendor
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * Created by rm3l on 27/06/16.
 */
@Deprecated("Deprecated. Use ProxyService instead")
interface MACOUILookupService {

    @Headers("Content-Type: application/json")
    @GET("{mac}")
    fun lookupMACAddress(@Path("mac") mac: String): Call<MACOUIVendor>
}
