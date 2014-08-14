package org.lemra.dd_wrt.tiles;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by armel on 8/14/14.
 */
public abstract class DDWRTTile<T> implements View.OnClickListener, LoaderManager.LoaderCallbacks<T> {

    protected final SherlockFragmentActivity mParentFragmentActivity;
    protected final Bundle mFragmentArguments;

    public DDWRTTile(@NotNull final SherlockFragmentActivity parentFragmentActivity, @NotNull final Bundle arguments) {
        this.mParentFragmentActivity = parentFragmentActivity;
        this.mFragmentArguments = arguments;
    }

    @Nullable
    public abstract ViewGroup getViewGroupLayout();
}
