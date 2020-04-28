#!/bin/sh
###
# https://docs.docker.com/network/
# https://docs.docker.com/network/bridge/
# Default network driver is "bridge".
# On a user-defined bridge network, containers can resolve each other
# by name (container ID) or alias (container name).
###
#
NETNAME="carlsbad"
#
INAME_DB="carlsbad_db"
INAME_UI="carlsbad_ui"
#
if [ $(whoami) != "root" ]; then
	echo "${0} should be run as root or via sudo."
	exit
fi
#
###
docker network create $NETNAME
#
docker network connect $NETNAME ${INAME_DB}_container
docker network connect $NETNAME ${INAME_UI}_container
#
docker network ls
#
docker exec ${INAME_UI}_container ping -c 1 ${INAME_DB}_container
#
docker exec -it ${INAME_UI}_container psql -h ${INAME_DB}_container -d carlsbad -U batman -c "SELECT name,version FROM dataset"
#
###
# If ok, app at: http://localhost:9091/carlsbad/carlsbadone
###
