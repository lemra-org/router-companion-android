package org.rm3l.router_companion.mgmt;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.util.Collections;
import java.util.List;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.resources.conn.Router;

/** Created by rm3l on 02/12/2016. */
// #199: app shortcuts
class RouterAvatarDownloadTargetForAppShortcut implements Target {

  private static final String TAG = RouterAvatarDownloadTargetForAppShortcut.class.getSimpleName();

  private final boolean isUpdateAppShortcutOperation;

  private final Context mContext;

  private final Router router;

  RouterAvatarDownloadTargetForAppShortcut(
      Context mContext, Router router, boolean isUpdateAppShortcutOperation) {
    this.mContext = mContext;
    this.router = router;
    this.isUpdateAppShortcutOperation = isUpdateAppShortcutOperation;
  }

  @Override
  public void onBitmapFailed(Exception e, Drawable errorDrawable) {}

  @Override
  public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    if (mContext == null || router == null) {
      FirebaseCrashlytics.getInstance().log("mContext == null || router == null");
      return;
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {

      final ShortcutManager shortcutManager = mContext.getSystemService(ShortcutManager.class);
      if (shortcutManager == null) {
        FirebaseCrashlytics.getInstance().log("shortcutManager == null");
        return;
      }
      final String routerUuid = router.getUuid();
      final String routerName = router.getName();
      final String routerCanonicalHumanReadableName = router.getCanonicalHumanReadableName();

      final Intent shortcutIntent = new Intent(mContext, DDWRTMainActivity.class);
      shortcutIntent.setAction(Intent.ACTION_VIEW);
      shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      shortcutIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED, routerUuid);

      final ShortcutInfo shortcut =
          new ShortcutInfo.Builder(mContext, routerUuid)
              .setShortLabel(
                  TextUtils.isEmpty(routerName)
                      ? (router.getRemoteIpAddress() + ":" + router.getRemotePort())
                      : routerName)
              .setLongLabel(routerCanonicalHumanReadableName)
              .setIcon(Icon.createWithBitmap(bitmap))
              .setIntent(shortcutIntent)
              .build();

      if (isUpdateAppShortcutOperation) {
        boolean exists = false;
        final List<ShortcutInfo> dynamicShortcuts = shortcutManager.getDynamicShortcuts();
        for (final ShortcutInfo dynamicShortcut : dynamicShortcuts) {
          if (dynamicShortcut == null) {
            continue;
          }
          if (routerUuid.equals(dynamicShortcut.getId())) {
            exists = true;
            break;
          }
        }
        if (exists) {
          shortcutManager.updateShortcuts(Collections.singletonList(shortcut));
        } else {
          shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
        }
      } else {
        shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
      }
    }
  }

  @Override
  public void onPrepareLoad(Drawable placeHolderDrawable) {}
}
