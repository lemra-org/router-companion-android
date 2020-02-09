package org.rm3l.router_companion.tiles.dashboard.network;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Collections;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.IPWhoisInfo;
import org.rm3l.router_companion.resources.None;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.status.wireless.ActiveIPConnectionsDetailActivity;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.PermissionsUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.widgets.map.MyOwnItemizedOverlay;

/**
 * Created by rm3l on 29/12/15.
 */
public class PublicIPGeoTile extends DDWRTTile<None> {

    private static final String TAG = PublicIPGeoTile.class.getSimpleName();

    public static final String STORAGE_PERMISSION_REQUEST_MESSAGE
            = "Storage access is required to cache and display map tiles. Tap to give permissions.";

    private boolean isThemeLight;

    private IPWhoisInfo mIPWhoisInfoWithGeo;

    private long mLastSync;

    private String mWanPublicIP;

    private final ImageButton zoomMapButton;

    public PublicIPGeoTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
            @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_public_ip_geo, null);

        isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

        final View mapThumbnailView = layout.findViewById(R.id.tile_public_ip_geo_container);

        zoomMapButton = layout.findViewById(R.id.tile_public_ip_geo_title_zoom);
        zoomMapButton.setOnClickListener(zoomMapButtonView -> {
            //Open IPGeoActivity with zoom effect
            if (isNullOrEmpty(mWanPublicIP)) {
                Toast.makeText(mParentFragmentActivity, "WAN Public IP Unknown at this time",
                        Toast.LENGTH_LONG).show();
            }
            final Intent intent = new Intent(mParentFragmentActivity, IPGeoActivity.class);
            intent.putExtra(IPGeoActivity.PUBLIC_IP_TO_DISPLAY, mWanPublicIP);
            intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());

            final ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeScaleUpAnimation(mapThumbnailView, 0, 0,
                            mapThumbnailView.getWidth(), mapThumbnailView.getHeight());
            ActivityCompat.startActivity(mParentFragmentActivity, intent, options.toBundle());
        });
    }

    @Override
    public Integer getTileHeaderViewId() {
        return R.id.tile_public_ip_geo_title_hdr;
    }

    @Override
    public Integer getTileTitleViewId() {
        return R.id.tile_public_ip_geo_title;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {
        try {//Set tiles
            FirebaseCrashlytics.getInstance().log("onLoadFinished: loader=" + loader + " / data=" + data);
            FirebaseCrashlytics.getInstance().log(
                    String.format("<mWanPublicIP=%s, mIPWhoisInfoWithGeo=%s>", mWanPublicIP,
                            mIPWhoisInfoWithGeo));

            layout.findViewById(R.id.tile_public_ip_geo_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_public_ip_geo_container).setVisibility(View.VISIBLE);

            if (data == null) {
                data = (None) new None().setException(new DDWRTNoDataException("No Data!"));
            }

            Exception exception = data.getException();

            Double latitude = null;
            Double longitude = null;
            if (exception == null) {
                if (mIPWhoisInfoWithGeo == null) {
                    data = (None) new None().setException(
                            new DDWRTNoDataException("Failed to retrieved Public IP Address geolocation data!"));
                } else {
                    try {
                        if (mIPWhoisInfoWithGeo.getLatitude() != null) {
                            latitude = Double.parseDouble(mIPWhoisInfoWithGeo.getLatitude());
                        }
                        if (mIPWhoisInfoWithGeo.getLongitude() != null) {
                            longitude = Double.parseDouble(mIPWhoisInfoWithGeo.getLongitude());
                        }
                    } catch (final NumberFormatException nfe) {
                        ReportingUtils.reportException(PublicIPGeoTile.this.mParentFragmentActivity, nfe);
                        nfe.printStackTrace();
                        //No worries
                    }
                    if (latitude == null || longitude == null) {
                        ReportingUtils.reportException(mParentFragmentActivity,
                                new IllegalStateException("latitude == null || longitude == null"));
                        data = (None) new None().setException(
                                new DDWRTNoDataException("Invalid coordinates - please try again later!"));
                    }
                }
                exception = data.getException();
            }

            final TextView errorPlaceHolderView =
                    this.layout.findViewById(R.id.tile_public_ip_geo_error);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Update last sync
                final RelativeTimeTextView lastSyncView =
                        layout.findViewById(R.id.tile_last_sync);
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText(
                        "Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(v -> {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (rootCause != null) {
                        Toast.makeText(parentContext, rootCause.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                updateProgressBarWithError();
            } else if (exception == null) {
                final MapView map = layout.findViewById(R.id.tile_public_ip_geo_map);

                if (!PermissionsUtils.isPermissionGranted(mParentFragmentActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    errorPlaceHolderView.setText(STORAGE_PERMISSION_REQUEST_MESSAGE);
                    errorPlaceHolderView.setOnClickListener(v -> {
                        PermissionsUtils.requestPermissions(mParentFragmentActivity,
                                Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                () -> {
                                    mParentFragmentActivity.runOnUiThread(() -> {
                                        errorPlaceHolderView.setVisibility(View.GONE);
                                        map.invalidate();
                                    });
                                    return null;
                                },
                                () -> {
                                    mParentFragmentActivity.runOnUiThread(() -> {
                                        errorPlaceHolderView.setText(STORAGE_PERMISSION_REQUEST_MESSAGE);
                                        errorPlaceHolderView.setVisibility(View.VISIBLE);
                                        map.invalidate();
                                    });
                                    return null;
                                },
                                "Storage access is required to display and cache map tiles");
                    });
                    errorPlaceHolderView.setVisibility(View.VISIBLE);
                    updateProgressBarWithError();
                } else {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                map.setTileSource(RouterCompanionAppConstants.TILE_SOURCE);

                map.getOverlayManager()
                        .getTilesOverlay()
                        .setColorFilter(isThemeLight ? null : TilesOverlay.INVERT_COLORS);

                map.setBuiltInZoomControls(false);
                map.setMultiTouchControls(false);

                //Act according to user-defined data usage control setting
                map.setUseDataConnection(Utils.canUseDataConnection(mParentFragmentActivity));

                final IMapController mapController = map.getController();
                mapController.setZoom(11);
                if (latitude != null && longitude != null) {
                    final GeoPoint publicIpPoint = new GeoPoint(latitude, longitude);
                    mapController.setCenter(publicIpPoint);

                    final OverlayItem overlayItem = new OverlayItem(mIPWhoisInfoWithGeo.getIp(),
                            String.format("- Prefix: %s\n"
                                            + "- Country: %s (%s)\n"
                                            + "- Region: %s\n"
                                            + "- City: %s\n"
                                            + "- Organization: %s\n"
                                            + "- ASN: %s\n"
                                            + "- Latitude: %s\n"
                                            + "- Longitude: %s", nullToEmpty(mIPWhoisInfoWithGeo.getPrefix()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getCountry()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getCountry_code()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getRegion()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getCity()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getOrganization()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getAsn()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getLatitude()),
                                    nullToEmpty(mIPWhoisInfoWithGeo.getLongitude())), publicIpPoint);
                    final ArrayList<OverlayItem> overlayItems = new ArrayList<>();
                    overlayItems.add(overlayItem);
                    final MyOwnItemizedOverlay overlay =
                            new MyOwnItemizedOverlay(mParentFragmentActivity, overlayItems);
                    map.getOverlays().add(overlay);
                }

                updateProgressBarWithSuccess();
            }
        } finally {
            FirebaseCrashlytics.getInstance().log("onLoadFinished(): done loading!");
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<None>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public None loadInBackground() {

                try {

                    FirebaseCrashlytics.getInstance().log("Init background loader for "
                            + PublicIPGeoTile.class
                            + ": routerInfo="
                            + mRouter
                            + " / nbRunsLoader="
                            + nbRunsLoader);

                    isThemeLight = ColorUtils.Companion.isThemeLight(mParentFragmentActivity);

                    if (mRefreshing.getAndSet(true)) {
                        return (None) new None().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mParentFragmentActivity.runOnUiThread(() ->
                            zoomMapButton.setImageDrawable(ContextCompat.getDrawable(mParentFragmentActivity,
                            isThemeLight ? R.drawable.ic_zoom_out_map_black_24dp
                                    : R.drawable.ic_zoom_out_map_white_24dp)));

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    try {

                        mWanPublicIP = mRouterConnector.getWanPublicIpAddress(mParentFragmentActivity, mRouter,
                                new RemoteDataRetrievalListener() {
                                    @Override
                                    public void doRegardlessOfStatus() {
                                        //Nothing to do here!
                                    }

                                    @Override
                                    public void onProgressUpdate(int progress) {
                                        updateProgressBarViewSeparator(progress);
                                    }
                                });

                        if (mWanPublicIP != null && Patterns.IP_ADDRESS.matcher(mWanPublicIP).matches()) {
                            mIPWhoisInfoWithGeo =
                                    ActiveIPConnectionsDetailActivity.mIPWhoisInfoCache.get(mWanPublicIP);
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
}