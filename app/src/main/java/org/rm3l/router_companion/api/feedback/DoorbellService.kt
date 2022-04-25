package org.rm3l.router_companion.api.feedback

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by rm3l on 25/04/16.
 */
interface DoorbellService {

    @Headers("Content-Type: application/json")
    @POST("applications/{appId}/open")
    fun openApplication(@Path("appId") appId: String,
                        @Query("key") apiKey: String): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("applications/{appId}/submit")
    fun submitFeedbackForm(@Path("appId") appId: String,
                           @Query("key") apiKey: String,
                           @Body request: DoorbellSubmitRequest): Call<ResponseBody>

    @Multipart
    @POST("applications/{appId}/upload")
    fun upload(@Path("appId") appId: String,
               @Query("key") apiKey: String,
               @Part("files[]\"; filename=\"screenshot.png\" ") filename: RequestBody): Call<Array<String>>
}
