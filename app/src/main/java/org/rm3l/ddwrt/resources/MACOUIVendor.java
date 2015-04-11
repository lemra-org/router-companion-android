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

/**
 * @see {https://www.macvendorlookup.com/mac-address-api}
 */
public class MACOUIVendor {

    public static final String MAC_VENDOR_LOOKUP_API_PREFIX = "http://www.macvendorlookup.com/api/v2";

    /*
    {
      "startHex":"0023AB000000",
      "endHex":"0023ABFFFFFF",
      "startDec":"153192759296",
      "endDec":"153209536511",
      "company":"CISCO SYSTEMS, INC.",
      "addressL1":"170 W. TASMAN DRIVE",
      "addressL2":"M\/S SJA-2",
      "addressL3":"SAN JOSE CA 95134-1706",
      "country":"UNITED STATES",
      "type":"oui24"
   }
     */

    /**
     * The start of the MAC address range the vendor owns in hexadecimal format
     */
    @Nullable
    private String startHex;

    /**
     * The end of the MAC address range the vendor owns in hexadecimal format
     */
    @Nullable
    private String endHex;

    /**
     * The start of the MAC address range the vendor owns in decimal format
     */
    @Nullable
    private String startDec;

    /**
     * The end of the MAC address range the vendor owns in decimal format
     */
    @Nullable
    private String endDec;

    /**
     * Company name of the vendor or manufacturer
     */
    @Nullable
    private String company;

    /**
     * First line of the address the company provided to IEEE
     */
    @Nullable
    private String addressL1;

    /**
     * Second line of the address the company provided to IEEE
     */
    @Nullable
    private String addressL2;

    /**
     * Third line of the address the company provided to IEEE
     */
    @Nullable
    private String addressL3;

    /**
     * Country the company is located in
     */
    @Nullable
    private String country;

    /**
     * There are 3 different IEEE databases: oui24, oui36, and iab
     */
    @Nullable
    private String type;

    public String getStartHex() {
        return startHex;
    }

    public void setStartHex(String startHex) {
        this.startHex = startHex;
    }

    public String getEndHex() {
        return endHex;
    }

    public void setEndHex(String endHex) {
        this.endHex = endHex;
    }

    public String getStartDec() {
        return startDec;
    }

    public void setStartDec(String startDec) {
        this.startDec = startDec;
    }

    public String getEndDec() {
        return endDec;
    }

    public void setEndDec(String endDec) {
        this.endDec = endDec;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddressL1() {
        return addressL1;
    }

    public void setAddressL1(String addressL1) {
        this.addressL1 = addressL1;
    }

    public String getAddressL2() {
        return addressL2;
    }

    public void setAddressL2(String addressL2) {
        this.addressL2 = addressL2;
    }

    public String getAddressL3() {
        return addressL3;
    }

    public void setAddressL3(String addressL3) {
        this.addressL3 = addressL3;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isNone() {
        return (startHex == null &&
                endHex == null &&
                startDec == null &&
                endDec == null &&
                company == null &&
                addressL1 == null &&
                addressL2 == null &&
                addressL3 == null &&
                country == null &&
                type == null);
    }

    @Override
    public String toString() {
        return "MACOUIVendor{" +
                "startHex='" + startHex + '\'' +
                ", endHex='" + endHex + '\'' +
                ", startDec='" + startDec + '\'' +
                ", endDec='" + endDec + '\'' +
                ", company='" + company + '\'' +
                ", addressL1='" + addressL1 + '\'' +
                ", addressL2='" + addressL2 + '\'' +
                ", addressL3='" + addressL3 + '\'' +
                ", country='" + country + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public String toCommandOutputString() {
        return "\n" +
                "startHex: " + startHex + "\n\n" +
                ", endHex: " + endHex + "\n\n" +
                ", startDec: " + startDec + "\n\n" +
                ", endDec: " + endDec + "\n\n" +
                ", company: " + company + "\n\n" +
                ", addressL1: " + addressL1 + "\n\n" +
                ", addressL2: " + addressL2 + "\n\n" +
                ", addressL3: " + addressL3 + "\n\n" +
                ", country: " + country + "\n\n" +
                ", type: " + type ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MACOUIVendor that = (MACOUIVendor) o;

        if (addressL1 != null ? !addressL1.equals(that.addressL1) : that.addressL1 != null)
            return false;
        if (addressL2 != null ? !addressL2.equals(that.addressL2) : that.addressL2 != null)
            return false;
        if (addressL3 != null ? !addressL3.equals(that.addressL3) : that.addressL3 != null)
            return false;
        if (company != null ? !company.equals(that.company) : that.company != null) return false;
        if (country != null ? !country.equals(that.country) : that.country != null) return false;
        if (endDec != null ? !endDec.equals(that.endDec) : that.endDec != null) return false;
        if (endHex != null ? !endHex.equals(that.endHex) : that.endHex != null) return false;
        if (startDec != null ? !startDec.equals(that.startDec) : that.startDec != null)
            return false;
        if (startHex != null ? !startHex.equals(that.startHex) : that.startHex != null)
            return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startHex != null ? startHex.hashCode() : 0;
        result = 31 * result + (endHex != null ? endHex.hashCode() : 0);
        result = 31 * result + (startDec != null ? startDec.hashCode() : 0);
        result = 31 * result + (endDec != null ? endDec.hashCode() : 0);
        result = 31 * result + (company != null ? company.hashCode() : 0);
        result = 31 * result + (addressL1 != null ? addressL1.hashCode() : 0);
        result = 31 * result + (addressL2 != null ? addressL2.hashCode() : 0);
        result = 31 * result + (addressL3 != null ? addressL3.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
