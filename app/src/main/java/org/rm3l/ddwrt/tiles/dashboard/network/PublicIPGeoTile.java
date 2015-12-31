package org.rm3l.ddwrt.tiles.dashboard.network;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
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
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.widgets.map.MyOwnItemizedOverlay;

import java.util.Collections;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

/**
 * Created by rm3l on 29/12/15.
 */
public class PublicIPGeoTile extends DDWRTTile<None> {

    private static final String TAG = PublicIPGeoTile.class.getSimpleName();

    private long mLastSync;
    private IPWhoisInfo mIPWhoisInfoWithGeo;
    private String mWanPublicIP;

    public PublicIPGeoTile(@NonNull Fragment parentFragment,
                           @NonNull Bundle arguments,
                           @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_public_ip_geo, null);

    }

    @Override
    public int getTileHeaderViewId() {
        return -1;
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

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    try {
                        updateProgressBarViewSeparator(40);

                        if (isDemoRouter(mRouter)) {
                            final long nbRunsLoaderModulo = (nbRunsLoader % 5);
                            if (nbRunsLoaderModulo == 0) {
                                //nbRunsLoader = 5k
                                mWanPublicIP = ("52.64." +
                                        (1 + new Random().nextInt(252))
                                        + "." +
                                        (1 + new Random().nextInt(252)));
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

                final MapView map = (MapView)
                        layout.findViewById(R.id.tile_public_ip_geo_map);
                map.setTileSource(TileSourceFactory.MAPNIK);

                map.setBuiltInZoomControls(true);
                map.setMultiTouchControls(false);

                final long dataUsageCtrl = mParentFragmentActivity
                        .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                        .getLong(DDWRTCompanionConstants.DATA_USAGE_NETWORK_PREF, 444);
                //If Only on WiFi flag, act accordingly
                map.setUseDataConnection(dataUsageCtrl != 333);

                final IMapController mapController = map.getController();
                mapController.setZoom(9);
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
            } else if (exception == null){
                updateProgressBarWithSuccess();
            }

        } finally {
            Crashlytics.log(Log.DEBUG, TAG, "onLoadFinished(): done loading!");
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }
}