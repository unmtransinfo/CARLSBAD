#!/bin/bash
###
# Instantiate and run containers.
# -dit = --detached --interactive --tty
###
set -e
#
if [ $(whoami) != "root" ]; then
	echo "${0} should be run as root or via sudo."
	exit
fi
#
cwd=$(pwd)
#
VTAG="latest"
#
###
# PostgreSQL db
INAME_DB="carlsbad_db"
#
DOCKERPORT_DB=5050
APPPORT_DB=5432
#
docker run -dit \
	--name "${INAME_DB}_container" \
	-p ${DOCKERPORT_DB}:${APPPORT_DB} \
	${INAME_DB}:${VTAG}
#
docker container logs "${INAME_DB}_container"
#
#
###
echo "Sleep while db server starting up..."
sleep 10
###
# Test db before proceeding.
docker exec "${INAME_DB}_container" sudo -u postgres psql -l
docker exec "${INAME_DB}_container" sudo -u postgres psql -d carlsbad -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
###
#
###
# Tomcat
INAME_UI="carlsbad_ui"
#
DOCKERPORT_UI=9091
APPPORT_UI=8080
#
docker run -dit \
	--name "${INAME_UI}_container" \
	-p ${DOCKERPORT_UI}:${APPPORT_UI} \
	${INAME_UI}:${VTAG}
#
docker container logs "${INAME_UI}_container"
#
###
docker container ls -a
#
printf "CARLSBAD PostgreSQL Endpoint: localhost:${DOCKERPORT_DB}\n" 
printf "Tomcat Web Application Manager: http://localhost:${DOCKERPORT_UI}/manager/html\n" 
printf "CARLSBAD-One Web Application: http://localhost:${DOCKERPORT_UI}/carlsbad/carlsbadone\n" 
#
${cwd}/sh/Go_DockerNetwork.sh
#
