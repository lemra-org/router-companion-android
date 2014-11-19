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

insmod xt_mac #k2.6
insmod ipt_mac #k2.4
iptables -N wanaccess 2> /dev/null
n=$(iptables -L FORWARD --line-numbers -n | awk 'BEGIN {f=0;n=1}
$2=="wanaccess" {f=1;n=0}
f==0 && $2=="lan2wan" {f=1;n=$1+1}
f==0 && $2=="ACCEPT" && $5=="0.0.0.0/0" && $6=="0.0.0.0/0" && $7=="state" && $8=="RELATED,ESTABLISHED" {f=1;n=$1}
END {print n}')
if [ $n -ne 0 ]
then
    iptables -I FORWARD $n -j wanaccess
fi
iptables -N counters > /dev/null 2>&1
iptables -L FORWARD --line-numbers -n | grep counters > /dev/null 2>&1
if [ $? -ne 0 ]
then
    n=$(iptables -L FORWARD --line-numbers -n | grep wanaccess | cut -d ' ' -f 1)
    n=$(expr $n + 1)
    iptables -I FORWARD $n -j counters
fi
exit 0