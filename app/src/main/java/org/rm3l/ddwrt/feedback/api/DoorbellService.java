package org.rm3l.ddwrt.feedback.api;

import org.rm3l.ddwrt.BuildConfig;

import java.util.Map;

import okhttp3.RequestBody;
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
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @POST("/applications/{id}/open")
    void openApplication(@Path("id") final int applicationId, @Query("key") final String key);

    @Headers({
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @POST("/applications/{id}/submit")
    void submitFeedbackForm(@Path("id") final int applicationId,
                            @Query("key") final String key,
                            @Query("email") final String email,
                            @Query("message") final String message,
                            @Query("name") final String userName,
                            @Query("properties") final Map<String, Object> properties,
                            @Query("attachments") final String[] attachments);

    @Headers({
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @Multipart
    @POST("/applications/{id}/upload")
    String[] upload(@Path("id") final int applicationId,
                            @Query("key") final String key,
                            @Part("screenshot") final RequestBody screenshot,
                            @Part("logs") final RequestBody logs);
}
