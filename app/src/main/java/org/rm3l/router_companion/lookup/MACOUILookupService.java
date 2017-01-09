package org.rm3l.router_companion.lookup;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.resources.MACOUIVendor;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

/**
 * Created by rm3l on 27/06/16.
 */
public interface MACOUILookupService {

    @Headers({
            "Content-Type: application/json",
            "User-Agent: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME
    })
    @GET("{mac}")
    Call<MACOUIVendor> lookupMACAddress(@Path("mac") final String mac);
}
