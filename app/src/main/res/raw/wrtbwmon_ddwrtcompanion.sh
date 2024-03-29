#!/bin/sh
#
# Traffic logging tool for OpenWRT-based routers
#
# Created by Emmanuel Brucy (e.brucy AT qut.edu.au)
#
# Based on work from Fredrik Erlandsson (erlis AT linux.nu)
# Based on traff_graph script by twist - http://wiki.openwrt.org/RrdTrafficWatch
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
# Modified by Chee Kok Aun (RemoveThisSpamProtectionwolfkodi AT gmail DOT com)
# to calculate live traffic in 10 seconds intervals.
#
# Adapted for DD-WRT Companion App by Armel S. <armel+router_companion@rm3l.org>
#

LAN_IFACE=$(nvram get lan_ifname)
SLEEP_TIME=1

case ${1} in

"setup" )

    insmod xt_mac #k2.6
    insmod ipt_mac #k2.4

    #
    # Create WANAccess Chain too and add it to the FORWARD Chain
    #
	iptables -N DDWRTCompWANAccess 2> /dev/null
	iptables -L FORWARD --line-numbers -n | grep "DDWRTCompWANAccess" | grep "2" > /dev/null
	if [ $? -ne 0 ]; then
	    iptables -L FORWARD -n | grep "DDWRTCompWANAccess" > /dev/null
	    if [ $? -eq 0 ]; then
			echo "DEBUG : DDWRTCompWANAccess iptables chain misplaced, recreating it..."
			iptables -D FORWARD -j DDWRTCompWANAccess
        fi
        iptables -I FORWARD -j DDWRTCompWANAccess
    fi

    #
	#Create the DDWRTCompanion CHAIN (it doesn't matter if it already exists).
	#
	iptables -N DDWRTCompanion 2> /dev/null

	#Add the DDWRTCompanion CHAIN to the FORWARD chain (if non existing).
	iptables -L FORWARD --line-numbers -n | grep "DDWRTCompanion" | grep "1" > /dev/null
	if [ $? -ne 0 ]; then
		iptables -L FORWARD -n | grep "DDWRTCompanion" > /dev/null
		if [ $? -eq 0 ]; then
			echo "DEBUG : iptables chain misplaced, recreating it..."
			iptables -D FORWARD -j DDWRTCompanion
		fi
		iptables -I FORWARD -j DDWRTCompanion
	fi

	#For each host in the ARP table
	grep ${LAN_IFACE} /proc/net/arp | while read IP TYPE FLAGS MAC MASK IFACE
	do
		#Add iptable rules (if non existing).
		iptables -nL DDWRTCompanion | grep "${IP} " > /dev/null
		if [ $? -ne 0 ]; then
			iptables -I DDWRTCompanion -d ${IP} -j RETURN
			iptables -I DDWRTCompanion -s ${IP} -j RETURN
		fi
	done

	;;

"read" )

	#Read counters
	iptables -L DDWRTCompanion -vnx > /tmp/.DDWRTCompanion_traffic_55.tmp
	;;

