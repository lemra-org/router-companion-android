package org.rm3l.router_companion.api.urlshortener.goo_gl

import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.api.urlshortener.goo_gl.resources.GooGlData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by rm3l on 25/04/16.
 */
interface GooGlService {

    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("url")
    fun shortenLongUrl(@Query("key") key: String,
                       @Body body: GooGlData): Call<GooGlData>

    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @GET("url")
    fun expandShortUrl(@Query("key") key: String,
                       @Query("shortUrl") shortUrl: String): Call<GooGlData>
}
