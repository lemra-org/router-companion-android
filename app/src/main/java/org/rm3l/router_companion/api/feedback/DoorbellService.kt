package org.rm3l.router_companion.api.feedback

import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.rm3l.router_companion.RouterCompanionAppConstants.DOORBELL_APIKEY
import org.rm3l.router_companion.RouterCompanionAppConstants.DOORBELL_APPID
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Created by rm3l on 25/04/16.
 */
interface DoorbellService {

    @Headers("Content-Type: application/json")
    @POST("applications/$DOORBELL_APPID/open?key=$DOORBELL_APIKEY")
    fun openApplication(): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("applications/$DOORBELL_APPID/submit?key=$DOORBELL_APIKEY")
    fun submitFeedbackForm(@Body request: DoorbellSubmitRequest): Call<ResponseBody>

    @Multipart
    @POST("applications/$DOORBELL_APPID/upload?key=$DOORBELL_APIKEY")
    fun upload(@Part("files[]\"; filename=\"screenshot.png\" ") filename: RequestBody): Call<Array<String>>
}
