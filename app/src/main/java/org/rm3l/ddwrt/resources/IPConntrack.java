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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;

public class IPConntrack {

    public static final Splitter PARSER_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();
    public static final Splitter EQUALS_SPLITTER = Splitter.on("=");

    /**
     * Protocol name and number
     */
    @Nullable
    private String transportProtocol;

    @Nullable
    private String sourceHostname;

    @Nullable
    private String destWhoisOrHostname;

    /**
     * Seconds until this entry expires.
     */
    private long timeout;

    /**
     * TCP only: TCP connection state.
     */
    @Nullable
    private String tcpConnectionState;

    /**
     * Source address of “original”-side packets (packets from the side
     * that initiated the connection).
     */
    @Nullable
    private String sourceAddressOriginalSide;

    /**
     * Destination address of original-side packets.
     */
    @Nullable
    private String destinationAddressOriginalSide;

    /**
     * Source port of original-side packets.
     */
    private int sourcePortOriginalSide;

    /**
     * Destination port of original-side packets.
     */
    private int destinationPortOriginalSide;

    /**
     * “[UNREPLIED]”, if this connection has not seen traffic in both directions.
     * Otherwise not present.
     */
    private boolean hasSeenTrafficInBothDirections;

    /**
     * Source address of “reply”-side packets (packets from the side that
     * received the connection).
     */
    @Nullable
    private String sourceAddressReplySide;

    /**
     * Destination address of reply-side packets.
     */
    @Nullable
    private String destinationAddressReplySide;

    /**
     * Source port of reply-side packets.
     */
    private int sourcePortReplySide;

    /**
     * Destination port of reply-side packets.
     */
    private int destinationPortReplySide;

    /**
     * “[ASSURED]”, if this connection has seen traffic in both directions (for UDP)
     * or an ACK in an ESTABLISHED connection (for TCP). Otherwise not present.
     */
    private boolean assured;

    /**
     * Use count of this connection structure.
     */
    private int structUseCount;

    private long packets;

    private long bytes;

    @Nullable
    private String icmpId;

    @Nullable
    private int icmpType;

    @Nullable
    private int icmpCode;

