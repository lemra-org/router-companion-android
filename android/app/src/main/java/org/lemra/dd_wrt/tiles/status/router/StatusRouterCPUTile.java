package org.lemra.dd_wrt.tiles.status.router;

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
import com.google.common.base.Splitter;
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

import java.util.List;

/**
 * Created by armel on 8/15/14.
 */
public class StatusRouterCPUTile extends DDWRTTile<NVRAMInfo> {

    public static final String GREP_MODEL_NAME_PROC_CPUINFO = "grep \"model name\" /proc/cpuinfo ";
    private static final String LOG_TAG = StatusRouterCPUTile.class.getSimpleName();

//    Drawable icon;

    public StatusRouterCPUTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router);
        // Parse the SVG file from the resource beforehand
//        try {
//            final SVG svg = SVGParser.getSVGFromResource(this.mParentFragmentActivity.getResources(), R.raw.cpu);
//            // Get a drawable from the parsed SVG and set it as the drawable for the ImageView
//            this.icon = svg.createPictureDrawable();
//        } catch (final Exception e) {
//            e.printStackTrace();
//            this.icon = this.mParentFragmentActivity.getResources().getDrawable(R.drawable.ic_icon_state);
//        }
    }

    @Nullable
    @Override
    public ViewGroup getViewGroupLayout() {
        final LinearLayout layout = (LinearLayout) this.mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_router_router_cpu, null);
        mToggleAutoRefreshButton = (ToggleButton) layout.findViewById(R.id.tile_status_router_router_cpu_togglebutton);
        mToggleAutoRefreshButton.setOnCheckedChangeListener(this);
        return layout;
//        final ImageView imageView = (ImageView) layout.findViewById(R.id.ic_tile_status_router_router_cpu);
//        imageView.setImageDrawable(this.icon);
//        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        return layout;
    }

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

                    Log.d(LOG_TAG, "Init background loader for " + StatusRouterCPUTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(mRouter,
                            NVRAMInfo.CPU_CLOCK_FREQ);

                    if (nvramInfo != null) {
                        final List<String> strings = Splitter.on(",")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(nvramInfo.getProperty(NVRAMInfo.CPU_CLOCK_FREQ));
                        Log.d(LOG_TAG, "strings for cpu clock: " + strings);
                        if (strings != null && strings.size() > 0) {
                            nvramInfo.setProperty(NVRAMInfo.CPU_CLOCK_FREQ, strings.get(0));
                        }
                    }

                    final String[] otherCmds = SSHUtils.getManualProperty(mRouter,
                            GREP_MODEL_NAME_PROC_CPUINFO +
                                    "| uniq", GREP_MODEL_NAME_PROC_CPUINFO + "| wc -l", "uptime");
                    if (otherCmds != null && otherCmds.length >= 3) {
                        //Model
                        List<String> strings = Splitter.on("model name\t:").omitEmptyStrings().trimResults().splitToList(otherCmds[0]);
                        Log.d(LOG_TAG, "strings for model name: " + strings);
                        if (strings != null && strings.size() >= 1) {
                            if (nvramInfo != null) {
                                nvramInfo.setProperty(NVRAMInfo.CPU_MODEL, strings.get(0));
                            }
                        }

                        //Nb Cores
                        if (nvramInfo != null) {
                            nvramInfo.setProperty(NVRAMInfo.CPU_CORES_COUNT, otherCmds[1]);
                        }

                        //Load Avg
                        if (nvramInfo != null) {
                            strings = Splitter.on("load average: ").omitEmptyStrings().trimResults().splitToList(otherCmds[2]);
                            Log.d(LOG_TAG, "strings for load avg: " + strings);
                            if (strings != null && strings.size() >= 2) {
                                nvramInfo.setProperty(NVRAMInfo.LOAD_AVERAGE, strings.get(1));
                            }
                        }
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

        final TextView errorPlaceHolderView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_router_router_cpu_error);

        final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            //Model
            final TextView cpuSpeedView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_router_router_cpu_speed);
            if (cpuSpeedView != null) {
                final String property = data.getProperty(NVRAMInfo.CPU_CLOCK_FREQ);
                cpuSpeedView.setText(Strings.isNullOrEmpty(property) ? "N/A" : (property + " MHz"));
            }

            //Model
            final TextView cpuModelView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_router_router_cpu_model);
            if (cpuModelView != null) {
                cpuModelView.setText(data.getProperty(NVRAMInfo.CPU_MODEL, "N/A"));
            }

            //Cores Count
            final TextView cpuCountView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_router_router_cpu_cores);
            if (cpuCountView != null) {
                cpuCountView.setText(data.getProperty(NVRAMInfo.CPU_CORES_COUNT, "N/A"));
            }

            //Load Avg
            final TextView loadAvgView = (TextView) this.mParentFragmentActivity.findViewById(R.id.tile_status_router_router_cpu_load_avg);
            if (loadAvgView != null) {
                loadAvgView.setText(data.getProperty(NVRAMInfo.LOAD_AVERAGE, "N/A"));
            }

        }

        if (exception != null) {
            if (errorPlaceHolderView != null) {
                errorPlaceHolderView.setText(Throwables.getRootCause(exception).getMessage());
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_router_router_cpu_togglebutton_title, R.id.tile_status_router_router_cpu_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
