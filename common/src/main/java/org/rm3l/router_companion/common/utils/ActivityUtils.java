package org.rm3l.router_companion.common.utils;

import static android.content.Intent.CATEGORY_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.net.Uri.parse;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;

/** Created by rm3l on 22/08/16. */
public final class ActivityUtils {

  public static void launchApp(
      @NonNull final Context ctx,
      @NonNull final String packageName,
      boolean openPlayStoreIfNotFound) {

    final Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
    if (intent == null) {
      Toast.makeText(ctx, "Package '" + packageName + "' not found", Toast.LENGTH_SHORT).show();
      if (openPlayStoreIfNotFound) {
        openPlayStoreForPackage(ctx, packageName);
      }
      return;
    }
    // We found the activity now start the activity
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
    try {
      ctx.startActivity(intent);
    } catch (final ActivityNotFoundException anfe) {
      openPlayStoreForPackage(ctx, packageName);
    }
  }

  public static void openPlayStoreForPackage(
      @NonNull final Context ctx, @NonNull final String packageName) {
    try {
      ctx.startActivity(
          new Intent(Intent.ACTION_VIEW, parse("market://details?id=" + packageName)));
    } catch (final ActivityNotFoundException anfe) {
      ctx.startActivity(
          new Intent(
              Intent.ACTION_VIEW,
              parse("https://play.google.com/store/apps/details?id=" + packageName)));
    }
  }

  public static void openApplicationSettings(@NonNull final Context ctx) {
    final Intent appSettingsIntent =
        new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            parse(String.format("package:%s", ctx.getPackageName())));
    appSettingsIntent.addCategory(CATEGORY_DEFAULT);
    appSettingsIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
    ctx.startActivity(appSettingsIntent);
  }

  private ActivityUtils() {
    throw new UnsupportedOperationException();
  }
}