    @Nullable
    public static IPConntrack parseIpConntrackRow(@NonNull final String row) {
        if (Strings.isNullOrEmpty(row)) {
            return null;
        }

        final List<String> toList = PARSER_SPLITTER.splitToList(row);
        if (toList == null || toList.isEmpty()) {
            return null;
        }

        final IPConntrack ipConntrack = new IPConntrack();

        try {

            final String proto = nullToEmpty(toList.get(0)).toUpperCase();
            ipConntrack.setTransportProtocol(proto);
            if (toList.size() >= 3) {
                ipConntrack.setTimeout(Long.parseLong(toList.get(2)));
            }
            switch (proto) {
                case "TCP":
                    if (toList.size() >= 4) {
                        ipConntrack.setTcpConnectionState(toList.get(3));
                    }
                    if (toList.size() >= 5) {
                        for (int i = 4; i < toList.size(); i++) {
                            final String sequence = toList.get(i);
                            if (sequence == null || sequence.isEmpty()) {
                                continue;
                            }
                            final List<String> stringList = EQUALS_SPLITTER.splitToList(sequence);
                            if (stringList != null && stringList.size() >= 2) {
                                final String firstPart = stringList.get(0);
                                final String secondPart = stringList.get(1);
                                if ("src".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourceAddressOriginalSide() == null) {
                                        ipConntrack.setSourceAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setSourceAddressReplySide(secondPart);
                                    }
                                } else if ("sport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourcePortOriginalSide() <= 0) {
                                        ipConntrack.setSourcePortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setSourcePortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if ("dst".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationAddressOriginalSide() == null) {
                                        ipConntrack.setDestinationAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setDestinationAddressReplySide(secondPart);
                                    }
                                } else if ("dport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationPortOriginalSide() <= 0) {
                                        ipConntrack.setDestinationPortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setDestinationPortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if (ipConntrack.getPackets() <= 0l &&
                                        "packets".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setPackets(Long.parseLong(secondPart));
                                } else if (ipConntrack.getBytes() <= 0l &&
                                        "bytes".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setBytes(Long.parseLong(secondPart));
                                } else if (ipConntrack.getStructUseCount() <= 0 &&
                                        "use".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setStructUseCount(Integer.parseInt(secondPart));
                                }
                            } else if (StringUtils.startsWith(sequence, "[")) {
                                ipConntrack.setAssured(StringUtils.containsIgnoreCase(sequence, "assured"));
                            }
                        }
                    }
                    break;
                case "UDP":
                    if (toList.size() >= 4) {
                        for (int i = 3; i < toList.size(); i++) {
                            final String sequence = toList.get(i);
                            if (sequence == null || sequence.isEmpty()) {
                                continue;
                            }
                            final List<String> stringList = EQUALS_SPLITTER.splitToList(sequence);
                            if (stringList != null && stringList.size() >= 2) {
                                final String firstPart = stringList.get(0);
                                final String secondPart = stringList.get(1);
                                if ("src".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourceAddressOriginalSide() == null) {
                                        ipConntrack.setSourceAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setSourceAddressReplySide(secondPart);
                                    }
                                } else if ("sport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourcePortOriginalSide() <= 0) {
                                        ipConntrack.setSourcePortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setSourcePortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if ("dst".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationAddressOriginalSide() == null) {
                                        ipConntrack.setDestinationAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setDestinationAddressReplySide(secondPart);
                                    }
                                } else if ("dport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationPortOriginalSide() <= 0) {
                                        ipConntrack.setDestinationPortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setDestinationPortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if (ipConntrack.getPackets() <= 0l &&
                                        "packets".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setPackets(Long.parseLong(secondPart));
                                } else if (ipConntrack.getBytes() <= 0l &&
                                        "bytes".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setBytes(Long.parseLong(secondPart));
                                } else if (ipConntrack.getStructUseCount() <= 0 &&
                                        "use".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setStructUseCount(Integer.parseInt(secondPart));
                                }
                            } else if (StringUtils.startsWith(sequence, "[")) {
                                ipConntrack.setAssured(StringUtils.containsIgnoreCase(sequence, "assured"));
                            }
                        }
                    }
                    break;
                case "ICMP":
                    if (toList.size() >= 4) {
                        for (int i = 3; i < toList.size(); i++) {
                            final String sequence = toList.get(i);
                            if (sequence == null || sequence.isEmpty()) {
                                continue;
                            }
                            final List<String> stringList = EQUALS_SPLITTER.splitToList(sequence);
                            if (stringList != null && stringList.size() >= 2) {
                                final String firstPart = stringList.get(0);
                                final String secondPart = stringList.get(1);
                                if ("src".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourceAddressOriginalSide() == null) {
                                        ipConntrack.setSourceAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setSourceAddressReplySide(secondPart);
                                    }
                                } else if ("sport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourcePortOriginalSide() <= 0) {
                                        ipConntrack.setSourcePortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setSourcePortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if ("dst".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationAddressOriginalSide() == null) {
                                        ipConntrack.setDestinationAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setDestinationAddressReplySide(secondPart);
                                    }
                                } else if ("dport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationPortOriginalSide() <= 0) {
                                        ipConntrack.setDestinationPortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setDestinationPortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if (ipConntrack.getPackets() <= 0l &&
                                        "packets".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setPackets(Long.parseLong(secondPart));
                                } else if (ipConntrack.getBytes() <= 0l &&
                                        "bytes".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setBytes(Long.parseLong(secondPart));
                                } else if (ipConntrack.getStructUseCount() <= 0 &&
                                        "use".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setStructUseCount(Integer.parseInt(secondPart));
                                } else if (ipConntrack.getIcmpId() == null &&
                                        "id".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setIcmpId(secondPart);
                                } else if (ipConntrack.getIcmpType() <= 0 &&
                                        "type".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setIcmpType(Integer.parseInt(secondPart));
                                } else if (ipConntrack.getIcmpCode() <= 0 &&
                                        "code".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setIcmpCode(Integer.parseInt(secondPart));
                                }
                            } else if (StringUtils.startsWith(sequence, "[")) {
                                ipConntrack.setAssured(StringUtils.containsIgnoreCase(sequence, "assured"));
                            }
                        }
                    }
                    break;
                default:
                    if (toList.size() >= 4) {
                        for (int i = 3; i < toList.size(); i++) {
                            final String sequence = toList.get(i);
                            if (sequence == null || sequence.isEmpty()) {
                                continue;
                            }
                            final List<String> stringList = EQUALS_SPLITTER.splitToList(sequence);
                            if (stringList != null && stringList.size() >= 2) {
                                final String firstPart = stringList.get(0);
                                final String secondPart = stringList.get(1);
                                if ("src".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourceAddressOriginalSide() == null) {
                                        ipConntrack.setSourceAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setSourceAddressReplySide(secondPart);
                                    }
                                } else if ("sport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getSourcePortOriginalSide() <= 0) {
                                        ipConntrack.setSourcePortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setSourcePortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if ("dst".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationAddressOriginalSide() == null) {
                                        ipConntrack.setDestinationAddressOriginalSide(secondPart);
                                    } else {
                                        ipConntrack.setDestinationAddressReplySide(secondPart);
                                    }
                                } else if ("dport".equalsIgnoreCase(firstPart)) {
                                    if (ipConntrack.getDestinationPortOriginalSide() <= 0) {
                                        ipConntrack.setDestinationPortOriginalSide(Integer.parseInt(secondPart));
                                    } else {
                                        ipConntrack.setDestinationPortReplySide(Integer.parseInt(secondPart));
                                    }
                                } else if (ipConntrack.getPackets() <= 0l &&
                                        "packets".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setPackets(Long.parseLong(secondPart));
                                } else if (ipConntrack.getBytes() <= 0l &&
                                        "bytes".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setBytes(Long.parseLong(secondPart));
                                } else if (ipConntrack.getStructUseCount() <= 0 &&
                                        "use".equalsIgnoreCase(firstPart)) {
                                    ipConntrack.setStructUseCount(Integer.parseInt(secondPart));
                                }
                            } else if (StringUtils.startsWith(sequence, "[")) {
                                ipConntrack.setAssured(StringUtils.containsIgnoreCase(sequence, "assured"));
                            }
                        }
                    }
                    break;
            }

        } catch (final Exception e) {
            Utils.reportException(new IllegalStateException("Error when parsing IP Conntrack row: " + row,
                    e));
            return null;
        }

