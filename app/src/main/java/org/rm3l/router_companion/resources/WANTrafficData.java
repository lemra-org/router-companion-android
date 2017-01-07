package org.rm3l.router_companion.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;

import static org.rm3l.router_companion.RouterCompanionAppConstants.WAN_CYCLE_DAY_PREF;

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

    public static MonthlyCycleItem getCurrentWANCycle(@Nullable final Context ctx,
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

        calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay);
        start = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DATE, -1);
        end = calendar.getTimeInMillis();

        final MonthlyCycleItem cycleItem =
                new MonthlyCycleItem(ctx, start, end);

        return (today < wanCycleDay ?
                cycleItem.prev() :
                cycleItem);
    }

}
