#!/bin/bash
###
#
INAME="carlsbad_ui"
#
###
#
if [ $(whoami) != "root" ]; then
	echo "${0} should be run as root or via sudo."
	exit
fi
if [ ! "$DOCKER_ID_USER" ]; then
	echo "ERROR: \$DOCKER_ID_USER not defined."
	exit
fi
#
set -x
#
docker images
#
docker login
#
TAG="v1.0.0"
#
docker tag ${INAME}:latest $DOCKER_ID_USER/${INAME}:${TAG}
#
docker push $DOCKER_ID_USER/${INAME}:${TAG}
#
