package org.rm3l.ddwrt.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.format.DateUtils;

import com.google.common.primitives.Longs;

import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.WAN_CYCLE_DAY_PREF;

/**
 * Created by rm3l on 11/11/15.
 */
public class WANTrafficData {

    /**
     * the internal id (in DB)
     */
    private long id = -1l;

    @NonNull
    private final String router;

    @NonNull
    //YYYY-MM-dd
    private final String date;

    @NonNull
    private final Number traffIn;

    @NonNull
    private final Number traffOut;

    public WANTrafficData(@NonNull final String router,
                          @NonNull final String date,
                          @NonNull final Number traffIn,
                          @NonNull final Number traffOut) {

        this.router = router;
        this.date = date;
        this.traffIn = traffIn;
        this.traffOut = traffOut;
    }

    @NonNull
    public String getRouter() {
        return router;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    @NonNull
    public Number getTraffIn() {
        return traffIn;
    }

    @NonNull
    public Number getTraffOut() {
        return traffOut;
    }

    public long getId() {
        return id;
    }

    public WANTrafficData setId(long id) {
        this.id = id;
        return this;
    }

    public static CycleItem getCurrentWANCycle(@Nullable final Context ctx,
                                               @Nullable final SharedPreferences routerPreferences) {
        final int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        final int wanCycleDay;
        if (routerPreferences != null) {
            final int cycleDay = routerPreferences.getInt(WAN_CYCLE_DAY_PREF, 1);
            wanCycleDay = (cycleDay < 1 ? 1 : (cycleDay > 31 ? 31 : cycleDay));
        } else {
            wanCycleDay = 1;
        }
        final Calendar calendar = Calendar.getInstance();
        final long start;
        final long end;
        if (today < wanCycleDay) {
            //Effective Period: [wanCycleDay-1M, wanCycleDay]
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
            end = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, -1);
            calendar.add(Calendar.DATE, 1);
            start = calendar.getTimeInMillis();
        } else {
            //Effective Period: [wanCycleDay, wanCycleDay + 1M]
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
            start = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DATE, -1);
            end = calendar.getTimeInMillis();
        }
        return new CycleItem(ctx, start, end);
    }

    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem implements Comparable<CycleItem> {
        private Context context;
        private CharSequence label;
        private CharSequence labelWithYears;
        private long start;
        private long end;

        CycleItem(CharSequence label) {
            this.label = label;
            this.labelWithYears = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.context = context;
            this.label = formatDateRange(context, FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH,
                    start, end);
            this.labelWithYears = formatDateRange(context, FORMAT_SHOW_YEAR | FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH,
                    start, end);
            this.start = start;
            this.end = end;
        }

        public CharSequence getLabelWithYears() {
            return labelWithYears;
        }

        public CycleItem setLabelWithYears(CharSequence labelWithYears) {
            this.labelWithYears = labelWithYears;
            return this;
        }

        public CharSequence getLabel() {
            return label;
        }

        public void setLabel(CharSequence label) {
            this.label = label;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        @Override
        public String toString() {
            return label.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CycleItem) {
                final CycleItem another = (CycleItem) o;
                return start == another.start && end == another.end;
            }
            return false;
        }

        @Override
        public int compareTo(@NonNull CycleItem another) {
            return Longs.compare(start, another.start);
        }

        public CycleItem prev() {
            final Pair<Long, Long> pair = slideCycleBy(Calendar.MONTH, -1);
            return new CycleItem(context, pair.first, pair.second);
        }

        public CycleItem next() {
            final Pair<Long, Long> pair = slideCycleBy(Calendar.MONTH, 1);
            return new CycleItem(context, pair.first, pair.second);
        }

        @NonNull
        public Pair<Long, Long> slideCycleBy(final int field, final int interval) {
            final Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(start);
            startCal.add(field, interval);

            final Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(end);
            endCal.add(field, interval);

            return Pair.create(startCal.getTimeInMillis(), endCal.getTimeInMillis());
        }

    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.US);

    public static String formatDateRange(Context context, int flags, long start, long end) {
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }
}
