/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.tiles;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.fragments.DDWRTBaseFragment;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.TILE_REFRESH_MILLIS;

/**
 * Abstract DDWRT Tile
 */
public abstract class DDWRTTile<T> implements View.OnClickListener, LoaderManager.LoaderCallbacks<T>, CompoundButton.OnCheckedChangeListener {

    public static final Handler HANDLER = new Handler();
    private static final String LOG_TAG = DDWRTTile.class.getSimpleName();
    @NotNull
    protected final FragmentActivity mParentFragmentActivity;
    @Nullable
    protected final SharedPreferences mParentFragmentPreferences;
    @NotNull
    protected final SharedPreferences mGlobalPreferences;
    @NotNull
    protected final Fragment mParentFragment;
    @NotNull
    protected final Bundle mFragmentArguments;
    protected final LoaderManager mSupportLoaderManager;
    @Nullable
    protected final Router mRouter;
    protected long nbRunsLoader = 0;
    protected boolean mAutoRefreshToggle = true;
    @Nullable
    protected CompoundButton mToggleAutoRefreshButton = null;
    protected ViewGroup layout;
    protected ViewGroup parentViewGroup;
    private boolean mLoaderStopped = true;

    public DDWRTTile(@NotNull final Fragment parentFragment, @NotNull final Bundle arguments, @Nullable Router router) {
        this.mParentFragment = parentFragment;
        this.mParentFragmentActivity = this.mParentFragment.getActivity();
        this.mParentFragmentPreferences = (router != null ? this.mParentFragmentActivity
                .getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE) : null);
        this.mGlobalPreferences = this.mParentFragmentActivity
                .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.mRouter = router;
        this.mSupportLoaderManager = this.mParentFragment.getLoaderManager();
        this.mFragmentArguments = arguments;
    }

    public DDWRTTile(@NotNull final Fragment parentFragment, @NotNull final Bundle arguments,
                     @Nullable final Router router, @Nullable final Integer layoutId, @Nullable final Integer toggleRefreshButtonId) {
        this(parentFragment, arguments, router);
        if (layoutId != null) {
            this.layout = (ViewGroup) this.mParentFragment.getLayoutInflater(arguments).inflate(layoutId, null);
        }
        if (toggleRefreshButtonId != null) {
            this.mToggleAutoRefreshButton = (CompoundButton) layout.findViewById(toggleRefreshButtonId);
            if (this.mToggleAutoRefreshButton != null) {
                this.mToggleAutoRefreshButton.setOnCheckedChangeListener(this);
                if (this.mParentFragmentPreferences != null) {
                    this.mToggleAutoRefreshButton.setChecked(
                            this.mParentFragmentPreferences.getBoolean(getAutoRefreshPreferenceKey(), true)
                    );
                }
            }
        }
    }

    protected ViewGroup getParentViewGroup() {
        return parentViewGroup;
    }

    public DDWRTTile<T> setParentViewGroup(ViewGroup parentViewGroup) {
        this.parentViewGroup = parentViewGroup;
        return this;
    }

    /**
     * @return <code>true</code> if this tile should be embedded within a ScrollView,
     * <code>false</code> otherwise (in this case it will be embedded within a LinearLayout)
     */
    public boolean isEmbeddedWithinScrollView() {
        return true;
    }

    public void setLoaderStopped(boolean mLoaderStopped) {
        this.mLoaderStopped = mLoaderStopped;
    }

    @Override
    public final void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Log.d(LOG_TAG, this.getClass() + "#onCheckedChanged: isChecked=" + isChecked);
        this.mAutoRefreshToggle = isChecked;
        if (this.mParentFragmentPreferences != null) {
            final SharedPreferences.Editor editor = this.mParentFragmentPreferences.edit();
            editor.putBoolean(getAutoRefreshPreferenceKey(), this.mAutoRefreshToggle);
            editor.apply();
        }
    }

    protected String getAutoRefreshPreferenceKey() {
        return getFormattedPrefKey("autoRefresh");
    }

    protected String getFormattedPrefKey(@NotNull final String scope) {
        return this.getClass().getCanonicalName() + "::" + scope;
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
        final Loader<T> loader = this.getLoader(id, args);
        if (loader != null) {
            loader.forceLoad();
        }
        return loader;
    }

    public abstract int getTileTitleViewId();

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

        final long refreshDelay = (this.mParentFragmentPreferences != null ?
                this.mParentFragmentPreferences.
                        getLong(DDWRTCompanionConstants.SYNC_INTERVAL_MILLIS_PREF, TILE_REFRESH_MILLIS) : TILE_REFRESH_MILLIS);

        if (!this.mLoaderStopped) {
//        Re-schedule it if loader has not been stopped!
            HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSupportLoaderManager.restartLoader(loader.getId(), mFragmentArguments, tile);
                }
            }, refreshDelay);
        }

        Log.d(LOG_TAG, "onLoadFinished(): done loading: " + loader +
                "\n->this.mLoaderStopped: " + this.mLoaderStopped + " - delay: " + refreshDelay + "ms");
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
        final OnClickIntent onClickIntentAndListener = getOnclickIntent();
        final Intent onClickIntent;
        if (onClickIntentAndListener != null &&
                (onClickIntent = onClickIntentAndListener.getIntent()) != null) {
            final String dialogMsg = onClickIntentAndListener.getDialogMessage();
            //noinspection ConstantConditions
            final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
                    Strings.isNullOrEmpty(dialogMsg) ? "Loading detailed view..." : dialogMsg, false, false);
            alertDialog.show();
            ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((DDWRTBaseFragment) mParentFragment)
                            .startActivityForResult(onClickIntent, onClickIntentAndListener.getListener());
                    alertDialog.cancel();
                }
            }, 2500);
        }
    }

    @Nullable
    protected abstract OnClickIntent getOnclickIntent();

    public interface ActivityResultListener {
        void onResultCode(int resultCode, Intent data);
    }

    protected class OnClickIntent {
        @Nullable
        private final Intent intent;

        @Nullable
        private final ActivityResultListener listener;

        @Nullable
        private final String dialogMessage;

        public OnClickIntent(@Nullable final String dialogMessage,
                             @Nullable Intent intent,
                             @Nullable ActivityResultListener listener) {
            this.intent = intent;
            this.listener = listener;
            this.dialogMessage = dialogMessage;
        }

        @Nullable
        public Intent getIntent() {
            return intent;
        }

        @Nullable
        public ActivityResultListener getListener() {
            return listener;
        }

        @Nullable
        public String getDialogMessage() {
            return dialogMessage;
        }
    }
}
