package org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks

import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.FirebaseDynamicLinksResponse
import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.ShortLinksDataRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface FirebaseDynamicLinksService {

    @Headers("Content-Type: application/json")
    @POST("shortLinks")
    fun shortLinks(
        @Query("key") apiKey: String,
        @Body body: ShortLinksDataRequest
    ): Call<FirebaseDynamicLinksResponse>
}
