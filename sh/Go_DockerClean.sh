#!/bin/sh
###
#
INAME="carlsbad"
CNAME="${INAME}_container"
#
###
# Stop and clean up.
sudo docker stop ${CNAME}
sudo docker ps -a
sudo docker rm ${CNAME}
sudo docker rmi ${INAME}
#
sudo docker images
sudo docker container ls -a
#
