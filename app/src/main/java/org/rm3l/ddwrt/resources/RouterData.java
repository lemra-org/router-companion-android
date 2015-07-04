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

package org.rm3l.ddwrt.resources;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.rm3l.ddwrt.resources.conn.Router;

import java.io.Serializable;

/**
 * Abstract Router Data type, encapsulating the actual data and an exception, if any
 *
 * @param <T> the data type
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public abstract class RouterData<T> implements Serializable {

    @Nullable
    private Router router;

    /**
     * The data wrapped
     */
    @Nullable
    private T data;

    /**
     * The exception wrapped
     */
    @Nullable
    private Exception exception;

    /**
     * @return the data wrapped
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Set the data
     *
     * @param data the data to set
     * @return this object
     */
    @NonNull
    public RouterData<T> setData(final T data) {
        this.data = data;
        return this;
    }

    /**
     * @return the exception
     */
    @Nullable
    public Exception getException() {
        return exception;
    }

    /**
     * Set the exception
     *
     * @param exception the exception to set
     * @return this object
     */
    @NonNull
    public RouterData<T> setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    @Nullable
    public Router getRouter() {
        return router;
    }

    public RouterData<T> setRouter(@Nullable Router router) {
        this.router = router;
        return this;
    }

    /**
     * @return the string representation
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "data=" + data +
                ", exception=" + exception +
                '}';
    }
}
