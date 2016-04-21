package org.rm3l.ddwrt.tiles.status.wireless.stats;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultimap;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.tiles.status.wireless.ActiveIPConnectionsDetailActivity;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

/**
 * Created by rm3l on 12/04/16.
 */
public class ActiveIPConnectionsStatsAdapter extends Adapter<ActiveIPConnectionsStatsAdapter.ViewHolder> {

    private final ActiveIPConnectionsDetailActivity activity;

    public static final int BY_SOURCE = 0;
    public static final int BY_PROTOCOL = 1;
    public static final int BY_DESTINATION_COUNTRY = 2;
    public static final int BY_DESTINATION = 3;
    public static final int BY_DESTINATION_PORT = 4;

    private RowSortedTable<Integer, String, Integer> statsTable = TreeBasedTable.create();
    private final boolean singleHost;

    public ActiveIPConnectionsStatsAdapter(final ActiveIPConnectionsDetailActivity activity,
                                           final boolean singleHost) {
        this.activity = activity;
        this.singleHost = singleHost;
    }

    public RowSortedTable<Integer, String, Integer> getStatsTable() {
        return statsTable;
    }

    public ActiveIPConnectionsStatsAdapter setStatsTable(RowSortedTable<Integer, String, Integer> statsTable) {
        this.statsTable = statsTable;
        return this;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_ip_connections_stats_cardview, parent, false);
        final long currentTheme = activity
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .getLong(THEMING_PREF, DDWRTCompanionConstants.DEFAULT_THEME);
        final CardView cardView = (CardView)
                v.findViewById(R.id.activity_ip_connections_stats_card_view);
        if (currentTheme == ColorUtils.LIGHT_THEME) {
            //Light
            cardView.setCardBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_light_background));
        } else {
            //Default is Dark
            cardView.setCardBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_dark_background));
        }