"update" )

	[ -z "${2}" ] && echo "ERROR : Missing argument 2" && exit 1
	[ -z "${3}" ] && echo "ERROR : Missing argument 3" && exit 1

	# Uncomment this line if you want to abort if database not found
	# [ -f "${2}" ] || exit 1

	#Read and reset counters
	iptables -L DDWRTCompanion -vnxZ > /tmp/.DDWRTCompanion_traffic_66.tmp

	grep -v "0x0" /proc/net/arp  | while read IP TYPE FLAGS MAC MASK IFACE
	do
		#Add new data to the graph. Count in Kbs to deal with 16 bits signed values (up to 2G only)
		#Have to use temporary files because of crappy busybox shell
		grep ${IP} /tmp/.DDWRTCompanion_traffic_55.tmp | while read PKTS BYTES TARGET PROT OPT IFIN IFOUT SRC DST
		do
			[ "${DST}" = "${IP}" ] && echo $((${BYTES}/1000)) > /tmp/.DDWRTCompanion_in_$$.tmp
			[ "${SRC}" = "${IP}" ] && echo $((${BYTES}/1000)) > /tmp/.DDWRTCompanion_out_$$.tmp
		done
		IN=$(cat /tmp/.DDWRTCompanion_in_$$.tmp)
		OUT=$(cat /tmp/.DDWRTCompanion_out_$$.tmp)
		rm -f /tmp/.DDWRTCompanion_in_$$.tmp
		rm -f /tmp/.DDWRTCompanion_out_$$.tmp
		
		if [ ${IN} -gt 0 -o ${OUT} -gt 0 ];  then
			#echo "DEBUG : New traffic for ${MAC} since last update : ${IN}k:${OUT}k"
			
			LINE=$(grep ${MAC} ${2})
			if [ -z "${LINE}" ]; then
				#echo "DEBUG : ${MAC} is a new host !"
				PEAKUSAGE_IN=0
				PEAKUSAGE_OUT=0
				OFFPEAKUSAGE_IN=0
				OFFPEAKUSAGE_OUT=0
			else
				PEAKUSAGE_IN=$(echo ${LINE} | cut -f2 -s -d, )
				PEAKUSAGE_OUT=$(echo ${LINE} | cut -f3 -s -d, )
				OFFPEAKUSAGE_IN=$(echo ${LINE} | cut -f4 -s -d, )
				OFFPEAKUSAGE_OUT=$(echo ${LINE} | cut -f5 -s -d, )
			fi
			
			OFFPEAKUSAGE_IN=$((${OFFPEAKUSAGE_IN}+${IN}))
			OFFPEAKUSAGE_OUT=$((${OFFPEAKUSAGE_OUT}+${OUT}))
			
			grep -v "${MAC}" ${2} > /tmp/.DDWRTCompanion_db_$$.tmp
			mv /tmp/.DDWRTCompanion_db_$$.tmp ${2}
#			echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${OFFPEAKUSAGE_IN},${OFFPEAKUSAGE_OUT},$(date "+%Y-%m-%d %H:%M") >> ${2}
#            echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${OFFPEAKUSAGE_IN},${OFFPEAKUSAGE_OUT},$(date +"%s") >> ${2}
            echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${OFFPEAKUSAGE_IN},${OFFPEAKUSAGE_OUT},${3} >> ${2}
		fi
	done

	grep -v "0x0" /proc/net/arp  | while read IP TYPE FLAGS MAC MASK IFACE
	do
		#Add new data to the graph. Count in Kbs to deal with 16 bits signed values (up to 2G only)
		#Have to use temporary files because of crappy busybox shell
		grep ${IP} /tmp/.DDWRTCompanion_traffic_66.tmp | while read PKTS BYTES TARGET PROT OPT IFIN IFOUT SRC DST
		do
			[ "${DST}" = "${IP}" ] && echo $((${BYTES}/1000)) > /tmp/.DDWRTCompanion_in_$$.tmp
			[ "${SRC}" = "${IP}" ] && echo $((${BYTES}/1000)) > /tmp/.DDWRTCompanion_out_$$.tmp
		done
		IN=$(cat /tmp/.DDWRTCompanion_in_$$.tmp)
		OUT=$(cat /tmp/.DDWRTCompanion_out_$$.tmp)
		rm -f /tmp/.DDWRTCompanion_in_$$.tmp
		rm -f /tmp/.DDWRTCompanion_out_$$.tmp
		
		if [ ${IN} -gt 0 -o ${OUT} -gt 0 ];  then
			LINE=$(grep ${MAC} ${2})
			PEAKUSAGE_IN=$(echo ${LINE} | cut -f2 -s -d, )
			PEAKUSAGE_OUT=$(echo ${LINE} | cut -f3 -s -d, )
			OFFPEAKUSAGE_IN=$(echo ${LINE} | cut -f4 -s -d, )
			OFFPEAKUSAGE_OUT=$(echo ${LINE} | cut -f5 -s -d, )
			
			PEAKUSAGE_IN=$((${PEAKUSAGE_IN}+${IN}))
			PEAKUSAGE_OUT=$((${PEAKUSAGE_OUT}+${OUT}))
			
			grep -v "${MAC}" ${2} > /tmp/.DDWRTCompanion_db_$$.tmp
			mv /tmp/.DDWRTCompanion_db_$$.tmp ${2}
