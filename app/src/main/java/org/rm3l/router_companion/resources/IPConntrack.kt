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

import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.base.Strings.nullToEmpty
import org.rm3l.router_companion.utils.ReportingUtils
import java.util.Locale

class IPConntrack {

    private var rawLine: String? = null

    /**
     * Protocol name and number
     */
    var transportProtocol: String? = null

    var sourceHostname: String? = null

    var destWhoisOrHostname: String? = null

    /**
     * Seconds until this entry expires.
     */
    var timeout: Long = 0

    /**
     * TCP only: TCP connection state.
     */
    var tcpConnectionState: String? = null

    /**
     * Source address of “original”-side packets (packets from the side
     * that initiated the connection).
     */
    var sourceAddressOriginalSide: String? = null

    /**
     * Destination address of original-side packets.
     */
    var destinationAddressOriginalSide: String? = null

    /**
     * Source port of original-side packets.
     */
    var sourcePortOriginalSide: Int = 0

    /**
     * Destination port of original-side packets.
     */
    var destinationPortOriginalSide: Int = 0

    /**
     * “[UNREPLIED]”, if this connection has not seen traffic in both directions.
     * Otherwise not present.
     */
    var isHasSeenTrafficInBothDirections: Boolean = false

    /**
     * Source address of “reply”-side packets (packets from the side that
     * received the connection).
     */
    var sourceAddressReplySide: String? = null

    /**
     * Destination address of reply-side packets.
     */
    var destinationAddressReplySide: String? = null

    /**
     * Source port of reply-side packets.
     */
    var sourcePortReplySide: Int = 0

    /**
     * Destination port of reply-side packets.
     */
    var destinationPortReplySide: Int = 0

    /**
     * “[ASSURED]”, if this connection has seen traffic in both directions (for UDP)
     * or an ACK in an ESTABLISHED connection (for TCP). Otherwise not present.
     */
    var isAssured: Boolean = false

    /**
     * Use count of this connection structure.
     */
    var structUseCount: Int = 0

    var packets: Long = 0

    var bytes: Long = 0

    var icmpId: String? = null

    var icmpType: Int = 0

    var icmpCode: Int = 0

    fun getRawLine(): String? {
        return rawLine
    }

    fun setRawLine(rawLine: String?): IPConntrack {
        this.rawLine = rawLine
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as IPConntrack?

        if (isAssured != that!!.isAssured) return false
        if (bytes != that.bytes) return false
        if (destinationPortOriginalSide != that.destinationPortOriginalSide) return false
        if (destinationPortReplySide != that.destinationPortReplySide) return false
        if (isHasSeenTrafficInBothDirections != that.isHasSeenTrafficInBothDirections) return false
        if (icmpCode != that.icmpCode) return false
        if (icmpType != that.icmpType) return false
        if (packets != that.packets) return false
        if (sourcePortOriginalSide != that.sourcePortOriginalSide) return false
        if (sourcePortReplySide != that.sourcePortReplySide) return false
        if (structUseCount != that.structUseCount) return false
        if (timeout != that.timeout) return false
        if (if (destWhoisOrHostname != null)
                destWhoisOrHostname != that.destWhoisOrHostname
            else
                that.destWhoisOrHostname != null
        ) {
            return false
        }
        if (if (destinationAddressOriginalSide != null)
                destinationAddressOriginalSide != that.destinationAddressOriginalSide
            else
                that.destinationAddressOriginalSide != null
        ) {
            return false
        }
        if (if (destinationAddressReplySide != null)
                destinationAddressReplySide != that.destinationAddressReplySide
            else
                that.destinationAddressReplySide != null
        ) {
            return false
        }
        if (if (icmpId != null) icmpId != that.icmpId else that.icmpId != null) return false
        if (if (sourceAddressOriginalSide != null)
                sourceAddressOriginalSide != that.sourceAddressOriginalSide
            else
                that.sourceAddressOriginalSide != null
        ) {
            return false
        }
        if (if (sourceAddressReplySide != null)
                sourceAddressReplySide != that.sourceAddressReplySide
            else
                that.sourceAddressReplySide != null
        ) {
            return false
        }
        if (if (sourceHostname != null)
                sourceHostname != that.sourceHostname
            else
                that.sourceHostname != null
        ) {
            return false
        }
        if (if (tcpConnectionState != null)
                tcpConnectionState != that.tcpConnectionState
            else
                that.tcpConnectionState != null
        ) {
            return false
        }
        if (if (transportProtocol != null)
                transportProtocol != that.transportProtocol
            else
                that.transportProtocol != null
        ) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = if (transportProtocol != null) transportProtocol!!.hashCode() else 0
        result = 31 * result + if (sourceHostname != null) sourceHostname!!.hashCode() else 0
        result = 31 * result + if (destWhoisOrHostname != null) destWhoisOrHostname!!.hashCode() else 0
        result = 31 * result + (timeout xor timeout.ushr(32)).toInt()
        result = 31 * result + if (tcpConnectionState != null) tcpConnectionState!!.hashCode() else 0
        result = 31 * result + if (sourceAddressOriginalSide != null)
            sourceAddressOriginalSide!!.hashCode()
        else
            0
        result = 31 * result + if (destinationAddressOriginalSide != null)
            destinationAddressOriginalSide!!.hashCode()
        else
            0
        result = 31 * result + sourcePortOriginalSide
        result = 31 * result + destinationPortOriginalSide
        result = 31 * result + if (isHasSeenTrafficInBothDirections) 1 else 0
        result = 31 * result + if (sourceAddressReplySide != null) sourceAddressReplySide!!.hashCode() else 0
        result = 31 * result + if (destinationAddressReplySide != null)
            destinationAddressReplySide!!.hashCode()
        else
            0
        result = 31 * result + sourcePortReplySide
        result = 31 * result + destinationPortReplySide
        result = 31 * result + if (isAssured) 1 else 0
        result = 31 * result + structUseCount
        result = 31 * result + (packets xor packets.ushr(32)).toInt()
        result = 31 * result + (bytes xor bytes.ushr(32)).toInt()
        result = 31 * result + if (icmpId != null) icmpId!!.hashCode() else 0
        result = 31 * result + icmpType
        result = 31 * result + icmpCode
        return result
    }

