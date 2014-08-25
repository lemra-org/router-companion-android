package org.lemra.dd_wrt.tiles.status.lan;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.exceptions.DDWRTNoDataException;
import org.lemra.dd_wrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.utils.SSHUtils;

/**
 * Created by armel on 8/26/14.
 */
public class DHCPStatusTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = DHCPStatusTile.class.getSimpleName();

    public DHCPStatusTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router);
    }

    @Nullable
    @Override
    public ViewGroup getViewGroupLayout() {
        final LinearLayout layout = (LinearLayout) this.mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_lan_dhcp_status, null);
        mToggleAutoRefreshButton = (ToggleButton) layout.findViewById(R.id.tile_status_lan_dhcp_status_togglebutton);
        mToggleAutoRefreshButton.setOnCheckedChangeListener(this);

        return layout;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + DHCPStatusTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    return SSHUtils.getNVRamInfoFromRouter(mRouter,
                            NVRAMInfo.LAN_PROTO,
                            NVRAMInfo.DHCP_DNSMASQ,
                            NVRAMInfo.DHCP_START,
                            NVRAMInfo.DHCP_NUM,
                            NVRAMInfo.DHCP_LEASE);

                } catch (final Exception e) {
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

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {

        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_lan_dhcp_status_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //Server Status
            final TextView dhcpServerView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_lan_dhcp_status_server);
            if (dhcpServerView != null) {
                final String lanProto = data.getProperty(NVRAMInfo.LAN_PROTO);
                String lanProtoTxt = "N/A";
                if ("dhcp".equalsIgnoreCase(lanProto)) {
                    lanProtoTxt = "Enabled";
                } else {
                    lanProtoTxt = "Disabled";
                }
                dhcpServerView.setText(lanProtoTxt);
            }

            //Daemon
            final TextView dhcpDaemonView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_lan_dhcp_status_daemon);
            if (dhcpDaemonView != null) {
                final String dhcpDnsmasq = data.getProperty(NVRAMInfo.DHCP_DNSMASQ);
                String dhcpDnsmasqTxt = "N/A";
                if ("1".equalsIgnoreCase(dhcpDnsmasq)) {
                    dhcpDnsmasqTxt = "DNSMasq";
                } else {
                    dhcpDnsmasqTxt = "???";
                }
                dhcpDaemonView.setText(dhcpDnsmasqTxt);
            }

            //FIXME Start IP
//            final TextView maskView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_lan_status_subnet_mask);
//            if (maskView != null) {
//                maskView.setText(data.getProperty(NVRAMInfo.LAN_NETMASK, "N/A"));
//            }

            //FIXME End IP
//            final TextView gwView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_lan_status_gateway);
//            if (gwView != null) {
//                gwView.setText(data.getProperty(NVRAMInfo.LAN_GATEWAY, "N/A"));
//            }

            final TextView clientLeaseView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_lan_dhcp_status_client_lease_time);
            if (clientLeaseView != null) {
                final String dhcpClientLeaseTime = data.getProperty(NVRAMInfo.DHCP_LEASE);
                clientLeaseView.setText(Strings.isNullOrEmpty(dhcpClientLeaseTime) ? "N/A" :
                        (dhcpClientLeaseTime + " min"));
            }

        }

        if (exception != null) {
            if (errorPlaceHolderView != null) {
                errorPlaceHolderView.setText(Throwables.getRootCause(exception).getMessage());
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_lan_status_togglebutton_title, R.id.tile_status_lan_status_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
