package org.rm3l.router_companion.resources

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_MONTH
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_YEAR
import com.google.common.primitives.Longs
import org.rm3l.router_companion.utils.Utils
import java.util.Calendar
import java.util.Locale

/**
 * Created by rm3l on 15/11/15.
 */
class MonthlyCycleItem : Comparable<MonthlyCycleItem> {

    @Transient private var context: Context? = null
    @Transient private var routerPreferences: SharedPreferences? = null
    @Transient private var wanCycleDay: Int? = null

    private var label: String? = null

    private var labelWithYears: String? = null

    var start: Long = 0

    var end: Long = 0

    constructor()

    internal constructor(label: String) {
        this.label = label
        this.labelWithYears = label
    }

    constructor(context: Context?, start: Long, end: Long) {
        this.context = context
        this.label = formatDateRange(context, FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH, start, end)
        this.labelWithYears = formatDateRange(context,
                FORMAT_SHOW_YEAR or FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH, start,
                end)
        this.start = start
        this.end = end
    }

    fun getRouterPreferences(): SharedPreferences? {
        return routerPreferences
    }

    fun setRouterPreferences(routerPreferences: SharedPreferences?): MonthlyCycleItem {
        this.routerPreferences = routerPreferences
        if (routerPreferences != null) {
            wanCycleDay = Utils.getWanCycleDay(routerPreferences)
        } else {
            wanCycleDay = null
        }
        return this
    }

    fun getContext(): Context? {
        return context
    }

    fun setContext(context: Context?): MonthlyCycleItem {
        this.context = context
        return this
    }

    fun getLabelWithYears(): CharSequence? {
        return labelWithYears
    }

    fun setLabelWithYears(labelWithYears: String): MonthlyCycleItem {
        this.labelWithYears = labelWithYears
        return this
    }

    fun getLabel(): CharSequence? {
        return label
    }

    fun setLabel(label: String?) {
        this.label = label
    }

    override fun toString(): String {
        return label ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (other is MonthlyCycleItem) {
            val another = other
            return start == another.start && end == another.end
        }
        return false
    }

    override fun compareTo(other: MonthlyCycleItem): Int {
        return Longs.compare(start, other.start)
    }

    fun prev(): MonthlyCycleItem {

        //[Feb 28 - Mar 30], with wanCycleDay=30 => [Jan 30 - Feb 28]

        //        if (wanCycleDay == null) {
        //            Toast.makeText(context, "Internal Error - issue will be reported", Toast.LENGTH_SHORT).show();
        //            Crashlytics.logException(new IllegalStateException("wanCycleDay == NULL"));
        //            return this;
        //        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = start

        calendar.add(Calendar.DATE, -31)
        val prevMonthActualMaximum = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (wanCycleDay!! > prevMonthActualMaximum) {
            //Start the day after
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay!!)
        }
        var startMillis = calendar.timeInMillis

        //end of prev is the day before current start
        val calendarForEnd = Calendar.getInstance()
        calendarForEnd.timeInMillis = start
        calendarForEnd.add(Calendar.DATE, -1)
        val endMillis = calendarForEnd.timeInMillis
        if (wanCycleDay!! > prevMonthActualMaximum && calendar.get(Calendar.MONTH) == calendarForEnd.get(
                Calendar.MONTH)) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
            startMillis = calendar.timeInMillis
        }

        return MonthlyCycleItem(context, startMillis, endMillis).setRouterPreferences(
                routerPreferences)
    }

    operator fun next(): MonthlyCycleItem {
        //        if (wanCycleDay == null) {
        //            Toast.makeText(context, "Internal Error - issue will be reported", Toast.LENGTH_SHORT).show();
        //            Crashlytics.logException(new IllegalStateException("wanCycleDay == NULL"));
        //            return this;
        //        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = end
        calendar.add(Calendar.DATE, 1)
        val startMillis = calendar.timeInMillis

        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = startMillis
        cal1.add(Calendar.MONTH, 1)
        cal1.add(Calendar.DAY_OF_MONTH, -1)
        val month1 = cal1.get(Calendar.MONTH)

        calendar.add(Calendar.DATE, 30)
        val month = calendar.get(Calendar.MONTH)
        if (month1 != month) {
            calendar.set(Calendar.MONTH, month1)
            calendar.set(Calendar.DAY_OF_MONTH, cal1.getActualMaximum(Calendar.DAY_OF_MONTH))
        } else {
            val dayAfter30d = calendar.get(Calendar.DAY_OF_MONTH)
            if (dayAfter30d >= wanCycleDay!! - 1) {
                calendar.set(Calendar.DAY_OF_MONTH, wanCycleDay!! - 1)
            }
        }
        val endMillis = calendar.timeInMillis

        return MonthlyCycleItem(context, startMillis, endMillis).setRouterPreferences(
                routerPreferences)
    }

    companion object {

        private val sBuilder = StringBuilder(50)
        private val sFormatter = java.util.Formatter(sBuilder, Locale.US)

        fun formatDateRange(context: Context?, flags: Int, start: Long, end: Long): String {
            synchronized(sBuilder) {
                sBuilder.setLength(0)
                return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null).toString()
            }
        }
    }
}
