@file:JvmName("ViewUtils")

package org.rm3l.router_companion.utils.kotlin

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.resources.conn.Router.RouterFirmware
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.ViewGroupUtils

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.hide() = gone()

fun View.show() = visible()

fun View.setHeight(height: Int) {
    val params = layoutParams
    params.height = height
    layoutParams = params
}

fun View.setBackgroundColorFromRouterFirmware(router: Router?) =
        this.setBackgroundColorFromRouterFirmware(router?.routerFirmware)

fun View.setBackgroundColorFromRouterFirmware(routerFirmware: RouterFirmware?) {
    val primaryColor = ColorUtils.getPrimaryColor(routerFirmware)
    if (primaryColor != null) {
        this.setBackgroundColor(ContextCompat.getColor(this.context, primaryColor))
    } else {
        //TODO Fix colors
        this.setBackgroundColor(ContextCompat.getColor(this.context, R.color.transparent_semi))
    }
}

fun ViewGroup.expand(expandCollapseButton: ImageButton? = null) {
    this.visible()
    val widthSpec = View.MeasureSpec.makeMeasureSpec(ViewGroupUtils.getParent(this)?.width?:0,
            View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(this.computeFullHeight(), View.MeasureSpec.AT_MOST)
    this.measure(widthSpec, heightSpec)
    val mAnimator = slideAnimator(0, this.measuredHeight)
    mAnimator.start()
    expandCollapseButton?.let {
        it.setImageResource(
                if (ColorUtils.isThemeLight(it.context))
                    R.drawable.ic_expand_less_black_24dp
                else R.drawable.ic_expand_less_white_24dp)
    }
}

fun View.collapse(expandCollapseButton: ImageButton? = null) {
    val finalHeight = this.height
    val mAnimator = slideAnimator(finalHeight, 0)
    mAnimator.addListener(object : AnimatorListener {

        override fun onAnimationEnd(animation: Animator?) {
            //Height=0, but it set visibility to GONE
            this@collapse.gone()
        }

        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationStart(animation: Animator?) {}
    })
    mAnimator.start()
    expandCollapseButton?.let {
        it.setImageResource(
                if (ColorUtils.isThemeLight(it.context))
                    R.drawable.ic_expand_more_black_24dp
                else R.drawable.ic_expand_more_white_24dp)
    }
}

fun ViewGroup.computeFullHeight(): Int {
    val specWidth = View.MeasureSpec.makeMeasureSpec(0 /* any */, View.MeasureSpec.UNSPECIFIED)
    val specHeight = View.MeasureSpec.makeMeasureSpec(0 /* any */, View.MeasureSpec.UNSPECIFIED)
    this.measure(specWidth, specHeight)
    var totalHeight = 0//this.getMeasuredHeight();
    val initialVisibility = this.visibility
    this.visibility = View.VISIBLE
    val numberOfChildren = this.childCount
    (0 until numberOfChildren)
            .asSequence()
            .map { this.getChildAt(it) }
            .forEach {
                totalHeight += if (it is ViewGroup) {
                    it.computeFullHeight()
                } else {
                    val desiredWidth = View.MeasureSpec.makeMeasureSpec(this.width,
                            View.MeasureSpec.AT_MOST)
                    it.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
                    it.measuredHeight
                }
            }
    this.visibility = initialVisibility
    return totalHeight
}

private fun View.slideAnimator(start: Int, end: Int): ValueAnimator {
    val animator = ValueAnimator.ofInt(start, end)
    animator.addUpdateListener { valueAnimator ->
        //Update Height
        val value = valueAnimator.animatedValue as Int
        val layoutParams = this@slideAnimator.layoutParams
        layoutParams.height = value
        this@slideAnimator.layoutParams = layoutParams
    }
    return animator
}

fun TextView.setClickable(onClickFunction: (View?) -> Unit) {
    this.movementMethod = LinkMovementMethod.getInstance()
    val spans = this.text as Spannable
    spans.setSpan(object: ClickableSpan() {
        override fun onClick(widget: View?) = onClickFunction(widget)
    }, 0, spans.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}