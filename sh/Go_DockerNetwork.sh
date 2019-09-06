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
###
sudo docker network create $NETNAME
#
sudo docker network connect $NETNAME ${INAME_DB}_container
sudo docker network connect $NETNAME ${INAME_UI}_container
#
sudo docker network ls
#
