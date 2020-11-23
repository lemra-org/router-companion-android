package org.rm3l.router_companion.resources

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_MONTH
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_YEAR
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
        this.labelWithYears = formatDateRange(
            context,
            FORMAT_SHOW_YEAR or FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH, start,
            end
        )
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

    fun getContext() = context

    fun setContext(context: Context?): MonthlyCycleItem {
        this.context = context
        return this
    }

    fun getLabelWithYears() = labelWithYears

    fun setLabelWithYears(labelWithYears: String?): MonthlyCycleItem {
        this.labelWithYears = labelWithYears
        return this
    }

    fun refreshLabelWithYears(): String? {
        this.setLabelWithYears(
            formatDateRange(
                context,
                FORMAT_SHOW_YEAR or FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH, start,
                end
            )
        )
        return this.labelWithYears
    }

    fun getLabel() = label

    fun setLabel(label: String?): MonthlyCycleItem {
        this.label = label
        return this
    }

    override fun toString(): String {
        return label ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (other is MonthlyCycleItem) {
            return start == other.start && end == other.end
        }
        return false
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

    override fun compareTo(other: MonthlyCycleItem) = start.compareTo(other.start)

    fun prev(): MonthlyCycleItem {

        // [Feb 28 - Mar 30], with wanCycleDay=30 => [Jan 30 - Feb 28]

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = start
        endCal.add(Calendar.DAY_OF_MONTH, -1)
        val endMillis = endCal.timeInMillis

        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start
        startCal.add(Calendar.MONTH, -1)
        startCal.set(
            Calendar.DAY_OF_MONTH,
            Math.min(startCal.getActualMaximum(Calendar.DAY_OF_MONTH), wanCycleDay!!)
        )
        val startMillis = startCal.timeInMillis

        return MonthlyCycleItem(context, startMillis, endMillis).setRouterPreferences(
            routerPreferences
        )
    }

    operator fun next(): MonthlyCycleItem {
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start
        startCal.set(Calendar.DAY_OF_MONTH, 1) // The first day of the month the start is in
        startCal.add(Calendar.MONTH, 1)
        startCal.set(
            Calendar.DAY_OF_MONTH,
            Math.min(startCal.getActualMaximum(Calendar.DAY_OF_MONTH), wanCycleDay!!)
        )
        val startMillis = startCal.timeInMillis

        val nextStartCal = Calendar.getInstance()
        nextStartCal.timeInMillis = startMillis
        nextStartCal.set(Calendar.DAY_OF_MONTH, 1) // The first day of the month the start is in
        nextStartCal.add(Calendar.MONTH, 1)
        nextStartCal.set(
            Calendar.DAY_OF_MONTH,
            Math.min(nextStartCal.getActualMaximum(Calendar.DAY_OF_MONTH), wanCycleDay!! - 1)
        )
        val endMillis = nextStartCal.timeInMillis

        return MonthlyCycleItem(context, startMillis, endMillis).setRouterPreferences(
            routerPreferences
        )
    }

    companion object {

        private val sBuilder = StringBuilder(50)
        private val sFormatter = java.util.Formatter(sBuilder, Locale.US)

        @JvmStatic
        private fun formatDateRange(context: Context?, flags: Int, start: Long, end: Long): String {
            synchronized(sBuilder) {
                sBuilder.setLength(0)
                return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null).toString()
            }
        }
    }
}
