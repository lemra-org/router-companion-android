package org.rm3l.router_companion.utils.retrofit;

import androidx.annotation.NonNull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Request;
import okio.Timeout;
import org.rm3l.router_companion.exceptions.TimeoutError;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/** Retries calls marked with {@link Retry}. */
public class RetryCallAdapterFactory<R, T> extends CallAdapter.Factory {

  private final ScheduledExecutorService mExecutor;

  private RetryCallAdapterFactory() {
    mExecutor = Executors.newScheduledThreadPool(1);
  }

  public static <R, T> RetryCallAdapterFactory<R, T> create() {
    return new RetryCallAdapterFactory<>();
  }

  @SuppressWarnings("unchecked")
  @Override
  public CallAdapter<R, T> get(
      @NonNull final Type returnType, Annotation[] annotations, @NonNull Retrofit retrofit) {
    boolean hasRetryAnnotation = false;
    int value = 0;
    for (Annotation annotation : annotations) {
      if (annotation instanceof Retry) {
        hasRetryAnnotation = true;
        value = ((Retry) annotation).value();
      }
    }
    final boolean shouldRetryCall = hasRetryAnnotation;
    final int maxRetries = value;
    final CallAdapter<R, T> delegate =
        (CallAdapter<R, T>) retrofit.nextCallAdapter(this, returnType, annotations);
    return new CallAdapter<R, T>() {
      @SuppressWarnings("NullableProblems")
      @Override
      public Type responseType() {
        return delegate.responseType();
      }

      @NonNull
      @Override
      public T adapt(@NonNull Call<R> call) {
        return delegate.adapt(
            shouldRetryCall ? new RetryingCall<>(call, mExecutor, maxRetries) : call);
      }
    };
  }

  static final class RetryingCall<T> implements Call<T> {
    private final Call<T> mDelegate;
    private final ScheduledExecutorService mExecutor;
    private final int mMaxRetries;

    private final AtomicBoolean isExecuted;
    private final AtomicBoolean isCanceled;

    public RetryingCall(Call<T> delegate, ScheduledExecutorService executor, int maxRetries) {
      mDelegate = delegate;
      mExecutor = executor;
      mMaxRetries = maxRetries;
      isExecuted = new AtomicBoolean(false);
      isCanceled = new AtomicBoolean(false);
    }

    @Override
    public Response<T> execute() throws IOException {
      isExecuted.set(true);
      return mDelegate.execute();
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
      mDelegate.enqueue(new RetryingCallback<>(mDelegate, callback, mExecutor, mMaxRetries));
    }

    @Override
    public boolean isExecuted() {
      return isExecuted.get();
    }

    @Override
    public void cancel() {
      mDelegate.cancel();
      isCanceled.set(true);
    }

    @Override
    public boolean isCanceled() {
      return isCanceled.get();
    }

    /* Performing deep clone */

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Call<T> clone() {
      return new RetryingCall<>(mDelegate.clone(), mExecutor, mMaxRetries);
    }

    @Override
    public Request request() {
      return mDelegate.request();
    }

    @Override
    public Timeout timeout() {
      return mDelegate.timeout();
    }
  }

  // Exponential backoff approach from https://developers.google.com/drive/web/handle-errors
  static final class RetryingCallback<T> implements Callback<T> {
    private static final Random random = new Random();
    private final int mMaxRetries;
    private final Call<T> mCall;
    private final Callback<T> mDelegate;
    private final ScheduledExecutorService mExecutor;
    private final int mRetries;

    RetryingCallback(
        Call<T> call, Callback<T> delegate, ScheduledExecutorService executor, int maxRetries) {
      this(call, delegate, executor, maxRetries, 0);
    }

    RetryingCallback(
        Call<T> call,
        Callback<T> delegate,
        ScheduledExecutorService executor,
        int maxRetries,
        int retries) {
      mCall = call;
      mDelegate = delegate;
      mExecutor = executor;
      mMaxRetries = maxRetries;
      mRetries = retries;
    }

    private void retryCall() {
      mExecutor.schedule(
          () -> {
            final Call<T> call = mCall.clone();
            call.enqueue(
                new RetryingCallback<>(call, mDelegate, mExecutor, mMaxRetries, mRetries + 1));
          },
          (1 << mRetries) * 1000 + random.nextInt(1001),
          TimeUnit.MILLISECONDS);
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
      mDelegate.onResponse(call, response);
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable throwable) {
      // Retry failed request
      if (mRetries < mMaxRetries) {
        retryCall();
      } else {
        mDelegate.onFailure(call, new TimeoutError(throwable));
      }
    }
  }
}
