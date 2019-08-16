#!/bin/sh
###
cwd=$(pwd)
#
sudo docker version
#
INAME="carlsbad_db"
TAG="v0.0.1-SNAPSHOT"
#
cp /home/data/carlsbad/carlsbad-pgdump.sql.gz ${cwd}/data/
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
rm -f ${cwd}/data/carlsbad-pgdump.sql.gz
#
sudo docker images -a
#
