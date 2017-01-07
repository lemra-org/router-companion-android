package org.rm3l.router_companion.tiles.status.wireless.share.qrcode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.support.v4.content.PermissionChecker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;

import org.rm3l.router_companion.R;
import org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingViewPagerAdapter.WifiSharingData;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.escapeString;
import static org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingActivity.WIFI_SHARING_DATA;
import static org.rm3l.router_companion.utils.ImageUtils.encodeAsBitmap;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

/**
 * Created by rm3l on 11/11/2016.
 */

public class WifiSharingQrCodeFragment extends Fragment {

    public static final int COMPRESSION_QUALITY = 100;
    public static final int DEFAULT_BITMAP_WIDTH = 400;
    public static final int DEFAULT_BITMAP_HEIGHT = 100;
    private File mFileToShare;

    public static Fragment newInstance(WifiSharingData wifiSharingData) {
        final WifiSharingQrCodeFragment fragment = new WifiSharingQrCodeFragment();
        final Bundle args = new Bundle();
        args.putSerializable(WIFI_SHARING_DATA, wifiSharingData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_wifi_sharing_qrcode, container, false);

        final FragmentActivity activity = getActivity();

        final FloatingActionButton shareButton = (FloatingActionButton)
                rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_share);

        final WifiSharingData wifiSharingData =
                (WifiSharingData) getArguments().getSerializable(WIFI_SHARING_DATA);

        final String mSsid = nullToEmpty(wifiSharingData.mSsid);
        final String wifiEncryptionType = wifiSharingData.mWifiEncType;
        final String wifiPassword = nullToEmpty(wifiSharingData.mWifiPassword);

        //https://github.com/zxing/zxing/wiki/Barcode-Contents
        //noinspection ConstantConditions
        final String mWifiQrCodeString = String.format("WIFI:S:%s;T:%s;P:%s;%s;",
                escapeString(mSsid),
                wifiEncryptionType,
                escapeString(nullToEmpty(wifiPassword)),
                mSsid.isEmpty() ? "H:true" : "");

        final ImageView qrCodeImageView = (ImageView) rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_image);

        final View loadingView = rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_image_loading_view);
        loadingView.setVisibility(View.VISIBLE);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Point outSize = new Point();
                            activity.getWindowManager().getDefaultDisplay().getSize(outSize);
                            final Bitmap mBitmap =
                                    encodeAsBitmap(mWifiQrCodeString,
                                            BarcodeFormat.QR_CODE, outSize.x, outSize.y / 2);
                            qrCodeImageView.setImageBitmap(mBitmap);
                            qrCodeImageView.setVisibility(View.VISIBLE);
                            loadingView.setVisibility(View.GONE);
//                            if (optionsMenu != null) {
//                                final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
//                                menuItem.setEnabled(true);
//                                menuItem.setVisible(true);
//                            }

                        } catch (final Exception e) {
                            e.printStackTrace();
//                            mException = e;
                            Utils.reportException(null, e);
                            rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_image_error)
                                    .setVisibility(View.VISIBLE);
                            qrCodeImageView.setVisibility(View.GONE);
                            loadingView.setVisibility(View.GONE);
                            qrCodeImageView.setVisibility(View.GONE);
//                            if (optionsMenu != null) {
//                                final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_wireless_iface_qrcode_share);
//                                menuItem.setEnabled(false);
//                                menuItem.setVisible(true);
//                            }
                        }
                    }
                });
            }
        });

        ((TextView) rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_ssid)).setText(mSsid);

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View viewToShare = rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_view_to_share);
                //Construct Bitmap and share it
                final int width = viewToShare.getWidth();
                final int height = viewToShare.getHeight();
                final Bitmap mBitmapToExport = Bitmap
                        .createBitmap(width > 0 ? width : DEFAULT_BITMAP_WIDTH,
                                height > 0 ? height : DEFAULT_BITMAP_HEIGHT,
                                Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(mBitmapToExport);
                viewToShare.draw(canvas);

                if (PermissionChecker.checkSelfPermission(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mFileToShare = new File(activity.getCacheDir(),
                            Utils.getEscapedFileName(String.format("QR-Code_for_Wireless_Network__%s__on_router_%s",
                                    nullToEmpty(mSsid), nullToEmpty(wifiSharingData.mRouterUuid))) + ".png");
                    OutputStream outputStream = null;
                    try {
                        outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
                        mBitmapToExport.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream);
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Utils.displayMessage(activity,
                                getString(R.string.internal_error_please_try_again), Style.ALERT);
                        return;
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


                    final Uri uriForFile = FileProvider
                            .getUriForFile(activity, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, mFileToShare);
                    activity.grantUriPermission(activity.getComponentName().getPackageName(),
                            uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    final Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("QR Code for Wireless Network '%s'", mSsid));
                    sendIntent.setType("text/html");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(String.format("%s%s",
                            ((TextView) rootView.findViewById(R.id.tile_status_wireless_iface_qrcode_note)).getText(),
                            Utils.getShareIntentFooter()).replaceAll("\n","<br/>")));

                    sendIntent.setData(uriForFile);
//        sendIntent.setType("image/png");
                    sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(sendIntent,  "Share Wi-Fi QR Code: " + mSsid));

                } else {

                    //Permission requests
                    // Should we show an explanation?
                    if (ActivityCompat
                            .shouldShowRequestPermissionRationale(
                                    activity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        SnackbarUtils.buildSnackbar(activity,
                                "Storage access is required to share WiFi QR Codes.",
                                "OK",
                                Snackbar.LENGTH_INDEFINITE,
                                new SnackbarCallback() {
                                    @Override
                                    public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                        //Request permission
                                        ActivityCompat.requestPermissions(activity,
                                                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                RouterCompanionAppConstants.Permissions.STORAGE);
                                    }

                                    @Override
                                    public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                                    }
                                },
                                null,
                                true);
                    } else {
                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(activity,
                                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                RouterCompanionAppConstants.Permissions.STORAGE);
                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }
            }
        });


        return rootView;
    }

}
