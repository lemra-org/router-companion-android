package org.rm3l.router_companion.widgets

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.Calendar

const val DATE_PICKER_LISTENER = "DATE_PICKER_LISTENER"
const val START_MILLIS = "START_MILLIS"
const val MIN_MILLIS = "MIN_MILLIS"
const val MAX_MILLIS = "MAX_MILLIS"

class DatePickerFragment : DialogFragment(), OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val bundleArguments = arguments
        if (bundleArguments?.containsKey(START_MILLIS) == true) {
            calendar.timeInMillis = bundleArguments.getLong(START_MILLIS)
        }
        val datePickerDialog = DatePickerDialog(activity!!,
                bundleArguments?.getParcelable<AbstractDatePickerListener>(DATE_PICKER_LISTENER)?:this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
        val datePicker = datePickerDialog.datePicker
        if (bundleArguments?.containsKey(MIN_MILLIS) == true) {
            datePicker.minDate = bundleArguments.getLong(MIN_MILLIS)
        }
        if (bundleArguments?.containsKey(MAX_MILLIS) == true) {
            datePicker.maxDate = bundleArguments.getLong(MAX_MILLIS)
        }
        return datePickerDialog
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        FirebaseCrashlytics.getInstance().log("No-op onDateSet: <year,month,dayOfMonth>=<$year,$month,$dayOfMonth>")
    }

    companion object {
        private val TAG = DatePickerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            datePickerListener: AbstractDatePickerListener,
            startFromMillis: Long? = null,
            minMillis: Long? = null,
            maxMillis: Long? = null
        ):
            DatePickerFragment {
                val fragment = DatePickerFragment()
                val bundle = Bundle()
                bundle.putParcelable(DATE_PICKER_LISTENER, datePickerListener)
                startFromMillis?.let { bundle.putLong(START_MILLIS, it) }
                minMillis?.let { bundle.putLong(MIN_MILLIS, it) }
                maxMillis?.let { bundle.putLong(MAX_MILLIS, it) }
                fragment.arguments = bundle
                return fragment
            }
    }
}

abstract class AbstractDatePickerListener : OnDateSetListener, Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) { /*no members => Nothing to do*/ }
    override fun describeContents() = 0
}
