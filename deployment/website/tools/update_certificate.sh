sudo letsencrypt --agree-tos -a letsencrypt-s3front:auth \
--letsencrypt-s3front:auth-s3-bucket ddwrt-companion.rm3l.org \
--letsencrypt-s3front:auth-s3-region us-east-1 \
-i letsencrypt-s3front:installer \
--letsencrypt-s3front:installer-cf-distribution-id EWELYDDN87GF5 \
-d ddwrt-companion.rm3l.org
