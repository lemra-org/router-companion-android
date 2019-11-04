package org.rm3l.router_companion.utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.FIREBASE_DYNAMIC_LINKS_BASE_URL;
import static org.rm3l.router_companion.RouterCompanionAppConstants.IS_GD_URL_SHORTENER_BASE_URL;
import static org.rm3l.router_companion.RouterCompanionAppConstants.PROXY_SERVER_BASE_URL;
import static org.rm3l.router_companion.RouterCompanionAppConstants.PROXY_SERVER_PASSWORD_AUTH_TOKEN_ENCODED;
import static org.rm3l.router_companion.RouterCompanionAppConstants.SERVICE_NAMES_PORT_NUMBERS_API_SERVER_BASE_URL;
import static org.rm3l.router_companion.RouterCompanionAppConstants.SERVICE_NAMES_PORT_NUMBERS_API_SERVER_PASSWORD_AUTH_TOKEN_ENCODED;
import static org.rm3l.router_companion.feedback.maoni.MaoniFeedbackHandler.FEEDBACK_API_BASE_URL;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.android.material.snackbar.Snackbar;
import com.readystatesoftware.chuck.ChuckInterceptor;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.RouterCompanionAppConstants.Permissions;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.api.feedback.DoorbellService;
import org.rm3l.router_companion.api.iana.ServiceNamePortNumbersService;
import org.rm3l.router_companion.api.proxy.ProxyService;
import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.FirebaseDynamicLinksService;
import org.rm3l.router_companion.api.urlshortener.is_gd.IsGdService;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.utils.Utils.OperationCallback;
import org.rm3l.router_companion.utils.retrofit.RetryCallAdapterFactory;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by rm3l on 15/05/16.
 */
public final class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    private static class ProxyServiceClientsHolder {
        private static final ProxyService PROXY_SERVICE =
                NetworkUtils.createApiService(null, PROXY_SERVER_BASE_URL,
                        ProxyService.class,
                        new AuthenticationInterceptor(PROXY_SERVER_PASSWORD_AUTH_TOKEN_ENCODED));
    }
    public static ProxyService getProxyService() {
        return ProxyServiceClientsHolder.PROXY_SERVICE;
    }

    private static class ServiceNamePortNumbersServiceHolder {
        private static final ServiceNamePortNumbersService SERVICE_NAMES_PORT_NUMBERS_MAPPING_SERVICE =
                NetworkUtils.createApiService(null, SERVICE_NAMES_PORT_NUMBERS_API_SERVER_BASE_URL,
                        ServiceNamePortNumbersService.class,
                        new AuthenticationInterceptor(SERVICE_NAMES_PORT_NUMBERS_API_SERVER_PASSWORD_AUTH_TOKEN_ENCODED));
    }
    public static ServiceNamePortNumbersService getServiceNamePortNumbersService() {
        return ServiceNamePortNumbersServiceHolder.SERVICE_NAMES_PORT_NUMBERS_MAPPING_SERVICE;
    }

    private static class FirebaseDynamicLinksServiceHolder {
        private static final FirebaseDynamicLinksService FIREBASE_DYNAMIC_LINKS_SERVICE =
                NetworkUtils.createApiService(null, FIREBASE_DYNAMIC_LINKS_BASE_URL, FirebaseDynamicLinksService.class);
    }
    public static FirebaseDynamicLinksService getFirebaseDynamicLinksService() {
        return FirebaseDynamicLinksServiceHolder.FIREBASE_DYNAMIC_LINKS_SERVICE;
    }

    private static class IsGdServiceHolder {
        private static final IsGdService IS_GD_URL_SHORTENER_SERVICE =
                NetworkUtils.createApiService(null, IS_GD_URL_SHORTENER_BASE_URL, IsGdService.class);
    }
    public static IsGdService getIsGdService() {
        return IsGdServiceHolder.IS_GD_URL_SHORTENER_SERVICE;
    }

    private static class DoorbellServiceHolder {
        private static final DoorbellService DOORBELL_SERVICE =
                NetworkUtils.createApiService(null, FEEDBACK_API_BASE_URL, DoorbellService .class);
    }
    public static DoorbellService getDoorbellService() {
        return DoorbellServiceHolder.DOORBELL_SERVICE;
    }

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

    private static OkHttpClient getHttpClientInstance(@Nullable final Context context,
            @Nullable final Interceptor... interceptors) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor());
        if (interceptors != null) {
            for (final Interceptor interceptor : interceptors) {
                if (interceptor == null) {
                    continue;
                }
                builder.addInterceptor(interceptor);
            }
        }
        final Context currentContext =
                (context != null ? context : RouterCompanionApplication.Companion.getCurrentActivity());
        if (currentContext != null) {
            builder.addInterceptor(new ChuckInterceptor(currentContext));
        }
        builder.readTimeout(5, TimeUnit.MINUTES);
        builder.connectTimeout(30, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            final HttpLoggingInterceptor interceptor =
                    new HttpLoggingInterceptor(message -> Crashlytics.log(Log.DEBUG, TAG, message));
            interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            builder.addInterceptor(interceptor);

            //Stetho
            builder.addNetworkInterceptor(new StethoInterceptor());
        }
        return builder.build();
    }

    @NonNull
    private static Retrofit getRetrofitInstance(@Nullable final Context context,
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

    public static void getWifiName(@Nullable Activity currentActivity, @NonNull OperationCallback<String, Void> callback) {
        if (currentActivity == null) {
            callback.run(null);
            return;
        }

        PermissionsUtils.requestPermissions(currentActivity, Collections.singletonList(permission.ACCESS_COARSE_LOCATION),
                () -> {
                    final ConnectivityManager connectivityManager =
                            (ConnectivityManager) currentActivity.getApplicationContext()
                                    .getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager == null) {
                        callback.run(null);
                        return null;
                    }

                    final NetworkInfo myNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (myNetworkInfo == null || myNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                        callback.run(null);
                        return null;
                    }

                    //        final NetworkInfo myNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (!myNetworkInfo.isConnected()) {
                        callback.run(null);
                        return null;
                    }

                    final WifiManager wifiManager = (WifiManager) currentActivity.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager == null) {
                        callback.run(null);
                        return null;
                    }
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo == null) {
                        callback.run(null);
                        return null;
                    }
                    String ssid = wifiInfo.getSSID();
                    if (ssid == null || "<unknown ssid>".equals(ssid)) {
                        //Try using extra-info
                        final String extraInfo = myNetworkInfo.getExtraInfo();
                        final int length;
                        if (extraInfo != null && (length = extraInfo.length()) >= 2) {
                            ssid = extraInfo.substring(1, length -1);
                        }
                    }
                    callback.run(ssid);
                    return null;
                },
                () -> null,
                "Approximate Location Permission is required to read your WiFi Network name.");
    }

    public static class AuthenticationInterceptor implements Interceptor {

        private final String authToken;

        AuthenticationInterceptor(@NonNull final String token) {
            this.authToken = token;
        }

        public AuthenticationInterceptor(@NonNull final String username, @NonNull final String password) {
            this("Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        }

        @NonNull
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

        @NonNull
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
