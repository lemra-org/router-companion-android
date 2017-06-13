package org.rm3l.router_companion.utils.kotlin

import android.view.View

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