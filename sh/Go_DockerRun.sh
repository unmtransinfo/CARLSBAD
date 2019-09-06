#!/bin/bash
###
# Instantiate and run containers.
# -dit = --detached --interactive --tty
###
set -e
#
cwd=$(pwd)
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
#
###
echo "Sleep while db server starting up..."
sleep 10
###
# Test db before proceeding.
sudo docker exec "${INAME_DB}_container" sudo -u postgres psql -l
sudo docker exec "${INAME_DB}_container" sudo -u postgres psql -d carlsbad -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
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
${cwd}/sh/Go_DockerNetwork.sh
#
