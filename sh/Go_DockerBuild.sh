#!/bin/sh
###
#
sudo docker version
#
INAME="carlsbad"
TAG="v0.0.1-SNAPSHOT"
#
###
# Build image from Dockerfile.
sudo docker build -t ${INAME}:${TAG} .
#
sudo docker images
#
