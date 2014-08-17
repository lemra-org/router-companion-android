package org.lemra.dd_wrt.tiles.status;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.tiles.DDWRTTile;

/**
 * Created by armel on 8/15/14.
 */
public class StatusRouterSpaceUsageTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusRouterSpaceUsageTile.class.getSimpleName();

//    Drawable icon;
    long nbRunsLoader = 0;


    public StatusRouterSpaceUsageTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router);
//        // Parse the SVG file from the resource beforehand
//        try {
//            final SVG svg = SVGParser.getSVGFromResource(this.mParentFragmentActivity.getResources(), R.raw.disk);
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
        final LinearLayout layout = (LinearLayout) this.mParentFragmentActivity.getLayoutInflater().inflate(R.layout.tile_status_router_router_space_usage, null);
        final ToggleButton toggle = (ToggleButton) layout.findViewById(R.id.tile_status_router_router_space_usage_togglebutton);
        toggle.setOnCheckedChangeListener(this);

        return layout;
//        final ImageView imageView = (ImageView) layout.findViewById(R.id.ic_tile_status_router_router_space_usage);
//        imageView.setImageDrawable(this.icon);
//        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        return layout;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<NVRAMInfo> onCreateLoader(int id, Bundle args) {
        final Loader<NVRAMInfo> loader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Override
            public NVRAMInfo loadInBackground() {

                Log.d(LOG_TAG, "Init background loader for " + StatusRouterSpaceUsageTile.class + ": routerInfo=" +
                        mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle+ " / nbRunsLoader="+nbRunsLoader);

                if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                    //Skip run
                    Log.d(LOG_TAG, "Skip loader run");
                    return null;
                }
                nbRunsLoader++;

                //TODO
                return null;
            }
        };
        loader.forceLoad();
        return loader;
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link FragmentManager#beginTransaction()
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
    public void onLoadFinished(final Loader<NVRAMInfo> loader, final NVRAMInfo data) {
        if (data != null) {
            //TODO
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSupportLoaderManager.restartLoader(loader.getId(), null, StatusRouterSpaceUsageTile.this);
            }
        }, 10000l);
        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<NVRAMInfo> loader) {

    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
