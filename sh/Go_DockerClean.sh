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
IIDS=$(sudo docker images -f dangling=true \
	|sed -e '1d' \
	|awk -e '{print $3}')
for iid in $IIDS ; do
	sudo docker rmi ${iid}
done
#
#
sudo docker container ls -a
#
