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
#
java -classpath $CLASSPATH edu.unm.health.biocomp.carlsbad.snake_app \
	-dbhost "$DBHOST" -dbname "$DBNAME" -dbusr "$DBUSR" -dbpw "$DBPW" \
	-v \
	-neighbortargets \
	-globaldegrees \
	-tids '304' \
	-sbs_id '4246930' \
	-sbs_id_type 'PubChem SID' \
	-o data/z_neigh.xgmml
#
