package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.crashlytics.android.Crashlytics;

/**
 * Created by rm3l on 22/12/15.
 */
public class RecyclerViewEmptySupport extends RecyclerView {

  private static final String TAG = RecyclerViewEmptySupport.class.getSimpleName();

  @Nullable View emptyView;
  @NonNull private final AdapterDataObserver observer = new AdapterDataObserver() {
    @Override public void onChanged() {
      Crashlytics.log(Log.DEBUG, TAG, "onChanged");
      super.onChanged();
      checkIfEmpty();
    }

    @Override public void onItemRangeInserted(int positionStart, int itemCount) {
      Crashlytics.log(Log.DEBUG, TAG,
          "onItemRangeInserted(" + positionStart + ", " + itemCount + ")");
      super.onItemRangeInserted(positionStart, itemCount);
      checkIfEmpty();
    }

    @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
      Crashlytics.log(Log.DEBUG, TAG,
          "onItemRangeInserted(" + positionStart + ", " + itemCount + ")");
      super.onItemRangeRemoved(positionStart, itemCount);
      checkIfEmpty();
    }
  };

  public RecyclerViewEmptySupport(Context context) {
    super(context);
  }

  public RecyclerViewEmptySupport(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RecyclerViewEmptySupport(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override public void setAdapter(@Nullable Adapter adapter) {
    final Adapter oldAdapter = getAdapter();
    if (oldAdapter != null) {
      oldAdapter.unregisterAdapterDataObserver(observer);
    }

    if (adapter != null) {
      adapter.registerAdapterDataObserver(observer);
    }
    super.setAdapter(adapter);
    checkIfEmpty();
  }

  @Override public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
    final Adapter oldAdapter = getAdapter();
    if (oldAdapter != null) {
      oldAdapter.unregisterAdapterDataObserver(observer);
    }

    if (adapter != null) {
      adapter.registerAdapterDataObserver(observer);
    }
    super.swapAdapter(adapter, removeAndRecycleExistingViews);
    checkIfEmpty();
  }

  /**
   * Indicates the view to be shown when the adapter for this object is empty
   *
   * @param emptyView the empty view
   */
  public void setEmptyView(@Nullable View emptyView) {
    if (this.emptyView != null) {
      this.emptyView.setVisibility(GONE);
    }

    this.emptyView = emptyView;
    checkIfEmpty();
  }

  /**
   * Check adapter item count and toggle visibility of empty view if the adapter is empty
   */
  private void checkIfEmpty() {
    if (emptyView == null || getAdapter() == null) {
      return;
    }

    if (getAdapter().getItemCount() > 0) {
      emptyView.setVisibility(GONE);
    } else {
      emptyView.setVisibility(VISIBLE);
    }
  }
}