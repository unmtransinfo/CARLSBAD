#!/bin/sh
#
#
#LIBDIR=/home/app/lib
LIBDIR=$HOME/dev/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
CLASSPATH=$CLASSPATH:/home/app/ChemAxon/JChem/lib/jchem.jar
CLASSPATH=$CLASSPATH:/usr/share/java/berkeleydb.jar
#
. cb_current_db.sh
echo "DBNAME = \"$DBNAME\""
#
#
set -x
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-v \
	-globaldegrees \
	-cids '10159,10169,10170,10177,10204,10205,10208,10209,10210,10211,10212,10213,10220,10221,60873,60874,60875,60876,60877,60880,60882,60886,60889,60893,60895,194479,307607,307608,307611,307614,308679,308695,308710,308721,308725,308734,308736,342653,342654,439078,439087,439093,439094' \
	-o data/z.xgmml
#
