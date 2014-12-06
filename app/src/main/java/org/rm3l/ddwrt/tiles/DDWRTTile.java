/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.tiles;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.Utils;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;

/**
 * Abstract DDWRT Tile
 */
public abstract class DDWRTTile<T> implements View.OnClickListener, LoaderManager.LoaderCallbacks<T>, CompoundButton.OnCheckedChangeListener {

    public static final Handler HANDLER = new Handler();
    private static final String LOG_TAG = DDWRTTile.class.getSimpleName();
    @NotNull
    protected final SherlockFragmentActivity mParentFragmentActivity;
    @NotNull final SherlockFragment mParentFragment;
    @NotNull
    protected final Bundle mFragmentArguments;
    protected final LoaderManager mSupportLoaderManager;
    @Nullable
    protected final Router mRouter;
    protected long nbRunsLoader = 0;
    protected boolean mAutoRefreshToggle = true;
    @Nullable
    protected ToggleButton mToggleAutoRefreshButton = null;
    protected ViewGroup layout;

    public DDWRTTile(@NotNull final SherlockFragment parentFragment, @NotNull final Bundle arguments, @Nullable Router router) {
        this.mParentFragment = parentFragment;
        this.mParentFragmentActivity = this.mParentFragment.getSherlockActivity();
        this.mRouter = router;
        this.mSupportLoaderManager = this.mParentFragment.getLoaderManager();
        this.mFragmentArguments = arguments;
    }

    public DDWRTTile(@NotNull final SherlockFragment parentFragment, @NotNull final Bundle arguments,
                     @Nullable final Router router, @Nullable final Integer layoutId, @Nullable final Integer toggleRefreshButtonId) {
        this(parentFragment, arguments, router);
        if (layoutId != null) {
            this.layout = (ViewGroup) this.mParentFragment.getLayoutInflater(arguments).inflate(layoutId, null);
        }
        if (toggleRefreshButtonId != null) {
            this.mToggleAutoRefreshButton = (ToggleButton) layout.findViewById(toggleRefreshButtonId);
            if (this.mToggleAutoRefreshButton != null) {
                this.mToggleAutoRefreshButton.setOnCheckedChangeListener(this);
            }
        }
    }

    @Override
    public final void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Log.d(LOG_TAG, this.getClass() + "#onCheckedChanged: isChecked=" + isChecked);
        this.mAutoRefreshToggle = isChecked;
    }

    @Nullable
    public ViewGroup getViewGroupLayout() {
        return this.layout;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    @Nullable
    public final Loader<T> onCreateLoader(int id, Bundle args) {
        @Nullable final Loader<T> loader = this.getLoader(id, args);
        if (loader == null) {
            return null;
        }
        loader.forceLoad();
        return loader;
    }

    @Nullable
    protected abstract Loader<T> getLoader(int id, Bundle args);

    protected <T extends DDWRTTile> void doneWithLoaderInstance(final T tile, @NotNull final Loader loader,
                                                                @Nullable final int... additionalButtonsToMakeVisible) {

        @Nullable final ViewGroup viewGroupLayout = this.getViewGroupLayout();
        if (viewGroupLayout != null && mToggleAutoRefreshButton != null) {
            mToggleAutoRefreshButton.setVisibility(View.VISIBLE);
            if (additionalButtonsToMakeVisible != null) {
                for (int viewToMakeVisible : additionalButtonsToMakeVisible) {
                    final View viewById = viewGroupLayout.findViewById(viewToMakeVisible);
                    if (viewById == null) {
                        continue;
                    }
                    viewById.setVisibility(View.VISIBLE);
                }
            }
        }

//        Re-schedule it!
        HANDLER.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSupportLoaderManager.restartLoader(loader.getId(), mFragmentArguments, tile);
            }
        }, TILE_REFRESH_MILLIS);

        Log.d(LOG_TAG, "onLoadFinished(): done loading: " + loader);
    }

    @Nullable
    protected abstract String getLogTag();

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public final void onLoaderReset(Loader<T> loader) {
        Log.d(getLogTag(), "onLoaderReset: loader=" + loader);
        loader.abandon();
    }

    @Override
    public final void onClick(View view) {
        final Intent onClickIntent = getOnclickIntent();
        if (onClickIntent != null) {
            final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                    String.format("Loading detailed view for '%s' ...", this.getClass().getSimpleName()), false, false);
            alertDialog.show();
            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mParentFragmentActivity.startActivity(onClickIntent);
                    alertDialog.cancel();
                }
            }, 2500);
        }
    }

    @Nullable
    protected abstract Intent getOnclickIntent();
}