        return ipConntrack;
    }

    @Nullable
    public String getIcmpId() {
        return icmpId;
    }

    public void setIcmpId(@Nullable String icmpId) {
        this.icmpId = icmpId;
    }

    public int getIcmpType() {
        return icmpType;
    }

    public void setIcmpType(int icmpType) {
        this.icmpType = icmpType;
    }

    public int getIcmpCode() {
        return icmpCode;
    }

    public void setIcmpCode(int icmpCode) {
        this.icmpCode = icmpCode;
    }

    @Nullable
    public String getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(@Nullable String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Nullable
    public String getTcpConnectionState() {
        return tcpConnectionState;
    }

    public void setTcpConnectionState(@Nullable String tcpConnectionState) {
        this.tcpConnectionState = tcpConnectionState;
    }

    @Nullable
    public String getSourceAddressOriginalSide() {
        return sourceAddressOriginalSide;
    }

    public void setSourceAddressOriginalSide(@Nullable String sourceAddressOriginalSide) {
        this.sourceAddressOriginalSide = sourceAddressOriginalSide;
    }

    @Nullable
    public String getDestinationAddressOriginalSide() {
        return destinationAddressOriginalSide;
    }

    public void setDestinationAddressOriginalSide(@Nullable String destinationAddressOriginalSide) {
        this.destinationAddressOriginalSide = destinationAddressOriginalSide;
    }

    public int getSourcePortOriginalSide() {
        return sourcePortOriginalSide;
    }

    public void setSourcePortOriginalSide(int sourcePortOriginalSide) {
        this.sourcePortOriginalSide = sourcePortOriginalSide;
    }

    public int getDestinationPortOriginalSide() {
        return destinationPortOriginalSide;
    }

    public void setDestinationPortOriginalSide(int destinationPortOriginalSide) {
        this.destinationPortOriginalSide = destinationPortOriginalSide;
    }

    public boolean isHasSeenTrafficInBothDirections() {
        return hasSeenTrafficInBothDirections;
    }

    public void setHasSeenTrafficInBothDirections(boolean hasSeenTrafficInBothDirections) {
        this.hasSeenTrafficInBothDirections = hasSeenTrafficInBothDirections;
    }

    @Nullable
    public String getSourceAddressReplySide() {
        return sourceAddressReplySide;
    }

    public void setSourceAddressReplySide(@Nullable String sourceAddressReplySide) {
        this.sourceAddressReplySide = sourceAddressReplySide;
    }

    @Nullable
    public String getDestinationAddressReplySide() {
        return destinationAddressReplySide;
    }

    public void setDestinationAddressReplySide(@Nullable String destinationAddressReplySide) {
        this.destinationAddressReplySide = destinationAddressReplySide;
    }

    public int getSourcePortReplySide() {
        return sourcePortReplySide;
    }

    public void setSourcePortReplySide(int sourcePortReplySide) {
        this.sourcePortReplySide = sourcePortReplySide;
    }

    public int getDestinationPortReplySide() {
        return destinationPortReplySide;
    }

    public void setDestinationPortReplySide(int destinationPortReplySide) {
        this.destinationPortReplySide = destinationPortReplySide;
    }

    public boolean isAssured() {
        return assured;
    }

    public void setAssured(boolean assured) {
        this.assured = assured;
    }

    public int getStructUseCount() {
        return structUseCount;
    }

    public void setStructUseCount(int structUseCount) {
        this.structUseCount = structUseCount;
    }

    public long getPackets() {
        return packets;
    }

    public void setPackets(long packets) {
        this.packets = packets;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @Nullable
    public String getSourceHostname() {
        return sourceHostname;
    }

    public void setSourceHostname(@Nullable String sourceHostname) {
        this.sourceHostname = sourceHostname;
    }

    @Nullable
    public String getDestWhoisOrHostname() {
        return destWhoisOrHostname;
    }

    public void setDestWhoisOrHostname(@Nullable String destWhoisOrHostname) {
        this.destWhoisOrHostname = destWhoisOrHostname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final IPConntrack that = (IPConntrack) o;

        if (assured != that.assured) return false;
        if (bytes != that.bytes) return false;
        if (destinationPortOriginalSide != that.destinationPortOriginalSide) return false;
        if (destinationPortReplySide != that.destinationPortReplySide) return false;
        if (hasSeenTrafficInBothDirections != that.hasSeenTrafficInBothDirections) return false;
        if (icmpCode != that.icmpCode) return false;
        if (icmpType != that.icmpType) return false;
        if (packets != that.packets) return false;
        if (sourcePortOriginalSide != that.sourcePortOriginalSide) return false;
        if (sourcePortReplySide != that.sourcePortReplySide) return false;
        if (structUseCount != that.structUseCount) return false;
        if (timeout != that.timeout) return false;
        if (destWhoisOrHostname != null ? !destWhoisOrHostname.equals(that.destWhoisOrHostname) : that.destWhoisOrHostname != null)
            return false;
        if (destinationAddressOriginalSide != null ? !destinationAddressOriginalSide.equals(that.destinationAddressOriginalSide) : that.destinationAddressOriginalSide != null)
            return false;
        if (destinationAddressReplySide != null ? !destinationAddressReplySide.equals(that.destinationAddressReplySide) : that.destinationAddressReplySide != null)
            return false;
        if (icmpId != null ? !icmpId.equals(that.icmpId) : that.icmpId != null) return false;
        if (sourceAddressOriginalSide != null ? !sourceAddressOriginalSide.equals(that.sourceAddressOriginalSide) : that.sourceAddressOriginalSide != null)
            return false;
        if (sourceAddressReplySide != null ? !sourceAddressReplySide.equals(that.sourceAddressReplySide) : that.sourceAddressReplySide != null)
            return false;
        if (sourceHostname != null ? !sourceHostname.equals(that.sourceHostname) : that.sourceHostname != null)
            return false;
        if (tcpConnectionState != null ? !tcpConnectionState.equals(that.tcpConnectionState) : that.tcpConnectionState != null)
            return false;
        if (transportProtocol != null ? !transportProtocol.equals(that.transportProtocol) : that.transportProtocol != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = transportProtocol != null ? transportProtocol.hashCode() : 0;
        result = 31 * result + (sourceHostname != null ? sourceHostname.hashCode() : 0);
        result = 31 * result + (destWhoisOrHostname != null ? destWhoisOrHostname.hashCode() : 0);
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        result = 31 * result + (tcpConnectionState != null ? tcpConnectionState.hashCode() : 0);
        result = 31 * result + (sourceAddressOriginalSide != null ? sourceAddressOriginalSide.hashCode() : 0);
        result = 31 * result + (destinationAddressOriginalSide != null ? destinationAddressOriginalSide.hashCode() : 0);
        result = 31 * result + sourcePortOriginalSide;
        result = 31 * result + destinationPortOriginalSide;
        result = 31 * result + (hasSeenTrafficInBothDirections ? 1 : 0);
        result = 31 * result + (sourceAddressReplySide != null ? sourceAddressReplySide.hashCode() : 0);
        result = 31 * result + (destinationAddressReplySide != null ? destinationAddressReplySide.hashCode() : 0);
        result = 31 * result + sourcePortReplySide;
        result = 31 * result + destinationPortReplySide;
        result = 31 * result + (assured ? 1 : 0);
        result = 31 * result + structUseCount;
        result = 31 * result + (int) (packets ^ (packets >>> 32));
        result = 31 * result + (int) (bytes ^ (bytes >>> 32));
        result = 31 * result + (icmpId != null ? icmpId.hashCode() : 0);
        result = 31 * result + icmpType;
        result = 31 * result + icmpCode;
        return result;
    }

    @Override
    public String toString() {
        return "IPConntrack{" +
                "transportProtocol='" + transportProtocol + '\'' +
                ", sourceHostname='" + sourceHostname + '\'' +
                ", destWhoisOrHostname='" + destWhoisOrHostname + '\'' +
                ", timeout=" + timeout +
                ", tcpConnectionState='" + tcpConnectionState + '\'' +
                ", sourceAddressOriginalSide='" + sourceAddressOriginalSide + '\'' +
                ", destinationAddressOriginalSide='" + destinationAddressOriginalSide + '\'' +
                ", sourcePortOriginalSide=" + sourcePortOriginalSide +
                ", destinationPortOriginalSide=" + destinationPortOriginalSide +
                ", hasSeenTrafficInBothDirections=" + hasSeenTrafficInBothDirections +
                ", sourceAddressReplySide='" + sourceAddressReplySide + '\'' +
                ", destinationAddressReplySide='" + destinationAddressReplySide + '\'' +
                ", sourcePortReplySide=" + sourcePortReplySide +
                ", destinationPortReplySide=" + destinationPortReplySide +
                ", assured=" + assured +
                ", structUseCount=" + structUseCount +
                ", packets=" + packets +
                ", bytes=" + bytes +
                ", icmpId='" + icmpId + '\'' +
                ", icmpType=" + icmpType +
                ", icmpCode=" + icmpCode +
                '}';
    }
}
