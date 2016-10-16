package org.rm3l.ddwrt.mgmt.adapters;

import org.rm3l.ddwrt.widgets.RecyclerViewEmptySupport;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

/**
 * Created by rm3l on 13/10/2016.
 */

public class RouterListItemTouchHelperCallback extends SimpleItemTouchHelperCallback {

    public RouterListItemTouchHelperCallback(RecyclerViewEmptySupport recyclerView, ItemTouchHelperAdapter mAdapter) {
        super(recyclerView, mAdapter);
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }
}
