package org.rm3l.router_companion.tasker.api.urlshortener.goo_gl;

import org.rm3l.router_companion.tasker.BuildConfig;
import org.rm3l.router_companion.tasker.api.urlshortener.goo_gl.resources.GooGlData;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by rm3l on 25/04/16.
 */
@Deprecated
public interface GooGlService {

    @Headers({
            "Content-Type: application/json"
    })
    @GET("url")
    Call<GooGlData> expandShortUrl(@Query("key") final String key,
            @Query("shortUrl") final String shortUrl);

    @Headers({
            "Content-Type: application/json"
    })
    @POST("url")
    Call<GooGlData> shortenLongUrl(@Query("key") final String key,
            @Body final GooGlData body);
}
