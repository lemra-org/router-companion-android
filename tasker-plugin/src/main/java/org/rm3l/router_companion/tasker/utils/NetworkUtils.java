package org.rm3l.router_companion.tasker.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.ihsanbal.logging.Level;
import com.ihsanbal.logging.LoggingInterceptor;
import com.readystatesoftware.chuck.ChuckInterceptor;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
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
          Crashlytics.log(Log.DEBUG, TAG,
              "onRemoval(" + notification.getKey() + ") - cause: " + notification.getCause());
        }
      })
      .build(new CacheLoader<String, Retrofit>() {
        @Override public Retrofit load(@Nullable String baseUrl) throws Exception {
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

  private NetworkUtils() {
  }

  public static OkHttpClient getHttpClientInstance() {
    if (HTTP_CLIENT_INSTANCE == null) {
      final OkHttpClient.Builder builder = new OkHttpClient.Builder();
      final Activity currentActivity = RouterCompanionTaskerPluginApplication.getCurrentActivity();
      if (currentActivity != null) {
        builder.addInterceptor(new ChuckInterceptor(currentActivity));
      }
      builder.readTimeout(10, TimeUnit.SECONDS);
      builder.connectTimeout(10, TimeUnit.SECONDS);

      builder.addInterceptor(new LoggingInterceptor.Builder().loggable(BuildConfig.DEBUG)
          .setLevel(Level.BASIC)
          .log(Log.INFO)
          .request("Request")
          .response("Response")
          .addHeader("X-APP-VERSION", BuildConfig.VERSION_NAME)
          .build());

      if (BuildConfig.DEBUG) {
        final HttpLoggingInterceptor interceptor =
            new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
              @Override public void log(String message) {
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

  @NonNull public static Retrofit getRetrofitInstance(@NonNull final String endpointBaseUrl) {
    return RETROFIT_CACHE.getUnchecked(endpointBaseUrl);
  }

  @NonNull public static <T> T createApiService(@NonNull final String endpointBaseUrl,
      @NonNull final Class<T> serviceType) {
    return getRetrofitInstance(endpointBaseUrl).create(serviceType);
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
}
