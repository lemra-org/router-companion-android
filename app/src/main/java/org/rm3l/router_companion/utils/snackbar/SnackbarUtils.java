package org.rm3l.router_companion.utils.snackbar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Strings;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 09/12/15.
 */
public final class SnackbarUtils {

  public enum Style {
    ALERT(R.color.win8_red),
    CONFIRM(R.color.win8_green),
    INFO(R.color.win8_blue),
    UNDEFINED(R.color.gray);

    public final int bgColor;

    Style(int bgColor) {
      this.bgColor = bgColor;
    }
  }

  private SnackbarUtils() {
  }

  public static Snackbar buildSnackbar(@Nullable final Context ctx, @Nullable final View view,
      @ColorInt final int bgColor, @Nullable final String title, @ColorInt final int titleColor,
      @Nullable final String actionText, @ColorInt final int actionTextColor,
      @Snackbar.Duration final int duration, @Nullable final SnackbarCallback callback,
      @Nullable final Bundle bundle, final boolean show) {

    if (view == null) {
      Toast.makeText(ctx, "Internal Error! Please try again later", Toast.LENGTH_SHORT).show();
      Utils.reportException(null, new IllegalArgumentException("view is NULL for Snackbar"));
      return null;
    }

    final Snackbar snackbar = Snackbar.make(view, Strings.nullToEmpty(title), duration)
        .setAction(actionText, new View.OnClickListener() {
          @Override public void onClick(View v) {
            //Do nothing here
          }
        })
        .setActionTextColor(actionTextColor);

    snackbar.addCallback(new Snackbar.Callback() {
      @Override public void onDismissed(Snackbar snackbar, int event) {
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
                    new IllegalStateException("Unknown Snackbar event: " + event));
                break;
            }
          }
        } catch (final Exception e) {
          e.printStackTrace();
          Utils.reportException(ctx, e);
          Toast.makeText(ctx, "Internal Error ("
              + Strings.nullToEmpty(Utils.handleException(e).first)
              + "). Please try again later.", Toast.LENGTH_SHORT).show();
        }
      }

      @Override public void onShown(Snackbar snackbar) {
        super.onShown(snackbar);
        try {
          if (callback != null) {
            callback.onShowEvent(bundle);
          }
        } catch (final Exception e) {
          Utils.reportException(ctx, e);
          Toast.makeText(ctx, "Internal Error! Please try again later.", Toast.LENGTH_SHORT).show();
        }
      }
    });

    final View snackbarView = snackbar.getView();
    snackbarView.setBackgroundColor(bgColor);
    final TextView textView =
        (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
    textView.setTextColor(titleColor);

    if (show) {
      snackbar.show();
    }

    return snackbar;
  }

  public static Snackbar buildSnackbar(@Nullable final Context ctx, @Nullable final View view,
      @Nullable final String title, @Nullable final String actionText,
      @Snackbar.Duration final int duration, @Nullable final SnackbarCallback callback,
      @Nullable final Bundle bundle, final boolean show) {

    return buildSnackbar(ctx, view, Color.DKGRAY, title, Color.YELLOW, actionText, Color.RED,
        duration, callback, bundle, show);
  }

  public static Snackbar buildSnackbar(@Nullable final Activity activity,
      @Nullable final String title, @Nullable final String actionText,
      @Snackbar.Duration final int duration, @Nullable final SnackbarCallback callback,
      @Nullable final Bundle bundle, final boolean show) {

    if (activity == null) {
      return null;
    }

    return buildSnackbar(activity, activity.findViewById(android.R.id.content), title, actionText,
        duration, callback, bundle, show);
  }

  public static Snackbar buildSnackbar(@Nullable final Activity activity,
      @ColorInt final int bgColor, @Nullable final String title, @ColorInt final int titleColor,
      @Nullable final String actionText, @ColorInt final int actionTextColor,
      @Snackbar.Duration final int duration, @Nullable final SnackbarCallback callback,
      @Nullable final Bundle bundle, final boolean show) {

    if (activity == null) {
      return null;
    }

    return buildSnackbar(activity, activity.findViewById(android.R.id.content), bgColor, title,
        titleColor, actionText, actionTextColor, duration, callback, bundle, show);
  }

  public static Snackbar createSnackbar(Context context, View view, String message) {
    final Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
    final ViewGroup group = (ViewGroup) snackbar.getView();
    group.setBackgroundColor(ContextCompat.getColor(context,
        ColorUtils.Companion.isThemeLight(context) ? R.color.lightTheme_primary : R.color.darkTheme_primary));
    return snackbar;
  }
}
