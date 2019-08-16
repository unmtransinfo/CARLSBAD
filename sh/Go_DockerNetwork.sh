#!/bin/sh
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
