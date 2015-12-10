package org.rm3l.ddwrt.utils.snackbar;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;

import org.rm3l.ddwrt.utils.Utils;

/**
 * Created by rm3l on 09/12/15.
 */
public final class SnackbarUtils {

    private SnackbarUtils() {}

    public static Snackbar buildSnackbar(
            @Nullable final Context ctx,
            @Nullable final View view,
            @Nullable final String title,
            @Nullable final String actionText,
            @Snackbar.Duration final int duration,
            @Nullable final SnackbarCallback callback,
            @Nullable final Bundle bundle,
            final boolean show) {

        if (view == null) {
            Toast.makeText(ctx, "Internal Error! Please try again later",
                    Toast.LENGTH_SHORT).show();
            Utils.reportException(null,
                    new IllegalArgumentException("view is NULL for Snackbar"));
            return null;
        }

        final Snackbar snackbar = Snackbar
                .make(
                        view,
                        Strings.nullToEmpty(title),
                        duration)
                .setAction(actionText, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Do nothing here
                    }
                })
                .setActionTextColor(Color.RED);

        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                try {
                    if (callback != null) {
                        switch (event) {
                            case DISMISS_EVENT_SWIPE:
                                callback.onDismissEventSwipe(event, bundle);
                                break;
                            case DISMISS_EVENT_ACTION:
                                callback.onDismissEventActionClick(event, bundle);
                                break;
                            case DISMISS_EVENT_TIMEOUT:
                                callback.onDismissEventTimeout(event, bundle);
                                break;
                            case DISMISS_EVENT_MANUAL:
                                callback.onDismissEventManual(event, bundle);
                                break;
                            case DISMISS_EVENT_CONSECUTIVE:
                                callback.onDismissEventConsecutive(event, bundle);
                                break;
                            default:
                                Utils.reportException(ctx,
                                        new IllegalStateException("Unknown Snackbar event: " +
                                                event));
                                break;
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    Utils.reportException(ctx, e);
                    Toast.makeText(ctx, "Internal Error! Please try again later.", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onShown(Snackbar snackbar) {
                super.onShown(snackbar);
                try {
                    if (callback != null) {
                        callback.onShowEvent(bundle);
                    }
                } catch (final Exception e) {
                    Utils.reportException(ctx, e);
                    Toast.makeText(ctx, "Internal Error! Please try again later.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        final View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        final TextView textView = (TextView)
                snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);

        if (show) {
            snackbar.show();
        }

        return snackbar;

    }
}
