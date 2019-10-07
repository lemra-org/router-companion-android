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

package org.rm3l.router_companion.resources

import com.google.common.base.Joiner

/**
 * @see {https://www.macvendorlookup.com/mac-address-api}

 * @see {https://github.com/klauspost/oui}
 */
class MACOUIVendor {
    /*
      {
        "data": {
          "manufacturer": "Liteon Technology Corporation",
          "address": [
            "Taipei  23585",
            "TAIWAN, PROVINCE OF CHINA"
          ],
          "prefix": "d0:df:9a",
          "country": "TAIWAN, PROVINCE OF CHINA"
        }
      }
       */

    private var data: DataDetails? = null

    fun getData(): DataDetails? {
        return data
    }

    fun setData(data: DataDetails?): MACOUIVendor {
        this.data = data
        return this
    }

    val company: String?
        get() = if (data != null) data!!.getManufacturer() else null

    val isNone: Boolean
        get() = data == null || (data!!.getAddress() == null || data!!.getAddress()!!.isEmpty())
                && data!!.getCountry() == null
                && data!!.getManufacturer() == null
                && data!!.getPrefix() == null

    override fun toString(): String {
        return "MACOUIVendor{data='$data'}"
    }

    fun toCommandOutputString(): String {
        return if (data == null)
            ""
        else
            "\n" + "- manufacturer: " + data!!.getManufacturer() + "\n" + "- address: " +
                    if (data!!.getAddress() != null)
                        Joiner.on(" , ").skipNulls().join(data!!.getAddress()!!)
                    else
                        "N/A" + "\n" + "- country: " + data!!.getCountry() + "\n" + "- prefix: " + data!!.getPrefix()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as MACOUIVendor?

        return if (data != null) data == that!!.data else that!!.data == null
    }

    override fun hashCode(): Int {
        return if (data != null) data!!.hashCode() else 0
    }

    class DataDetails {

        private var manufacturer: String? = null

        private var address: List<String>? = null

        private var prefix: String? = null

        private var country: String? = null

        fun getManufacturer(): String? {
            return manufacturer
        }

        fun setManufacturer(manufacturer: String?): DataDetails {
            this.manufacturer = manufacturer
            return this
        }

        fun getAddress(): List<String>? {
            return address
        }

        fun setAddress(address: List<String>?): DataDetails {
            this.address = address
            return this
        }

        fun getPrefix(): String? {
            return prefix
        }

        fun setPrefix(prefix: String?): DataDetails {
            this.prefix = prefix
            return this
        }

        fun getCountry(): String? {
            return country
        }

        fun setCountry(country: String?): DataDetails {
            this.country = country
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val that = other as DataDetails?

            if (if (manufacturer != null)
                manufacturer != that!!.manufacturer
            else
                that!!.manufacturer != null) {
                return false
            }
            if (if (address != null) address != that.address else that.address != null) return false
            if (if (prefix != null) prefix != that.prefix else that.prefix != null) return false
            return if (country != null) country == that.country else that.country == null
        }

        override fun hashCode(): Int {
            var result = if (manufacturer != null) manufacturer!!.hashCode() else 0
            result = 31 * result + if (address != null) address!!.hashCode() else 0
            result = 31 * result + if (prefix != null) prefix!!.hashCode() else 0
            result = 31 * result + if (country != null) country!!.hashCode() else 0
            return result
        }

        override fun toString(): String {
            return "DataDetails{manufacturer='$manufacturer', address=$address, prefix='$prefix', country='$country'}"
        }
    }

    companion object {

        val TOOLS_RM3L_PREFIX = "https://mac-oui.services.rm3l.org/"

        @JvmField
        val BASE_API_ENDPOINT = "https://mac-oui.services.rm3l.org"
    }
}
