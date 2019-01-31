#!/bin/sh
#
set -x
#
#LIBDIR=/home/app/lib
LIBDIR=$HOME/src/java/lib
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
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-describe \
	-v
#
# 'G-protein coupled receptor 3' search  -> TIDs:327,365,1012,1210
#
#java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
#	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
#	-v \
#	-globaldegrees \
#	-tids '159' \
#	-o data/z.xgmml
#
#
#java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
#	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
#	-v \
#	-globaldegrees \
#	-tids '327,365,1012,1210' \
#	-o data/z.xgmml
