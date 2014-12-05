/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.resources;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract Router Data type, encapsulating the actual data and an exception, if any
 *
 * @param <T> the data type
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public abstract class RouterData<T> {

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
    @NotNull
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
    @NotNull
    public RouterData<T> setException(Exception exception) {
        this.exception = exception;
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
