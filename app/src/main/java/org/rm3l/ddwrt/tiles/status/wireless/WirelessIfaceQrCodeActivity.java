/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
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
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.tiles.status.wireless;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

public class WirelessIfaceQrCodeActivity extends ActionBarActivity {

    public static final String WIFI_QR_CODE = "WIFI_QR_CODE";
    public static final String SSID = "SSID";
    public static final int COMPRESSION_QUALITY = 100;
    public static final int DEFAULT_BITMAP_WIDTH = 600;
    public static final int DEFAULT_BITMAP_HEIGHT = 300;
    public static final String UTF_8 = "UTF-8";
    private static final String LOG_TAG = WirelessIfaceQrCodeActivity.class.getSimpleName();
    /**
     * ***********************************************************
     * getting from com.google.zxing.client.android.encode.QRCodeEncoder
     * <p/>
     * See the sites below
     * http://code.google.com/p/zxing/
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/EncodeActivity.java
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/QRCodeEncoder.java
     */

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private Toolbar mToolbar;
    private String mTitle;
    private String mRouterUuid;
    private String mWifiQrCodeString;
    private String mSsid;
    private Bitmap mBitmap;
    private File mFileToShare;
    private Exception mException;
    private ShareActionProvider mShareActionProvider;

    private Menu optionsMenu;

    @Nullable
    private static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int imgWidth, int imgHeight) throws WriterException {
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        final String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        final MultiFormatWriter writer = new MultiFormatWriter();
        final BitMatrix result;
        try {
            result = writer.encode(contents, format, imgWidth, imgHeight, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        final int width = result.getWidth();
        final int height = result.getHeight();
        final int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            final int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    @Nullable
    private static String guessAppropriateEncoding(@NonNull final CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return UTF_8;
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long currentTheme = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
        if (currentTheme == ColorUtils.LIGHT_THEME) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.tile_status_wireless_iface_qrcode);

        mToolbar = (Toolbar) findViewById(R.id.tile_status_wireless_iface_qrcode_window_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(mTitle);
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final Intent intent = getIntent();
        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        mSsid = intent.getStringExtra(SSID);
        mWifiQrCodeString = intent.getStringExtra(WIFI_QR_CODE);

        mTitle = String.format("QR Code for '%s'", mSsid);

        if (mToolbar != null) {
            mToolbar.setTitle(mTitle);
            setSupportActionBar(mToolbar);
        }

        final ImageView qrCodeImageView = (ImageView) findViewById(R.id.tile_status_wireless_iface_qrcode_image);

        final View loadingView = findViewById(R.id.tile_status_wireless_iface_qrcode_image_loading_view);
        loadingView.setVisibility(View.VISIBLE);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Point outSize = new Point();
                            getWindowManager().getDefaultDisplay().getSize(outSize);
                            mBitmap =
                                    encodeAsBitmap(mWifiQrCodeString, BarcodeFormat.QR_CODE, outSize.x, outSize.y / 2);
                            qrCodeImageView.setImageBitmap(mBitmap);
                            qrCodeImageView.setVisibility(View.VISIBLE);
                            loadingView.setVisibility(View.GONE);
                            if (optionsMenu != null) {
                                optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share)
                                        .setEnabled(true);
                            }

                        } catch (final Exception e) {
                            e.printStackTrace();
                            mException = e;
                            Utils.reportException(e);
                            findViewById(R.id.tile_status_wireless_iface_qrcode_image_error)
                                    .setVisibility(View.VISIBLE);
                            qrCodeImageView.setVisibility(View.GONE);
                            loadingView.setVisibility(View.GONE);
                            qrCodeImageView.setVisibility(View.GONE);
                            if (optionsMenu != null) {
                                optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share)
                                        .setEnabled(false);
                            }
                        }
                    }
                });
            }
        });

        ((TextView) findViewById(R.id.tile_status_wireless_iface_qrcode_ssid)).setText(mSsid);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_wireless_iface_qr_code_options, menu);

        this.optionsMenu = menu;

        /* Getting the actionprovider associated with the menu item whose id is share */
        final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
        shareMenuItem.setEnabled(mException == null);

        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        final View viewToShare = findViewById(R.id.tile_status_wireless_iface_qrcode_view_to_share);
        //Construct Bitmap and share it
        final int width = viewToShare.getWidth();
        final int height = viewToShare.getHeight();
        final Bitmap bitmapToExport = Bitmap
                .createBitmap(width > 0 ? width : DEFAULT_BITMAP_WIDTH,
                        height > 0 ? height : DEFAULT_BITMAP_HEIGHT,
                        Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmapToExport);
        viewToShare.draw(canvas);

        mFileToShare = new File(getCacheDir(),
                String.format("QR-Code_for_Wireless_Network__%s__on_router_%s.png",
                        nullToEmpty(mSsid), nullToEmpty(mRouterUuid)));
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
            bitmapToExport.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Crouton.makeText(this, getString(R.string.internal_error_please_try_again), Style.ALERT)
                    .show();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                //No Worries
            }
        }

        setShareFile(mFileToShare);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private void setShareFile(File file) {
        if (mShareActionProvider == null) {
            return;
        }

        final Uri uriForFile = FileProvider
                .getUriForFile(this, "org.rm3l.fileprovider", file);

        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                grantUriPermission(intent.getComponent().getPackageName(),
                        uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return true;
            }
        });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("QR Code for Wireless Network '%s'", mSsid));

        sendIntent.setData(uriForFile);
        sendIntent.setType("image/png");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);

    }

    @Override
    protected void onDestroy() {
        if (mFileToShare != null) {
            //noinspection ResultOfMethodCallIgnored
            mFileToShare.delete();
        }
        super.onDestroy();
    }

}
