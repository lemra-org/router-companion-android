package org.rm3l.router_companion.tasker.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.Objects;

public class FirebaseUtils {

  @NonNull
  public static String getFirebaseApiKey(@NonNull Context context) {
    return Objects.requireNonNull(ContextUtils.getConfigProperty(context, "FIREBASE_API_KEY", ""));
  }
}
