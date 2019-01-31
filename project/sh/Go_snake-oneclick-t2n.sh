#!/bin/sh
#
# Target2Network
#
LIBDIR=$HOME/src/java/lib
CLASSPATH=$LIBDIR/unm_biocomp_carlsbad.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_http.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_cytoscape.jar
LIBDIR=/home/app/lib
CLASSPATH=$CLASSPATH:$LIBDIR/opencsv-2.3.jar
CLASSPATH=$CLASSPATH:/home/app/ChemAxon/JChem/lib/jchem.jar
CLASSPATH=$CLASSPATH:/usr/share/java/berkeleydb.jar
#
. cb_current_db.sh
echo "DBNAME = \"$DBNAME\""
#
set -x
#
TID=35
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.carlsbad_utils_oneclick \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-tid $TID \
	-o data/tid${TID}_oneclick.xgmml \
	-minweight 30
#
