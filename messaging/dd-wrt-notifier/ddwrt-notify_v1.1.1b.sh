#!/bin/sh
#
# ddwrt-notify version 1.1.1b - 20160708 - max@fuckaround.org
#
#	- solved issue with auto-update local file
#	- added tmpfile
#	- added migration from old version
#	- bugfix
#
# ddwrt-notify version 1.0b - 20160708
#	- this script uses:
#	  lftp, awk and grep (you have to install them);
#     (optionally also mutt/mail, etc.)
#	
#   USAGE: ./ddwrt-notify.sh
#
#	first time the script download whole dir list from dd-wrt server.
#	From second time it compare if there're new version(s).
#	Feel free to put it in cron daemon ;-)
#
#	Comments, suggestions and other max@fuckaround.org
#	a feedback is appreciate :-O thanks!
#
############################################################
#	- DO NOT REMOVE "ftp_existent" file after created!!!   #
############################################################
###														 ###
### IF YOU LIKE MY WORK, GET ME A COFFEE or BEER ;-) !!! ###
### paypal: mmorelli1000@gmail.com 						 ###
###									         			 ###
###               THANKS!!! :-) 						 ###
############################################################
# ________________________________
#/                            _   \
#|  _| _|__  .__|_ ._  __|_o_|_   |
#| (_|(_|\/\/|  |_ | |(_)|_| |\/  |
#\                            /   /
# --------------------------------
#                       \                    ^    /^
#                        \                  / \  // \
#                         \   |\___/|      /   \//  .\
#                          \  /O  O  \__  /    //  | \ \           *----*
#                            /     /  \/_/    //   |  \  \          \   |
#                            @___@`    \/_   //    |   \   \         \/\ \
#                           0/0/|       \/_ //     |    \    \         \  \
#                       0/0/0/0/|        \///      |     \     \       |  |
#                    0/0/0/0/0/_|_ /   (  //       |      \     _\     |  /
#                 0/0/0/0/0/0/`/,_ _ _/  ) ; -.    |    _ _\.-~       /   /
#                             ,-}        _      *-.|.-~-.           .~    ~
#            \     \__/        `/\      /                 ~-. _ .-~      /
#             \____(oo)           *.   }            {                   /
#             (    (--)          .----~-.\        \-`                 .~
#             //__\\  \__ Ack!   ///.----..<        \             _ -~
#            //    \\               ///-._ _ _ _ _ _ _{^ - - - - ~
#
#
#
#

#
# Updated by rm3l for DD-WRT Companion
#

# Do you want notify by email? (yes / no)
notify=yes
# subject
sub="New DD-WRT release available!"

# FCM Topic
FCM_TOPIC="/topics/DDWRTBuildUpdates"

#
#
# END OF CONFIG #
#
#
tmpdir=/tmp
datadir=/data
#
# migrate from 1.0b to 1.1b
if [ -f ddwrt_ftp_existent ]
then echo "migrating in progress..." ; mv ddwrt_ftp_existent ${datadir}/.ddwrt_ftp_existent ; echo done
else echo
fi
#
if ! type "lftp" > /dev/null 2>&1
then echo "lftp not found. Please install it." ; exit ; fi

echo > $tmpdir/ddwrt_ftp_today
echo > $tmpdir/ddwrt_to_check

year=`date +"%Y"`

echo "retrieving from dd-wrt..."
lftp -p 21 -u anonymous,anonymous ftp://ftp.dd-wrt.com/betas/$year/ <<EOF
ls -t > $tmpdir/ddwrt_ftp_today
QUIT
EOF
echo done

awk '{print $NF}' $tmpdir/ddwrt_ftp_today > $tmpdir/ddwrt_ftp_today_tmp

if [ ! -f ${datadir}/.ddwrt_ftp_existent ];
then 
echo "This is first time you run the script. Initializing in progress..."
mv $tmpdir/ddwrt_ftp_today_tmp ${datadir}/.ddwrt_ftp_existent
rm $tmpdir/ddwrt_ftp_today $tmpdir/ddwrt_to_check
echo "Done! Execute the script to check dd-wrt updates."
exit
fi

grep -v -F -x -f ${datadir}/.ddwrt_ftp_existent $tmpdir/ddwrt_ftp_today_tmp > $tmpdir/ddwrt_to_check

#echo
#echo "*** ddwrt_ftp_existent ***"
#cat ${datadir}/.ddwrt_ftp_existent

#echo
#echo "*** ddwrt_ftp_today_tmp ***"
#cat $tmpdir/ddwrt_ftp_today_tmp
#echo
#echo

cp -r $tmpdir/ddwrt_ftp_today_tmp ${datadir}/.ddwrt_ftp_existent

if [[ ! -s $tmpdir/ddwrt_to_check ]]
then echo "No new releases found."
else
echo "Possible new release(s) found:"
cat $tmpdir/ddwrt_to_check
#cat $tmpdir/ddwrt_to_check >> ${datadir}/.ddwrt_ftp_existent

echo "Send dd-wrt notify report..."
echo "{"\
		"\"protocol\": \"HTTP\", "\
		"\"message\": { "\
			"\"to\": \"${FCM_TOPIC}\", "\
			"\"data\": { "\
				"\"message\": \"Possible new releases\", "\
				"\"releases\": \"`head -n 1 $tmpdir/ddwrt_to_check`\" "\
			"}"\
		"}"\
	"}" | python -c 'import json,sys; print json.dumps(json.loads(sys.stdin.read()))' > \
	${tmpdir}/ddwrt-notify-report-json.msg.json

cat $tmpdir/ddwrt-notify-report-json.msg.json

curl -k -i -H'Content-Type: application/json' -H'Accept: application/json' \
	http://fcm-app-server:4260/message \
	-d@${tmpdir}/ddwrt-notify-report-json.msg.json
	
rm -rf ${tmpdir}/ddwrt-notify-report-json.msg.json

#mutt -s "dd-wrt notify report" $emailto < $tmpdir/ddwrt_to_check

fi

rm $tmpdir/ddwrt_ftp_today
rm $tmpdir/ddwrt_ftp_today_tmp
rm $tmpdir/ddwrt_to_check