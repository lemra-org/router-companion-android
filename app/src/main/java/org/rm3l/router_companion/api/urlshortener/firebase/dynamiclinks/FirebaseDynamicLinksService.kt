package org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks

import org.rm3l.router_companion.RouterCompanionAppConstants.FIREBASE_API_KEY
import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.FirebaseDynamicLinksResponse
import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.ShortLinksDataRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FirebaseDynamicLinksService {

    @Headers("Content-Type: application/json")
    @POST("shortLinks?key=$FIREBASE_API_KEY")
    fun shortLinks(@Body body: ShortLinksDataRequest): Call<FirebaseDynamicLinksResponse>
}
