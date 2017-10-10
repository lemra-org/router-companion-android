package org.rm3l.router_companion.utils;

import static com.google.common.base.Strings.isNullOrEmpty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.readystatesoftware.chuck.ChuckInterceptor;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.utils.retrofit.RetryCallAdapterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by rm3l on 15/05/16.
 */
public final class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    @SuppressLint("DefaultLocale")
    public static void checkResponseSuccessful(@NonNull final Response<?> response) {
        if (!response.isSuccessful()) {
            final int code = response.code();
            final Object body = response.body();
            final String errorMsg = String.format("[%d] : %s", code, body);
            if (code >= 400 && code < 500) {
                throw new IllegalArgumentException(errorMsg);
            }
            if (code >= 500) {
                throw new IllegalStateException(errorMsg);
            }
            throw new RuntimeException(errorMsg);
        }
    }

    @NonNull
    public static <T> T createApiService(@Nullable final Context context,
            @NonNull final String endpointBaseUrl, @NonNull final Class<T> serviceType) {
        return getRetrofitInstance(context, endpointBaseUrl).create(serviceType);
    }

    public static OkHttpClient getHttpClientInstance(@Nullable final Context context) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final Context currentContext =
                (context != null ? context : RouterCompanionApplication.getCurrentActivity());
        if (currentContext != null) {
            builder.addInterceptor(new ChuckInterceptor(currentContext));
        }
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
        return builder.build();
    }

    @NonNull
    public static Retrofit getRetrofitInstance(@Nullable final Context context,
            @NonNull final String baseUrl) {
        if (isNullOrEmpty(baseUrl)) {
            throw new IllegalArgumentException();
        }
        try {
            return new Retrofit.Builder().addCallAdapterFactory(RetryCallAdapterFactory.create())
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getHttpClientInstance(context))
                    .build();
        } catch (final Exception e) {
            throw new DDWRTCompanionException(e);
        }
    }

    private NetworkUtils() {
    }
}
