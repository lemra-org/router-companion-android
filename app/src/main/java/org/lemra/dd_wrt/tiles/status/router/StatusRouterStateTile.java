package org.lemra.dd_wrt.tiles.status.router;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Splitter;
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

import java.util.List;


/**
 * Created by armel on 8/14/14.
 */
public class StatusRouterStateTile extends DDWRTTile<NVRAMInfo> {

    public static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
    //    Drawable icon;
    private static final String LOG_TAG = StatusRouterStateTile.class.getSimpleName();

    public StatusRouterStateTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_router_router_state, R.id.tile_status_router_router_state_togglebutton);
//        // Parse the SVG file from the resource beforehand
//        try {
//            final SVG svg = SVGParser.getSVGFromResource(this.mParentFragmentActivity.getResources(), R.raw.router);
//            // Get a drawable from the parsed SVG and set it as the drawable for the ImageView
//            this.icon = svg.createPictureDrawable();
//        } catch (final Exception e) {
//            e.printStackTrace();
//            this.icon = this.mParentFragmentActivity.getResources().getDrawable(R.drawable.ic_icon_state);
//        }
    }
//
//    @Override
//    public ViewGroup getViewGroupLayout() {
//        final LinearLayout linearLayout = (LinearLayout) this.mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_router_router_state, null);
//        mToggleAutoRefreshButton = (ToggleButton) linearLayout.findViewById(R.id.tile_status_router_router_state_togglebutton);
//        mToggleAutoRefreshButton.setOnCheckedChangeListener(this);
//        return linearLayout;
////        final ImageView imageView = (ImageView) layout.findViewById(R.id.ic_tile_status_router_router_state);
////        imageView.setImageDrawable(this.icon);
////        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
////        return layout;
//    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Override
            public NVRAMInfo loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterStateTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(mRouter,
                            NVRAMInfo.ROUTER_NAME,
                            NVRAMInfo.WAN_IPADDR,
                            NVRAMInfo.MODEL,
                            NVRAMInfo.DIST_TYPE,
                            NVRAMInfo.LAN_IPADDR);

                    //Add FW, Kernel and Uptime
                    final String[] otherCmds = SSHUtils.getManualProperty(mRouter, "uptime", "uname -a");
                    if (otherCmds != null && otherCmds.length >= 2) {
                        //Uptime
                        final List<String> strings = SPLITTER.splitToList(otherCmds[0]);
                        if (strings != null && strings.size() > 0) {
                            if (nvramInfo != null) {
                                nvramInfo.setProperty(NVRAMInfo.UPTIME, strings.get(0));
                            }
                        }

                        //Kernel
                        if (nvramInfo != null) {
                            nvramInfo.setProperty(NVRAMInfo.KERNEL, otherCmds[1]);
                        }

                        //Firmware
                        //TODO

                    }

                    return nvramInfo;
                } catch (final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }

            }
        };
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
    public void onLoadFinished(final Loader<NVRAMInfo> loader, NVRAMInfo data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //Router Name
            final TextView routerNameView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_title);
            if (routerNameView != null) {
                routerNameView.setText(data.getProperty(NVRAMInfo.ROUTER_NAME, "N/A"));
            }

            //We can change the action bar title
//        this.layout.getSupportActionBar().setTitle((String) data);

            //WAN IP
            final TextView wanIpView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_wan_ip);
            if (wanIpView != null) {
                wanIpView.setText(data.getProperty(NVRAMInfo.WAN_IPADDR, "N/A"));
            }

            final TextView routerModelView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_model);
            if (routerModelView != null) {
                routerModelView.setText(data.getProperty(NVRAMInfo.MODEL, "N/A"));
            }

            final TextView lanIpView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_lan_ip);
            if (lanIpView != null) {
                lanIpView.setText(data.getProperty(NVRAMInfo.LAN_IPADDR, "N/A"));
            }

            final TextView fwView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_firmware);
            if (fwView != null) {
                fwView.setText(data.getProperty(NVRAMInfo.FIRMWARE, "N/A"));
            }

            final TextView kernelView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_kernel);
            if (kernelView != null) {
                kernelView.setText(data.getProperty(NVRAMInfo.KERNEL, "N/A"));
            }

            final TextView uptimeView = (TextView) this.layout.findViewById(R.id.tile_status_router_router_state_uptime);
            if (uptimeView != null) {
                uptimeView.setText(data.getProperty(NVRAMInfo.UPTIME, "N/A"));
            }

        }

        if (exception != null) {
            if (errorPlaceHolderView != null) {
                errorPlaceHolderView.setText(Throwables.getRootCause(exception).getMessage());
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_router_router_state_togglebutton_title, R.id.tile_status_router_router_state_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");

    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
