package org.rm3l.router_companion.widgets.map;

import android.app.AlertDialog;
import android.content.Context;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.List;

/**
 * Created by rm3l on 30/12/15.
 */
public class MyOwnItemizedOverlay extends ItemizedIconOverlay<OverlayItem>
{
    protected Context mContext;

    public MyOwnItemizedOverlay(final Context context, final List<OverlayItem> aList) {
        super(context, aList, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                return false;
            }
            @Override public boolean onItemLongPress(final int index, final OverlayItem item) {
                return false;
            }
        } );
        mContext = context;
    }

    @Override
    protected boolean onSingleTapUpHelper(final int index, final OverlayItem item, final MapView mapView) {
        //Toast.makeText(mContext, "Item " + index + " has been tapped!", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle(item.getTitle());
        dialog.setMessage(item.getSnippet());
        dialog.show();
        return true;
    }
}
