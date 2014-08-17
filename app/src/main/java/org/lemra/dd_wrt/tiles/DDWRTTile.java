package org.lemra.dd_wrt.tiles;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;

/**
 * Created by armel on 8/14/14.
 */
public abstract class DDWRTTile<T> implements View.OnClickListener, LoaderManager.LoaderCallbacks<T>, CompoundButton.OnCheckedChangeListener {

    protected final SherlockFragmentActivity mParentFragmentActivity;
    protected final Bundle mFragmentArguments;
    protected final LoaderManager mSupportLoaderManager;
    protected boolean mAutoRefreshToggle = true;

    private static final String LOG_TAG = DDWRTTile.class.getSimpleName();

    @Nullable
    protected final Router mRouter;

    public DDWRTTile(@NotNull final SherlockFragmentActivity parentFragmentActivity, @NotNull final Bundle arguments, @Nullable Router router) {
        this.mParentFragmentActivity = parentFragmentActivity;
        this.mRouter = router;
        this.mSupportLoaderManager = this.mParentFragmentActivity.getSupportLoaderManager();
        this.mFragmentArguments = arguments;
    }

    @Override
    public final void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Log.d(LOG_TAG, this.getClass()+"#onCheckedChanged: isChecked="+isChecked);
        this.mAutoRefreshToggle = isChecked;
    }

    @Nullable
    public abstract ViewGroup getViewGroupLayout();
}
