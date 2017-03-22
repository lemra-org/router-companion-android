package org.rm3l.router_companion.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Calendar;
import org.rm3l.router_companion.utils.Utils;

/**
 * Created by rm3l on 11/11/15.
 */
public class WANTrafficData {

  @NonNull private final String router;
  @NonNull
  //YYYY-MM-dd
  private final String date;
  @NonNull private final Number traffIn;
  @NonNull private final Number traffOut;
  /**
   * the internal id (in DB)
   */
  private long id = -1l;

  public WANTrafficData(@NonNull final String router, @NonNull final String date,
      @NonNull final Number traffIn, @NonNull final Number traffOut) {

    this.router = router;
    this.date = date;
    this.traffIn = traffIn;
    this.traffOut = traffOut;
  }

  public static MonthlyCycleItem getCurrentWANCycle(@Nullable final Context ctx,
      @Nullable final SharedPreferences routerPreferences) {

    final int wanCycleDay = Utils.getWanCycleDay(routerPreferences);

    final Calendar calendar = Calendar.getInstance();
    final int today = calendar.get(Calendar.DAY_OF_MONTH);

    final long start;
    final long end;

    if (today < wanCycleDay) {
      //Example 1: wanCycleDay=30, and today is Feb 18 (and Feb has 28 days) => [Jan 30 - Feb 28]
      //Example 2: wanCycleDay=30, and today is May 15 => [Apr 30 - May 29]
      //Example 3: wanCycleDay=30, and today is Mar 18 (and Feb has 28 days) => [Feb 28 - Mar 29]

      //Start
      final Calendar calendarForStartComputation = Calendar.getInstance();
      calendarForStartComputation.add(Calendar.MONTH, -1);
      final int prevMonthActualMaximum =
          calendarForStartComputation.getActualMaximum(Calendar.DAY_OF_MONTH);
      if (wanCycleDay > prevMonthActualMaximum) {
        calendarForStartComputation.set(Calendar.DAY_OF_MONTH, prevMonthActualMaximum);
      } else {
        calendarForStartComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay);
      }
      start = calendarForStartComputation.getTimeInMillis();

      //End
      final Calendar calendarForEndComputation = Calendar.getInstance();
      final int currentMonthActualMaximum =
          calendarForEndComputation.getActualMaximum(Calendar.DAY_OF_MONTH);
      if (wanCycleDay - 1 > currentMonthActualMaximum) {
        calendarForEndComputation.set(Calendar.DAY_OF_MONTH, currentMonthActualMaximum);
      } else {
        calendarForEndComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay - 1);
      }
      end = calendarForEndComputation.getTimeInMillis();
    } else {
      //Example 1: wanCycleDay=27, and today is Feb 29 (and Feb has 29 days) => [Feb 27 - Mar 26]
      //Example 2: wanCycleDay=27, and today is May 31 => [May 27 - Jun 26]

      //Start
      final Calendar calendarForStartComputation = Calendar.getInstance();
      final int currentMonthActualMaximum =
          calendarForStartComputation.getActualMaximum(Calendar.DAY_OF_MONTH);
      if (wanCycleDay > currentMonthActualMaximum) {
        calendarForStartComputation.set(Calendar.DAY_OF_MONTH, currentMonthActualMaximum);
      } else {
        calendarForStartComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay);
      }
      start = calendarForStartComputation.getTimeInMillis();

      //End
      final Calendar calendarForEndComputation = Calendar.getInstance();
      calendarForEndComputation.add(Calendar.MONTH, 1);
      final int nextMonthActualMaximum =
          calendarForEndComputation.getActualMaximum(Calendar.DAY_OF_MONTH);
      if (wanCycleDay - 1 > nextMonthActualMaximum) {
        calendarForEndComputation.set(Calendar.DAY_OF_MONTH, nextMonthActualMaximum);
      } else {
        calendarForEndComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay - 1);
      }
      end = calendarForEndComputation.getTimeInMillis();
    }

    return new MonthlyCycleItem(ctx, start, end).setRouterPreferences(routerPreferences);
  }

  @NonNull public String getRouter() {
    return router;
  }

  @NonNull public String getDate() {
    return date;
  }

  @NonNull public Number getTraffIn() {
    return traffIn;
  }

  @NonNull public Number getTraffOut() {
    return traffOut;
  }

  public long getId() {
    return id;
  }

  public WANTrafficData setId(long id) {
    this.id = id;
    return this;
  }
}
