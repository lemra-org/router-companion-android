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

import android.support.annotation.Nullable;

import com.google.common.base.Joiner;

import java.util.List;

/**
 * @see {https://www.macvendorlookup.com/mac-address-api}
 * @see {https://github.com/klauspost/oui}
 */
public class MACOUIVendor {

    public static final String TOOLS_RM3L_PREFIX = "http://tools.rm3l.org:5000/";
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

    @Nullable
    private DataDetails data;

    @Nullable
    public DataDetails getData() {
        return data;
    }

    public MACOUIVendor setData(@Nullable DataDetails data) {
        this.data = data;
        return this;
    }

    public String getCompany() {
        return data != null ? data.getManufacturer() : null;
    }

    public static class DataDetails {

        @Nullable
        private String manufacturer;

        @Nullable
        private List<String> address;

        @Nullable
        private String prefix;

        @Nullable
        private String country;

        @Nullable
        public String getManufacturer() {
            return manufacturer;
        }

        public DataDetails setManufacturer(@Nullable String manufacturer) {
            this.manufacturer = manufacturer;
            return this;
        }

        @Nullable
        public List<String> getAddress() {
            return address;
        }

        public DataDetails setAddress(@Nullable List<String> address) {
            this.address = address;
            return this;
        }

        @Nullable
        public String getPrefix() {
            return prefix;
        }

        public DataDetails setPrefix(@Nullable String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Nullable
        public String getCountry() {
            return country;
        }

        public DataDetails setCountry(@Nullable String country) {
            this.country = country;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataDetails that = (DataDetails) o;

            if (manufacturer != null ? !manufacturer.equals(that.manufacturer) : that.manufacturer != null)
                return false;
            if (address != null ? !address.equals(that.address) : that.address != null)
                return false;
            if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) return false;
            return country != null ? country.equals(that.country) : that.country == null;

        }

        @Override
        public int hashCode() {
            int result = manufacturer != null ? manufacturer.hashCode() : 0;
            result = 31 * result + (address != null ? address.hashCode() : 0);
            result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
            result = 31 * result + (country != null ? country.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "DataDetails{" +
                    "manufacturer='" + manufacturer + '\'' +
                    ", address=" + address +
                    ", prefix='" + prefix + '\'' +
                    ", country='" + country + '\'' +
                    '}';
        }
    }

    public boolean isNone() {
        return (data == null ||
                ((data.getAddress() == null || data.getAddress().isEmpty()) &&
                data.getCountry() == null &&
                data.getManufacturer() == null &&
                data.getPrefix() == null));
    }

    @Override
    public String toString() {
        return "MACOUIVendor{" +
                "data='" + data + '\'' +
                '}';
    }

    public String toCommandOutputString() {
        return data == null ? "" :
                "\n" +
                "- manufacturer: " + data.getManufacturer() + "\n" +
                "- address: " + (data.getAddress() != null ?
                        Joiner.on(" , ").skipNulls().join(data.getAddress()) : "N/A") + "\n" +
                "- country: " + data.getCountry() + "\n" +
                "- prefix: " + data.getPrefix() ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MACOUIVendor that = (MACOUIVendor) o;

        return data != null ? data.equals(that.data) : that.data == null;

    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }
}
