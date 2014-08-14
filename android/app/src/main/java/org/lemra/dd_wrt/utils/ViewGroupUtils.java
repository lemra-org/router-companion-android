package org.lemra.dd_wrt.utils;

import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by armel on 8/13/14.
 */
public final class ViewGroupUtils {

    private ViewGroupUtils() {}

    @Nullable
    public static ViewGroup getParent(@NotNull final View view) {
        return (ViewGroup)view.getParent();
    }

    public static void removeView(@NotNull final View view) {
        ViewGroup parent = getParent(view);
        if(parent != null) {
            parent.removeView(view);
        }
    }

    public static void replaceView(@NotNull final View currentView, @NotNull final View newView) {
        ViewGroup parent = getParent(currentView);
        if(parent == null) {
            return;
        }
        final int index = parent.indexOfChild(currentView);
        removeView(currentView);
        removeView(newView);
        parent.addView(newView, index);
    }

}