#			echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${OFFPEAKUSAGE_IN},${OFFPEAKUSAGE_OUT},$(date "+%Y-%m-%d %H:%M") >> ${2}
#			echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${OFFPEAKUSAGE_IN},${OFFPEAKUSAGE_OUT},$(date +"%s") >> ${2}
            echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${OFFPEAKUSAGE_IN},${OFFPEAKUSAGE_OUT},${3} >> ${2}
		fi
	done
	;;

"publish-raw" )

	[ -z "${2}" ] && echo "ERROR : Missing argument 2" && exit 1
	[ -z "${3}" ] && echo "ERROR : Missing argument 3" && exit 1

	USERSFILE="/etc/dnsmasq.conf"
	[ -f "${USERSFILE}" ] || USERSFILE="/tmp/dnsmasq.conf"
	[ -z "${4}" ] || USERSFILE=${4}
	[ -f "${USERSFILE}" ] || USERSFILE="/dev/null"

    # first do some number crunching - rewrite the database so that it is sorted
	touch /tmp/.DDWRTCompanion_sorted_$$.tmp
	cat ${2} | while IFS=, read MAC PEAKUSAGE_IN PEAKUSAGE_OUT OFFPEAKUSAGE_IN OFFPEAKUSAGE_OUT LASTSEEN
	do
		echo ${PEAKUSAGE_IN},${PEAKUSAGE_OUT},$(((${PEAKUSAGE_IN}-${OFFPEAKUSAGE_IN})/${SLEEP_TIME})),$(((${PEAKUSAGE_OUT}-${OFFPEAKUSAGE_OUT})/${SLEEP_TIME})),${MAC},${LASTSEEN} >> /tmp/.DDWRTCompanion_sorted_$$.tmp
	done

    cat /tmp/.DDWRTCompanion_sorted_$$.tmp | while IFS=, read PEAKUSAGE_IN PEAKUSAGE_OUT OFFPEAKUSAGE_IN OFFPEAKUSAGE_OUT MAC LASTSEEN
	do
	    echo ${MAC},${LASTSEEN},${PEAKUSAGE_IN}000,${PEAKUSAGE_OUT}000,${OFFPEAKUSAGE_IN}000,${OFFPEAKUSAGE_OUT}000 >> ${3}
	done

    #Free some memory
	rm -f /tmp/.DDWRTCompanion_sorted_$$.tmp

	#Make previous bandwidth values match the current
	touch /tmp/.DDWRTCompanion_matched_$$.tmp
	cat ${2} | while IFS=, read MAC PEAKUSAGE_IN PEAKUSAGE_OUT OFFPEAKUSAGE_IN OFFPEAKUSAGE_OUT LASTSEEN
	do
		echo ${MAC},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${PEAKUSAGE_IN},${PEAKUSAGE_OUT},${LASTSEEN} >> /tmp/.DDWRTCompanion_matched_$$.tmp
	done
	mv /tmp/.DDWRTCompanion_matched_$$.tmp ${2}

	;;


*)
	echo "Usage : $0 {setup|update|read|publish-raw} [options...]"
	echo "Options : "
	echo "   $0 setup"
	echo "   $0 read"
	echo "   $0 update database_file"
	echo "   $0 publish database_file path_of_html_report [user_file]"
	echo "Examples : "
	echo "   $0 setup"
	echo "   $0 read"
	echo "   $0 update /tmp/.DDWRTCompanion_usage.db"
	echo "   $0 publish-raw /tmp/.DDWRTCompanion_usage.db /www/user/usage.raw"
	exit
	;;
esac