package org.lemra.dd_wrt.tiles.status.bandwidth;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Throwables;

import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.exceptions.DDWRTNoDataException;
import org.lemra.dd_wrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.tiles.status.wan.WANConfigTile;
import org.lemra.dd_wrt.utils.SSHUtils;

/**
 * Created by armel on 8/31/14.
 */
public class IfacesTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = IfacesTile.class.getSimpleName();

    public IfacesTile(SherlockFragmentActivity parentFragmentActivity, Bundle arguments, Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_bandwidth_ifaces, R.id.tile_status_bandwidth_ifaces_togglebutton);
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + WANConfigTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    return SSHUtils.getNVRamInfoFromRouter(mRouter,
                            NVRAMInfo.LAN_IFNAME,
                            NVRAMInfo.WAN_IFNAME,
                            NVRAMInfo.LANDEVS);

                } catch (final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wan_traffic_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //LAN
            final TextView lanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_lan);
            if (lanIfaceView != null) {
                lanIfaceView.setText(data.getProperty(NVRAMInfo.LAN_IFNAME, "N/A"));
            }

            //WAN
            final TextView wanIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wan);
            if (wanIfaceView != null) {
                wanIfaceView.setText(data.getProperty(NVRAMInfo.WAN_IFNAME, "N/A"));
            }

            //Wireless
            final TextView wlIfaceView = (TextView) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wireless);
            if (wlIfaceView != null) {
                wlIfaceView.setText(data.getProperty(NVRAMInfo.LANDEVS, "N/A"));
            }
        }

        if (exception != null) {
            if (errorPlaceHolderView != null) {
                errorPlaceHolderView.setText(Throwables.getRootCause(exception).getMessage());
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_bandwidth_ifaces_togglebutton_title, R.id.tile_status_bandwidth_ifaces_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
