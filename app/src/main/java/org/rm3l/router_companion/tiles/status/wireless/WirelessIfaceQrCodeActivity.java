/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.tiles.status.wireless;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.escapeString;
import static org.rm3l.router_companion.utils.ImageUtils.encodeAsBitmap;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.PermissionChecker;
import androidx.core.view.MenuItemCompat;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Strings;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.zxing.BarcodeFormat;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingActivity;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

@Deprecated
public class WirelessIfaceQrCodeActivity extends AppCompatActivity {

  public static final int COMPRESSION_QUALITY = 100;

  public static final int DEFAULT_BITMAP_WIDTH = 600;

  public static final int DEFAULT_BITMAP_HEIGHT = 300;

  private static final String LOG_TAG = WirelessIfaceQrCodeActivity.class.getSimpleName();

  private Bitmap mBitmap;

  private Bitmap mBitmapToExport;

  private Exception mException;

  private File mFileToShare;

  private String mRouterUuid;

  private ShareActionProvider mShareActionProvider;

  private String mSsid;

  private String mTitle;

  /**
   * *********************************************************** getting from
   * com.google.zxing.client.android.encode.QRCodeEncoder
   *
   * <p>See the sites below http://code.google.com/p/zxing/
   * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/EncodeActivity.java
   * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/QRCodeEncoder.java
   */
  private Toolbar mToolbar;

  private String mWifiQrCodeString;

  private Menu optionsMenu;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Intent intent = getIntent();
    mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
    final DDWRTCompanionDAO dao = RouterManagementActivity.Companion.getDao(this);
    final Router router;
    if ((router = dao.getRouter(mRouterUuid)) == null) {
      Toast.makeText(this, "Internal Error: Router could not be determined", Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }

    ColorUtils.Companion.setAppTheme(this, router.getRouterFirmware(), false);

    final boolean themeLight = ColorUtils.Companion.isThemeLight(this);
    //        if (themeLight) {
    //            //Light
    //            setTheme(R.style.AppThemeLight);
    ////            getWindow().getDecorView()
    ////                    .setBackgroundColor(ContextCompat.getColor(this,
    ////                            android.R.color.white));
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark);
    //        }

    setContentView(R.layout.tile_status_wireless_iface_qrcode);

    mSsid = Strings.nullToEmpty(intent.getStringExtra(WifiSharingActivity.SSID));
    final String wifiEncryptionType = intent.getStringExtra(WifiSharingActivity.ENC_TYPE);
    final String wifiPassword = intent.getStringExtra(WifiSharingActivity.PWD);

    // https://github.com/zxing/zxing/wiki/Barcode-Contents
    //noinspection ConstantConditions
    mWifiQrCodeString =
        String.format(
            "WIFI:S:%s;T:%s;P:%s;%s;",
            escapeString(mSsid),
            wifiEncryptionType,
            escapeString(nullToEmpty(wifiPassword)),
            mSsid.isEmpty() ? "H:true" : "");

    mTitle = ("WiFi QR Code: " + mSsid);

