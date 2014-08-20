package org.lemra.dd_wrt.tiles;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;

import static org.lemra.dd_wrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;

/**
 * Created by armel on 8/14/14.
 */
public abstract class DDWRTTile<T> implements View.OnClickListener, LoaderManager.LoaderCallbacks<T>, CompoundButton.OnCheckedChangeListener {

    public static final Handler HANDLER = new Handler();
    private static final String LOG_TAG = DDWRTTile.class.getSimpleName();
    protected final SherlockFragmentActivity mParentFragmentActivity;
    protected final Bundle mFragmentArguments;
    protected final LoaderManager mSupportLoaderManager;
    @Nullable
    protected final Router mRouter;
    protected boolean mAutoRefreshToggle = true;
    protected ToggleButton mToggleAutoRefreshButton = null;

    public DDWRTTile(@NotNull final SherlockFragmentActivity parentFragmentActivity, @NotNull final Bundle arguments, @Nullable Router router) {
        this.mParentFragmentActivity = parentFragmentActivity;
        this.mRouter = router;
        this.mSupportLoaderManager = this.mParentFragmentActivity.getSupportLoaderManager();
        this.mFragmentArguments = arguments;
    }

    @Override
    public final void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Log.d(LOG_TAG, this.getClass() + "#onCheckedChanged: isChecked=" + isChecked);
        this.mAutoRefreshToggle = isChecked;
    }

    @Nullable
    public abstract ViewGroup getViewGroupLayout();

    protected <T extends DDWRTTile<NVRAMInfo>> void doneWithLoaderInstance(final T tile, final Loader<NVRAMInfo> loader,
                                                                           final int... additionalButtonsToMakeVisible) {
        if (mToggleAutoRefreshButton != null) {
            mToggleAutoRefreshButton.setVisibility(View.VISIBLE);
            if (additionalButtonsToMakeVisible != null) {
                for (int viewToMakeVisible : additionalButtonsToMakeVisible) {
                    final View viewById = this.mParentFragmentActivity.findViewById(viewToMakeVisible);
                    if (viewById == null) {
                        continue;
                    }
                    viewById.setVisibility(View.VISIBLE);
                }
            }
        }

        //Re-schedule it!
        HANDLER.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSupportLoaderManager.restartLoader(loader.getId(), mFragmentArguments, tile);
            }
        }, TILE_REFRESH_MILLIS);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }
}
