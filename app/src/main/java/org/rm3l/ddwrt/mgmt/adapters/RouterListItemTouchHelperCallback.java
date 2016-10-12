package org.rm3l.ddwrt.mgmt.adapters;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

/**
 * Created by rm3l on 13/10/2016.
 */

public class RouterListItemTouchHelperCallback extends SimpleItemTouchHelperCallback {

    public RouterListItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        super(adapter);
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }
}
