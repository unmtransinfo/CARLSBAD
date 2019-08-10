#!/bin/sh
###
set -e
#
INAME="carlsbad"
CNAME="${INAME}_container"
#
DOCKERPORT=9091
APPPORT=8080
#
###
# Instantiate and run container.
# -dit = --detached --interactive --tty
sudo docker run -dit --name ${CNAME} -p ${DOCKERPORT}:${APPPORT} ${INAME}
#
sudo docker container logs ${CNAME}
#
set -x
sudo docker container ls -a
set +x
#
printf "Tomcat Web Application Manager: http://localhost:%s/manager/html\n" "${DOCKERPORT}"
printf "NAESE Web Application: http://localhost:%s/${INAME}\n" "${DOCKERPORT}"
#
