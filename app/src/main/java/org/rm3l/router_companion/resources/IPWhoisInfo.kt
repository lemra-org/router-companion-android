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
package org.rm3l.router_companion.resources

class IPWhoisInfo {
    //    public static final String IP_WHOIS_INFO_API_PREFIX = TOOLS_RM3L_GEOIP_API_PREFIX;

    /* === dazzlepod.com ===
       * {
       * "ip": "216.58.208.234",
       * "prefix": "216.58.208.0/24",
       * "country_code": "US",
       * "asn": "AS15169",
       * "city": "Mountain View",
       * "country": "United States",
       * "region": "California",
       * "hostname": "par10s22-in-f10.1e100.net",
       * "longitude": -122.0574,
       * "latitude": 37.4192,
       * "organization": "GOOGLE - Google Inc.,US"
       * }
       */

    /* === tools.rm3l.org ===
      {
      "city": "Mountain View",
      "country_code": "US",
      "country_name": "United States",
      "ip": "216.58.208.234",
      "latitude": 37.4192,
      "longitude": -122.0574,

      "metro_code": 807,
      "region_code": "CA",

      "region_name": "California",

      "time_zone": "America/Los_Angeles",

      "zip_code": "94043"
  }
       */

    var ip: String? = null

    var prefix: String? = null

    var country_code: String? = null

    var asn: String? = null

    var city: String? = null

    var country: String? = null

    var region: String? = null

    var hostname: String? = null

    var longitude: String? = null

    var latitude: String? = null

    var organization: String? = null

    val isNone: Boolean
        get() = asn == null &&
            city == null &&
            country == null &&
            country_code == null &&
            hostname == null &&
            ip == null &&
            latitude == null &&
            longitude == null &&
            organization == null &&
            prefix == null &&
            region == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as IPWhoisInfo?

        if (if (asn != null) asn != that!!.asn else that!!.asn != null) return false
        if (if (city != null) city != that.city else that.city != null) return false
        if (if (country != null) country != that.country else that.country != null) return false
        if (if (country_code != null)
                country_code != that.country_code
            else
                that.country_code != null
        ) {
            return false
        }
        if (if (hostname != null) hostname != that.hostname else that.hostname != null) return false
        if (if (ip != null) ip != that.ip else that.ip != null) return false
        if (if (latitude != null) latitude != that.latitude else that.latitude != null) return false
        if (if (longitude != null) longitude != that.longitude else that.longitude != null) {
            return false
        }
        if (if (organization != null)
                organization != that.organization
            else
                that.organization != null
        ) {
            return false
        }
        if (if (prefix != null) prefix != that.prefix else that.prefix != null) return false
        if (if (region != null) region != that.region else that.region != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = if (ip != null) ip!!.hashCode() else 0
        result = 31 * result + if (prefix != null) prefix!!.hashCode() else 0
        result = 31 * result + if (country_code != null) country_code!!.hashCode() else 0
        result = 31 * result + if (asn != null) asn!!.hashCode() else 0
        result = 31 * result + if (city != null) city!!.hashCode() else 0
        result = 31 * result + if (country != null) country!!.hashCode() else 0
        result = 31 * result + if (region != null) region!!.hashCode() else 0
        result = 31 * result + if (hostname != null) hostname!!.hashCode() else 0
        result = 31 * result + if (longitude != null) longitude!!.hashCode() else 0
        result = 31 * result + if (latitude != null) latitude!!.hashCode() else 0
        result = 31 * result + if (organization != null) organization!!.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return "IPWhoisInfo{ip='$ip', prefix='$prefix', country_code='$country_code', asn='$asn', city='$city', country='$country', region='$region', hostname='$hostname', longitude='$longitude', latitude='$latitude', organization='$organization'}"
    }

    fun toCommandOutputString(): String {
        return "\n- IP: $ip\n- Prefix: $prefix\n- Country Code: $country_code\n- ASN: $asn\n- City: $city\n- Country: $country\n- Region: $region\n- Hostname: $hostname\n- Longitude: $longitude\n- Latitude: $latitude\n- Organization: $organization"
    }

    companion object {

        @JvmField
        val TOOLS_RM3L_GEOIP_API_PREFIX = "http://tools.rm3l.org:5080/json"

        @JvmField
        val IP_WHOIS_INFO_API_PREFIX = "https://dazzlepod.com/ip"
    }
}
