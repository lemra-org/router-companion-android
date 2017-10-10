package org.rm3l.router_companion.api.feedback

import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.rm3l.ddwrt.BuildConfig
import retrofit2.Call
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

    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("applications/{id}/open")
    fun openApplication(
            @Path("id") applicationId: Int, @Query("key") key: String): Call<ResponseBody>

    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("applications/{id}/submit")
    fun submitFeedbackForm(
            @Path("id") applicationId: Int, @Query("key") key: String,
            @Query("email") email: String, @Query("message") message: String,
            @Query("name") userName: String, @Query("properties") propertiesJson: String,
            @Query("attachments[]") attachments: Array<String>): Call<ResponseBody>

    @Headers("User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @Multipart
    @POST("applications/{id}/upload")
    fun upload(
            @Path("id") applicationId: Int, @Query("key") key: String,
            @Part("files[]\"; filename=\"screenshot.png\" ") filename: RequestBody): Call<Array<String>>
}
