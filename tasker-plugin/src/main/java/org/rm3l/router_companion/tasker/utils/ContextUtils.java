package org.rm3l.router_companion.tasker.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class ContextUtils {

  @Nullable
  public static String getConfigProperty(
      @Nullable Context context, @NonNull String identifier, @Nullable String defaultValue) {
    final Resources resources;
    if (context == null || (resources = context.getResources()) == null) {
      return defaultValue;
    }
    final int id = resources.getIdentifier(identifier, "string", context.getPackageName());
    if (id == 0) {
      return defaultValue;
    }
    return getConfigProperty(context, id, defaultValue);
  }

  @Nullable
  public static String getConfigProperty(
      @Nullable Context context, @StringRes int identifier, @Nullable String defaultValue) {
    final Resources resources;
    if (context == null || (resources = context.getResources()) == null) {
      return defaultValue;
    }
    try {
      final String value = resources.getString(identifier);
      if (value == null) {
        return defaultValue;
      }
      return value;
    } catch (final Resources.NotFoundException rnfe) {
      Log.d(
          context.getClass().getSimpleName(),
          String.format(
              "Resource %d of type string not found in package %s",
              identifier, context.getPackageName()),
          rnfe);
      return defaultValue;
    }
  }
}
