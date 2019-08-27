#!/bin/bash
###
#
set -e
#
cwd=$(pwd)
#
sudo docker version
#
INAME="carlsbad_db"
TAG="v0.0.1-SNAPSHOT"
#
if [ ! -e "${cwd}/data" ]; then
	mkdir ${cwd}/data/
fi
#
cp /home/data/carlsbad/carlsbad.dump ${cwd}/data/
#
T0=$(date +%s)
#
###
# Build image from Dockerfile.
dockerfile="${cwd}/Dockerfile_Db"
sudo docker build -f ${dockerfile} -t ${INAME}:${TAG} .
#
printf "Elapsed time: %ds\n" "$[$(date +%s) - ${T0}]"
#
rm -f ${cwd}/data/carlsbad.dump
#
sudo docker images -a
#
