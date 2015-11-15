package org.rm3l.ddwrt.resources;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.format.DateUtils;

import com.google.common.primitives.Longs;

import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;

/**
 * Created by rm3l on 15/11/15.
 */
public class MonthlyCycleItem implements Comparable<MonthlyCycleItem> {

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.US);

    private transient Context context;

    private String label;

    private String labelWithYears;

    private long start;

    private long end;

    public MonthlyCycleItem() {}

    MonthlyCycleItem(String label) {
        this.label = label;
        this.labelWithYears = label;
    }

    public MonthlyCycleItem(Context context, long start, long end) {
        this.context = context;
        this.label = formatDateRange(context, FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH,
                start, end);
        this.labelWithYears = formatDateRange(context, FORMAT_SHOW_YEAR | FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH,
                start, end);
        this.start = start;
        this.end = end;
    }

    public Context getContext() {
        return context;
    }

    public MonthlyCycleItem setContext(Context context) {
        this.context = context;
        return this;
    }

    public CharSequence getLabelWithYears() {
        return labelWithYears;
    }

    public MonthlyCycleItem setLabelWithYears(String labelWithYears) {
        this.labelWithYears = labelWithYears;
        return this;
    }

    public CharSequence getLabel() {
        return label;
    }

    public void setLabel(String label) {
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
        if (o instanceof MonthlyCycleItem) {
            final MonthlyCycleItem another = (MonthlyCycleItem) o;
            return start == another.start && end == another.end;
        }
        return false;
    }

    @Override
    public int compareTo(@NonNull MonthlyCycleItem another) {
        return Longs.compare(start, another.start);
    }

    public MonthlyCycleItem prev() {
        final Pair<Long, Long> pair = slideCycleBy(Calendar.MONTH, -1);
        return new MonthlyCycleItem(context, pair.first, pair.second);
    }

    public MonthlyCycleItem next() {
        final Pair<Long, Long> pair = slideCycleBy(Calendar.MONTH, 1);
        return new MonthlyCycleItem(context, pair.first, pair.second);
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

    public static String formatDateRange(Context context, int flags, long start, long end) {
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }
}
