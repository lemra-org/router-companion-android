package org.rm3l.router_companion.tasker.api.feedback;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.rm3l.router_companion.tasker.BuildConfig;
import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by rm3l on 25/04/16.
 */
public interface DoorbellService {

    @Headers({
            "Content-Type: application/json",
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @POST("applications/{id}/open")
    Call<ResponseBody> openApplication(
            @Path("id") final int applicationId, @Query("key") final String key);

    @Headers({
            "Content-Type: application/json",
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @POST("applications/{id}/submit")
    Call<ResponseBody> submitFeedbackForm(
            @Path("id") final int applicationId, @Query("key") final String key,
            @Query("email") final String email, @Query("message") final String message,
            @Query("name") final String userName, @Query("properties") final String propertiesJson,
            @Query("attachments[]") final String[] attachments);

    @Headers({
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @Multipart
    @POST("applications/{id}/upload")
    Call<String[]> upload(
            @Path("id") final int applicationId, @Query("key") final String key,
            @Part("files[]\"; filename=\"screenshot.png\" ") final RequestBody filename);
}
