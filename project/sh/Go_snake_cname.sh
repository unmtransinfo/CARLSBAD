#!/bin/sh
#
set -x
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
#
# 'G-protein coupled receptor 3' search  -> TIDs:327,365,1012,1210
#	-tids2xgmml '327,365,1012,1210' \
#	-cname_exact 'clozapine' \
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-v \
	-cname_exact 'cilomilast' \
	-o data/z.xgmml
#
