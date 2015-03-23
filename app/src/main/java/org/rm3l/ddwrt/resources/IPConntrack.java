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
    @NonNull
    private TransportProtocol transportProtocol;

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
            ipConntrack.setTransportProtocol(TransportProtocol.valueOf(nullToEmpty(toList.get(0)).toUpperCase()));
            if (toList.size() >= 3) {
                ipConntrack.setTimeout(Long.parseLong(toList.get(2)));
            }
            if (toList.size() >= 4) {
                final String s = toList.get(3);
                if (!StringUtils.startsWithIgnoreCase(s, "src=")) {
                    //udp
                    ipConntrack.setTcpConnectionState(s);
                }
            }

            if (toList.size() >= 5) {
                final String s;
                if (ipConntrack.getTcpConnectionState() == null) {
                    //udp
                    s = toList.get(3);
                } else {
                    s = toList.get(4);
                }
                final List<String> stringList = EQUALS_SPLITTER.splitToList(s);
                if (stringList != null && stringList.size() >= 2) {
                    ipConntrack.setSourceAddressOriginalSide(stringList.get(1));
                }
            }

            if (toList.size() >= 6) {
                final String s;
                if (ipConntrack.getTcpConnectionState() == null) {
                    //udp
                    s = toList.get(4);
                } else {
                    s = toList.get(5);
                }
                final List<String> stringList = EQUALS_SPLITTER.splitToList(s);
                if (stringList != null && stringList.size() >= 2) {
                    ipConntrack.setDestinationAddressOriginalSide(stringList.get(1));
                }
            }

            if (toList.size() >= 7) {
                final String s;
                if (ipConntrack.getTcpConnectionState() == null) {
                    //udp
                    s = toList.get(5);
                } else {
                    s = toList.get(6);
                }
                final List<String> stringList = EQUALS_SPLITTER.splitToList(s);
                if (stringList != null && stringList.size() >= 2) {
                    ipConntrack.setSourcePortOriginalSide(Integer.parseInt(stringList.get(1)));
                }
            }

            if (toList.size() >= 8) {
                final String s;
                if (ipConntrack.getTcpConnectionState() == null) {
                    //udp
                    s = toList.get(6);
                } else {
                    s = toList.get(7);
                }
                final List<String> stringList = EQUALS_SPLITTER.splitToList(s);
                if (stringList != null && stringList.size() >= 2) {
                    ipConntrack.setDestinationPortOriginalSide(Integer.parseInt(stringList.get(1)));
                }
            }

            if (toList.size() >= 9) {
                final String s;
                if (ipConntrack.getTcpConnectionState() == null) {
                    //udp
                    s = toList.get(7);
                } else {
                    s = toList.get(8);
                }
                final List<String> stringList = EQUALS_SPLITTER.splitToList(s);
                if (stringList != null && stringList.size() >= 2) {
                    ipConntrack.setPackets(Long.parseLong(stringList.get(1)));
                }
            }

            if (toList.size() >= 10) {
                final String s;
                if (ipConntrack.getTcpConnectionState() == null) {
                    //udp
                    s = toList.get(8);
                } else {
                    s = toList.get(9);
                }
                final List<String> stringList = EQUALS_SPLITTER.splitToList(s);
                if (stringList != null && stringList.size() >= 2) {
                    ipConntrack.setBytes(Long.parseLong(stringList.get(1)));
                }
            }


        } catch (final Exception e) {
            Utils.reportException(new IllegalStateException("Error when parsing IP Conntrack row: " + row,
                    e));
            return null;
        }

        return ipConntrack;
    }

    @NonNull
    public TransportProtocol getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(@NonNull TransportProtocol transportProtocol) {
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

    public enum TransportProtocol {
        TCP("TCP", 6),
        UDP("UDP", 17);

        @NonNull
        private final String displayName;
        private final int protocolNumber;

        TransportProtocol(@NonNull final String displayName, final int protocolNumber) {
            this.displayName = displayName;
            this.protocolNumber = protocolNumber;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }
    }

}
