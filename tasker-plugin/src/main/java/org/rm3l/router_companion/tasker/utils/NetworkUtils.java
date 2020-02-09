package org.rm3l.router_companion.tasker.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.readystatesoftware.chuck.ChuckInterceptor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.rm3l.router_companion.tasker.BuildConfig;
import org.rm3l.router_companion.tasker.RouterCompanionTaskerPluginApplication;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by rm3l on 15/05/16.
 */
public final class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    private static OkHttpClient HTTP_CLIENT_INSTANCE = null;

    private static final LoadingCache<String, Retrofit> RETROFIT_CACHE = CacheBuilder.newBuilder()
            .maximumSize(5)
            .removalListener(new RemovalListener<String, Retrofit>() {
                @Override
                public void onRemoval(@NonNull RemovalNotification<String, Retrofit> notification) {
                    FirebaseCrashlytics.getInstance().log(
                            "onRemoval(" + notification.getKey() + ") - cause: " + notification.getCause());
                }
            })
            .build(new CacheLoader<String, Retrofit>() {
                @Override
                public Retrofit load(@Nullable String baseUrl) throws Exception {
                    if (TextUtils.isEmpty(baseUrl)) {
                        throw new IllegalArgumentException();
                    }
                    try {
                        return new Retrofit.Builder().baseUrl(baseUrl)
                                .addConverterFactory(GsonConverterFactory.create())
                                .client(getHttpClientInstance())
                                .build();
                    } catch (final Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            });

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
    public static <T> T createApiService(@NonNull final String endpointBaseUrl,
            @NonNull final Class<T> serviceType) {
        return getRetrofitInstance(endpointBaseUrl).create(serviceType);
    }

    public static OkHttpClient getHttpClientInstance() {
        if (HTTP_CLIENT_INSTANCE == null) {
            final OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor());
            final Activity currentActivity = RouterCompanionTaskerPluginApplication.getCurrentActivity();
            if (currentActivity != null) {
                builder.addInterceptor(new ChuckInterceptor(currentActivity));
            }
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.connectTimeout(10, TimeUnit.SECONDS);

            if (BuildConfig.DEBUG) {
                final HttpLoggingInterceptor interceptor =
                        new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                            @Override
                            public void log(String message) {
                                FirebaseCrashlytics.getInstance().log(message);
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

    @NonNull
    public static Retrofit getRetrofitInstance(@NonNull final String endpointBaseUrl) {
        return RETROFIT_CACHE.getUnchecked(endpointBaseUrl);
    }

    private NetworkUtils() {
    }

    public static class AuthenticationInterceptor implements Interceptor {

        private final String authToken;

        public AuthenticationInterceptor(@NonNull final String token) {
            this.authToken = token;
        }

        public AuthenticationInterceptor(@NonNull final String username, @NonNull final String password) {
            this("Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        }

        @Override
        public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
            final Request original = chain.request();
            final Request.Builder builder = original.newBuilder()
                    .header("Authorization", authToken);
            final Request request = builder.build();
            return chain.proceed(request);
        }
    }

    public static class UserAgentInterceptor implements Interceptor {

        @Override
        public okhttp3.Response intercept(@NonNull final Chain chain) throws IOException {
            final Request original = chain.request();
            final Request.Builder builder = original.newBuilder()
                    .header("User-Agent", BuildConfig.APPLICATION_ID + " v " + BuildConfig.VERSION_NAME);
            final Request request = builder.build();
            return chain.proceed(request);
        }
    }
}
