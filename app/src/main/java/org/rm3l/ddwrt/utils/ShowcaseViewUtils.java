package org.rm3l.ddwrt.utils;

import android.view.View;

import com.github.amlcurran.showcaseview.ApiUtils;

/**
 * Created by rm3l on 03/11/15.
 */
public final class ShowcaseViewUtils {

    public static void setAlpha(float alpha, View... views) {
        if (new ApiUtils().isCompatWithHoneycomb()) {
            for (View view : views) {
                view.setAlpha(alpha);
            }
        }
    }
}
