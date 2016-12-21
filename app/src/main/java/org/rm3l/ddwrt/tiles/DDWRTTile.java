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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.common.base.Strings;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.fragments.AbstractBaseFragment;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.service.tasks.RouterInfoForFeedbackServiceTask;
import org.rm3l.ddwrt.service.tasks.RouterModelUpdaterServiceTask;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.AUTO_REFRESH_INTERVAL_SECONDS_PREF;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.AUTO_REFRESH_PREF;

/**
 * Abstract DDWRT Tile
 */
public abstract class DDWRTTile<T>
        implements
        View.OnClickListener,
        LoaderManager.LoaderCallbacks<T> {

    private static final String LOG_TAG = DDWRTTile.class.getSimpleName();
    @NonNull
    public final FragmentActivity mParentFragmentActivity;
    @Nullable
    public final SharedPreferences mParentFragmentPreferences;
    @NonNull
    public final SharedPreferences mGlobalPreferences;
    @NonNull
    protected final Fragment mParentFragment;
    @NonNull
    protected final Bundle mFragmentArguments;
    protected final LoaderManager mSupportLoaderManager;
    @Nullable
    protected final Router mRouter;
    @NonNull
    private final DDWRTCompanionDAO mDao;
    static {
        if (Looper.myLooper() == null) {
            //Check for this - otherwise it yields the following error:
            // "only one looper may be created per thread")
            //cf. http://stackoverflow.com/questions/23038682/java-lang-runtimeexception-only-one-looper-may-be-created-per-thread
            Looper.prepare();
        }
    }
    protected long nbRunsLoader = 0;
    protected ViewGroup layout;
    protected ViewGroup parentViewGroup;
    protected Integer layoutId;
    private boolean mLoaderStopped = true;

    private final AtomicBoolean mForceRefresh = new AtomicBoolean(false);

    protected final AtomicBoolean mRefreshing = new AtomicBoolean(false);

    private final AtomicReference<DDWRTTileRefreshListener> mRefreshListener = new AtomicReference<>();

    private final AtomicReference<ProgressBar> mProgressBarViewSeparator
            = new AtomicReference<>(null);

    @Nullable
    private InterstitialAd mTileClickInterstitialAd;

    public final RouterModelUpdaterServiceTask routerModelUpdaterServiceTask;
    public final RouterInfoForFeedbackServiceTask routerInfoForFeedbackServiceTask;

    public DDWRTTile(@NonNull final Fragment parentFragment, @NonNull final Bundle arguments, @Nullable Router router) {
        this.mParentFragment = parentFragment;
        this.mParentFragmentActivity = this.mParentFragment.getActivity();
        mDao = RouterManagementActivity.getDao(mParentFragmentActivity);
        this.mParentFragmentPreferences = ((router != null && this.mParentFragmentActivity != null) ? this.mParentFragmentActivity
                .getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE) : null);
        this.mGlobalPreferences = (this.mParentFragmentActivity != null ? this.mParentFragmentActivity
                .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE) : null);
        this.mRouter = router;
        this.mSupportLoaderManager = this.mParentFragment.getLoaderManager();
        this.mFragmentArguments = arguments;

        mParentFragmentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTileClickInterstitialAd = AdUtils.requestNewInterstitial(mParentFragmentActivity,
                        R.string.interstitial_ad_unit_id_tile_click);

            }
        });
        this.routerModelUpdaterServiceTask =
                new RouterModelUpdaterServiceTask(mParentFragmentActivity);
        this.routerInfoForFeedbackServiceTask =
                new RouterInfoForFeedbackServiceTask(mParentFragmentActivity);
    }

    public DDWRTTile(@NonNull final Fragment parentFragment, @NonNull final Bundle arguments,
                     @Nullable final Router router, @Nullable final Integer layoutId, @Nullable final Integer toggleRefreshButtonId) {
        this(parentFragment, arguments, router);
        this.layoutId = layoutId;
        if (layoutId != null) {
            this.layout = (ViewGroup) this.mParentFragment.getLayoutInflater(arguments).inflate(layoutId, null);
        }
    }

    protected void runBgServiceTaskAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    routerModelUpdaterServiceTask
                            .runBackgroundServiceTask(mRouter);
                    routerInfoForFeedbackServiceTask
                            .runBackgroundServiceTask(mRouter);
                } catch (final Exception e) {
                    //No worries
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public long getNbRunsLoader() {
        return nbRunsLoader;
    }

    public void setNbRunsLoader(long nbRunsLoader) {
        this.nbRunsLoader = nbRunsLoader;
    }

    @Nullable
    public Integer getTileBackgroundColor() {
        return null; //Automatic
    }

    @Nullable
    public Integer getLayoutId() {
        return layoutId;
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

    protected String getFormattedPrefKey(@NonNull final String scope) {
        return getFormattedPrefKey(this.getClass(), scope);
    }

    public static <T extends DDWRTTile> String getFormattedPrefKey(@NonNull final Class<T> clazz, @NonNull final String scope) {
        return clazz.getCanonicalName() + "::" + scope;
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

    public abstract int getTileHeaderViewId();

    public abstract int getTileTitleViewId();

    @Nullable
    protected abstract Loader<T> getLoader(int id, Bundle args);

    protected <T extends DDWRTTile> void doneWithLoaderInstance(final T tile, @NonNull final Loader loader,
                                                                final long nextRunMillis, @Nullable final int... additionalButtonsToMakeVisible) {

        try {
            final ViewGroup viewGroupLayout = this.getViewGroupLayout();
            if (viewGroupLayout != null) {
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

            mSupportLoaderManager.destroyLoader(loader.getId());

            final boolean schedNextRun =
                    ((!Utils.isDemoRouter(mRouter)) &&
                            !(this.mLoaderStopped ||
                                    nextRunMillis <= 0 ||
                                    mRouter == null ||
                                    mDao.getRouter(mRouter.getUuid()) == null));
            /*
             * Check if router still exists - if not, entry may have been deleted.
             * In this case, do NOT schedule next run.
             * Also re-schedule it if loader has not been stopped!
             */

            if (schedNextRun) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSupportLoaderManager.restartLoader(loader.getId(), mFragmentArguments, tile);
                    }
                }, nextRunMillis);
            }

            Crashlytics.log(Log.DEBUG,  LOG_TAG, String.format("onLoadFinished(): done loading: %s" +
                            "\n" +
                            "-> schedNextRun: %s\n" +
                            "->this.mLoaderStopped: %s" +
                            "\n" +
                            " - delay: %dms",
                    loader, schedNextRun, this.mLoaderStopped, nextRunMillis));
        } finally {
            final DDWRTTileRefreshListener refreshListener = this.mRefreshListener.get();
            if (refreshListener != null) {
                refreshListener.onTileRefreshed(this);
            }
        }
    }

    protected <T extends DDWRTTile> void doneWithLoaderInstance(final T tile, @NonNull final Loader loader,
                                                                @Nullable final int... additionalButtonsToMakeVisible) {
        final boolean isAutoRefreshEnabled = (this.mParentFragmentPreferences != null &&
                this.mParentFragmentPreferences.getBoolean(AUTO_REFRESH_PREF, false));
        Crashlytics.log(Log.DEBUG, LOG_TAG, "isAutoRefreshEnabled: " + isAutoRefreshEnabled);
        doneWithLoaderInstance(tile, loader,
                isAutoRefreshEnabled ?
                        (this.mParentFragmentPreferences != null ?
                                this.mParentFragmentPreferences.
                                        getLong(AUTO_REFRESH_INTERVAL_SECONDS_PREF, -1) * 1000 : -1)
                        : -1,
                additionalButtonsToMakeVisible);

        //No auto-refresh, now that user can refresh data manually
//        doneWithLoaderInstance(tile,
//                loader,
//                -1l,
//                additionalButtonsToMakeVisible);
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
        Crashlytics.log(Log.DEBUG,  getLogTag(), "onLoaderReset: loader=" + loader);
        loader.abandon();
    }

    @Override
    public final void onClick(View view) {
        final OnClickIntent onClickIntentAndListener = getOnclickIntent();
        final Intent onClickIntent;
        if (onClickIntentAndListener != null &&
                (onClickIntent = onClickIntentAndListener.getIntent()) != null) {

            onClickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            ReportingUtils.reportContentViewEvent(new ContentViewEvent()
                    .putContentType("Tile OnClick")
                    .putContentName(onClickIntent.getComponent() != null ?
                            onClickIntent.getComponent().getShortClassName() : "???")
                    .putContentId(this.getClass().getSimpleName()));


            if (BuildConfig.WITH_ADS &&
                    mTileClickInterstitialAd != null &&
                    AdUtils.canDisplayInterstialAd(mParentFragmentActivity)) {

                mTileClickInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        final AdRequest adRequest = AdUtils.buildAdRequest(mParentFragmentActivity);
                        if (adRequest != null) {
                            mTileClickInterstitialAd.loadAd(adRequest);
                        }
                        ((AbstractBaseFragment) mParentFragment)
                                .startActivityForResult(onClickIntent, onClickIntentAndListener.getListener());
                    }

                    @Override
                    public void onAdOpened() {
                        //Save preference
                        mGlobalPreferences.edit()
                                .putLong(
                                        DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                        System.currentTimeMillis())
                                .apply();
                    }
                });

                if (mTileClickInterstitialAd.isLoaded()) {
                    mTileClickInterstitialAd.show();
                } else {
                    final String dialogMsg = onClickIntentAndListener.getDialogMessage();
                    //noinspection ConstantConditions
//                    final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
//                            Strings.isNullOrEmpty(dialogMsg) ? "Loading detailed view..." : dialogMsg, false, false);
//                    alertDialog.show();
//                    ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
//
                    final ProgressDialog alertDialog = ProgressDialog.show(mParentFragmentActivity,
                            "Opening tile details",
                            Strings.isNullOrEmpty(dialogMsg) ? "Please Wait..." : dialogMsg,
                            true);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((AbstractBaseFragment) mParentFragment)
                                    .startActivityForResult(onClickIntent, onClickIntentAndListener.getListener());
                            mParentFragmentActivity.overridePendingTransition(
                                    R.anim.right_in, R.anim.left_out);
                            alertDialog.cancel();
                        }
                    }, 1000);
                }

            } else {
                final String dialogMsg = onClickIntentAndListener.getDialogMessage();
                //noinspection ConstantConditions
//                final AlertDialog alertDialog = Utils.buildAlertDialog(mParentFragmentActivity, null,
//                        Strings.isNullOrEmpty(dialogMsg) ? "Loading detailed view..." : dialogMsg, false, false);
//                alertDialog.show();
//                ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                final ProgressDialog alertDialog = ProgressDialog.show(mParentFragmentActivity,
                        "Opening tile details",
                        Strings.isNullOrEmpty(dialogMsg) ? "Please Wait..." : dialogMsg,
                        true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((AbstractBaseFragment) mParentFragment)
                                .startActivityForResult(onClickIntent, onClickIntentAndListener.getListener());
                        mParentFragmentActivity.overridePendingTransition(
                                R.anim.right_in, R.anim.left_out);
                        alertDialog.cancel();
                    }
                }, 1000);
            }
        }
    }

    public void onStop() {

    }

    public boolean isForceRefresh() {
        return mForceRefresh.get();
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.mForceRefresh.set(forceRefresh);
    }

    public void setRefreshListener(@Nullable final DDWRTTileRefreshListener refreshListener) {
        this.mRefreshListener.set(refreshListener);
    }

    public boolean isAdTile() {
        return false;
    }

    @Nullable
    protected abstract OnClickIntent getOnclickIntent();

    @Nullable
    protected Integer getProgressBarViewSeparatorId() {
        return R.id.tile_progress_bar;
    }

    protected void updateProgressBarViewSeparator(final int progressPercent) {
        mParentFragmentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateProgressBar(getOrSetProgressBar(),
                            R.drawable.progressbar_drawable_info,
                            Math.min(100, progressPercent));
                } catch (final Exception e) {
                    e.printStackTrace();
                    Utils.reportException(mParentFragmentActivity, e);
                }
            }
        });
    }

    public void updateProgressBarWithError() {
        mParentFragmentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ProgressBar progressBar = getOrSetProgressBar();
                    if (progressBar == null) {
                        return;
                    }
                    updateProgressBar(progressBar, R.drawable.progressbar_drawable_error,
                            progressBar.getProgress()+1);
                } catch (final Exception e) {
                    e.printStackTrace();
                    Utils.reportException(mParentFragmentActivity, e);
                }
            }
        });
    }

    public void updateProgressBarWithSuccess() {
        updateProgressBarViewSeparator(100);
    }

    private void updateProgressBar(@Nullable final ProgressBar progressBar,
                                   final int drawableResId, final int progress) {
        mParentFragmentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (progressBar == null) {
                        return;
                    }
                    progressBar.setProgressDrawable(
                            ContextCompat.getDrawable(mParentFragmentActivity,
                                    drawableResId));
                    progressBar.setProgress(progress);
                } catch (final Exception e) {
                    e.printStackTrace();
                    Utils.reportException(mParentFragmentActivity, e);
                }
            }
        });
    }

    @Nullable
    public ProgressBar getOrSetProgressBar() {
        ProgressBar progressBarViewSeparator = mProgressBarViewSeparator.get();
        if (progressBarViewSeparator == null) {
            final Integer progressBarViewSeparatorId = getProgressBarViewSeparatorId();
            if (progressBarViewSeparatorId != null) {
                final View viewById = layout.findViewById(progressBarViewSeparatorId);
                if (viewById instanceof ProgressBar) {
                    progressBarViewSeparator = (ProgressBar) viewById;
                    progressBarViewSeparator.setMax(100);
                    progressBarViewSeparator.setScaleY(0.11f);
                }
            }
            if (progressBarViewSeparator == null) {
                return null;
            }
            mProgressBarViewSeparator.set(progressBarViewSeparator);
        }
        return progressBarViewSeparator;
    }

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

        private String contentUniqueId;

        private String contentName;

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

    public interface DDWRTTileRefreshListener {

        void onTileRefreshed(@NonNull final DDWRTTile tile);

    }

}