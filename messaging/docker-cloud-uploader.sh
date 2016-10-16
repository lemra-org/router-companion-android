#!/bin/sh

export DOCKER_ID_USER="rm3l"
DCKR_REPO="rm3l/private-repo"
IMG_VERSION=${1:-0.0-0}

TEMP_WORK_DIR=`mktemp -d`
CURRENT_DIR=`pwd`

cp -r ./fcm-app-server ${TEMP_WORK_DIR}/
cp -r ./dd-wrt-notifier ${TEMP_WORK_DIR}/

echo ">>> Build and pushing Docker image for fcm-app-server: ${VERSION}"
cd ${TEMP_WORK_DIR}/fcm-app-server
docker build -t ddwrt-companion-fcm-app-server .
docker tag ddwrt-companion-fcm-app-server rm3l/private-repo:ddwrt-companion-fcm-app-server_${IMG_VERSION}
docker push rm3l/private-repo:ddwrt-companion-fcm-app-server_${IMG_VERSION}
echo "<<< ... done with Docker image for fcm-app-server: ${VERSION}"
echo

echo ">>> Build and pushing Docker image for dd-wrt-notifier: ${VERSION}"
cd ${TEMP_WORK_DIR}/dd-wrt-notifier
docker build -t ddwrt-companion-notifier .
docker tag ddwrt-companion-notifier rm3l/private-repo:ddwrt-companion-notifier_${IMG_VERSION}
docker push rm3l/private-repo:ddwrt-companion-notifier_${IMG_VERSION}
echo "<<< ... done with Docker image for dd-wrt-notifier: ${VERSION}"
echo

# Now update docker-cloud.yml, which should be versioned
echo "Now updating ${CURRENT_DIR}/docker-cloud.yml"
cd ${CURRENT_DIR}
sed -i -e "s/.*rm3l\/private-repo:ddwrt-companion-fcm-app-server.*/  image: rm3l\/private-repo:ddwrt-companion-fcm-app-server_${IMG_VERSION}/" docker-cloud.yml \
	|| exit 1
sed -i -e "s/.*rm3l\/private-repo:ddwrt-companion-notifier.*/  image: rm3l\/private-repo:ddwrt-companion-notifier_${IMG_VERSION}/" docker-cloud.yml \
	|| exit 1
rm -rf ${CURRENT_DIR}/docker-cloud.yml-e
echo "Done. Do not forget to redeploy Docker stacks as needed!"
echo
echo "=== Content ==="
echo
cat ${CURRENT_DIR}/docker-cloud.yml
echo
echo "==============="
echo
