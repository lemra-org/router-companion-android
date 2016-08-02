package org.rm3l.ddwrt.api.urlshortener.goo_gl;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.api.urlshortener.goo_gl.resources.GooGlData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by rm3l on 25/04/16.
 */
public interface GooGlService {

    @Headers({
            "Content-Type: application/json",
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @POST("url")
    Call<GooGlData> shortenLongUrl(@Query("key") final String key,
                               @Body final GooGlData body);

    @Headers({
            "Content-Type: application/json",
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @GET("url")
    Call<GooGlData> expandShortUrl(@Query("key") final String key,
                                          @Query("shortUrl") final String shortUrl);

}
