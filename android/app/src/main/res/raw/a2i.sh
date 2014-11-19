#!/bin/sh
#
# The MIT License (MIT)
#
# Copyright (c) 2014 Armel S.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

arp=/tmp/arp.$$
ipt=/tmp/ipt.$$
trap "rm -f $arp $ipt" EXIT
iptables -L counters -n | awk '{if ($4=="0.0.0.0/0") print $5}' > $ipt
grep 0x /proc/net/arp | awk '{print $1}' > $arp
for ip in $(cat $arp)
do
    grep "^$ip\$" $ipt > /dev/null
    if [ $? -ne 0 ]
    then
        echo adding $ip
        iptables -A counters -s $ip -j RETURN
        iptables -A counters -d $ip -j RETURN
    fi
done
for ip in $(cat $ipt)
do
    grep "^$ip\$" $arp > /dev/null
    if [ $? -ne 0 ]
    then
        echo deleting $ip
        iptables -D counters -s $ip -j RETURN
        iptables -D counters -d $ip -j RETURN
    fi
done

