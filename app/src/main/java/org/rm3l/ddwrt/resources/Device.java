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

import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.Device.WANAccessState.WAN_ACCESS_UNKNOWN;

/**
 * Device Type: represents a device reported as a Client (Wireless/DHCP/...)
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class Device implements Comparable<Device> {

    @NonNull
    private final String macAddress;

    @Nullable
    private MACOUIVendor macouiVendorDetails;

    @Nullable
    private String alias;

    @Nullable
    private String ipAddress;

    @Nullable
    private WANAccessState wanAccessState = WAN_ACCESS_UNKNOWN;

    @Nullable
    private String systemName;

    private boolean active = false;

    private double txRate = -1.;
    private double rxRate = -1.;

    private double rxTotal = -1.;
    private double txTotal = -1.;

    private long lastSeen;

    private Set<String> activeIpConnections;

    @Nullable
    private WirelessConnectionInfo wirelessConnectionInfo;

    /**
     * Constructor
     *
     * @param macAddress the device MAC
     */
    public Device(@NonNull final String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * @return the alias
     */
    @Nullable
    public String getAlias() {
        return alias;
    }

    /**
     * Set the alias
     *
     * @param alias the alias to set
     * @return this object
     */
    @NonNull
    public Device setAlias(@Nullable final String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * @return the IP
     */
    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Set the IP
     *
     * @param ipAddress the IP to set
     * @return this object
     */
    @NonNull
    public Device setIpAddress(@Nullable final String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    /**
     * @return the MAC
     */
    @NonNull
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * @return the system name
     */
    @Nullable
    public String getSystemName() {
        return systemName;
    }

    @NonNull
    public Device setSystemName(@Nullable final String systemName) {
        this.systemName = systemName;
        return this;
    }

    @Nullable
    public WirelessConnectionInfo getWirelessConnectionInfo() {
        return wirelessConnectionInfo;
    }

    public void setWirelessConnectionInfo(@Nullable WirelessConnectionInfo wirelessConnectionInfo) {
        this.wirelessConnectionInfo = wirelessConnectionInfo;
    }

    @Override
    public int compareTo(@NonNull final Device device) {
        return this.macAddress.compareTo(device.macAddress);
//        if (this.macAddress == null) {
//            if (device.alias == null) {
//                return 0;
//            }
//            return -1;
//        }
//
//        if (device.alias == null) {
//            return 1;
//        }
//
//        return this.alias.compareTo(device.alias);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public String getName() {
        if (!isNullOrEmpty(this.alias)) {
            return this.alias;
        }
        if (!isNullOrEmpty(this.systemName)) {
            return this.systemName;
        }
        return this.macAddress;
    }

    @Nullable
    public MACOUIVendor getMacouiVendorDetails() {
        return macouiVendorDetails;
    }

    public void setMacouiVendorDetails(@Nullable MACOUIVendor macouiVendorDetails) {
        this.macouiVendorDetails = macouiVendorDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Device device = (Device) o;

        if (!macAddress.equals(device.macAddress))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }

    @NonNull
    public WANAccessState getWanAccessState() {
        return wanAccessState;
    }

    public void setWanAccessState(@Nullable WANAccessState wanAccessState) {
        this.wanAccessState = (wanAccessState != null ? wanAccessState : WAN_ACCESS_UNKNOWN);
    }

    @Override
    public String toString() {
        return "Device{" +
                "macAddress='" + macAddress + '\'' +
                ", macouiVendorDetails=" + macouiVendorDetails +
                ", alias='" + alias + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", wanAccessState=" + wanAccessState +
                ", systemName='" + systemName + '\'' +
                ", active=" + active +
                ", txRate=" + txRate +
                ", rxRate=" + rxRate +
                ", rxTotal=" + rxTotal +
                ", txTotal=" + txTotal +
                ", lastSeen=" + lastSeen +
                '}';
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getRxRate() {
        return rxRate;
    }

    public void setRxRate(double rxRate) {
        this.rxRate = rxRate;
    }

    public double getTxRate() {
        return txRate;
    }

    public void setTxRate(double txRate) {
        this.txRate = txRate;
    }

    public double getRxTotal() {
        return rxTotal;
    }

    public void setRxTotal(double rxTotal) {
        this.rxTotal = rxTotal;
    }

    public double getTxTotal() {
        return txTotal;
    }

    public void setTxTotal(double txTotal) {
        this.txTotal = txTotal;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Nullable
    public Set<String> getActiveIpConnections() {
        return activeIpConnections;
    }

    public void setActiveIpConnections(@Nullable Set<String> activeIpConnections) {
        this.activeIpConnections = activeIpConnections;
    }

    public int getActiveIpConnectionsCount() {
        return (activeIpConnections != null ? activeIpConnections.size() : -1);
    }

    public enum WANAccessState {
        WAN_ACCESS_UNKNOWN("Unknown"),

        WAN_ACCESS_ENABLED("Enabled"),

        WAN_ACCESS_DISABLED("Disabled");

        private final String mToString;

        private WANAccessState(final String mToString) {
            this.mToString = mToString;
        }

        @Override
        public String toString() {
            return mToString;
        }
    }

    public static class WirelessConnectionInfo {
        @Nullable
        private String ssid;
        @Nullable
        private String snr;
        @Nullable
        private String rssi;

        public WirelessConnectionInfo() {
        }

        @Nullable
        public String getSsid() {
            return ssid;
        }

        public void setSsid(@Nullable String ssid) {
            this.ssid = ssid;
        }

        @Nullable
        public String getSnr() {
            return snr;
        }

        public void setSnr(@Nullable String snr) {
            this.snr = snr;
        }

        @Nullable
        public String getRssi() {
            return rssi;
        }

        public void setRssi(@Nullable String rssi) {
            this.rssi = rssi;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WirelessConnectionInfo that = (WirelessConnectionInfo) o;

            if (rssi != null ? !rssi.equals(that.rssi) : that.rssi != null) return false;
            if (snr != null ? !snr.equals(that.snr) : that.snr != null) return false;
            if (ssid != null ? !ssid.equals(that.ssid) : that.ssid != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = ssid != null ? ssid.hashCode() : 0;
            result = 31 * result + (snr != null ? snr.hashCode() : 0);
            result = 31 * result + (rssi != null ? rssi.hashCode() : 0);
            return result;
        }
    }
}
