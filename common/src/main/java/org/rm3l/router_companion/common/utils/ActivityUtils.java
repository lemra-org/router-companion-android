package org.rm3l.router_companion.common.utils;

import static android.net.Uri.parse;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import android.widget.Toast;

/**
 * Created by rm3l on 22/08/16.
 */
public final class ActivityUtils {

    public static void launchApp(@NonNull final Context ctx, @NonNull final String packageName,
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(intent);
        } catch (final ActivityNotFoundException anfe) {
            openPlayStoreForPackage(ctx, packageName);
        }
    }

    public static void openPlayStoreForPackage(@NonNull final Context ctx,
            @NonNull final String packageName) {
        try {
            ctx.startActivity(
                    new Intent(Intent.ACTION_VIEW, parse("market://details?id=" + packageName)));
        } catch (final ActivityNotFoundException anfe) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                    parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private ActivityUtils() {
        throw new UnsupportedOperationException();
    }
}
