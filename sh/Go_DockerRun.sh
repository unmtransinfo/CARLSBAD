#!/bin/sh
###
# Instantiate and run containers.
# -dit = --detached --interactive --tty
###
set -e
#
VTAG="v0.0.1-SNAPSHOT"
#
###
# PostgreSQL db
INAME_DB="carlsbad_db"
#
DOCKERPORT_DB=5050
APPPORT_DB=5432
#
sudo docker run -dit \
	--name "${INAME_DB}_container" \
	-p ${DOCKERPORT_DB}:${APPPORT_DB} \
	${INAME_DB}:${VTAG}
#
sudo docker container logs "${INAME_DB}_container"
#
###
# Can we test db before proceeding?
###
#
###
# Tomcat
INAME_UI="carlsbad_ui"
#
DOCKERPORT_UI=9091
APPPORT_UI=8080
#
sudo docker run -dit \
	--name "${INAME_UI}_container" \
	-p ${DOCKERPORT_UI}:${APPPORT_UI} \
	${INAME_UI}:${VTAG}
#
sudo docker container logs "${INAME_UI}_container"
#
###
sudo docker container ls -a
#
printf "CARLSBAD PostgreSQL Endpoint: localhost:%s\n" "${DOCKERPORT_UI}"
#
printf "Tomcat Web Application Manager: http://localhost:%s/manager/html\n" "${DOCKERPORT_UI}"
printf "CARLSBAD Web Application: http://localhost:%s/${INAME_UI}\n" "${DOCKERPORT_UI}"
#
