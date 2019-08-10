#!/bin/sh
###
#
INAME="carlsbad"
#
###
if [ ! "$DOCKER_ID_USER" ]; then
	echo "ERROR: \$DOCKER_ID_USER not defined."
	exit
fi
#
set -x
#
sudo docker images
#
sudo -E docker login
#
TAG="v0.0.1-SNAPSHOT"
#
sudo -E docker tag ${INAME}:${TAG} $DOCKER_ID_USER/${INAME}:${TAG}
#
sudo -E docker push $DOCKER_ID_USER/${INAME}:${TAG}
#
