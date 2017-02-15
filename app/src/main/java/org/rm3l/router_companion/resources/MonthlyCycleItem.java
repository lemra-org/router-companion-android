package org.rm3l.router_companion.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.common.primitives.Longs;

import org.rm3l.router_companion.utils.Utils;

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
    private transient SharedPreferences routerPreferences;
    private transient Integer wanCycleDay;

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

    public SharedPreferences getRouterPreferences() {
        return routerPreferences;
    }

    public MonthlyCycleItem setRouterPreferences(SharedPreferences routerPreferences) {
        this.routerPreferences = routerPreferences;
        if (routerPreferences != null) {
            wanCycleDay = Utils.getWanCycleDay(routerPreferences);
        } else {
            wanCycleDay = null;
        }
        return this;
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
        return label;
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

        //[Feb 28 - Mar 30], with wanCycleDay=30 => [Jan 30 - Feb 28]

//        if (wanCycleDay == null) {
//            Toast.makeText(context, "Internal Error - issue will be reported", Toast.LENGTH_SHORT).show();
//            Crashlytics.logException(new IllegalStateException("wanCycleDay == NULL"));
//            return this;
//        }

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);

        calendar.add(Calendar.MONTH, -1);
        final int prevMonthActualMaximum = calendar
                .getActualMaximum(Calendar.DAY_OF_MONTH);
        if (wanCycleDay > prevMonthActualMaximum) {
            //Start the day after
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
        }
        long startMillis = calendar.getTimeInMillis();

        //end of prev is the day before current start
        final Calendar calendarForEnd = Calendar.getInstance();
        calendarForEnd.setTimeInMillis(start);
        calendarForEnd.add(Calendar.DATE, -1);
        final long endMillis = calendarForEnd.getTimeInMillis();
        if (wanCycleDay > prevMonthActualMaximum &&
                calendar.get(Calendar.MONTH) == calendarForEnd.get(Calendar.MONTH)) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
            startMillis = calendar.getTimeInMillis();
        }

        return new MonthlyCycleItem(context, startMillis, endMillis)
                .setRouterPreferences(routerPreferences);
    }

    public MonthlyCycleItem next() {
//        if (wanCycleDay == null) {
//            Toast.makeText(context, "Internal Error - issue will be reported", Toast.LENGTH_SHORT).show();
//            Crashlytics.logException(new IllegalStateException("wanCycleDay == NULL"));
//            return this;
//        }

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);

        calendar.add(Calendar.MONTH, 1);
        final int nextMonthActualMaximum = calendar
                .getActualMaximum(Calendar.DAY_OF_MONTH);
        if (wanCycleDay > nextMonthActualMaximum) {
            //Start the day after
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
        }
        final long startMillis = calendar.getTimeInMillis();

        final Calendar calendarForEnd = Calendar.getInstance();
        calendarForEnd.setTimeInMillis(startMillis);
        final int monthActualMaximum = calendarForEnd
                .getActualMaximum(Calendar.DAY_OF_MONTH);
        if (wanCycleDay - 1 > monthActualMaximum) {
            calendarForEnd.set(Calendar.DAY_OF_MONTH, monthActualMaximum);
        } else {
            calendarForEnd.set(Calendar.DAY_OF_MONTH, wanCycleDay - 1);
        }
        final long endMillis = calendarForEnd.getTimeInMillis();

        return new MonthlyCycleItem(context, startMillis, endMillis)
                .setRouterPreferences(routerPreferences);
    }

    public static String formatDateRange(Context context, int flags, long start, long end) {
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }
}
