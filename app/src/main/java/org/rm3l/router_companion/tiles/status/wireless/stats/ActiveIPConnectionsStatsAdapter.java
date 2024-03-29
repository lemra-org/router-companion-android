package org.rm3l.router_companion.tiles.status.wireless.stats;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultimap;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.tiles.status.wireless.ActiveIPConnectionsDetailActivity;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.ViewGroupUtils;

/** Created by rm3l on 12/04/16. */
public class ActiveIPConnectionsStatsAdapter
    extends Adapter<ActiveIPConnectionsStatsAdapter.ViewHolder> {

  class ViewHolder extends RecyclerView.ViewHolder {

    final View mItemView;

    final ImageButton shareImageButton;

    final View stats1;

    final TextView stats1PercentValue;

    final ProgressBar stats1ProgressBar;

    final TextView stats1Text;

    final View stats2;

    final TextView stats2PercentValue;

    final ProgressBar stats2ProgressBar;

    final TextView stats2Text;

    final View stats3;

    final TextView stats3PercentValue;

    final ProgressBar stats3ProgressBar;

    final TextView stats3Text;

    final View stats4;

    final TextView stats4PercentValue;

    final ProgressBar stats4ProgressBar;

    final TextView stats4Text;

    final View stats5;

    final TextView stats5PercentValue;

    final ProgressBar stats5ProgressBar;

    final TextView stats5Text;

    final View stats6Other;

    final TextView stats6OtherPercentValue;

    final ProgressBar stats6OtherProgressBar;

    final TextView stats6OtherText;

    final TextView statsErrorView;

    final ProgressBar statsLoadingView;

    private final Context mContext;

    private TextView title;

    public ViewHolder(final Context context, View itemView) {
      super(itemView);
      this.mContext = context;
      this.mItemView = itemView;

      this.shareImageButton =
          (ImageButton) itemView.findViewById(R.id.activity_ip_connections_stats_share);

      this.statsLoadingView =
          (ProgressBar) itemView.findViewById(R.id.activity_ip_connections_stats_loading_view);

      this.title = (TextView) itemView.findViewById(R.id.activity_ip_connections_stats_title);

      this.stats1 = itemView.findViewById(R.id.activity_ip_connections_stats_1);
      this.stats1PercentValue =
          (TextView) this.stats1.findViewById(R.id.activity_ip_connections_stats_1_percent);
      this.stats1Text =
          (TextView) this.stats1.findViewById(R.id.activity_ip_connections_stats_1_text);
      this.stats1ProgressBar =
          (ProgressBar) this.stats1.findViewById(R.id.activity_ip_connections_stats_1_progressbar);

      this.stats2 = itemView.findViewById(R.id.activity_ip_connections_stats_2);
      this.stats2PercentValue =
          (TextView) this.stats2.findViewById(R.id.activity_ip_connections_stats_2_percent);
      this.stats2Text =
          (TextView) this.stats2.findViewById(R.id.activity_ip_connections_stats_2_text);
      this.stats2ProgressBar =
          (ProgressBar) this.stats2.findViewById(R.id.activity_ip_connections_stats_2_progressbar);

      this.stats3 = itemView.findViewById(R.id.activity_ip_connections_stats_3);
      this.stats3PercentValue =
          (TextView) this.stats3.findViewById(R.id.activity_ip_connections_stats_3_percent);
      this.stats3Text =
          (TextView) this.stats3.findViewById(R.id.activity_ip_connections_stats_3_text);
      this.stats3ProgressBar =
          (ProgressBar) this.stats3.findViewById(R.id.activity_ip_connections_stats_3_progressbar);

      this.stats4 = itemView.findViewById(R.id.activity_ip_connections_stats_4);
      this.stats4PercentValue =
          (TextView) this.stats4.findViewById(R.id.activity_ip_connections_stats_4_percent);
      this.stats4Text =
          (TextView) this.stats4.findViewById(R.id.activity_ip_connections_stats_4_text);
      this.stats4ProgressBar =
          (ProgressBar) this.stats4.findViewById(R.id.activity_ip_connections_stats_4_progressbar);

      this.stats5 = itemView.findViewById(R.id.activity_ip_connections_stats_5);
      this.stats5PercentValue =
          (TextView) this.stats5.findViewById(R.id.activity_ip_connections_stats_5_percent);
      this.stats5Text =
          (TextView) this.stats5.findViewById(R.id.activity_ip_connections_stats_5_text);
      this.stats5ProgressBar =
          (ProgressBar) this.stats5.findViewById(R.id.activity_ip_connections_stats_5_progressbar);

      this.statsErrorView =
          (TextView) itemView.findViewById(R.id.activity_ip_connections_stats_error);

      this.stats6Other = itemView.findViewById(R.id.activity_ip_connections_stats_6_other);
      this.stats6OtherPercentValue =
          (TextView)
              this.stats6Other.findViewById(R.id.activity_ip_connections_stats_6_other_percent);
      this.stats6OtherText =
          (TextView) this.stats6Other.findViewById(R.id.activity_ip_connections_stats_6_other_text);
      this.stats6OtherProgressBar =
          (ProgressBar)
              this.stats6Other.findViewById(R.id.activity_ip_connections_stats_6_other_progressbar);
    }
  }

  public static final DecimalFormat PERCENTAGE_DECIMAL_FORMAT = new DecimalFormat("#.##");

  public static final int BY_SOURCE = 0;

  public static final int BY_PROTOCOL = 1;

  public static final int BY_DESTINATION_COUNTRY = 2;

  public static final int BY_DESTINATION_PORT = 3;

  public static final int BY_DESTINATION_ORG = 4;

  public static final int BY_DESTINATION_HOSTNAME = 5;

  public static final int BY_DESTINATION_IP = 6;

  public static final String SEPARATOR =
      ("__" + ActiveIPConnectionsStatsAdapter.class.getSimpleName() + "__");

  public static final Splitter SPLITTER = Splitter.on(SEPARATOR).omitEmptyStrings();

  private final ActiveIPConnectionsDetailActivity activity;

  private final boolean singleHost;

  private RowSortedTable<Integer, String, Integer> statsTable = TreeBasedTable.create();

  public ActiveIPConnectionsStatsAdapter(
      final ActiveIPConnectionsDetailActivity activity, final boolean singleHost) {
    this.activity = activity;
    this.singleHost = singleHost;
  }

  @Override
  public int getItemCount() {
    if (statsTable != null) {
      if (singleHost) {
        return Math.max(statsTable.rowKeySet().size() - 1, 0);
      } else {
        return statsTable.rowKeySet().size();
      }
    } else {
      return 0;
    }
  }

  public RowSortedTable<Integer, String, Integer> getStatsTable() {
    return statsTable;
  }

  public ActiveIPConnectionsStatsAdapter setStatsTable(
      RowSortedTable<Integer, String, Integer> statsTable) {
    this.statsTable = statsTable;
    return this;
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, final int position) {
    final int newPosition = position + (singleHost ? 1 : 0);
    final Map<String, Integer> statsAt =
        (newPosition >= 0 && newPosition < statsTable.rowKeySet().size())
            ? statsTable.row(newPosition)
            : null;
    switch (newPosition) {
      case BY_SOURCE:
        holder.title.setText("Source");
        break;
      case BY_DESTINATION_COUNTRY:
        holder.title.setText("Destination Country");
        break;
      case BY_PROTOCOL:
        holder.title.setText("Protocol");
        break;
      case BY_DESTINATION_IP:
        holder.title.setText("Destination IP");
        break;
      case BY_DESTINATION_PORT:
        holder.title.setText("Destination Port");
        break;
      case BY_DESTINATION_HOSTNAME:
        holder.title.setText("Destination Hostname");
        break;
      case BY_DESTINATION_ORG:
        holder.title.setText("Destination Organization");
        break;
      default:
        Toast.makeText(activity, "Internal Error", Toast.LENGTH_SHORT).show();
        break;
    }

    holder.statsLoadingView.setVisibility(View.GONE);
    holder.statsErrorView.setVisibility(View.GONE);

    if (statsAt == null) {
      holder.statsErrorView.setVisibility(View.VISIBLE);
      holder.statsErrorView.setText("No data!");
      return;
    }

    // Recompute with actual percentages
    double totalSize = 0;
    for (final Integer value : statsAt.values()) {
      totalSize += (value == null ? 0 : value);
    }
    if (totalSize == 0) {
      holder.statsErrorView.setVisibility(View.VISIBLE);
      holder.statsErrorView.setText("No data!");
      return;
    }

    final SortedSetMultimap<Double, String> percentages =
        TreeMultimap.create(Ordering.<Double>natural().reverse(), Ordering.<String>natural());
    for (final Map.Entry<String, Integer> statsAtEntry : statsAt.entrySet()) {
      percentages.put(100 * statsAtEntry.getValue() / totalSize, statsAtEntry.getKey());
    }
    // Now rank based upon percentage values
    final List<Integer> viewsSet = new ArrayList<>();
    int i = 0;
    double totalPercentagesSum = 0;
    labelToBreak:
    for (final Map.Entry<Double, Collection<String>> percentageEntry :
        percentages.asMap().entrySet()) {
      for (final String item : percentageEntry.getValue()) {
        i++;
        final List<String> itemComponents = SPLITTER.splitToList(item);
        if (itemComponents == null || itemComponents.isEmpty()) {
          continue;
        }
        final String host = itemComponents.get(0);
        final String itemTitle;
        final String itemTitleForToast;
        if (itemComponents.size() == 1) {
          itemTitle = Utils.truncateText(host, 20);
          itemTitleForToast = host;
        } else {
          final String ip = itemComponents.get(1);
          if (TextUtils.isEmpty(ip) || "-".equals(ip)) {
            itemTitle = Utils.truncateText(host, 20);
            itemTitleForToast = host;
          } else {
            itemTitle = String.format("%s\n(%s)", Utils.truncateText(host, 20), ip);
            itemTitleForToast = String.format("%s\n(%s)", host, ip);
          }
        }

        final View.OnClickListener clickListener =
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                Toast.makeText(activity, itemTitleForToast, Toast.LENGTH_SHORT).show();
              }
            };

        final Double percentage = percentageEntry.getKey();
        totalPercentagesSum += percentage;
        final String percentageValueText =
            ((Strings.nullToEmpty(itemTitle).contains("\n") ? "\n" : "")
                + PERCENTAGE_DECIMAL_FORMAT.format(percentage)
                + "%");
        final int percentageProgress = percentage.intValue();
        if (i == 1) {
          holder.stats1PercentValue.setText(percentageValueText);
          holder.stats1Text.setText(itemTitle);
          holder.stats1Text.setOnClickListener(clickListener);
          holder.stats1ProgressBar.setProgress(percentageProgress);
          viewsSet.add(i);
        } else if (i == 2) {
          holder.stats2PercentValue.setText(percentageValueText);
          holder.stats2Text.setText(itemTitle);
          holder.stats2Text.setOnClickListener(clickListener);
          holder.stats2ProgressBar.setProgress(percentageProgress);
          viewsSet.add(i);
        } else if (i == 3) {
          holder.stats3PercentValue.setText(percentageValueText);
          holder.stats3Text.setText(itemTitle);
          holder.stats3Text.setOnClickListener(clickListener);
          holder.stats3ProgressBar.setProgress(percentageProgress);
          viewsSet.add(i);
        } else if (i == 4) {
          holder.stats4PercentValue.setText(percentageValueText);
          holder.stats4Text.setText(itemTitle);
          holder.stats4Text.setOnClickListener(clickListener);
          holder.stats4ProgressBar.setProgress(percentageProgress);
          viewsSet.add(i);
        } else if (i == 5) {
          holder.stats5PercentValue.setText(percentageValueText);
          holder.stats5Text.setText(itemTitle);
          holder.stats5Text.setOnClickListener(clickListener);
          holder.stats5ProgressBar.setProgress(percentageProgress);
          viewsSet.add(i);
          break labelToBreak;
        }
      }
    }

    final Double otherPercentage = (100 - totalPercentagesSum);
    if (otherPercentage > 0) {
      holder.stats6Other.setVisibility(View.VISIBLE);
      final String otherPercentageFormat = PERCENTAGE_DECIMAL_FORMAT.format(otherPercentage);
      if ("0".equals(otherPercentageFormat)) {
        holder.stats6Other.setVisibility(View.GONE);
      } else {
        holder.stats6OtherPercentValue.setText(otherPercentageFormat + "%");
        holder.stats6OtherProgressBar.setProgress(otherPercentage.intValue());
        viewsSet.add(6);
      }
    } else {
      holder.stats6Other.setVisibility(View.GONE);
    }

    // Final pass to remove anything that is not set
    holder.stats1.setVisibility(viewsSet.contains(1) ? View.VISIBLE : View.GONE);
    holder.stats2.setVisibility(viewsSet.contains(2) ? View.VISIBLE : View.GONE);
    holder.stats3.setVisibility(viewsSet.contains(3) ? View.VISIBLE : View.GONE);
    holder.stats4.setVisibility(viewsSet.contains(4) ? View.VISIBLE : View.GONE);
    holder.stats5.setVisibility(viewsSet.contains(5) ? View.VISIBLE : View.GONE);
    holder.stats6Other.setVisibility(viewsSet.contains(6) ? View.VISIBLE : View.GONE);

    holder.shareImageButton.setImageDrawable(
        ContextCompat.getDrawable(
            activity,
            ColorUtils.Companion.isThemeLight(activity)
                ? R.drawable.ic_share_black_24dp
                : R.drawable.ic_share_white_24dp));

    holder.shareImageButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final File file =
                new File(
                    activity.getCacheDir(),
                    Utils.getEscapedFileName(
                            String.format("IP Connections Stats By %s", holder.title.getText()))
                        + ".png");
            holder.shareImageButton.setVisibility(View.GONE);
            ViewGroupUtils.exportViewToFile(activity, holder.itemView, file);
            holder.shareImageButton.setVisibility(View.VISIBLE);
            final Uri uriForFile =
                FileProvider.getUriForFile(
                    activity, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, file);
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            sendIntent.setType("text/html");
            sendIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                String.format("IP Connections Stats By %s", holder.title.getText()));

            sendIntent.setData(uriForFile);
            //        sendIntent.setType("image/png");
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(Intent.createChooser(sendIntent, "Share"));
          }
        });
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.activity_ip_connections_stats_cardview, parent, false);
    final CardView cardView =
        (CardView) v.findViewById(R.id.activity_ip_connections_stats_card_view);
    if (ColorUtils.Companion.isThemeLight(activity)) {
      // Light
      cardView.setCardBackgroundColor(
          ContextCompat.getColor(activity, R.color.cardview_light_background));
    } else {
      // Default is Dark
      cardView.setCardBackgroundColor(
          ContextCompat.getColor(activity, R.color.cardview_dark_background));
    }

    //        return new ViewHolder(this.context,
    //                RippleViewCreator.addRippleToView(v));
    return new ViewHolder(this.activity, v);
  }
}
