/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

@Deprecated
public class VerticalScrollView extends ScrollView {

  // Return false if we're scrolling in the x direction
  class YScrollDetector extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      FirebaseCrashlytics.getInstance().log("VerticalScrollView.YScrollDetector#onScroll");
      return Math.abs(distanceY) > Math.abs(distanceX);
    }
  }

  private static final String LOG_TAG = VerticalScrollView.class.getSimpleName();

  View.OnTouchListener mGestureListener;

  private final GestureDetector mGestureDetector;

  public VerticalScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mGestureDetector = new GestureDetector(context, new YScrollDetector());
    setFadingEdgeLength(0);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    FirebaseCrashlytics.getInstance().log("VerticalScrollView#onInterceptTouchEvent");
    return super.onInterceptTouchEvent(ev) && mGestureDetector.onTouchEvent(ev);
  }
}
