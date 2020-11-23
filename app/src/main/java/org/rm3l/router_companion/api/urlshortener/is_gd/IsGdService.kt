package org.rm3l.router_companion.api.urlshortener.is_gd

import org.rm3l.router_companion.api.urlshortener.is_gd.resources.IsGdResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface IsGdService {

    @Headers("Content-Type: application/json")
    @GET("create.php?format=json")
    fun shortLinks(@Query("url") url: String): Call<IsGdResponse>
}
