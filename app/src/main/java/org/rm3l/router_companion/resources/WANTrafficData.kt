package org.rm3l.router_companion.resources

import android.content.Context
import android.content.SharedPreferences
import org.rm3l.router_companion.utils.Utils
import java.util.Calendar

data class WANTrafficData @JvmOverloads constructor(
    val id: Long = -1L, // the internal id (in DB)
    val router: String, // YYYY-MM-dd
    val date: String,
    val traffIn: Number,
    val traffOut: Number
) {

    companion object {

        const val INBOUND = 0
        const val OUTBOUND = 1

        @JvmStatic
        fun getCurrentWANCycle(
            ctx: Context?,
            routerPreferences: SharedPreferences?
        ): MonthlyCycleItem {

            val wanCycleDay = Utils.getWanCycleDay(routerPreferences)

            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_MONTH)

            val start: Long
            val end: Long

            if (today < wanCycleDay) {
                // Example 1: wanCycleDay=30, and today is Feb 18 (and Feb has 28 days) => [Jan 30 - Feb 28]
                // Example 2: wanCycleDay=30, and today is May 15 => [Apr 30 - May 29]
                // Example 3: wanCycleDay=30, and today is Mar 18 (and Feb has 28 days) => [Feb 28 - Mar 29]

                // Start
                val calendarForStartComputation = Calendar.getInstance()
                calendarForStartComputation.add(Calendar.MONTH, -1)
                val prevMonthActualMaximum = calendarForStartComputation.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
                if (wanCycleDay > prevMonthActualMaximum) {
                    calendarForStartComputation.set(Calendar.DAY_OF_MONTH, prevMonthActualMaximum)
                } else {
                    calendarForStartComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay)
                }
                start = calendarForStartComputation.timeInMillis

                // End
                val calendarForEndComputation = Calendar.getInstance()
                val currentMonthActualMaximum = calendarForEndComputation.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
                if (wanCycleDay - 1 > currentMonthActualMaximum) {
                    calendarForEndComputation.set(Calendar.DAY_OF_MONTH, currentMonthActualMaximum)
                } else {
                    calendarForEndComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay - 1)
                }
                end = calendarForEndComputation.timeInMillis
            } else {
                // Example 1: wanCycleDay=27, and today is Feb 29 (and Feb has 29 days) => [Feb 27 - Mar 26]
                // Example 2: wanCycleDay=27, and today is May 31 => [May 27 - Jun 26]

                // Start
                val calendarForStartComputation = Calendar.getInstance()
                val currentMonthActualMaximum = calendarForStartComputation.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
                if (wanCycleDay > currentMonthActualMaximum) {
                    calendarForStartComputation.set(Calendar.DAY_OF_MONTH, currentMonthActualMaximum)
                } else {
                    calendarForStartComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay)
                }
                start = calendarForStartComputation.timeInMillis

                // End
                val calendarForEndComputation = Calendar.getInstance()
                calendarForEndComputation.add(Calendar.MONTH, 1)
                val nextMonthActualMaximum = calendarForEndComputation.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
                if (wanCycleDay - 1 > nextMonthActualMaximum) {
                    calendarForEndComputation.set(Calendar.DAY_OF_MONTH, nextMonthActualMaximum)
                } else {
                    calendarForEndComputation.set(Calendar.DAY_OF_MONTH, wanCycleDay - 1)
                }
                end = calendarForEndComputation.timeInMillis
            }

            return MonthlyCycleItem(ctx, start, end).setRouterPreferences(routerPreferences)
        }
    }
}
