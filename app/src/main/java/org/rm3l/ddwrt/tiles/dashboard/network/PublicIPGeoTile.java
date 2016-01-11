package org.rm3l.ddwrt.tiles.dashboard.network;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Throwables;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.IPWhoisInfo;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.PublicIPInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.ActiveIPConnectionsDetailActivity;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.snackbar.SnackbarCallback;
import org.rm3l.ddwrt.utils.snackbar.SnackbarUtils;
import org.rm3l.ddwrt.widgets.map.MyOwnItemizedOverlay;

import java.util.Collections;
import java.util.Random;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

/**
 * Created by rm3l on 29/12/15.
 */
public class PublicIPGeoTile extends DDWRTTile<None> {

    private static final String TAG = PublicIPGeoTile.class.getSimpleName();

    private long mLastSync;
    private IPWhoisInfo mIPWhoisInfoWithGeo;
    private String mWanPublicIP;

    private boolean isThemeLight;

    private final ImageButton zoomMapButton;

    public PublicIPGeoTile(@NonNull Fragment parentFragment,
                           @NonNull Bundle arguments,
                           @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_public_ip_geo, null);

        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

        //Permission requests
        final int rwExternalStoragePermissionCheck = ContextCompat.checkSelfPermission(mParentFragmentActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            mParentFragmentActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                        "Storage access is required to cache and display map tiles.",
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
                                ActivityCompat.requestPermissions(mParentFragmentActivity,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
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
                ActivityCompat.requestPermissions(mParentFragmentActivity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        zoomMapButton = (ImageButton)
                layout.findViewById(R.id.tile_public_ip_geo_title_zoom);
        zoomMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open IPGeoActivity with zoom effect
                if (isNullOrEmpty(mWanPublicIP)) {
                    Toast.makeText(mParentFragmentActivity,
                            "WAN Public IP Unknown at this time", Toast.LENGTH_LONG).show();
                }
                final Intent intent = new Intent(mParentFragmentActivity, IPGeoActivity.class);
                intent.putExtra(IPGeoActivity.PUBLIC_IP_TO_DISPLAY, mWanPublicIP);
                mParentFragmentActivity.startActivity(intent);
                mParentFragmentActivity.overridePendingTransition(
                        R.anim.zoom_enter, R.anim.zoom_exit);
            }
        });

    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_public_ip_geo_title_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_public_ip_geo_title;
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public None loadInBackground() {

                try {

                    Crashlytics.log(Log.DEBUG, TAG, "Init background loader for " + PublicIPGeoTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    if (mRefreshing.getAndSet(true)) {
                        return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zoomMapButton.setImageDrawable(ContextCompat
                            .getDrawable(mParentFragmentActivity,
                                    isThemeLight ?
                                            R.drawable.ic_zoom_out_map_black_24dp :
                                            R.drawable.ic_zoom_out_map_white_24dp));
                        }
                    });

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    try {
                        updateProgressBarViewSeparator(40);

                        if (isDemoRouter(mRouter)) {
                            final int c = 1 + new Random().nextInt(252);
                            final int d = 1 + new Random().nextInt(252);
                            final long modulo = nbRunsLoader % 5;
                            if (modulo == 0) {
                                mWanPublicIP = ("52.64." + c + "." + d);
                            } else if (modulo == 1) {
                                mWanPublicIP = ("8.8." + c + "." + d);
                            } else if (modulo == 2) {
                                mWanPublicIP = ("78.87." + c + "." + d);
                            } else if (modulo == 3) {
                                mWanPublicIP = ("34.56." + c + "." + d);
                            } else if (modulo == 4) {
                                mWanPublicIP = ("67.78." + c + "." + d);
                            } else {
                                mWanPublicIP = null;
                            }
                        } else {
                            //Check actual connections to the outside from the router
                            final CharSequence applicationName = Utils.getApplicationName(mParentFragmentActivity);
                            final String[] wanPublicIpCmdStatus = SSHUtils.getManualProperty(mParentFragmentActivity,
                                    mRouter, mGlobalPreferences,
//                                        "echo -e \"GET / HTTP/1.1\\r\\nHost:icanhazip.com\\r\\nUser-Agent:DD-WRT Companion/3.3.0\\r\\n\" | nc icanhazip.com 80"
                                    String.format("echo -e \"" +
                                                    "GET / HTTP/1.1\\r\\n" +
                                                    "Host:%s\\r\\n" +
                                                    "User-Agent:%s/%s\\r\\n\" " +
                                                    "| /usr/bin/nc %s %d",
                                            PublicIPInfo.ICANHAZIP_HOST,
                                            applicationName != null ?
                                                    applicationName :
                                                    BuildConfig.APPLICATION_ID,
                                            BuildConfig.VERSION_NAME,
                                            PublicIPInfo.ICANHAZIP_HOST,
                                            PublicIPInfo.ICANHAZIP_PORT));
                            if (wanPublicIpCmdStatus != null && wanPublicIpCmdStatus.length > 0) {
                                final String wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1]
                                        .trim();
                                if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
                                    mWanPublicIP = wanPublicIp;
                                } else {
                                    mWanPublicIP = null;
                                }
                            } else {
                                mWanPublicIP = null;
                            }
                        }

