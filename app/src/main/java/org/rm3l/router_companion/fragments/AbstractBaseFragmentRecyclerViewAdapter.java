package org.rm3l.router_companion.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

/** Created by rm3l on 20/11/15. */
public class AbstractBaseFragmentRecyclerViewAdapter
    extends RecyclerView.Adapter<AbstractBaseFragmentRecyclerViewAdapter.ViewHolder> {

  // Provide a reference to the views for each data item
  // Complex data items may need more than one view per item, and
  // you provide access to all the views for a data item in a view holder
  public class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final CardView cardView;

    private final View itemView;

    public ViewHolder(View itemView) {
      super(itemView);
      this.itemView = itemView;
      this.cardView = (CardView) this.itemView.findViewById(R.id.base_tile_cardview);
    }
  }

  private final Context mContext;

  private final Router mRouter;

  private final List<DDWRTTile> mTiles;

  public AbstractBaseFragmentRecyclerViewAdapter(
      Context context, Router router, List<DDWRTTile> tiles) {
    this.mContext = context;
    this.mRouter = router;
    this.mTiles = tiles;
  }

  @Override
  public int getItemCount() {
    return mTiles != null ? mTiles.size() : 0;
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    final DDWRTTile ddwrtTile = this.mTiles.get(position);
    final ViewGroup viewGroupLayout;
    if (ddwrtTile == null || (viewGroupLayout = ddwrtTile.getViewGroupLayout()) == null) {
      Utils.reportException(
          null,
          new IllegalStateException(
              "ddwrtTile == null || " + "ddwrtTile.getViewGroupLayout() == null"));
      Toast.makeText(mContext, "Internal Error - please try again later", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    final boolean isThemeLight = ColorUtils.Companion.isThemeLight(mContext);

    final Integer tileTitleViewId = ddwrtTile.getTileTitleViewId();
    if (tileTitleViewId != null) {
      final TextView titleTextView = viewGroupLayout.findViewById(tileTitleViewId);
      ColorUtils.Companion.setTextColor(
          titleTextView, mRouter != null ? mRouter.getRouterFirmware() : null);
      //        if (isThemeLight) {
      //            if (titleTextView != null) {
      //                titleTextView.setTextColor(ContextCompat.getColor(mContext,
      //                        android.R.color.holo_blue_dark));
      //            }
      //        }
    }
    viewGroupLayout.setBackgroundColor(
        ContextCompat.getColor(mContext, android.R.color.transparent));

    holder.cardView.removeAllViews();
    if (viewGroupLayout.getParent() == null) {
      holder.cardView.addView(viewGroupLayout);
    }
    holder.cardView.setContentPadding(15, 5, 15, 5);
    holder.cardView.setOnClickListener(ddwrtTile);

    //                cardView.setCardBackgroundColor(themeBackgroundColor);
    // Add padding to CardView on v20 and before to prevent intersections between the Card content
    // and rounded corners.
    holder.cardView.setPreventCornerOverlap(true);
    // Add padding in API v21+ as well to have the same measurements with previous versions.
    holder.cardView.setUseCompatPadding(true);
    holder.cardView.setCardElevation(5f);

    final Integer tileBackgroundColor = ddwrtTile.getTileBackgroundColor();
    if (tileBackgroundColor != null) {
      holder.cardView.setCardBackgroundColor(tileBackgroundColor);
    } else {
      if (isThemeLight) {
        // Light
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(mContext, R.color.cardview_light_background));
      } else {
        // Default is Dark
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(mContext, R.color.cardview_dark_background));
      }
    }
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    // create a new view
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.base_tiles_container_tile_recycler_row_view, parent, false);
    // set the view's size, margins, paddings and layout parameters
    // ...
    final ViewHolder vh = new ViewHolder(v);
    return vh;
  }
}
