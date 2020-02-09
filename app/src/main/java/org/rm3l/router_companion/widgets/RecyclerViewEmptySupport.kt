package org.rm3l.router_companion.widgets

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.firebase.crashlytics.FirebaseCrashlytics

class RecyclerViewEmptySupport : RecyclerView {

    internal var emptyView: View? = null

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            FirebaseCrashlytics.getInstance().log("onChanged")
            super.onChanged()
            checkIfEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            FirebaseCrashlytics.getInstance().log(
                    "onItemRangeInserted($positionStart, $itemCount)")
            super.onItemRangeInserted(positionStart, itemCount)
            checkIfEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            FirebaseCrashlytics.getInstance().log(
                    "onItemRangeInserted($positionStart, $itemCount)")
            super.onItemRangeRemoved(positionStart, itemCount)
            checkIfEmpty()
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        adapter?.registerAdapterDataObserver(observer)
        super.setAdapter(adapter)
        checkIfEmpty()
    }

    /**
     * Indicates the view to be shown when the adapter for this object is empty
     *
     * @param emptyView the empty view
     */
    fun setEmptyView(emptyView: View?) {
        if (this.emptyView != null) {
            this.emptyView!!.visibility = View.GONE
        }
        this.emptyView = emptyView
        checkIfEmpty()
    }

    override fun swapAdapter(adapter: RecyclerView.Adapter<*>?, removeAndRecycleExistingViews: Boolean) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        adapter?.registerAdapterDataObserver(observer)
        super.swapAdapter(adapter, removeAndRecycleExistingViews)
        checkIfEmpty()
    }

    /**
     * Check adapter item count and toggle visibility of empty view if the adapter is empty
     */
    private fun checkIfEmpty() {
        if (emptyView == null || adapter == null) {
            return
        }
        if (adapter != null && adapter!!.itemCount > 0) {
            emptyView!!.visibility = View.GONE
        } else {
            emptyView!!.visibility = View.VISIBLE
        }
    }

    companion object {
        private val TAG = RecyclerViewEmptySupport::class.java.simpleName
    }
}