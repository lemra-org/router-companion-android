package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;

import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

public class NetworkTopologyMapTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = NetworkTopologyMapTile.class.getSimpleName();

    public NetworkTopologyMapTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_network_map_overview,
                R.id.tile_network_map_togglebutton);
    }

    @Override
    public int getTileHeaderViewId() {
        return -1;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_network_map_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + NetworkTopologyMapTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        if (isDemoRouter(mRouter)) {
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(NVRAMInfo.ROUTER_NAME, "Demo Router (Test Data)")
                                    .setProperty(NVRAMInfo.WAN_IPADDR, "1.2.3.4")
                                    .setProperty(NVRAMInfo.LAN_IPADDR, "255.255.255.255");
                        } else {
                            nvramInfoTmp =
                                    SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                                            mGlobalPreferences,
                                            NVRAMInfo.ROUTER_NAME,
                                            NVRAMInfo.WAN_IPADDR,
                                            NVRAMInfo.LAN_IPADDR);
                        }
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }
                        //TODO Add other info over here
                    }

                    if (nvramInfo.isEmpty()) {
                        throw new DDWRTNoDataException("No Data!");
                    }

                    return nvramInfo;
                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        layout.findViewById(R.id.tile_network_map_loading_view)
                .setVisibility(View.GONE);
        layout.findViewById(R.id.tile_network_map_gridLayout)
                .setVisibility(View.VISIBLE);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_network_map_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                errorPlaceHolderView.setVisibility(View.GONE);
            }

            //Router Name
            final TextView routerNameView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_name);
            final String routerName = data.getProperty(NVRAMInfo.ROUTER_NAME);
            final boolean routerNameNull = (routerName == null);
            String routerNameToSet = routerName;
            if (routerNameNull) {
                routerNameToSet = "(empty)";
            }
            routerNameView.setTypeface(null, routerNameNull ? Typeface.ITALIC : Typeface.NORMAL);
            routerNameView.setText(routerNameToSet);

            //WAN IP
            final TextView wanIpView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_wan_ip);
            wanIpView.setText("WAN IP: " + data.getProperty(NVRAMInfo.WAN_IPADDR, "-"));

            //WAN IP
            final TextView lanIpView = (TextView) this.layout.findViewById(R.id.tile_network_map_router_lan_ip);
            lanIpView.setText("LAN IP: " + data.getProperty(NVRAMInfo.LAN_IPADDR, "-"));

            //TODO
            final TextView devicesCountTextView
                    = (TextView) layout.findViewById(R.id.tile_network_map_wan_lan_textView);



            layout.findViewById(R.id.tile_network_map_router)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Open Router State tab
                            if (mParentFragmentActivity instanceof DDWRTMainActivity) {
                                ((DDWRTMainActivity) mParentFragmentActivity)
                                        .selectItem(2);
                            } else {
                                //TODO Set proper flags ???
                                final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
                                intent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
                                intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 2);
                                mParentFragmentActivity.startActivity(intent);
                            }
                        }
                    });

            devicesCountTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Open tab with wireless devices
                    if (mParentFragmentActivity instanceof DDWRTMainActivity) {
                        ((DDWRTMainActivity) mParentFragmentActivity)
                                .selectItem(4);
                    } else {
                        //TODO Set proper flags ???
                        final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
                        intent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
                        intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 4);
                        mParentFragmentActivity.startActivity(intent);
                    }
                }
            });
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
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_network_map_togglebutton_title,
                R.id.tile_network_map_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }
}