    override fun toString(): String {
        return "IPConntrack{transportProtocol='$transportProtocol', sourceHostname='$sourceHostname', destWhoisOrHostname='$destWhoisOrHostname', timeout=$timeout, tcpConnectionState='$tcpConnectionState', sourceAddressOriginalSide='$sourceAddressOriginalSide', destinationAddressOriginalSide='$destinationAddressOriginalSide', sourcePortOriginalSide=$sourcePortOriginalSide, destinationPortOriginalSide=$destinationPortOriginalSide, hasSeenTrafficInBothDirections=$isHasSeenTrafficInBothDirections, sourceAddressReplySide='$sourceAddressReplySide', destinationAddressReplySide='$destinationAddressReplySide', sourcePortReplySide=$sourcePortReplySide, destinationPortReplySide=$destinationPortReplySide, assured=$isAssured, structUseCount=$structUseCount, packets=$packets, bytes=$bytes, icmpId='$icmpId', icmpType=$icmpType, icmpCode=$icmpCode}"
    }

    companion object {

        val PARSER_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults()
        val EQUALS_SPLITTER = Splitter.on("=")

        fun parseIpConntrackRow(raw: String): IPConntrack? {
            if (Strings.isNullOrEmpty(raw)) {
                return null
            }

            val toList = PARSER_SPLITTER.splitToList(raw)
            if (toList == null || toList.isEmpty()) {
                return null
            }

            val ipConntrack = IPConntrack()

            ipConntrack.rawLine = raw

            try {

                val proto = nullToEmpty(toList[0]).uppercase(Locale.getDefault())
                ipConntrack.transportProtocol = proto
                if (toList.size >= 3) {
                    ipConntrack.timeout = java.lang.Long.parseLong(toList[2])
                }
                when (proto) {
                    "TCP" -> {
                        if (toList.size >= 4) {
                            ipConntrack.tcpConnectionState = toList[3]
                        }
                        if (toList.size >= 5) {
                            for (i in 4..toList.size - 1) {
                                val sequence = toList[i]
                                if (sequence == null || sequence.isEmpty()) {
                                    continue
                                }
                                val stringList = EQUALS_SPLITTER.splitToList(sequence)
                                if (stringList != null && stringList.size >= 2) {
                                    val firstPart = stringList[0]
                                    val secondPart = stringList[1]
                                    if ("src".equals(firstPart, ignoreCase = true)) {
                                        if (ipConntrack.sourceAddressOriginalSide == null) {
                                            ipConntrack.sourceAddressOriginalSide = secondPart
                                        } else {
                                            ipConntrack.sourceAddressReplySide = secondPart
                                        }
                                    } else if ("sport".equals(firstPart, ignoreCase = true)) {
                                        if (ipConntrack.sourcePortOriginalSide <= 0) {
                                            ipConntrack.sourcePortOriginalSide = Integer.parseInt(secondPart)
                                        } else {
                                            ipConntrack.sourcePortReplySide = Integer.parseInt(secondPart)
                                        }
                                    } else if ("dst".equals(firstPart, ignoreCase = true)) {
                                        if (ipConntrack.destinationAddressOriginalSide == null) {
                                            ipConntrack.destinationAddressOriginalSide = secondPart
                                        } else {
                                            ipConntrack.destinationAddressReplySide = secondPart
                                        }
                                    } else if ("dport".equals(firstPart, ignoreCase = true)) {
                                        if (ipConntrack.destinationPortOriginalSide <= 0) {
                                            ipConntrack.destinationPortOriginalSide = Integer.parseInt(secondPart)
                                        } else {
                                            ipConntrack.destinationPortReplySide = Integer.parseInt(secondPart)
                                        }
                                    } else if (ipConntrack.packets <= 0L && "packets".equals(
                                            firstPart, ignoreCase = true
                                        )
                                    ) {
                                        ipConntrack.packets = java.lang.Long.parseLong(secondPart)
                                    } else if (ipConntrack.bytes <= 0L && "bytes".equals(
                                            firstPart,
                                            ignoreCase = true
                                        )
                                    ) {
                                        ipConntrack.bytes = java.lang.Long.parseLong(secondPart)
                                    } else if (ipConntrack.structUseCount <= 0 && "use".equals(
                                            firstPart, ignoreCase = true
                                        )
                                    ) {
                                        ipConntrack.structUseCount = Integer.parseInt(secondPart)
                                    }
                                } else if (sequence.startsWith("[")) {
                                    ipConntrack.isAssured = sequence.contains("assured", ignoreCase = true)
                                }
                            }
                        }
                    }
                    "UDP" -> if (toList.size >= 4) {
                        for (i in 3..toList.size - 1) {
                            val sequence = toList[i]
                            if (sequence == null || sequence.isEmpty()) {
                                continue
                            }
                            val stringList = EQUALS_SPLITTER.splitToList(sequence)
                            if (stringList != null && stringList.size >= 2) {
                                val firstPart = stringList[0]
                                val secondPart = stringList[1]
                                if ("src".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.sourceAddressOriginalSide == null) {
                                        ipConntrack.sourceAddressOriginalSide = secondPart
                                    } else {
                                        ipConntrack.sourceAddressReplySide = secondPart
                                    }
                                } else if ("sport".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.sourcePortOriginalSide <= 0) {
                                        ipConntrack.sourcePortOriginalSide = Integer.parseInt(secondPart)
                                    } else {
                                        ipConntrack.sourcePortReplySide = Integer.parseInt(secondPart)
                                    }
                                } else if ("dst".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.destinationAddressOriginalSide == null) {
                                        ipConntrack.destinationAddressOriginalSide = secondPart
                                    } else {
                                        ipConntrack.destinationAddressReplySide = secondPart
                                    }
                                } else if ("dport".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.destinationPortOriginalSide <= 0) {
                                        ipConntrack.destinationPortOriginalSide = Integer.parseInt(secondPart)
                                    } else {
                                        ipConntrack.destinationPortReplySide = Integer.parseInt(secondPart)
                                    }
                                } else if (ipConntrack.packets <= 0L && "packets".equals(
                                        firstPart, ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.packets = java.lang.Long.parseLong(secondPart)
                                } else if (ipConntrack.bytes <= 0L && "bytes".equals(
                                        firstPart,
                                        ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.bytes = java.lang.Long.parseLong(secondPart)
                                } else if (ipConntrack.structUseCount <= 0 && "use".equals(
                                        firstPart, ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.structUseCount = Integer.parseInt(secondPart)
                                }
                            } else if (sequence.startsWith("[")) {
                                ipConntrack.isAssured = sequence.contains("assured", ignoreCase = true)
                            }
                        }
                    }
                    "ICMP" -> if (toList.size >= 4) {
                        for (i in 3..toList.size - 1) {
                            val sequence = toList[i]
                            if (sequence == null || sequence.isEmpty()) {
                                continue
                            }
                            val stringList = EQUALS_SPLITTER.splitToList(sequence)
                            if (stringList != null && stringList.size >= 2) {
                                val firstPart = stringList[0]
                                val secondPart = stringList[1]
                                if ("src".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.sourceAddressOriginalSide == null) {
                                        ipConntrack.sourceAddressOriginalSide = secondPart
                                    } else {
                                        ipConntrack.sourceAddressReplySide = secondPart
                                    }
                                } else if ("sport".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.sourcePortOriginalSide <= 0) {
                                        ipConntrack.sourcePortOriginalSide = Integer.parseInt(secondPart)
                                    } else {
                                        ipConntrack.sourcePortReplySide = Integer.parseInt(secondPart)
                                    }
                                } else if ("dst".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.destinationAddressOriginalSide == null) {
                                        ipConntrack.destinationAddressOriginalSide = secondPart
                                    } else {
                                        ipConntrack.destinationAddressReplySide = secondPart
                                    }
                                } else if ("dport".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.destinationPortOriginalSide <= 0) {
                                        ipConntrack.destinationPortOriginalSide = Integer.parseInt(secondPart)
                                    } else {
                                        ipConntrack.destinationPortReplySide = Integer.parseInt(secondPart)
                                    }
                                } else if (ipConntrack.packets <= 0L && "packets".equals(
                                        firstPart, ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.packets = java.lang.Long.parseLong(secondPart)
                                } else if (ipConntrack.bytes <= 0L && "bytes".equals(
                                        firstPart,
                                        ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.bytes = java.lang.Long.parseLong(secondPart)
                                } else if (ipConntrack.structUseCount <= 0 && "use".equals(
                                        firstPart, ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.structUseCount = Integer.parseInt(secondPart)
                                } else if (ipConntrack.icmpId == null && "id".equals(
                                        firstPart,
                                        ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.icmpId = secondPart
                                } else if (ipConntrack.icmpType <= 0 && "type".equals(
                                        firstPart,
                                        ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.icmpType = Integer.parseInt(secondPart)
                                } else if (ipConntrack.icmpCode <= 0 && "code".equals(
                                        firstPart,
                                        ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.icmpCode = Integer.parseInt(secondPart)
                                }
                            } else if (sequence.startsWith("[")) {
                                ipConntrack.isAssured = sequence.contains("assured", ignoreCase = true)
                            }
                        }
                    }
                    else -> if (toList.size >= 4) {
                        for (i in 3..toList.size - 1) {
                            val sequence = toList[i]
                            if (sequence == null || sequence.isEmpty()) {
                                continue
                            }
                            val stringList = EQUALS_SPLITTER.splitToList(sequence)
                            if (stringList != null && stringList.size >= 2) {
                                val firstPart = stringList[0]
                                val secondPart = stringList[1]
                                if ("src".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.sourceAddressOriginalSide == null) {
                                        ipConntrack.sourceAddressOriginalSide = secondPart
                                    } else {
                                        ipConntrack.sourceAddressReplySide = secondPart
                                    }
                                } else if ("sport".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.sourcePortOriginalSide <= 0) {
                                        ipConntrack.sourcePortOriginalSide = Integer.parseInt(secondPart)
                                    } else {
                                        ipConntrack.sourcePortReplySide = Integer.parseInt(secondPart)
                                    }
                                } else if ("dst".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.destinationAddressOriginalSide == null) {
                                        ipConntrack.destinationAddressOriginalSide = secondPart
                                    } else {
                                        ipConntrack.destinationAddressReplySide = secondPart
                                    }
                                } else if ("dport".equals(firstPart, ignoreCase = true)) {
                                    if (ipConntrack.destinationPortOriginalSide <= 0) {
                                        ipConntrack.destinationPortOriginalSide = Integer.parseInt(secondPart)
                                    } else {
                                        ipConntrack.destinationPortReplySide = Integer.parseInt(secondPart)
                                    }
                                } else if (ipConntrack.packets <= 0L && "packets".equals(
                                        firstPart, ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.packets = java.lang.Long.parseLong(secondPart)
                                } else if (ipConntrack.bytes <= 0L && "bytes".equals(
                                        firstPart,
                                        ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.bytes = java.lang.Long.parseLong(secondPart)
                                } else if (ipConntrack.structUseCount <= 0 && "use".equals(
                                        firstPart, ignoreCase = true
                                    )
                                ) {
                                    ipConntrack.structUseCount = Integer.parseInt(secondPart)
                                }
                            } else if (sequence.startsWith("[")) {
                                ipConntrack.isAssured = sequence.contains("assured", ignoreCase = true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                ReportingUtils.reportException(
                    null,
                    IllegalStateException("Error when parsing IP Conntrack raw: " + raw, e)
                )
                return null
            }

            return ipConntrack
        }
    }
}
