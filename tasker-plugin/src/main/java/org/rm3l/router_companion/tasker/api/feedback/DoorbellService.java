package org.rm3l.router_companion.tasker.api.feedback;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Created by rm3l on 25/04/16. */
public interface DoorbellService {

  @Headers({"Content-Type: application/json"})
  @POST("applications/{id}/open")
  Call<ResponseBody> openApplication(
      @Path("id") final int applicationId, @Query("key") final String key);

  @Headers({"Content-Type: application/json"})
  @POST("applications/{id}/submit")
  Call<ResponseBody> submitFeedbackForm(
      @Path("id") final int applicationId,
      @Query("key") final String key,
      @Body final DoorbellSubmitRequest request);

  @Multipart
  @POST("applications/{id}/upload")
  Call<String[]> upload(
      @Path("id") final int applicationId,
      @Query("key") final String key,
      @Part("files[]\"; filename=\"screenshot.png\" ") final RequestBody filename);
}
