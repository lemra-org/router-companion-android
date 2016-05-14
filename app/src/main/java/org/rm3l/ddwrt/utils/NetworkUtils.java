package org.rm3l.ddwrt.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.rm3l.ddwrt.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by rm3l on 15/05/16.
 */
public final class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    private NetworkUtils() {}

    private static OkHttpClient HTTP_CLIENT_INSTANCE = null;

    public static OkHttpClient getHttpClientInstance() {
        if (HTTP_CLIENT_INSTANCE == null) {
            final OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.connectTimeout(10, TimeUnit.SECONDS);
            if (BuildConfig.DEBUG) {
                final HttpLoggingInterceptor interceptor =
                        new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                            @Override
                            public void log(String message) {
                                Crashlytics.log(Log.DEBUG, TAG, message);
                            }
                        });
                interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
                builder.addInterceptor(interceptor);

                //Stetho
                builder.addNetworkInterceptor(new StethoInterceptor());
            }
            HTTP_CLIENT_INSTANCE = builder.build();
        }
        return HTTP_CLIENT_INSTANCE;
    }

}
