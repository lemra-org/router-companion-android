FROM gliderlabs/alpine:3.3

# Install lftp
RUN apk add --update lftp \
	&& rm -rf /var/cache/apk/*

RUN mkdir -p /code
COPY ddwrt-notify_v1.1.1b.sh /code/ddwrt-notify.sh
COPY root /var/spool/cron/crontabs/root

RUN chmod +x /code/ddwrt-notify.sh
# CMD [/usr/sbin/crond, -l, 2, -f]
