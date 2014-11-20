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

# Perform various checks
echo "Performing basic commands checks..."
echo echo ok | echo || exit 1
echo cat ok | cat || exit 1
echo grep ok | grep grep || exit 1
echo cut seems to be ok | cut -d ' ' -f 1,5 || exit 1
echo sort ok | sort -nr || exit 1
echo "sed is not ok" | sed 's/is not //' || exit 1
echo awk is ok | awk '{if ($1="awk") print $1,$3}' || exit 1
echo Hello World | md5sum | sed 's/e59ff97941044f85df5297e1c302d260  -/md5sum ok/'
# trap?
echo -n "Now checking for lan2wan iptables chain..."
iptables -L FORWARD --line-numbers -n | grep lan2wan > /dev/null
if [ $? -ne 0 ]
then
    echo "... not found"
    iptables -L FORWARD --line-numbers | grep '^1' > /dev/null
    if [ $? -eq 0 ]
    then
        exit 1
    else
        echo "... Empty iptables FORWARD chain!"
    fi
else
	echo "...found"
fi
exit 0
