@file:JvmName("ViewUtils")

package org.rm3l.router_companion.utils.kotlin

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.view.View
import android.widget.ImageButton
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.utils.ColorUtils

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

fun View.expand(expandCollapseButton: ImageButton? = null) {
    this.visible()
    val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
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