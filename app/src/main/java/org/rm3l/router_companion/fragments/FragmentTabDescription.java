package org.rm3l.router_companion.fragments;

import androidx.annotation.NonNull;

/**
 * Created by rm3l on 01/09/15.
 */
public abstract class FragmentTabDescription<T extends AbstractBaseFragment> {

    @NonNull
    private final Class<T> clazz;

    protected FragmentTabDescription(@NonNull Class<T> clazz) {
        this.clazz = clazz;
    }

    @NonNull
    public Class<T> getClazz() {
        return clazz;
    }

    public abstract int getTitleRes();
}
