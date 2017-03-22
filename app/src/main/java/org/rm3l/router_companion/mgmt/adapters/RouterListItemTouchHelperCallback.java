package org.rm3l.router_companion.mgmt.adapters;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport;

/**
 * Created by rm3l on 13/10/2016.
 */

public class RouterListItemTouchHelperCallback extends SimpleItemTouchHelperCallback {

  public RouterListItemTouchHelperCallback(RecyclerViewEmptySupport recyclerView,
      ItemTouchHelperAdapter mAdapter) {
    super(recyclerView, mAdapter);
  }

  @Override public boolean isItemViewSwipeEnabled() {
    return true;
  }
}