//        return new ViewHolder(this.context,
//                RippleViewCreator.addRippleToView(v));
        return new ViewHolder(this.activity, v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final int newPosition = position + (singleHost ? 1 : 0);
        final Map<String, Integer> statsAt = (newPosition >= 0 && newPosition < statsTable.rowKeySet().size()) ?
                statsTable.row(newPosition) : null;
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
            case BY_DESTINATION:
                holder.title.setText("Destination");
                break;
            case BY_DESTINATION_PORT:
                holder.title.setText("Destination Port");
                break;
            default:
                Toast.makeText(activity, "Internal Error", Toast.LENGTH_SHORT).show();
                break;
        }

        holder.statsLoadingView.setVisibility(View.GONE);
        holder.statsErrorView.setVisibility(View.GONE);

        if (statsAt == null) {
            holder.statsErrorView.setVisibility(View.VISIBLE);
            holder.statsErrorView.setText("No stats!");
            return;
        }

        //Recompute with actual percentages
        int totalSize = 0;
        for (final Integer value : statsAt.values()) {
            totalSize += (value == null ? 0 : value);
        }
        if (totalSize == 0) {
            holder.statsErrorView.setVisibility(View.VISIBLE);
            holder.statsErrorView.setText("E500. Internal Error. Please try again later.");
            return;
        }
        final SortedSetMultimap<Integer, String> percentages = TreeMultimap.create(
                Ordering.<Integer>natural().reverse(), Ordering.<String>natural());
        for (final Map.Entry<String, Integer> statsAtEntry : statsAt.entrySet()) {
            percentages.put(100 * statsAtEntry.getValue() / totalSize, statsAtEntry.getKey());
        }
        //Now rank based upon percentage values
        final List<Integer> viewsSet = new ArrayList<>();
        int i = 0;
        int totalPercentagesSum = 0;
        for (final Map.Entry<Integer, Collection<String>> percentageEntry : percentages.asMap().entrySet()) {
            for (final String item : percentageEntry.getValue()) {
                i++;
                final Integer percentage = percentageEntry.getKey();
                totalPercentagesSum += percentage;
                final String percentageValueText =
                        ((Strings.nullToEmpty(item).contains("\n") ? "\n" : "") + percentage + "%");
                if (i == 1) {
                    holder.stats1PercentValue.setText(percentageValueText);
                    holder.stats1Text.setText(item);
                    holder.stats1ProgressBar.setProgress(percentage);
                    viewsSet.add(i);
                } else if (i == 2) {
                    holder.stats2PercentValue.setText(percentageValueText);
                    holder.stats2Text.setText(item);
                    holder.stats2ProgressBar.setProgress(percentage);
                    viewsSet.add(i);
                } else if (i == 3) {
                    holder.stats3PercentValue.setText(percentageValueText);
                    holder.stats3Text.setText(item);
                    holder.stats3ProgressBar.setProgress(percentage);
                    viewsSet.add(i);
                } else if (i == 4) {
                    holder.stats4PercentValue.setText(percentageValueText);
                    holder.stats4Text.setText(item);
                    holder.stats4ProgressBar.setProgress(percentage);
                    viewsSet.add(i);
                } else if (i == 5) {
                    holder.stats5PercentValue.setText(percentageValueText);
                    holder.stats5Text.setText(item);
                    holder.stats5ProgressBar.setProgress(percentage);
                    viewsSet.add(i);
                }
            }
        }

        if (totalPercentagesSum < 100) {
            final int otherPercentage = (100 - totalPercentagesSum);
            holder.stats6Other.setVisibility(View.VISIBLE);
            holder.stats5PercentValue.setText(otherPercentage + "%");
            holder.stats5ProgressBar.setProgress(otherPercentage);
            viewsSet.add(6);
        } else {
            holder.stats6Other.setVisibility(View.GONE);
        }

        //Final pass to remove anything that is not set
        holder.stats1.setVisibility(viewsSet.contains(1) ? View.VISIBLE : View.GONE);
        holder.stats2.setVisibility(viewsSet.contains(2) ? View.VISIBLE : View.GONE);
        holder.stats3.setVisibility(viewsSet.contains(3) ? View.VISIBLE : View.GONE);
        holder.stats4.setVisibility(viewsSet.contains(4) ? View.VISIBLE : View.GONE);
        holder.stats5.setVisibility(viewsSet.contains(5) ? View.VISIBLE : View.GONE);
        holder.stats6Other.setVisibility(viewsSet.contains(6) ? View.VISIBLE : View.GONE);
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

    class ViewHolder extends RecyclerView.ViewHolder {

        private final Context mContext;
        final View mItemView;

        private TextView title;

        final View stats1;
        final TextView stats1PercentValue;
        final TextView stats1Text;
        final ProgressBar stats1ProgressBar;

        final View stats2;
        final TextView stats2PercentValue;
        final TextView stats2Text;
        final ProgressBar stats2ProgressBar;

        final View stats3;
        final TextView stats3PercentValue;
        final TextView stats3Text;
        final ProgressBar stats3ProgressBar;

        final View stats4;
        final TextView stats4PercentValue;
        final TextView stats4Text;
        final ProgressBar stats4ProgressBar;

        final View stats5;
        final TextView stats5PercentValue;
        final TextView stats5Text;
        final ProgressBar stats5ProgressBar;

        final View stats6Other;
        final TextView stats6OtherPercentValue;
        final TextView stats6OtherText;
        final ProgressBar stats6OtherProgressBar;

        final ProgressBar statsLoadingView;
        final TextView statsErrorView;

        public ViewHolder(final Context context, View itemView) {
            super(itemView);
            this.mContext = context;
            this.mItemView = itemView;

            this.statsLoadingView = (ProgressBar)
                    itemView.findViewById(R.id.activity_ip_connections_stats_loading_view);

            this.title = (TextView) itemView.findViewById(R.id.activity_ip_connections_stats_title);

            this.stats1 = itemView.findViewById(R.id.activity_ip_connections_stats_1);
            this.stats1PercentValue = (TextView)
                    this.stats1.findViewById(R.id.activity_ip_connections_stats_1_percent);
            this.stats1Text = (TextView)
                    this.stats1.findViewById(R.id.activity_ip_connections_stats_1_text);
            this.stats1ProgressBar = (ProgressBar)
                    this.stats1.findViewById(R.id.activity_ip_connections_stats_1_progressbar);

            this.stats2 = itemView.findViewById(R.id.activity_ip_connections_stats_2);
            this.stats2PercentValue = (TextView)
                    this.stats2.findViewById(R.id.activity_ip_connections_stats_2_percent);
            this.stats2Text = (TextView)
                    this.stats2.findViewById(R.id.activity_ip_connections_stats_2_text);
            this.stats2ProgressBar = (ProgressBar)
                    this.stats2.findViewById(R.id.activity_ip_connections_stats_2_progressbar);

            this.stats3 = itemView.findViewById(R.id.activity_ip_connections_stats_3);
            this.stats3PercentValue = (TextView)
                    this.stats3.findViewById(R.id.activity_ip_connections_stats_3_percent);
            this.stats3Text = (TextView)
                    this.stats3.findViewById(R.id.activity_ip_connections_stats_3_text);
            this.stats3ProgressBar = (ProgressBar)
                    this.stats3.findViewById(R.id.activity_ip_connections_stats_3_progressbar);

            this.stats4 = itemView.findViewById(R.id.activity_ip_connections_stats_4);
            this.stats4PercentValue = (TextView)
                    this.stats4.findViewById(R.id.activity_ip_connections_stats_4_percent);
            this.stats4Text = (TextView)
                    this.stats4.findViewById(R.id.activity_ip_connections_stats_4_text);
            this.stats4ProgressBar = (ProgressBar)
                    this.stats4.findViewById(R.id.activity_ip_connections_stats_4_progressbar);

            this.stats5 = itemView.findViewById(R.id.activity_ip_connections_stats_5);
            this.stats5PercentValue = (TextView)
                    this.stats5.findViewById(R.id.activity_ip_connections_stats_5_percent);
            this.stats5Text = (TextView)
                    this.stats5.findViewById(R.id.activity_ip_connections_stats_5_text);
            this.stats5ProgressBar = (ProgressBar)
                    this.stats5.findViewById(R.id.activity_ip_connections_stats_5_progressbar);

            this.statsErrorView = (TextView)
                    itemView.findViewById(R.id.activity_ip_connections_stats_error);

            this.stats6Other = itemView.findViewById(R.id.activity_ip_connections_stats_6_other);
            this.stats6OtherPercentValue = (TextView)
                    this.stats6Other.findViewById(R.id.activity_ip_connections_stats_6_other_percent);
            this.stats6OtherText = (TextView)
                    this.stats6Other.findViewById(R.id.activity_ip_connections_stats_6_other_text);
            this.stats6OtherProgressBar = (ProgressBar)
                    this.stats6Other.findViewById(R.id.activity_ip_connections_stats_6_other_progressbar);
        }
    }

}
