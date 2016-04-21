/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
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
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.rm3l.ddwrt.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Android ViewGroup Utilities
 */
public final class ViewGroupUtils {

    public static final int COMPRESSION_QUALITY = 100;
    public static final int DEFAULT_BITMAP_WIDTH = 640;
    public static final int DEFAULT_BITMAP_HEIGHT = 480;

    private ViewGroupUtils() {
    }

    @Nullable
    public static ViewGroup getParent(@NonNull final View view) {
        return (ViewGroup) view.getParent();
    }

    public static void removeView(@NonNull final View view) {
        final ViewGroup parent = getParent(view);
        if (parent != null) {
            parent.removeView(view);
        }
    }

    public static void replaceView(@NonNull final View currentView, @NonNull final View newView) {
        final ViewGroup parent = getParent(currentView);
        if (parent == null) {
            return;
        }
        final int index = parent.indexOfChild(currentView);
        removeView(currentView);
        removeView(newView);
        parent.addView(newView, index);
    }

    public static void requestDisallowParentInterceptTouchEvent(View __v, Boolean __disallowIntercept) {
        while (__v.getParent() != null && __v.getParent() instanceof View) {
//            if (__v.getParent() instanceof ScrollView) {
            __v.getParent().requestDisallowInterceptTouchEvent(__disallowIntercept);
//            }
            __v = (View) __v.getParent();
        }
    }

    @NonNull
    public static Rect getLocationOnScreen(@NonNull final View mView) {
        final Rect mRect = new Rect();
        final int[] location = new int[2];

        mView.getLocationOnScreen(location);

        mRect.left = location[0];
        mRect.top = location[1];
        mRect.right = location[0] + mView.getWidth();
        mRect.bottom = location[1] + mView.getHeight();

        return mRect;
    }

    @NonNull
    public static PointF getTopLeftCorner(@NonNull final View view) {
        final float src[] = new float[8];
        final float[] dst = new float[]{0, 0, view.getWidth(), 0, 0, view.getHeight(), view.getWidth(), view.getHeight()};
        view.getMatrix().mapPoints(src, dst);
        return new PointF(view.getX() + src[0], view.getY() + src[1]);
    }

    @NonNull
    public static PointF getTopRightCorner(@NonNull final View view) {
        final float src[] = new float[8];
        final float[] dst = new float[]{0, 0, view.getWidth(), 0, 0, view.getHeight(), view.getWidth(), view.getHeight()};
        view.getMatrix().mapPoints(src, dst);
        return new PointF(view.getX() + src[2], view.getY() + src[3]);
    }

    @NonNull
    public static PointF getBottomLeftCorner(@NonNull final View view) {
        final float src[] = new float[8];
        final float[] dst = new float[]{0, 0, view.getWidth(), 0, 0, view.getHeight(), view.getWidth(), view.getHeight()};
        view.getMatrix().mapPoints(src, dst);
        return new PointF(view.getX() + src[4], view.getY() + src[5]);
    }

    @NonNull
    public static PointF getBottomRightCorner(@NonNull final View view) {
        final float src[] = new float[8];
        final float[] dst = new float[]{0, 0, view.getWidth(), 0, 0, view.getHeight(), view.getWidth(), view.getHeight()};
        view.getMatrix().mapPoints(src, dst);
        return new PointF(view.getX() + src[6], view.getY() + src[7]);
    }

    public static int getSpinnerIndex(@NonNull final Spinner spinner, @NonNull final String value) {
        int index = 0;

        for (int i = 0; i < spinner.getCount(); i++){
            if (value.equals(spinner.getItemAtPosition(i).toString())) {
                index = i;
                break;
            }
        }
        return index;
    }

    @NonNull
    public static List<View> getLinearLayoutChildren(@Nullable final LinearLayout linearLayout) {
        final List<View> views = new ArrayList<>();
        if (linearLayout != null) {
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                views.add(linearLayout.getChildAt(i));
            }
        }
        return views;
    }

    @Nullable
    public static Float dpFromPx(final Context context, final float px) {
        final float density = context.getResources().getDisplayMetrics().density;
        return (density != 0 ? (px / density) : null);
    }

    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    @Nullable
    public static Bitmap toBitmap(@Nullable final View view) {
        if (view == null) {
            return null;
        }
        final int width = view.getWidth();
        final int height = view.getHeight();
        final Bitmap bitmapToExport = Bitmap
                .createBitmap(width > 0 ? width : DEFAULT_BITMAP_WIDTH,
                        height > 0 ? height : DEFAULT_BITMAP_HEIGHT,
                        Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmapToExport);
        view.draw(canvas);
        return bitmapToExport;
    }

    public static void exportViewToFile(@NonNull final Context context,
                                        @NonNull final View view, @NonNull final File file) {
        final Bitmap bitmap = toBitmap(view);
        if (bitmap == null) {
            return;
        }
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file, false));
            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream);
            outputStream.flush();
        } catch (final IOException e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.internal_error_please_try_again), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                //No Worries
            }
        }
    }

}
