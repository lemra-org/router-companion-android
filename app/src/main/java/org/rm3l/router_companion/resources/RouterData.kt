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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.resources

import org.rm3l.router_companion.resources.conn.Router
import java.io.Serializable

/**
 * Abstract Router Data type, encapsulating the actual data and an exception, if any

 * @param <T> the data type
 * *
 * @author [Armel S.](mailto:armel+router_companion@rm3l.org)
</T> */
abstract class RouterData<T> : Serializable {

    private var router: Router? = null

    /**
     * The data wrapped
     */
    private var data: T? = null

    /**
     * The exception wrapped
     */
    private var exception: Exception? = null

    /**
     * @return the data wrapped
     */
    fun getData(): T? {
        return data
    }

    /**
     * Set the data

     * @param data the data to set
     * *
     * @return this object
     */
    fun setData(data: T): RouterData<T> {
        this.data = data
        return this
    }

    /**
     * @return the exception
     */
    fun getException(): Exception? {
        return exception
    }

    /**
     * Set the exception

     * @param exception the exception to set
     * *
     * @return this object
     */
    open fun setException(exception: Exception?): RouterData<T> {
        this.exception = exception
        return this
    }

    fun getRouter(): Router? {
        return router
    }

    fun setRouter(router: Router?): RouterData<T> {
        this.router = router
        return this
    }

    /**
     * @return the string representation
     */
    override fun toString(): String {
        return "${this.javaClass.simpleName} data=$data, exception=$exception}"
    }
}
