package org.rm3l.router_companion.tasker.api.urlshortener.firebase.dynamiclinks;

import org.rm3l.router_companion.tasker.api.urlshortener.firebase.dynamiclinks.resources.ShortLinksDataRequest;
import org.rm3l.router_companion.tasker.api.urlshortener.firebase.dynamiclinks.resources.ShortLinksDataResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface FirebaseDynamicLinksService {

    @Headers("Content-Type: application/json")
    @POST("shortLinks")
    Call<ShortLinksDataResponse> shortLinks(@Query("key") final String key, @Body final ShortLinksDataRequest body);

}
