package org.rm3l.router_companion.utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.PROXY_SERVER_PASSWORD_AUTH_TOKEN_ENCODED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.readystatesoftware.chuck.ChuckInterceptor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.api.proxy.ProxyService;
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

    public static final ProxyService PROXY_SERVICE =
            NetworkUtils.createApiService(null, RouterCompanionAppConstants.PROXY_SERVER_BASE_URL,
                    ProxyService.class,
                    new AuthenticationInterceptor(PROXY_SERVER_PASSWORD_AUTH_TOKEN_ENCODED));

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
            @NonNull final String endpointBaseUrl, @NonNull final Class<T> serviceType,
            @Nullable final Interceptor... interceptors) {
        return getRetrofitInstance(context, endpointBaseUrl, interceptors).create(serviceType);
    }

    public static OkHttpClient getHttpClientInstance(@Nullable final Context context,
            @Nullable final Interceptor... interceptors) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (interceptors != null) {
            for (final Interceptor interceptor : interceptors) {
                if (interceptor == null) {
                    continue;
                }
                builder.addInterceptor(interceptor);
            }
        }
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
            @NonNull final String baseUrl,
            @Nullable final Interceptor... interceptors) {
        if (isNullOrEmpty(baseUrl)) {
            throw new IllegalArgumentException();
        }
        try {
            return new Retrofit.Builder().addCallAdapterFactory(RetryCallAdapterFactory.create())
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getHttpClientInstance(context, interceptors))
                    .build();
        } catch (final Exception e) {
            throw new DDWRTCompanionException(e);
        }
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
}