    mToolbar = findViewById(R.id.tile_status_wireless_iface_qrcode_window_toolbar);
    if (mToolbar != null) {
      mToolbar.setTitle(mTitle);
      mToolbar.setSubtitle(
          String.format(
              "%s (%s:%d)",
              router.getDisplayName(), router.getRemoteIpAddress(), router.getRemotePort()));
      mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
      mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
      mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
      mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
      setSupportActionBar(mToolbar);
    }

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeButtonEnabled(true);
    }

    final ImageView qrCodeImageView = findViewById(R.id.tile_status_wireless_iface_qrcode_image);

    final View loadingView =
        findViewById(R.id.tile_status_wireless_iface_qrcode_image_loading_view);
    loadingView.setVisibility(View.VISIBLE);

    new Handler()
        .post(
            new Runnable() {
              @Override
              public void run() {
                runOnUiThread(
                    new Runnable() {
                      @Override
                      public void run() {
                        try {
                          final Point outSize = new Point();
                          getWindowManager().getDefaultDisplay().getSize(outSize);
                          mBitmap =
                              encodeAsBitmap(
                                  mWifiQrCodeString,
                                  BarcodeFormat.QR_CODE,
                                  outSize.x,
                                  outSize.y / 2);
                          qrCodeImageView.setImageBitmap(mBitmap);
                          qrCodeImageView.setVisibility(View.VISIBLE);
                          loadingView.setVisibility(View.GONE);
                          if (optionsMenu != null) {
                            final MenuItem menuItem =
                                optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
                            menuItem.setEnabled(true);
                            menuItem.setVisible(true);
                          }
                        } catch (final Exception e) {
                          e.printStackTrace();
                          mException = e;
                          Utils.reportException(null, e);
                          findViewById(R.id.tile_status_wireless_iface_qrcode_image_error)
                              .setVisibility(View.VISIBLE);
                          qrCodeImageView.setVisibility(View.GONE);
                          loadingView.setVisibility(View.GONE);
                          qrCodeImageView.setVisibility(View.GONE);
                          if (optionsMenu != null) {
                            final MenuItem menuItem =
                                optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
                            menuItem.setEnabled(false);
                            menuItem.setVisible(true);
                          }
                        }
                      }
                    });
              }
            });

    ((TextView) findViewById(R.id.tile_status_wireless_iface_qrcode_ssid)).setText(mSsid);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    // No call for super(). Bug on API Level > 11.
  }

  @Override
  protected void onDestroy() {
    if (mFileToShare != null) {
      //noinspection ResultOfMethodCallIgnored
      mFileToShare.delete();
    }
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {

    switch (requestCode) {
      case RouterCompanionAppConstants.Permissions.STORAGE:
        {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted, yay!
            FirebaseCrashlytics.getInstance().log("Yay! Permission granted for #" + requestCode);
            if (optionsMenu != null) {
              final MenuItem menuItem =
                  optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
              menuItem.setEnabled(true);
            }
          } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            FirebaseCrashlytics.getInstance().log("Boo! Permission denied for #" + requestCode);
            Utils.displayMessage(this, "Sharing of WiFi QR Codes will be unavailable", Style.INFO);
            if (optionsMenu != null) {
              final MenuItem menuItem =
                  optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
              menuItem.setEnabled(false);
            }
          }
          return;
        }
      default:
        break;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.tile_wireless_iface_qr_code_options, menu);

    this.optionsMenu = menu;

    // Permission requests
    final int rwExternalStoragePermissionCheck =
        PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
        SnackbarUtils.buildSnackbar(
            this,
            "Storage access is required to share WiFi QR Codes.",
            "OK",
            Snackbar.LENGTH_INDEFINITE,
            new SnackbarCallback() {
              @Override
              public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                  throws Exception {
                // Request permission
                ActivityCompat.requestPermissions(
                    WirelessIfaceQrCodeActivity.this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RouterCompanionAppConstants.Permissions.STORAGE);
              }
            },
            null,
            true);
      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(
            this,
            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
            RouterCompanionAppConstants.Permissions.STORAGE);
        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
        // app-defined int constant. The callback method gets the
        // result of the request.
      }
    }

    /* Getting the actionprovider associated with the menu item whose id is share */
    final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
    shareMenuItem.setEnabled(mException == null);

    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
    if (mShareActionProvider == null) {
      mShareActionProvider = new ShareActionProvider(this);
      MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
    }

    final View viewToShare = findViewById(R.id.tile_status_wireless_iface_qrcode_view_to_share);
    // Construct Bitmap and share it
    final int width = viewToShare.getWidth();
    final int height = viewToShare.getHeight();
    mBitmapToExport =
        Bitmap.createBitmap(
            width > 0 ? width : DEFAULT_BITMAP_WIDTH,
            height > 0 ? height : DEFAULT_BITMAP_HEIGHT,
            Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(mBitmapToExport);
    viewToShare.draw(canvas);

    if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {

      // permission was granted, yay! Do the
      // contacts-related task you need to do.
      mFileToShare =
          new File(
              getCacheDir(),
              Utils.getEscapedFileName(
                      String.format(
                          "QR-Code_for_Wireless_Network__%s__on_router_%s",
                          nullToEmpty(mSsid), nullToEmpty(mRouterUuid)))
                  + ".png");
      OutputStream outputStream = null;
      try {
        outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
        mBitmapToExport.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream);
        outputStream.flush();
      } catch (IOException e) {
        e.printStackTrace();
        Utils.displayMessage(
            this, getString(R.string.internal_error_please_try_again), Style.ALERT);
      } finally {
        try {
          if (outputStream != null) {
            outputStream.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
          // No Worries
        }
      }

      setShareFile(mFileToShare);
    }

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int i = item.getItemId();
    if (i == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (i == R.id.action_feedback) {
      Utils.openFeedbackForm(this, mRouterUuid);
      //                final Intent intent = new Intent(WirelessIfaceQrCodeActivity.this,
      // FeedbackActivity.class);
      //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
      //                final File screenshotFile = new File(getCacheDir(),
      // "feedback_screenshot.png");
      //                ViewGroupUtils.exportViewToFile(WirelessIfaceQrCodeActivity.this,
      // getWindow().getDecorView(), screenshotFile);
      //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE,
      // screenshotFile.getAbsolutePath());
      //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY,
      // this.getClass().getCanonicalName());
      //                startActivity(intent);
      ////                Utils.buildFeedbackDialog(this, true);
      return true;
    } else {
    }
    return super.onOptionsItemSelected(item);
  }

  private void setShareFile(File file) {
    if (mShareActionProvider == null) {
      return;
    }

    final Uri uriForFile =
        FileProvider.getUriForFile(this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, file);

    mShareActionProvider.setOnShareTargetSelectedListener(
        new ShareActionProvider.OnShareTargetSelectedListener() {
          @Override
          public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
            grantUriPermission(
                intent.getComponent().getPackageName(),
                uriForFile,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
          }
        });

    final Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
    sendIntent.putExtra(
        Intent.EXTRA_SUBJECT, String.format("QR Code for Wireless Network '%s'", mSsid));
    sendIntent.setType("text/html");
    sendIntent.putExtra(
        Intent.EXTRA_TEXT,
        fromHtml(
            String.format(
                    "%s%s",
                    ((TextView) findViewById(R.id.tile_status_wireless_iface_qrcode_note))
                        .getText(),
                    Utils.getShareIntentFooter())
                .replaceAll("\n", "<br/>")));

    sendIntent.setData(uriForFile);
    //        sendIntent.setType("image/png");
    sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    setShareIntent(sendIntent);
  }

  // Call to update the share intent
  private void setShareIntent(Intent shareIntent) {
    if (mShareActionProvider != null) {
      mShareActionProvider.setShareIntent(shareIntent);
    }
  }
}
