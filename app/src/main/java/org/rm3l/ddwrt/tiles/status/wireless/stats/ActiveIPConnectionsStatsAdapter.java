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

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.tiles.status.wireless.ActiveIPConnectionsDetailActivity;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.util.HashMap;
import java.util.Map;

import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.THEMING_PREF;

/**
 * Created by rm3l on 12/04/16.
 */
public class ActiveIPConnectionsStatsAdapter extends Adapter<ActiveIPConnectionsStatsAdapter.ViewHolder> {

    private final ActiveIPConnectionsDetailActivity activity;

    public static final int BY_SOURCE = 0;
    public static final int BY_DESTINATION_IP = 1;
    public static final int BY_DESTINATION_IP_COUNTRY = 2;

    private Map<Integer, Object> items = new HashMap<>();

    public ActiveIPConnectionsStatsAdapter(final ActiveIPConnectionsDetailActivity activity) {
        this.activity = activity;
    }

    public Map<Integer, Object> getItems() {
        return items;
    }

    public ActiveIPConnectionsStatsAdapter setItems(Map<Integer, Object> items) {
        this.items = items;
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
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Object itemsAt = items.get(position);
        switch (position) {
            case BY_SOURCE:
                holder.title.setText("Source");
                break;
            case BY_DESTINATION_IP:
                holder.title.setText("Destination IP");
                break;
            case BY_DESTINATION_IP_COUNTRY:
                holder.title.setText("Destination Country");
                break;
            default:
                Toast.makeText(activity, "Internal Error", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public int getItemCount() {
        return (items != null ? items.keySet().size() : 0);
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