                        if (mWanPublicIP != null &&
                                Patterns.IP_ADDRESS.matcher(mWanPublicIP).matches()) {
                            mIPWhoisInfoWithGeo =
                                    ActiveIPConnectionsDetailActivity.mIPWhoisInfoCache
                                            .get(mWanPublicIP);
                        } else {
                            mIPWhoisInfoWithGeo = null;
                        }

                        return new None();

                    } finally {
                        updateProgressBarViewSeparator(85);
                    }

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return (None) new None().setException(e);
                }
            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {
        try {//Set tiles
            Crashlytics.log(Log.DEBUG, TAG, "onLoadFinished: loader=" + loader + " / data=" + data);
            Crashlytics.log(Log.DEBUG, TAG, String.format("<mWanPublicIP=%s, mIPWhoisInfoWithGeo=%s>",
                    mWanPublicIP, mIPWhoisInfoWithGeo));

            layout.findViewById(R.id.tile_public_ip_geo_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_public_ip_geo_container)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
            }

            Exception exception = data.getException();

            Double latitude = null;
            Double longitude = null;
            if (exception == null) {
                if (mIPWhoisInfoWithGeo == null) {
                    data = (None) new None().setException(new
                            DDWRTNoDataException("Failed to retrieved Public IP Address geolocation data!"));
                } else {
                    try {
                        latitude = Double.parseDouble(mIPWhoisInfoWithGeo.getLatitude());
                        longitude = Double.parseDouble(mIPWhoisInfoWithGeo.getLongitude());
                    } catch (final NumberFormatException nfe) {
                        Crashlytics.logException(nfe);
                        nfe.printStackTrace();
                        //No worries
                    }
                    if (latitude == null || longitude == null) {
                        Crashlytics.logException(new IllegalStateException("latitude == null || longitude == null"));
                        data = (None) new None().setException(new
                                DDWRTNoDataException("Invalid coordinates - please try again later!"));
                    }
                }
                exception = data.getException();
            }

            final TextView errorPlaceHolderView = (TextView) this.layout
                    .findViewById(R.id.tile_public_ip_geo_error);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView)
                        layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if (rootCause != null) {
                            Toast.makeText(parentContext,
                                    rootCause.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                updateProgressBarWithError();
            } else if (exception == null) {

                final MapView map = (MapView)
                        layout.findViewById(R.id.tile_public_ip_geo_map);
                map.setTileSource(TileSourceFactory.MAPNIK);

                map.setBuiltInZoomControls(true);
                map.setMultiTouchControls(false);

                //Act according to user-defined data usage control setting
                map.setUseDataConnection(
                        Utils.canUseDataConnection(mParentFragmentActivity));

                final IMapController mapController = map.getController();
                mapController.setZoom(11);
                if (latitude != null && longitude != null) {
                    final GeoPoint publicIpPoint = new GeoPoint(latitude, longitude);
                    mapController.setCenter(publicIpPoint);

                    final OverlayItem overlayItem = new OverlayItem(
                            mIPWhoisInfoWithGeo.getIp(),
                            String.format("- Prefix: %s\n" +
                                            "- Country: %s (%s)\n" +
                                            "- Region: %s\n" +
                                            "- City: %s\n" +
                                            "- Organization: %s\n" +
                                            "- ASN: %s\n" +
                                            "- Latitude: %s\n" +
                                            "- Longitude: %s",
                                    nullToEmpty(mIPWhoisInfoWithGeo.getPrefix()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getCountry()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getCountry_code()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getRegion()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getCity()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getOrganization()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getAsn()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getLatitude()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getLongitude())),
                            publicIpPoint);
                    final MyOwnItemizedOverlay overlay =
                            new MyOwnItemizedOverlay(mParentFragmentActivity,
                                    Collections.singletonList(overlayItem));
                    map.getOverlays().add(overlay);
                }

                updateProgressBarWithSuccess();
            }

        } finally {
            Crashlytics.log(Log.DEBUG, TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }
}